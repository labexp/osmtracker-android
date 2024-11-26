package net.osmtracker.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class FileSystemUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File sourceFile;
    private File destinationDirectory;
    private File sourceDirectory;
    private File targetFile;

    @Before
    public void setUp() throws IOException {
        // Create a temporary source file
        sourceFile = temporaryFolder.newFile("source.txt");

        // Create a temporary destination directory
        destinationDirectory = temporaryFolder.newFolder("destinationDir");

        // Create a temporary source directory
        sourceDirectory = temporaryFolder.newFolder("sourceDir");

        // Create a temporary target file
        targetFile = new File(destinationDirectory, "target.txt");
    }

    @After
    public void tearDown() {
        // No cleanup needed for temporary files and directories
    }

    @Test
    public void testCopyFileSuccess() {
        assertTrue(FileSystemUtils.copyFile(destinationDirectory, sourceFile, "target.txt"));
        assertTrue(targetFile.exists());
    }

    @Test
    public void testCopyFileFailure() {
        // Attempt to copy to a non-existent directory
        File nonExistentDirectory = new File(temporaryFolder.getRoot(), "nonExistentDir");
        assertFalse(FileSystemUtils.copyFile(nonExistentDirectory, sourceFile, "target.txt"));
    }

    @Test
    public void testCopyDirectoryContentsSuccess() throws IOException {
        // Create a file in the source directory
        File fileInSourceDirectory = new File(sourceDirectory, "fileInSource.txt");
        assertTrue(fileInSourceDirectory.createNewFile());

        assertTrue(FileSystemUtils.copyDirectoryContents(destinationDirectory, sourceDirectory));
        assertTrue(new File(destinationDirectory, "fileInSource.txt").exists());
    }

    @Test
    public void testCopyDirectoryContentsFailure() {
        // Attempt to copy from a non-existent directory
        File nonExistentDirectory = new File(temporaryFolder.getRoot(), "nonExistentDir");
        assertFalse(FileSystemUtils.copyDirectoryContents(destinationDirectory, nonExistentDirectory));
    }

    @Test
    public void testCopyDirectoryContentsDestinationNull() {
        // Attempt to copy with a null destination directory
        assertFalse(FileSystemUtils.copyDirectoryContents(null, sourceDirectory));
    }

    @Test
    public void testCopyDirectoryContentsSourceNull() {
        // Attempt to copy with a null source directory
        assertFalse(FileSystemUtils.copyDirectoryContents(destinationDirectory, null));
    }

    @Test
    public void testDeleteFileSuccess() {
        assertTrue(FileSystemUtils.delete(sourceFile, false));
        assertFalse(sourceFile.exists());
    }

    @Test
    public void testDeleteDirectorySuccess() {
        assertTrue(FileSystemUtils.delete(sourceDirectory, true));
        assertFalse(sourceDirectory.exists());
    }

    @Test
    public void testDeleteDirectoryFailure() throws IOException {
        // Create a nested directory structure
        File nestedDirectory = new File(sourceDirectory, "nestedDir");
        assertTrue(nestedDirectory.mkdir());
        File nestedFile = new File(nestedDirectory, "nestedFile.txt");
        assertTrue(nestedFile.createNewFile());

        // Attempt to delete the directory with recursion depth greater than 1
        assertFalse(FileSystemUtils.delete(sourceDirectory, true));
    }

    @Test
    public void testGetUniqueChildNameFor() throws IOException {
        String uniqueName = FileSystemUtils.getUniqueChildNameFor(destinationDirectory, "test", ".txt");
        assertEquals("test.txt", uniqueName);

        // Create a file with the same name to test the uniqueness
        File existingFile = new File(destinationDirectory, "test.txt");
        assertTrue(existingFile.createNewFile());

        uniqueName = FileSystemUtils.getUniqueChildNameFor(destinationDirectory, "test", ".txt");
        assertEquals("test1.txt", uniqueName);
    }
}
