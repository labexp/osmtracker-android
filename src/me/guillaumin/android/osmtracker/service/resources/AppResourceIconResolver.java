package me.guillaumin.android.osmtracker.service.resources;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

/**
 * {@link IconResolver} implementation which gets the icon in the
 * drawable/ resouces.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class AppResourceIconResolver implements IconResolver {

	/**
	 * Access to app resources.
	 */
	private Resources resources;
	
	/**
	 * Packages for accessing resources
	 */
	private String resourcesPackage;
	
	/**
	 * Name of the drawabe resource type.
	 */
	private static final String DRAWABLE_TYPE = "drawable";
	
	public AppResourceIconResolver(Resources r, String defPackage) {
		resources = r;
		resourcesPackage = defPackage;
	}
	
	@Override
	public Drawable getIcon(String key) {
		if (key != null) {
			int resId = resources.getIdentifier(key, DRAWABLE_TYPE, resourcesPackage);
			if(resId != 0) {
				return resources.getDrawable(resId);
			}
		}
		return null;
	}

}
