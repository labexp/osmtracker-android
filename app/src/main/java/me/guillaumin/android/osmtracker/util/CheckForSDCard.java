package me.guillaumin.android.osmtracker.util;
import android.os.Environment;

/**
 * Created by aton1698 on 23/11/17.
 */

public class CheckForSDCard {
    public boolean isSDCardPresent() {
        if (Environment.getExternalStorageState().equals(

                Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }
}
