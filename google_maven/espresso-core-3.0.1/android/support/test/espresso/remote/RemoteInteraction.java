/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package android.support.test.espresso.remote;

import android.os.IBinder;
import android.view.View;
import android.support.test.espresso.Root;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewAssertion;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matcher;

/**
 * Facilitates communication between other Espresso instance that may be
 * running in different processes.
 */
public interface RemoteInteraction {
  static final String BUNDLE_EXECUTION_STATUS = "executionStatus";

  ListeningExecutorService REMOTE_EXECUTOR =
      MoreExecutors.listeningDecorator(
          new ThreadPoolExecutor(
              0 /*corePoolSize*/,
              5 /*maximumPoolSize*/,
              10,
              TimeUnit.SECONDS,
              new LinkedBlockingQueue<Runnable>(),
              new ThreadFactoryBuilder().setNameFormat("Espresso Remote #%d").build()));

  /** @return {@code true} if the current Espresso instance running in a remote process. */
  boolean isRemoteProcess();

  /**
   * Attempts to run Espresso check interaction on a remote process.
   *
   * <p>If there are no remote Espressos currently running in a timely manner the interaction will
   * not be executed and a {@link NoRemoteEspressoInstanceException} will be thrown.
   *
   * @param rootMatcher the root matcher to use.
   * @param viewMatcher the view matcher to use.
   * @param iBinders a list of binders to pass along to the remote process instance
   * @param viewAssert the assertion to check.
   * @return a {@link ListenableFuture} representing pending completion of the task.
   */
  ListenableFuture<Void> runCheckRemotely(
      Matcher<Root> rootMatcher,
      Matcher<View> viewMatcher,
      Map<String, IBinder> iBinders,
      ViewAssertion viewAssert);

  /**
   * Attempts to run Espresso perform interaction on a remote process.
   *
   * <p>If there no remote Espresso currently running in a timely manner the interaction will not be
   * executed and a {@link NoRemoteEspressoInstanceException} will be thrown.
   *
   * @param rootMatcher the root matcher to use.
   * @param viewMatcher the view matcher to use.
   * @param viewActions one or more actions to execute.
   * @param iBinders a list of binders to pass along to the remote process instance
   * @return a {@link ListenableFuture} representing pending completion of the task.
   */
  ListenableFuture<Void> runPerformRemotely(
      Matcher<Root> rootMatcher,
      Matcher<View> viewMatcher,
      Map<String, IBinder> iBinders,
      ViewAction... viewActions);
}
