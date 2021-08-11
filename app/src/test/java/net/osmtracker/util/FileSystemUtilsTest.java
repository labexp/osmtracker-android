package net.osmtracker.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;


public class FileSystemUtilsTest {

    @Test
    public void copyFile() throws IOException {
        String URLDestinationDirectory = new File("src/test/java/net/osmtracker/destinationDirectory").getAbsolutePath();
        String URLSourceFile = new File("src/test/java/net/osmtracker/sourceDirectory/test.txt").getAbsolutePath();

        File destinationDirectory = new File(URLDestinationDirectory);
        File sourceFile = new File(URLSourceFile);
        String targetFileName = "test.txt";
        Boolean result = FileSystemUtils.copyFile(destinationDirectory, sourceFile, targetFileName);
        assertTrue(result);
    }

    @Test
    public void copyDirectoryContents() {
        String URLDestinationDirectory = new File("src/test/java/net/osmtracker/destinationDirectory").getAbsolutePath();
        String URLSourceDirectory = new File("src/test/java/net/osmtracker/sourceDirectory").getAbsolutePath();

        File destinationDirectory = new File(URLDestinationDirectory);
        File sourceFile = new File(URLSourceDirectory);
        Boolean resultado = FileSystemUtils.copyDirectoryContents(destinationDirectory, sourceFile);
        assertTrue(resultado);
    }

    @Test
    public void delete() {
        String URLfileToDelete = new File("src/test/java/net/osmtracker/sourceDirectory/testDelete.txt").getAbsolutePath();
        File fileToDelete = new File(URLfileToDelete);

        Boolean result= FileSystemUtils.delete(fileToDelete, true);
        assertTrue(result);
    }
}