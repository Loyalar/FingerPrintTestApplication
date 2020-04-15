package Logic;


import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.loyalar.fingerprinttestapplication.R;

import java.util.List;

/**
 * Created by lj on 27-03-2017.
 */

public class PermissionsLogic {

    /**
     * Requests the permissions
     *
     * @param activity            Base activity the permission should be requested from
     * @param snackbarRootView    Base view for the SnackBar to be shown if the permission request is cancelled or denied
     * @param snackbarText        Text of the SnackBar to be shown if the permission request is cancelled or denied
     * @param rationaleDialogText Text to show if any of the permissions have previously been denied
     * @param callback            Callback to be invoked when permissions are granted or SnackBar action is clicked
     * @param permissions         Permissions to be requested. If they have previously been granted, these will be not be requested
     */
    public static void requestPermissions(final Activity activity, final View snackbarRootView, final String snackbarText, final String rationaleDialogText, final OnPermissionRequestCallback callback, String... permissions) {
        Dexter.withActivity(activity)
                .withPermissions(permissions)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            callback.allPermissionsGranted();
                        } else {
                            final Snackbar snackbar = Snackbar.make(snackbarRootView, snackbarText, Snackbar.LENGTH_LONG);
                            snackbar.setAction(R.string.retry, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    callback.snackBarRetryClicked();
                                }
                            });
                            snackbar.show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, final PermissionToken token) {
                        new MaterialDialog.Builder(activity)
                                .title(R.string.permissions)
                                .content(rationaleDialogText)
                                .positiveText(R.string.okay)
                                .negativeText(R.string.cancel)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        token.continuePermissionRequest();
                                    }
                                })
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        token.cancelPermissionRequest();
                                    }
                                })
                                .show();
                    }
                })
                .check();
    }

    public interface OnPermissionRequestCallback {
        void allPermissionsGranted();

        void snackBarRetryClicked();
    }
}
