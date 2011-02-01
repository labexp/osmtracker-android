package me.guillaumin.android.osmtracker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public final class FileSystemUtils {
	
	private static final String TAG = FileSystemUtils.class.getSimpleName();
	
	/**
	 * The maximum recursion depth we allow when deleting directories
	 */
	private static final int DELETE_MAX_RECURSION_DEPTH = 1;
	
	/**
	 * Copy copy file sourceFile to the directory destination directory
	 * @param destinationDirectory location where the file to be copied
	 * @param sourceFile the location of the file to copy
	 * @return true if the file was copied successfully, false otherwise
	 */	
	public static boolean copyFile(final File destinationDirectory, final File sourceFile) {
		boolean _return = false;
		
		if (null != destinationDirectory && null != sourceFile) {
			FileInputStream inputStream = null;
			FileOutputStream outputStream = null;
			byte[] dataBuffer = new byte[1024];
			File outputFile = new File(destinationDirectory.getAbsoluteFile()
					+ File.separator + sourceFile.getName()); 
			try {
				inputStream = new FileInputStream(sourceFile);
				outputStream = new FileOutputStream(outputFile);
				
				try {
					int bytesRead = inputStream.read(dataBuffer); 
					while (-1 != bytesRead) {
						outputStream.write(dataBuffer, 0, bytesRead);
						bytesRead = inputStream.read(dataBuffer); 
					}
					
					// No errors copying the file, look like we're good
					_return = true;
				} catch (IOException e) {
					Log.w(TAG,"IOException trying to write copy file [" 
							+ sourceFile.getAbsolutePath() + "] to [" 
							+ destinationDirectory.getAbsolutePath() +"]: [" 
							+ e.getMessage() + "]");
				}				
			} catch (FileNotFoundException e) {
				Log.w(TAG,"File not found exception trying to write copy file [" 
						+ sourceFile.getAbsolutePath() + "] to [" 
						+ destinationDirectory.getAbsolutePath() +"]: [" 
						+ e.getMessage() + "]");
			}
		}		
		return _return;
	}

	/**
	 * copies all files within a directory to another directory
	 * @param destinationDirectory the target directory
	 * @param sourceDirectory the source directory 
	 * @return true if all contents were copied successfully, false otherwise
	 */
	public static boolean copyDirectoryContents(File destinationDirectory, File sourceDirectory){
		if(destinationDirectory == null){
			Log.e(TAG, "Unable to copy: destinationDirectory is null");
			return false;
		}
		if(sourceDirectory == null){
			Log.e(TAG, "Unable to copy: sourceDirectory is null");
			return false;
		}
		
		// If the source and destination directories exist then copy the files 
		if (sourceDirectory.exists() && sourceDirectory.isDirectory() 
				&& destinationDirectory.exists() && destinationDirectory.isDirectory() 
				&& destinationDirectory.canWrite()) {

			List<String> failedCopy = null;
			for (File fileToCopy: sourceDirectory.listFiles()) {
				// Find and copy the file to the output directory
				Log.i(TAG,"Copying link file [" + fileToCopy.getName() + "] from ["
						+ sourceDirectory.getAbsolutePath() + "] to [" + destinationDirectory + "]");
				
				if (! copyFile(destinationDirectory, fileToCopy) ) {
					if (failedCopy == null) {
						failedCopy = new ArrayList<String>();
					}
					failedCopy.add(fileToCopy.getName());
				}
			}
			
			if (failedCopy != null) {
				// Report on the files that could not be copied
				Log.w(TAG,"Failed to copy the following files: ");
				for(String fileName: failedCopy) {
					Log.w(TAG,"\t [" + fileName + "]");
				}
			}else{
				return true;
			}
		} else {
			Log.w(TAG,"Unable to copy:\n\tInput dir Exists? [" + sourceDirectory.exists() 
					+ "]\n\tInput dir is directory? [" + sourceDirectory.isDirectory()
					+ "]\n\tOutput dir Exists? [" + destinationDirectory.exists()
					+ "]\n\tOutput dir is directory [" + destinationDirectory.isDirectory()
					+ "]\n\tOutput dir is writable [" + destinationDirectory.canWrite() 
					+ "]"); 
		}
		
		return false;
	}

	/**
	 * Delete a file/directory
	 * @param fileToDelete the file/directory to be deleted
	 * @param recursive if a directory needs to be deleted with all of it's content, set this to true. please note, that recursion is currently limited to a depth of 1 subfolder
	 * @return true if the file/directory was completely deleted, false otherwise
	 */
	public static boolean delete(File fileToDelete, boolean recursive) {
		return delete(fileToDelete, recursive, 0);
	}
	
	/**
	 * Delete a file/directory
	 * @param fileToDelete the file/directory to be deleted
	 * @param recursive if the deletion should be recursive
	 * @param recursionDepth takes care of the depth of recursion and aborts deletion if DELETE_MAX_RECURSION_DEPTH is reached
	 * @return
	 */
	private static boolean delete(File fileToDelete, boolean recursive, int recursionDepth){
		// if we're deeper than one recursion/subfolder, we'll cancel deletion
		if(recursionDepth > DELETE_MAX_RECURSION_DEPTH){
			Log.w(TAG, "DELETE_MAX_RECURSION_DEPTH ("+DELETE_MAX_RECURSION_DEPTH+") reached. Directory deletion aborted.");
			return false;
		}
		
		boolean deleted = false;
		
		//If it's a directory and we should delete it recursively, try to delete all childs
		if(fileToDelete.isDirectory() && recursive){
			for(File child:fileToDelete.listFiles()){
				if(!delete(child, true, recursionDepth+1)){
					Log.w(TAG, "deletion of ["+child+"] failed, aborting now...");
					return false;
				}
			}
		}
		
		deleted = fileToDelete.delete();
		boolean isDir = fileToDelete.isDirectory();
		if(deleted){
			Log.i(TAG, "deleted "+(isDir ? "directory" : "file")+" ["+fileToDelete+"]");
		}else{
			Log.w(TAG, "unable to delete "+(isDir ? "directory" : "file")+" ["+fileToDelete+"]");
		}

		return deleted;		
	}
	
}
