/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.test.espresso;

import static android.support.test.internal.util.LogUtil.logDebug;
import static android.support.test.internal.util.LogUtil.logDebugWithProcess;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import android.support.test.espresso.remote.NoRemoteEspressoInstanceException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class to wait and handle interaction responses after one or more interaction completes
 * its execution.
 * <p>
 * The {@link #get()} method will block until the first successful interaction response is
 * received or all interactions finished executing.
 * <p>
 * In the case where all interactions fail, InteractionResultHandler will favor any
 * {@link EspressoException} over {@link NoRemoteEspressoInstanceException}. Since local Espresso
 * interaction exception is more useful for the the test author.
 */
@VisibleForTesting
class InteractionResultsHandler {
  private static final String TAG = "InteractionResultsHandl";
  private static final int LOCAL_OR_REMOTE_ERROR_PRIORITY = Integer.MAX_VALUE;

  private final CountDownLatch latch = new CountDownLatch(1);
  private final List<ListenableFuture<Void>> interactions;
  private final int totalInteractionsCount;

  private final AtomicInteger responseCount = new AtomicInteger();
  private volatile boolean isSuccessful;
  private volatile ExecutionException interactionException;

  InteractionResultsHandler(List<ListenableFuture<Void>> interactions) {
    checkNotNull(interactions, "interactions cannot be null!");
    checkState(!interactions.isEmpty(), "interactions cannot be empty!");
    this.interactions = interactions;
    this.totalInteractionsCount = interactions.size();
  }

  /**
   * Creates and returns a completion listener to run after a given {@link ListenableFuture} is
   * complete.
   *
   * @param future A {@link ListenableFuture} that accepts a completion listener.
   * @return the listener that runs when the computation is complete.
   */
  @VisibleForTesting
  Runnable listenerFor(final ListenableFuture<Void> future) {
    checkNotNull(future, "future cannot be null!");
    return new Runnable() {
      @Override
      public void run() {
        try {
          future.get();
          // Successful future completion, unblock
          logDebug(TAG, "SUCCESS in: " + future);
          isSuccessful = true;
          responseCount.incrementAndGet();
          latch.countDown();
        } catch (ExecutionException ee) {
          logDebug(TAG, "FAILURE ExecutionException in [%s]. \n%s",  future, ee);

          // Maintain interaction exception with the highest priority for future rethrow
          if (getPriority(interactionException) <= getPriority(ee)) {
            interactionException = ee;
          }

          responseCount.incrementAndGet();
          if (responseCount.get() == totalInteractionsCount
              || getPriority(interactionException) == LOCAL_OR_REMOTE_ERROR_PRIORITY) {
            // All responses received or assertion error occurred, unblock
            latch.countDown();
          }
        } catch (InterruptedException ie) {
          // The instrumentation thread is interrupted
          throw new IllegalStateException(
              "Interrupted while handling interaction response.", ie.getCause());
        } catch (CancellationException ce) {
          // Suppress cancellation exception
        }
      }
    };
  }

  /**
   * Returns an integer representing the priority of the given exception, where Integer.MAX_VALUE is
   * the highest priority and Integer.MIN_VALUE is the lowest.
   */
  private static int getPriority(ExecutionException e) {
    if (null == e) {
      return Integer.MIN_VALUE;
    } else if (e.getCause() instanceof NoRemoteEspressoInstanceException) {
      // Local interaction exception should take precedence over NoRemoteEspressoInstanceException
      return 0;
    } else if (e.getCause() instanceof NoActivityResumedException) {
      // Local or remote assertion errors should take precedence over NoActivityResumedException
      return 1;
    } else {
      // Local or remote assertion errors should take precedence over everything else
      return LOCAL_OR_REMOTE_ERROR_PRIORITY; // Integer.MAX_VALUE
    }
  }

  @VisibleForTesting
  void addListeners() {
    for (ListenableFuture<Void> interaction : interactions) {
      logDebugWithProcess(TAG, "addListeners: " + interaction);
      interaction.addListener(listenerFor(interaction), MoreExecutors.directExecutor());
    }
  }

  private void cancelInteractions() {
    for (ListenableFuture<Void> interaction : interactions) {
      logDebugWithProcess(TAG, "cancelInteractions: " + interaction);
      interaction.cancel(true);
    }
  }

  /**
   * Blocks until the first successful interaction response is received or all interactions finished
   * executing. Will propagate all exception back to the caller.
   *
   * @throws InterruptedException if interrupted while waiting for interaction response.
   */
  public void get() throws InterruptedException {
    addListeners();
    try {
      // Block until a response is received
      logDebugWithProcess(TAG, "Waiting for interaction result...");
      latch.await();
      if (!isSuccessful && interactionException != null) {
        logDebugWithProcess(TAG, "Un-successful interaction result received");
        Throwable cause = interactionException.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        } else if (cause instanceof java.lang.Error) {
          throw (java.lang.Error) cause;
        } else {
          throw new RuntimeException("Unknown interactionException:", cause);
        }
      }
      logDebugWithProcess(TAG, "Successful interaction result received");
    } finally {
      cancelInteractions();
    }
  }
}
