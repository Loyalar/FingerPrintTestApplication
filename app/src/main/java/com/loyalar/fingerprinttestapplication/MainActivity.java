package com.loyalar.fingerprinttestapplication;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.afollestad.materialdialogs.MaterialDialog;

public class MainActivity extends AppCompatActivity {

    private Button btn_go_to_fingerprint_choice, btn_login;
    private AddFingerprintDialog mAddFingerprintDialogClass;
    private Activity mActivity;
    private MaterialDialog mFingerprintDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity = this;

        btn_go_to_fingerprint_choice = (Button) findViewById(R.id.btn_go_to_fingerprint_choice);
        btn_login = (Button) findViewById(R.id.btn_login);

        btn_go_to_fingerprint_choice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FingerprintChoiceActivity.class);
                intent.putExtra(getString(R.string.INTENT_EXTRAS_PIN_CODE), "5555");
                startActivity(intent);
            }
        });

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAddFingerprintDialogClass = new AddFingerprintDialog();
                mFingerprintDialog = mAddFingerprintDialogClass.getNewDialog(mActivity);
                mFingerprintDialog.show();


            }
        });

    }
}
