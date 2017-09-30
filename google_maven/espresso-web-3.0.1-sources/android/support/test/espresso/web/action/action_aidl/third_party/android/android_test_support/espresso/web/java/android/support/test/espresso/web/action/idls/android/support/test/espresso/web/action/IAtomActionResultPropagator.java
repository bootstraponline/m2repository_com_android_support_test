// Generated with go/better-aidl
package android.support.test.espresso.web.action;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.test.espresso.web.model.Evaluation;
import com.google.android.aidl.BaseProxy;
import com.google.android.aidl.BaseStub;
import com.google.android.aidl.Codecs;
import java.lang.Override;
import java.lang.String;

/**
 * Enables the ability to propagate results back from a remote ViewAction running in a different
 * process.
 */
public interface IAtomActionResultPropagator extends IInterface {
  void setResult(Evaluation eval) throws RemoteException;

  void setError(Bundle errorMsg) throws RemoteException;

  abstract class Stub extends BaseStub implements IAtomActionResultPropagator {
    private static final String DESCRIPTOR = "android.support.test.espresso.web.action.IAtomActionResultPropagator";

    static final int TRANSACTION_setResult = IBinder.FIRST_CALL_TRANSACTION + 0;

    static final int TRANSACTION_setError = IBinder.FIRST_CALL_TRANSACTION + 1;

    public Stub() {
      attachInterface(this, DESCRIPTOR);
    }

    public static IAtomActionResultPropagator asInterface(IBinder obj) {
      if (obj == null) {
        return null;
      }
      IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (iin instanceof IAtomActionResultPropagator) {
        return (IAtomActionResultPropagator) iin;
      }
      return new Proxy(obj);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws
        RemoteException {
      if (routeToSuperOrEnforceInterface(code, data, reply, flags)) {
        return true;
      }
      switch (code) {
        case TRANSACTION_setResult: {
          Evaluation eval = Codecs.createParcelable(data, Evaluation.CREATOR);
          setResult(eval);
          break;
        }
        case TRANSACTION_setError: {
          Bundle errorMsg = Codecs.createParcelable(data, Bundle.CREATOR);
          setError(errorMsg);
          break;
        }
        default: {
          return false;
        }
      }
      reply.writeNoException();
      return true;
    }

    public static class Proxy extends BaseProxy implements IAtomActionResultPropagator {
      Proxy(IBinder remote) {
        super(remote, DESCRIPTOR);
      }

      @Override
      public void setResult(Evaluation eval) throws RemoteException {
        Parcel data = obtainAndWriteInterfaceToken();
        Codecs.writeParcelable(data, eval);
        transactAndReadExceptionReturnVoid(TRANSACTION_setResult, data);
      }

      @Override
      public void setError(Bundle errorMsg) throws RemoteException {
        Parcel data = obtainAndWriteInterfaceToken();
        Codecs.writeParcelable(data, errorMsg);
        transactAndReadExceptionReturnVoid(TRANSACTION_setError, data);
      }
    }
  }
}
