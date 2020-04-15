package com.loyalar.fingerprinttestapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

import Logic.ViewLogic;

/**
 * Created by lj on 26-10-2017.
 */

public class AddFingerprintDialog {

    private View mView;
    private Activity mActivity;

    private LinearLayout mFingerprintDialogLogoLayout;
    private ImageSwitcher mFingerprintDialogLogoImageSwitcher;
    private TextSwitcher mFingerprintDialogLogoTextSwitcher;

    private MaterialDialog mMaterialDialog;


    /**
     * Required empty constructor to be able to new this class
     */
    public AddFingerprintDialog() {}

    /**
     * Where the magic happens
     */

    @SuppressLint("InflateParams")
    public MaterialDialog getNewDialog(Activity activity) {
        this.mActivity = activity;

        mView = LayoutInflater.from(mActivity).inflate(R.layout.fragment_add_fingerprint_dialog, null);
        mFingerprintDialogLogoLayout = (LinearLayout) mView.findViewById(R.id.fingerprint_dialog_logo_layout);
        mFingerprintDialogLogoImageSwitcher = (ImageSwitcher) mView.findViewById(R.id.fingerprint_dialog_logo_image_switcher);
        mFingerprintDialogLogoTextSwitcher = (TextSwitcher) mView.findViewById(R.id.fingerprint_dialog_logo_text_switcher);

        setDefaultFingerprintState();
        bindAnimationsAndEvents();

        mMaterialDialog = buildDialog();
        return mMaterialDialog;
    }

    private void bindAnimationsAndEvents() {
        Animation in = AnimationUtils.loadAnimation(mActivity, R.anim.fingerprint_image_fadein);
        Animation out = AnimationUtils.loadAnimation(mActivity, R.anim.fingerprint_image_fadeout);

        mFingerprintDialogLogoImageSwitcher.setInAnimation(in);
        mFingerprintDialogLogoImageSwitcher.setOutAnimation(out);
        mFingerprintDialogLogoTextSwitcher.setInAnimation(in);
        mFingerprintDialogLogoTextSwitcher.setOutAnimation(out);
    }

    private MaterialDialog buildDialog() {
        return new MaterialDialog.Builder(mActivity)
                .customView(mView, false)
                .title("Confirm fingerprint")
                .negativeText("Cancel")
                .theme(Theme.LIGHT)
//                .onNegative(new MaterialDialog.SingleButtonCallback() {
//                    @Override
//                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//
//                    }
//                })
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //TODO: Stop listening for fingerprint on dialog dismissal
                    }
                })
                .build();
    }

    public void setDefaultFingerprintState() {
        ViewLogic.setBackground(mFingerprintDialogLogoLayout, ContextCompat.getDrawable(mActivity, R.drawable.fingerprint_background_default));
        mFingerprintDialogLogoImageSwitcher.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_fingerprint_white));
        mFingerprintDialogLogoTextSwitcher.setText(mActivity.getString(R.string.touch_sensor));
    }

    public void setConfirmedFingerprintState() {
        ViewLogic.setBackground(mFingerprintDialogLogoLayout, ContextCompat.getDrawable(mActivity, R.drawable.fingerprint_background_success));
        mFingerprintDialogLogoImageSwitcher.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.checkmark));
        mFingerprintDialogLogoTextSwitcher.setText(mActivity.getString(R.string.fingerprint_recognized));
    }

    public void setErrorFingerprintState(String errorMessage) {
        ViewLogic.setBackground(mFingerprintDialogLogoLayout, ContextCompat.getDrawable(mActivity, R.drawable.fingerprint_background_error));
        mFingerprintDialogLogoImageSwitcher.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.exclamation_mark));
        mFingerprintDialogLogoTextSwitcher.setText(errorMessage);
    }
}
