package net.osmtracker.Layouts;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Contains common and reusable static methods used for tests
 */
public class TestUtils {

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
        File newDir = new File(parentDir.getAbsolutePath()+"/"+newDirName);
        newDir.mkdir();
        return newDir;
    }

    /**
     * Create a file inside a directory
     */
    public static File createFile(File parentDir, String newFileName){
        File newFile = new File(parentDir.getAbsolutePath()+"/"+newFileName);
        return newFile;
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
        String storageDir =  Environment.getExternalStorageDirectory().getAbsolutePath();
        File appDirectory = new File(storageDir + "/osmtracker");
        appDirectory.mkdirs();
        return appDirectory;
    }

    /**
     * Get the app's layouts directory
     * - If it doesn't exist then should create it before returning
     */
    public static File getLayoutsDirectory(){
        String appDirectory = getAppDirectory().getAbsolutePath();
        File layoutsDirectory = new File(appDirectory + "/layouts");
        layoutsDirectory.mkdirs();
        return layoutsDirectory;
    }

    public static void checkToastIsShownWith(String text){
        onView(withText(text)).inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }
}
