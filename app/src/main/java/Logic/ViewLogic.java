package Logic;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

/**
 * Created by lj on 26-10-2017.
 */

public class ViewLogic {

    /**
     * Sets the background of a view, regardless of API level.
     * Use this instead of using View.setBackground or View.setBackgroundDrawable
     *
     * @param view The view to set the background on
     * @param drawable The drawable to set the background to, can be null
     */
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void setBackground(View view, Drawable drawable) {
        final int sdk = android.os.Build.VERSION.SDK_INT;

        if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN)
            view.setBackgroundDrawable(drawable);
        else
            view.setBackground(drawable);
    }
}
