// Generated with go/better-aidl
package android.support.test.services.shellexecutor;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.google.android.aidl.BaseProxy;
import com.google.android.aidl.BaseStub;
import com.google.android.aidl.Codecs;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;
import java.util.Map;

public interface Command extends IInterface {
  /**
   * Parceling generic Lists requires an unchecked conversion from ArrayList to List<T>.Parceling generic Lists requires using the raw type Map. */
  @SuppressWarnings({
      "unchecked",
      "rawtypes"
  })
  void execute(String command, List<String> parameters, Map shellEnv, boolean executeThroughShell,
      ParcelFileDescriptor pfd) throws RemoteException;

  abstract class Stub extends BaseStub implements Command {
    private static final String DESCRIPTOR = "android.support.test.services.shellexecutor.Command";

    static final int TRANSACTION_execute = IBinder.FIRST_CALL_TRANSACTION + 0;

    public Stub() {
      attachInterface(this, DESCRIPTOR);
    }

    public static Command asInterface(IBinder obj) {
      if (obj == null) {
        return null;
      }
      IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (iin instanceof Command) {
        return (Command) iin;
      }
      return new Proxy(obj);
    }

    /**
     * Parceling generic Lists requires an unchecked conversion from ArrayList to List<T>.Parceling generic Lists requires using the raw type Map. */
    @Override
    @SuppressWarnings({
        "unchecked",
        "rawtypes"
    })
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws
        RemoteException {
      if (routeToSuperOrEnforceInterface(code, data, reply, flags)) {
        return true;
      }
      if (code == TRANSACTION_execute) {
        String command = data.readString();
        List<String> parameters = data.createStringArrayList();
        Map shellEnv = Codecs.createMap(data);
        boolean executeThroughShell = Codecs.createBoolean(data);
        ParcelFileDescriptor pfd = Codecs.createParcelable(data, ParcelFileDescriptor.CREATOR);
        execute(command, parameters, shellEnv, executeThroughShell, pfd);
        reply.writeNoException();
        return true;
      } else {
        return false;
      }
    }

    public static class Proxy extends BaseProxy implements Command {
      Proxy(IBinder remote) {
        super(remote, DESCRIPTOR);
      }

      /**
       * Parceling generic Lists requires an unchecked conversion from ArrayList to List<T>.Parceling generic Lists requires using the raw type Map. */
      @Override
      @SuppressWarnings({
          "unchecked",
          "rawtypes"
      })
      public void execute(String command, List<String> parameters, Map shellEnv,
          boolean executeThroughShell, ParcelFileDescriptor pfd) throws RemoteException {
        Parcel data = obtainAndWriteInterfaceToken();
        data.writeString(command);
        data.writeStringList(parameters);
        data.writeMap(shellEnv);
        Codecs.writeBoolean(data, executeThroughShell);
        Codecs.writeParcelable(data, pfd);
        transactAndReadExceptionReturnVoid(TRANSACTION_execute, data);
      }
    }
  }
}
