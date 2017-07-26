/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.test.orchestrator;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.test.internal.runner.tracker.AnalyticsBasedUsageTracker;
import android.support.test.internal.runner.tracker.UsageTrackerRegistry.AtslVersions;
import android.support.test.orchestrator.TestRunnable.RunFinishedListener;
import android.support.test.orchestrator.callback.OrchestratorCallback;
import android.support.test.orchestrator.listeners.OrchestrationListenerManager;
import android.support.test.orchestrator.listeners.OrchestrationResult;
import android.support.test.orchestrator.listeners.OrchestrationResultPrinter;
import android.support.test.orchestrator.listeners.OrchestrationXmlTestRunListener;
import android.support.test.runner.MonitoringInstrumentation;
import android.support.test.runner.UsageTrackerFacilitator;
import android.support.test.services.shellexecutor.ShellExecSharedConstants;
import android.text.TextUtils;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

/**
 * An {@link Instrumentation} that executes other instrumentations.
 *
 * <p>Takes parameters {@code targetPackage} and {@code targetInstrumentation}, and executes that
 * instrumentation with the same class parameters.
 *
 * <p>When run normally (i.e. without setting the isolated flag to false) the on device orchestrator
 * will handle test collection and execution. The target instrumentation is executed via shell
 * commmands on the device, with one shell command for test collection, followed by one shell comand
 * per test.
 *
 * <p>Each test runs in its own isolated process when its own instrumentation.
 *
 * <h3>Setup</h3>
 *
 * <p>The AndroidTestOrchestrator requires installation of a test services APK {@code
 * android.support.test.services}, and the stubapp APK {@code
 * android.support.test.orchestrator.stubapp}. The orchestrator is technically instrumenting the
 * stubapp, but it's real purpose is to issue commands to {@link AndroidJUnitRunner} or another
 * instrumentation, to run your tests.
 *
 * <h3>Typical usage</h3>
 *
 * <p>Whereas previously you might have called {@code am instrument -w
 * com.example.app/android.support.test.runner.AndroidJUnitRunner} you would now execute {@code
 * 'CLASSPATH=$(pm path com.google.android.apps.common.testing.services) app_process /
 * android.support.test.services.shellexecutor.ShellMain am instrument -w -e targetInstrumentation
 * com.example.app/android.support.test.runner.AndroidJUnitRunner
 * android.support.test.orchestrator/android.support.test.orchestrator.AndroidTestOrchestrator'}
 *
 * <p>Pass the {@code -e isolated false} flag if you wish the orchestrator to run all your tests in a
 * single process (as if you invoked the target instrumentation directly
 *
 * <p>All flags besides {@code isolated} and {@code targetInstrumentation} are passed by the
 * orchestrator to the target instrumentation.
 */
