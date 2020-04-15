package Logic;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.v13.app.ActivityCompat;

/**
 * Created by lj on 24-10-2017.
 * Inspiration taken from https://www.androidauthority.com/how-to-add-fingerprint-authentication-to-your-android-app-747304/
 */

@TargetApi(Build.VERSION_CODES.M)
public class FingerprintHandler {

    // You should use the CancellationSignal method whenever your app can no longer process user input, for example when your app goes
    // into the background. If you don’t use this method, then other apps will be unable to access the touch sensor, including the lockscreen!//

    private CancellationSignal cancellationSignal;
    private Context mContext;
    private final FingerprintManager.AuthenticationCallback mCallback;

    public FingerprintHandler(Context context, FingerprintManager.AuthenticationCallback callback) {
        mContext = context;
        mCallback = callback;
    }

    //Implement the startAuth method, which is responsible for starting the fingerprint authentication process//
    public void startAuth(FingerprintManager manager, FingerprintManager.CryptoObject cryptoObject) {
        cancellationSignal = new CancellationSignal();
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.authenticate(cryptoObject, cancellationSignal, 0, mCallback, null);
    }

    public CancellationSignal getCancellationSignal() { return cancellationSignal; }
}