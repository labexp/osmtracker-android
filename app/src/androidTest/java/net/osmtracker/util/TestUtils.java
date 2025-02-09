package net.osmtracker.util;

import static net.osmtracker.util.LogcatHelper.checkLogForMessage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.platform.app.InstrumentationRegistry;

import net.osmtracker.OSMTracker;
import net.osmtracker.activity.Preferences;
import net.osmtracker.data.Mocks;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * Contains common and reusable static methods used for tests
 */
public class TestUtils {

    public static String TESTING_GITHUB_USER = "labexp";
    public static String TESTING_GITHUB_REPOSITORY = "osmtracker-android-layouts";
    public static String TESTING_GITHUB_BRANCH = "for_tests";

    /**
     * List all the files in a folder and return a list of the names
     */
    public static ArrayList<String> listFiles(File directory){
        ArrayList result = new ArrayList();
        for(File file : directory.listFiles()){
            result.add(file.getName());
        }
        return  result;
    }

    /**
     * Create a directory inside a directory and return the corresponding file
     */
    public static File createDirectory(File parentDir, String newDirName){
        File newDir = new File(parentDir.getAbsolutePath() + File.separator + newDirName);
        newDir.mkdir();
        return newDir;
    }

    /**
     * Create a file inside a directory
     */
    public static File createFile(File parentDir, String newFileName){
        File newFile = new File(parentDir.getAbsolutePath()+ File.separator + newFileName);
        return newFile;
    }

    /**
     * Install a mock layout in the phone
     *  - Creates the xml, the icons directory and some empty png files inside
     */
    public static void injectMockLayout(String layoutName, String ISOLangCode) {
        File layoutsDir = getLayoutsDirectory();

        // Create a mock layout file
        String layoutFileName = CustomLayoutsUtils.createFileName(layoutName, ISOLangCode);
        File newLayout = createFile(layoutsDir,layoutFileName);
        writeToFile(newLayout, Mocks.MOCK_LAYOUT_CONTENT);

        // Create the icons directory
        File iconsDir = createDirectory(layoutsDir, layoutName + Preferences.ICONS_DIR_SUFFIX);

        // And put some mock files inside
        int pngsToCreate = 4;
        File png;
        for (int i = 1; i <= pngsToCreate; i++) {
            png = createFile(iconsDir, i+".png");
            writeToFile(png, "foo");
        }
    }


    /**
     * Write content to a file
     */
    public static void writeToFile(File file, String content){
        try{
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
        }catch (Exception e){
            System.out.println("Error writing to file");
        }
    }

    /**
     * Get the app's storage directory
     * - If it doesn't exist then should create it before returning
     */
    public static File getAppDirectory(){
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String storageDir =  context.getExternalFilesDir(null).getAbsolutePath();
        File appDirectory = new File(storageDir + OSMTracker.Preferences.VAL_STORAGE_DIR);
        appDirectory.mkdirs();
        return appDirectory;
    }

    /**
     * Get the app's layouts directory
     * - If it doesn't exist then should create it before returning
     */
    public static File getLayoutsDirectory(){
        String appDirectory = getAppDirectory().getAbsolutePath();
        File layoutsDirectory = new File(appDirectory + File.separator + Preferences.LAYOUTS_SUBDIR);
        layoutsDirectory.mkdirs();
        return layoutsDirectory;
    }

    public static void checkToastIsShownWith(String text){
        // Espresso can not check Toast for android >= 11
        // https://github.com/android/android-test/issues/803
        //onView(withText(text)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
        checkLogForMessage("TOAST", text);
    }

    public static String getStringResource(int resourceId){
        return InstrumentationRegistry.getInstrumentation().getTargetContext().getString(resourceId);
    }

    public static void setGithubRepositorySettings(String user, String repo, String branch){
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(OSMTracker.Preferences.KEY_GITHUB_USERNAME, user);
        editor.putString(OSMTracker.Preferences.KEY_REPOSITORY_NAME, repo);
        editor.putString(OSMTracker.Preferences.KEY_BRANCH_NAME, branch);
        editor.commit();
    }

    public static void setLayoutsTestingRepository(){
        setGithubRepositorySettings(TESTING_GITHUB_USER,TESTING_GITHUB_REPOSITORY,TESTING_GITHUB_BRANCH);
    }

}
