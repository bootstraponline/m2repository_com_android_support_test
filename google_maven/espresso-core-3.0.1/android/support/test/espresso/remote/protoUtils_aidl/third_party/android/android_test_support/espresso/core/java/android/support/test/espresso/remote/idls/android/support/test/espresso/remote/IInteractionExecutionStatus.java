// Generated with go/better-aidl
package android.support.test.espresso.remote;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.aidl.BaseProxy;
import com.google.android.aidl.BaseStub;
import com.google.android.aidl.Codecs;
import java.lang.Override;
import java.lang.String;

/**
 * Enables the ability to share interaction execution status cross process
 */
public interface IInteractionExecutionStatus extends IInterface {
  /**
   * Returns {@code false} if the given interaction was already executed on the
   * remote process
   */
  boolean canExecute() throws RemoteException;

  abstract class Stub extends BaseStub implements IInteractionExecutionStatus {
    private static final String DESCRIPTOR = "android.support.test.espresso.remote.IInteractionExecutionStatus";

    static final int TRANSACTION_canExecute = IBinder.FIRST_CALL_TRANSACTION + 0;

    public Stub() {
      attachInterface(this, DESCRIPTOR);
    }

    public static IInteractionExecutionStatus asInterface(IBinder obj) {
      if (obj == null) {
        return null;
      }
      IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (iin instanceof IInteractionExecutionStatus) {
        return (IInteractionExecutionStatus) iin;
      }
      return new Proxy(obj);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws
        RemoteException {
      if (routeToSuperOrEnforceInterface(code, data, reply, flags)) {
        return true;
      }
      if (code == TRANSACTION_canExecute) {
        boolean retval = canExecute();
        reply.writeNoException();
        Codecs.writeBoolean(reply, retval);
        return true;
      } else {
        return false;
      }
    }

    public static class Proxy extends BaseProxy implements IInteractionExecutionStatus {
      Proxy(IBinder remote) {
        super(remote, DESCRIPTOR);
      }

      @Override
      public boolean canExecute() throws RemoteException {
        Parcel data = obtainAndWriteInterfaceToken();
        Parcel reply = transactAndReadException(TRANSACTION_canExecute, data);
        boolean retval = Codecs.createBoolean(reply);
        reply.recycle();
        return retval;
      }
    }
  }
}
