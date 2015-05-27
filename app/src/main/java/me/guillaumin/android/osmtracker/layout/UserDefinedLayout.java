package me.guillaumin.android.osmtracker.layout;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.service.resources.AppResourceIconResolver;
import me.guillaumin.android.osmtracker.service.resources.ExternalDirectoryIconResolver;
import me.guillaumin.android.osmtracker.util.UserDefinedLayoutReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Manages user-definable layout. User can define his own buttons
 * and pages of buttons in an XML file.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class UserDefinedLayout extends LinearLayout {

	@SuppressWarnings("unused")
	private static final String TAG = UserDefinedLayout.class.getSimpleName();
	
	/**
	 * Name of the root layout.
	 */
	private static final String ROOT_LAYOUT_NAME = "root";
	
	/**
	 * List of layouts (button pages) read from XML
	 */
	private HashMap<String, ViewGroup> layouts = new HashMap<String, ViewGroup>();

	/**
	 * Stack for keeping track of user navigation in pages
	 */
	private Stack<String> layoutStack = new Stack<String>();

	public UserDefinedLayout(Context ctx) {
		super(ctx);
	}
	
	public UserDefinedLayout(TrackLogger activity, long trackId, File xmlLayout) throws XmlPullParserException, IOException {
		super(activity);
		
		// Set default presentation parameters
		setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT, 1));
		
		UserDefinedLayoutReader udlr;
		XmlPullParser parser;
		if (xmlLayout == null) {
			// No user file, use default file
			parser = getResources().getXml(R.xml.default_buttons_layout);
			udlr = new UserDefinedLayoutReader(this, getContext(), activity, trackId, parser, new AppResourceIconResolver(getResources(), OSMTracker.class.getPackage().getName()));
		} else {
			// User file specified, parse it
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			parser = factory.newPullParser();
			parser.setInput(new FileReader(xmlLayout));
			udlr = new UserDefinedLayoutReader(this, getContext(), activity, trackId, parser, new ExternalDirectoryIconResolver(xmlLayout.getParentFile()));
		}
		
		layouts = udlr.parseLayout();
		
		if (layouts == null || layouts.isEmpty() || layouts.get(ROOT_LAYOUT_NAME) == null) {
			throw new IOException("Error in layout file. Is there a layout name '" + ROOT_LAYOUT_NAME + "' defined ?");
		}
		
		// XML file parsed, push the root layout on the view
		push(ROOT_LAYOUT_NAME);
		
	}
	
	/**
	 * Push the specified layout on top of the view
	 * @param s Name of layout to push.
	 */
	public void push(String s) {
		if (layouts.get(s) != null) {
			layoutStack.push(s);
			if (this.getChildCount() > 0) {
				this.removeAllViews();
			}
			this.addView(layouts.get(layoutStack.peek()));
		}
	}
	
	/**
	 * Pops the current top-layout, and set the view to
	 * the new top-layout.
	 * @return The name of the popped layout
	 */
	public String pop() {
		String out = layoutStack.pop();
		if (this.getChildCount() > 0) {
			this.removeAllViews();
		}
		this.addView(layouts.get(layoutStack.peek()));
		return out;
	}
	
	/**
	 * @return the number of layouts stacked
	 */
	public int getStackSize() {
		return layoutStack.size();
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		this.getChildAt(0).setEnabled(enabled);
	}


}
