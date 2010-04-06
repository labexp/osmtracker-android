package me.guillaumin.android.osmtracker.service.resources;

import android.graphics.drawable.Drawable;

/**
 * Resolver for finding button icons.
 * 
 * @author Nicolas Guillaumin
 *
 */
public interface IconResolver {

	/**
	 * @param key Key for icon
	 * @return The {@link Drawable} for the key.
	 */
	public Drawable getIcon(String key);
	
}
