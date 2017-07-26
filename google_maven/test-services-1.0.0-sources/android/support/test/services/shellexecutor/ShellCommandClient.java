/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.test.services.shellexecutor;

import android.content.Context;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.support.test.services.shellexecutor.Command.Stub;
import android.support.test.services.speakeasy.SpeakEasyProtocol.FindResult;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for the ShellCommandExecutor service, to allow an instrumentation to executes a shell
 * command in a similar manner and environment as {@link @UiAutomation.executeShellCommand()} (i.e.
 * as either root or shell)
 *
 * <p>To use this method while running from the shell, you must prefix am instrument with: {@code
 * CLASSPATH=$(pm path com.google.android.apps.common.testing.services) app_process /
 * com.google.android.apps.common.testing.services.exec.ShellMain} to start the ShellCommandExecutor
 * service.
 */
public final class ShellCommandClient {

  /**
   * Indicates that the ShellCommandClient got an I/O error trying to reach the remote server.
   *
   * <p>Likely thrown if the ShellCommandExecutor was never started.
   */
  public static class ClientNotConnected extends Exception {};

  private static final String TAG = "ShellCommandClient";

  private ShellCommandClient() {
    // Should not be initialized
  }

  /**
   * Execute a command with elevated permissions.
   *
   * @param context A context
   * @param secret A string representing the speakeasy binder key
   * @param command The shell command to be executed.
   * @param parameters A @link{Map} parameters to be given to the shell command
   * @param shellEnv A @link {Map} of shell environment variables to be set
   * @param executeThroughShell If set to true, the command string will be executed through the
   *     shell with parameters given as additional shell arguments.
   * @throws IOException if cannot execute command on executor service.
   */
  public static synchronized InputStream execOnServer(
      Context context,
      String secret,
      String command,
      List<String> parameters,
      Map<String, String> shellEnv,
      boolean executeThroughShell)
      throws ClientNotConnected, IOException, RemoteException {

    if (TextUtils.isEmpty(command)) {
      throw new IllegalArgumentException("Null or empty command");
    }

    if (Looper.myLooper() == Looper.getMainLooper()) {
      throw new IllegalStateException(
          "Shell commands are blocking and should not be run from the main thread");
    }

    if (null == parameters) {
      parameters = new ArrayList<>();
    }

    if (null == shellEnv) {
      shellEnv = new HashMap<>();
    }

    FindResult result;

    try {
      result = BlockingFind.getResult(Looper.getMainLooper(), context, secret);
      if (!result.found) {
        Log.e(TAG, "Couldn't find a published binder");
        throw new ClientNotConnected();
      }
    } catch (InterruptedException e) {
      throw new ClientNotConnected();
    }

    ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();

    Command commandStub = Stub.asInterface(result.binder);
    commandStub.execute(command, parameters, shellEnv, executeThroughShell, pipe[1]);

    // Closes the write pipe client-side. Server-side to be closed by server.
    pipe[1].close();

    return new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]);
  }
}