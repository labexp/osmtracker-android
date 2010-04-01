package me.guillaumin.android.osmtracker.service.resources;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Implementation of {@link IconResolver} which reads icon
 * from an external directory.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class ExternalDirectoryIconResolver implements IconResolver {

	/**
	 * PNG file extension
	 */
	public static final String EXTENSION_PNG = "png";
	
	/**
	 * Base directory to read icon files.
	 */
	private File directory;
	
	/**
	 * File extension for icon files.
	 */
	private String extension;
	
	public ExternalDirectoryIconResolver(File baseDir, String iconExtension) {
		if (!baseDir.isDirectory()) {
			throw new IllegalArgumentException("baseDir must be a directory. " + baseDir + " is not.");
		}
		
		directory = baseDir;
	}
	
	@Override
	public Drawable getIcon(String key) {
		File iconFile = new File(directory, key + "." + extension);
		if (iconFile.exists() && iconFile.canRead()) {
			Bitmap iconBitmap = BitmapFactory.decodeFile(iconFile.getAbsolutePath());
			BitmapDrawable iconDrawable = new BitmapDrawable(iconBitmap);
			return iconDrawable;
		} else {
			return null;
		}
	}

}
