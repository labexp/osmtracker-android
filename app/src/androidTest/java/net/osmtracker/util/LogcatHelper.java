package net.osmtracker.util;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class LogcatHelper {

    public static boolean checkLogForMessage(String tag, String message) {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d " + tag + ":I *:S");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            Pattern pattern = Pattern.compile(".*\\b" + Pattern.quote(message) + "\\b.*");

            while ((line = bufferedReader.readLine()) != null) {
                if (pattern.matcher(line).matches()) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e("LogcatHelper", "Error reading logcat output", e);
        }

        return false;
    }
}
