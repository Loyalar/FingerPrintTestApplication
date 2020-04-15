package com.loyalar.fingerprinttestapplication;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import Logic.FingerprintHandler;
import Logic.PermissionsLogic;

public class FingerprintChoiceActivity extends BaseActivity {

    private static String KEY_NAME = null;
    private RelativeLayout fingerprint_add_fingerprint_login_button;
    private Activity mActivity;
    private Toolbar mMyToolbar;

    private KeyguardManager keyguardManager;
    private FingerprintManager fingerprintManager;
    private Cipher cipher;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private FingerprintManager.CryptoObject cryptoObject;
    private MaterialDialog mFingerprintDialog;
    private AddFingerprintDialog mAddFingerprintDialogClass;

    private Handler setDefaultStateHandler;
    private Runnable setDefaultStateRunnable;

    private String mPinCode;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint_choice);

        KEY_NAME = getString(R.string.GOT_ETHICS_KEY_NAME);
        mActivity = this;
        fingerprint_add_fingerprint_login_button = (RelativeLayout) findViewById(R.id.fingerprint_add_fingerprint_login_button);
        mMyToolbar = (Toolbar) findViewById(R.id.my_toolbar);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);

        setSupportActionBar(mMyToolbar);
        mMyToolbar.setTitle("Fingerprint");

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(getString(R.string.INTENT_EXTRAS_PIN_CODE)))
            mPinCode = extras.getString(getString(R.string.INTENT_EXTRAS_PIN_CODE));

        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

            if (!keyguardManager.isKeyguardSecure()) {
                Toast.makeText(mActivity, "Keyguard has not been setup. Device is not secure.", Toast.LENGTH_LONG).show();
                fingerprint_add_fingerprint_login_button.setEnabled(false);
                return;
            }

            try {
                if (!fingerprintManager.isHardwareDetected()) {
                    Toast.makeText(mActivity, "Device does not have fingerprint hardware.", Toast.LENGTH_LONG).show();
                    fingerprint_add_fingerprint_login_button.setEnabled(false);
                    return;
                }
                if (!fingerprintManager.hasEnrolledFingerprints()) {
                    Toast.makeText(mActivity, "Device has fingerprint hardware but no fingerprints are enrolled.", Toast.LENGTH_LONG).show();
                    fingerprint_add_fingerprint_login_button.setEnabled(false);
                    return;
                }
            } catch (SecurityException securityException) {
                securityException.printStackTrace();
                fingerprint_add_fingerprint_login_button.setEnabled(false);
                return;
            }

            fingerprint_add_fingerprint_login_button.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onClick(View v) {
                    requestFingerprintPermission();
                }
            });

        } else {
            fingerprint_add_fingerprint_login_button.setEnabled(false);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void requestFingerprintPermission() {
        PermissionsLogic.requestPermissions(mActivity, findViewById(android.R.id.content), "Must grant fingerprint permission.", "We cannot authenticate with fingerprint if you deny the fingerprint permission.", new PermissionsLogic.OnPermissionRequestCallback() {
            @Override
            public void allPermissionsGranted() {
                showFingerprintDialog();
                try {
                    generateKey();
                } catch (FingerprintException e) {
                    e.printStackTrace();
                }
                startListeningForFingerprint();
            }

            @Override
            public void snackBarRetryClicked() {
                requestFingerprintPermission();
            }
        }, Manifest.permission.USE_FINGERPRINT);
    }

    private void showFingerprintDialog() {
        mAddFingerprintDialogClass = new AddFingerprintDialog();
        mFingerprintDialog = mAddFingerprintDialogClass.getNewDialog(mActivity);
        mFingerprintDialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startListeningForFingerprint() {
        setDefaultStateRunnable = new Runnable() {
            @Override
            public void run() {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAddFingerprintDialogClass.setDefaultFingerprintState();
                    }
                });
            }
        };

        setDefaultStateHandler = new Handler();

        if (initCipher(Cipher.DECRYPT_MODE)) {
            //If the cipher is initialized successfully, then create a CryptoObject instance
            cryptoObject = new FingerprintManager.CryptoObject(cipher);

            // Here, I’m referencing the FingerprintHandler class that we’ll create in the next section. This class will be responsible
            // for starting the authentication process (via the startAuth method) and processing the authentication process events
            FingerprintHandler helper = new FingerprintHandler(this, new FingerprintManager.AuthenticationCallback() {
                @Override
                // onAuthenticationError is called when a fatal error has occurred. It provides the error code and error message as its parameters
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    Log.e("GEDebug", "Fingerprint OnAuthenticationError invoked - ErrorCode = [" + errorCode + "], ErrorString = [" + errString + "]");

                    if (errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT)
                        Log.e("GEDebug", "FINGERPRINT_ERROR_LOCKOUT registered, show message to user to try again in 30 seconds.");
                    else if (errorCode == FingerprintManager.FINGERPRINT_ERROR_CANCELED)
                        Log.e("GEDebug", "FINGERPRINT_ERROR_CANCELED registered.");
                    else if (errorCode == FingerprintManager.FINGERPRINT_ERROR_TIMEOUT)
                        startListeningForFingerprint(); // restart the fingerprint listening since the previous listening timed out (~30s)

                    setDefaultStateHandler.removeCallbacks(setDefaultStateRunnable);
                    mAddFingerprintDialogClass.setErrorFingerprintState(errString.toString());
                    setDefaultStateHandler.postDelayed(setDefaultStateRunnable, 5000);
                }

                @Override
                // onAuthenticationHelp is called when a non-fatal error has occurred. This method provides additional information about the error,
                // so to provide the user with as much feedback as possible I’m incorporating this information into my toast
                public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                    Log.e("GEDebug", "Fingerprint OnAuthenticationHelp invoked - HelpCode = [" + helpCode + "], HelpString = [" + helpString + "]");
                    setDefaultStateHandler.removeCallbacks(setDefaultStateRunnable);
                    mAddFingerprintDialogClass.setErrorFingerprintState(helpString.toString());
                    setDefaultStateHandler.postDelayed(setDefaultStateRunnable, 5000);
                }

                @Override
                // onAuthenticationSucceeded is called when a fingerprint has been successfully matched to one of the fingerprints stored on the user’s device
                public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                    Log.e("GEDebug", "OnAuthenticationSucceeded invoked!");
                    setDefaultStateHandler.removeCallbacks(setDefaultStateRunnable);
                    mAddFingerprintDialogClass.setConfirmedFingerprintState();

                    FingerprintManager.CryptoObject resultCryptoObject = result.getCryptoObject();

//                    String encryptedPincode = encryptString(mPinCode, resultCryptoObject.getCipher());
//
//
//                    SharedPreferences.Editor editor = mSharedPreferences.edit();
//                    editor.putString(getString(R.string.SHARED_PREF_ENCRYPTED_PIN_CODE), encryptedPincode);
//                    editor.apply();


                    String encryptedPincodeFromSharedPrefs = mSharedPreferences.getString(getString(R.string.SHARED_PREF_ENCRYPTED_PIN_CODE), null);
                    String decryptedPincode = decryptString(encryptedPincodeFromSharedPrefs, resultCryptoObject.getCipher());

                    String s = "" + "";
                }

                // onAuthenticationFailed is called when the fingerprint doesn’t match with any of the fingerprints registered on the device
                @Override
                public void onAuthenticationFailed() {
                    Log.e("GEDebug", "OnAuthenticationFailed invoked");
                    setDefaultStateHandler.removeCallbacks(setDefaultStateRunnable);
                    mAddFingerprintDialogClass.setErrorFingerprintState(getString(R.string.fingerprint_not_recognized));
                    setDefaultStateHandler.postDelayed(setDefaultStateRunnable, 5000);
                }
            });
            helper.startAuth(fingerprintManager, cryptoObject);
        }
    }

    private String encryptString(String textToEncrypt, Cipher cipher) {
        String encryptedString = null;

        try {
            byte[] bytes = cipher.doFinal(textToEncrypt.getBytes());
            encryptedString = Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return encryptedString;
    }

    private String decryptString(String textToDecrypt, Cipher cipher) {
        byte[] bytes = Base64.decode(textToDecrypt, Base64.NO_WRAP);
        String finalText = null;

        try {
            finalText = new String(cipher.doFinal(bytes));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finalText;
    }

    private void stopListeningForFingerprint() {

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean initCipher(int mode) {
        try {
            //Obtain a cipher instance and configure it with the properties required for fingerprint authentication//
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
//            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME, null);


            if (mode == Cipher.ENCRYPT_MODE) {
                cipher.init(Cipher.ENCRYPT_MODE, key);
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putString(getString(R.string.SHARED_PREF_PIN_CODE_IV), Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP));
                editor.apply();
            } else {
                byte[] iv = Base64.decode(mSharedPreferences.getString(getString(R.string.SHARED_PREF_PIN_CODE_IV), ""), Base64.NO_WRAP);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(mode, key, ivSpec);
            }

            //Return true if the cipher has been initialized successfully//
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            //Return false if cipher initialization failed//
            return false;
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void generateKey() throws FingerprintException {
        try {
            // Obtain a reference to the Keystore using the standard Android keystore container identifier (“AndroidKeystore”)//
            keyStore = KeyStore.getInstance("AndroidKeyStore");

            //Generate the key//
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            //Initialize an empty KeyStore//
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_NAME))
                return;

            //Initialize the KeyGenerator//
            keyGenerator.init(new
                    //Specify the operation(s) this key can be used for//
                    KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    //Configure this key so that the user has to confirm their identity with a fingerprint each time they want to use it//
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            //Generate the key
            keyGenerator.generateKey();

        } catch (KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException
                | InvalidAlgorithmParameterException | CertificateException | IOException exc) {
            exc.printStackTrace();
            throw new FingerprintException(exc);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private class FingerprintException extends Exception {
        public FingerprintException(Exception e) {
            super(e);
        }
    }
}
