package me.guillaumin.android.osmtracker.listener;

import java.util.HashMap;
import java.util.Stack;

import me.guillaumin.android.osmtracker.layout.UserDefinedLayout;

import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

/**
 * Listener for page-type buttons
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
