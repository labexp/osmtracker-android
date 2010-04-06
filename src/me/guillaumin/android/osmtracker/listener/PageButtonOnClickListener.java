package me.guillaumin.android.osmtracker.listener;

import me.guillaumin.android.osmtracker.layout.UserDefinedLayout;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Listener for page-type buttons. Provokes a navigation
 * to the target page.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class PageButtonOnClickListener implements OnClickListener {

	/**
	 * Name of the target layout (page) for this button
	 */
	private String targetLayoutName;
	
	/**
	 * Main layout
	 */
	private UserDefinedLayout rootLayout;

	public PageButtonOnClickListener(UserDefinedLayout layout, String target) {
		rootLayout = layout;
		targetLayoutName = target;
	}

	@Override
	public void onClick(View v) {
		if (targetLayoutName != null) {
			rootLayout.push(targetLayoutName);
		}
	}

}