public final class AndroidTestOrchestrator extends MonitoringInstrumentation
    implements RunFinishedListener {

  private static final String TAG = "AndroidTestOrchestrator";
  // As defined in the AndroidManifest of the Orchestrator app.
  private static final String ORCHESTRATOR_SERVICE_LOCATION = "OrchestratorService";
  private static final String ORCHESTRATOR_SERVICE_ARGUMENT = "orchestratorService";

  private static final String CLASS_ARGUMENT = "class";

  private static final String TEST_COLLECTION_FILENAME = "testCollection.txt";
  private static final String TEST_RUN_FILENAME = "%s.txt";

  private static final Pattern FULLY_QUALIFIED_CLASS_AND_METHOD =
      Pattern.compile("[\\w\\.?]+#\\w+");

  private final List<String> mListOfTests = new ArrayList<>();

  private final OrchestrationXmlTestRunListener xmlTestRunListener =
      new OrchestrationXmlTestRunListener();
  private final OrchestrationResult.Builder resultBuilder = new OrchestrationResult.Builder();
  private final OrchestrationResultPrinter resultPrinter = new OrchestrationResultPrinter();
  private final OrchestrationListenerManager listenerManager =
      new OrchestrationListenerManager(this);

  private final ExecutorService mExecutorService;

  private static AndroidTestOrchestrator sInstance;
  private UsageTrackerFacilitator mUsageTrackerFacilitator;
  private Bundle mArguments;
  private String mTest;

  private Iterator<String> mTestIterator;

  public static AndroidTestOrchestrator getDefaultInstance() {
    return AndroidTestOrchestrator.sInstance;
  }

  public AndroidTestOrchestrator() {
    super();
    // We never want to execute multiple tests in parallel.
    mExecutorService =
        Executors.newSingleThreadExecutor(
            new ThreadFactory() {
              @Override
              public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setName(TAG); // Required for TikTok to not kill the thread.
                return t;
              }
            });
  }

  @Override
  public void onCreate(Bundle arguments) {
    sInstance = this;

    mArguments = arguments;
    mArguments.putString(ORCHESTRATOR_SERVICE_ARGUMENT, ORCHESTRATOR_SERVICE_LOCATION);


    super.onCreate(arguments);
    start();
  }

  @Override
  public void onStart() {
    super.onStart();
    try {
      registerUserTracker();
      connectOrchestratorService();
    } catch (RuntimeException e) {
      final String msg = "Fatal exception when setting up.";
      Log.e(TAG, msg, e);
      // Report the startup exception to instrumentation out.
      Bundle failureBundle = createResultBundle();
      failureBundle.putString(
          Instrumentation.REPORT_KEY_STREAMRESULT, msg + "\n" + Log.getStackTraceString(e));
      finish(Activity.RESULT_OK, failureBundle);
    }
  }

  // Note: We connect to the orchestrator service mostly so that we can verify that it is up and
  // running, but communication between AndroidTestOrchestrator and the remote instrumentation
  // is done via executing shell commands.
  private void connectOrchestratorService() {
    Intent intent = new Intent(getContext(), OrchestratorService.class);
    getContext().bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
  }

  private final ServiceConnection mConnection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
          Log.i(TAG, "AndroidTestOrchestrator has connected to the orchestration service");
          collectTests();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
          Log.e(
              TAG,
              "AndroidTestOrchestrator has prematurely disconnected from the orchestration service,"
                  + "run cancelled.");
          finish(Activity.RESULT_CANCELED, createResultBundle());
        }
      };

  private void collectTests() {
    String classArg = mArguments.getString(CLASS_ARGUMENT);
    // If we are given a single, fully qualified test then there's no point in test collection.
    // Proceed as if we had done collection and gotten the single argument.
    if (isSingleMethodTest(classArg)) {
      Log.i(TAG, String.format("Single test parameter %s, skipping test collection", classArg));
      mListOfTests.add(classArg);
      runFinished();
    } else {
      Log.i(TAG, String.format("Multiple test parameter %s, starting test collection", classArg));
      mExecutorService.execute(
          TestRunnable.testCollectionRunnable(
              getContext(),
              getSecret(mArguments),
              mArguments,
              getOutputStream(),
              AndroidTestOrchestrator.this));
    }
  }

  public static boolean isSingleMethodTest(String classArg) {
    if (TextUtils.isEmpty(classArg)) {
      return false;
    }
    return FULLY_QUALIFIED_CLASS_AND_METHOD.matcher(classArg).matches();
  }

  /** Invoked every time the TestRunnable finishes, including after test collection. */
  @Override
  public void runFinished() {
    // The first run complete will occur during test collection.
    if (null == mTest) {
      if (mListOfTests.isEmpty()) {
        finish(Activity.RESULT_CANCELED, createResultBundle());
        return;
      }

      mTestIterator = mListOfTests.listIterator();
      addListeners();
    } else {
      listenerManager.testProcessFinished(getOutputFile());
    }

    if (runsInIsolatedMode(mArguments)) {
      executeNextTest();
    } else {
      executeEntireTestSuite();
    }
  }

  private void executeEntireTestSuite() {
    if (null != mTest) {
      finish(Activity.RESULT_OK, createResultBundle());
      return;
    }

    // We don't actually need mTest to have any particular value,
    // just to indicate we've started execution.
    mTest = "";
    mExecutorService.execute(
        TestRunnable.legacyTestRunnable(
            getContext(), getSecret(mArguments), mArguments, getOutputStream(), this));
  }

  private void executeNextTest() {
    if (!mTestIterator.hasNext()) {
      finish(Activity.RESULT_OK, createResultBundle());
      return;
    }
    mTest = mTestIterator.next();
    mExecutorService.execute(
        TestRunnable.singleTestRunnable(
            getContext(), getSecret(mArguments), mArguments, getOutputStream(), this, mTest));
  }


  private OutputStream getOutputStream() {
    try {
      return getContext().openFileOutput(getOutputFile(), 0);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Could not open stream for output");
    }
  }

  private String getOutputFile() {
    if (null == mTest) {
      return TEST_COLLECTION_FILENAME;
    } else {
      return String.format(TEST_RUN_FILENAME, mTest);
    }
  }

  private void addListeners() {
    listenerManager.addListener(xmlTestRunListener);
    listenerManager.addListener(resultBuilder);
    listenerManager.addListener(resultPrinter);
    listenerManager.orchestrationRunStarted(mListOfTests.size());
  }

  /**
   * Returns a callback representing the OrchestratorCallback.aidl
   *
   * @return An instance of {@link OrchestratorCallback.Stub}
   */
  public OrchestratorCallback.Stub getCallBack() {
    return mCallback;
  }

  private final OrchestratorCallback.Stub mCallback =
      new OrchestratorCallback.Stub() {
        @Override
        public void addTest(String test) {
          mListOfTests.add(test);
        }

        @Override
        public void sendTestNotification(Bundle bundle) {
          listenerManager.handleNotification(bundle);
        }
      };

  private Bundle createResultBundle() {
    OutputStream stream = new ByteArrayOutputStream();
    PrintStream writer = new PrintStream(stream);
    Bundle bundle = new Bundle();

    try {
      resultBuilder.orchestrationRunFinished();
      resultPrinter.orchestrationRunFinished(writer, resultBuilder.build());
    } finally {
      writer.close();
    }

    bundle.putString(
        Instrumentation.REPORT_KEY_STREAMRESULT, String.format("\n%s", stream.toString()));
    return bundle;
  }

  @Override
  public void finish(int resultCode, Bundle results) {
    xmlTestRunListener.orchestrationRunFinished();
    try {
      mUsageTrackerFacilitator.trackUsage("AndroidTestOrchestrator", AtslVersions.RUNNER_VERSION);
      mUsageTrackerFacilitator.sendUsages();
    } catch (RuntimeException re) {
      Log.w(TAG, "Failed to send analytics.", re);
    } finally {
      try {
        super.finish(resultCode, results);
      } catch (SecurityException e) {
        Log.e(TAG, "Security exception thrown on shutdown", e);
        // On API Level 18 a security exception can be occasionally thrown when calling finish
        // with a result bundle taken from a remote message.  Recreating the result bundle and
        // retrying finish has a high probability of suppressing the flake.
        results = createResultBundle();
        super.finish(resultCode, results);
      }
    }
  }

  @Override
  public boolean onException(Object obj, Throwable e) {
    resultPrinter.reportProcessCrash(e);
    return super.onException(obj, e);
  }

  private static boolean runsInIsolatedMode(Bundle arguments) {
    // We run in isolated mode always, unless flag isolated is explicitly false.
    return !(Boolean.FALSE.toString().equalsIgnoreCase(
        arguments.getString(TestRunnable.ISOLATED_KEY)));
  }

  private static boolean shouldTrackUsage(Bundle arguments) {
    return !Boolean.parseBoolean(arguments.getString("disableAnalytics"));
  }

  private static String getSecret(Bundle arguments) {
    String secret = arguments.getString(ShellExecSharedConstants.BINDER_KEY);
    if (null == secret) {
      throw new IllegalArgumentException(
          "Cannot find secret for ShellExecutor binder published at "
              + ShellExecSharedConstants.BINDER_KEY);
    }
    return secret;
  }

  private void registerUserTracker() {
    mUsageTrackerFacilitator = new UsageTrackerFacilitator(shouldTrackUsage(mArguments));
    Context targetContext = getTargetContext();
    if (targetContext != null) {
      mUsageTrackerFacilitator.registerUsageTracker(
          new AnalyticsBasedUsageTracker.Builder(targetContext).buildIfPossible());
    }
  }
}
