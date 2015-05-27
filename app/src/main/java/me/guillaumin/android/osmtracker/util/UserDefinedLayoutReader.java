package me.guillaumin.android.osmtracker.util;

import java.io.IOException;
import java.util.HashMap;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.layout.DisablableTableLayout;
import me.guillaumin.android.osmtracker.layout.UserDefinedLayout;
import me.guillaumin.android.osmtracker.listener.PageButtonOnClickListener;
import me.guillaumin.android.osmtracker.listener.StillImageOnClickListener;
import me.guillaumin.android.osmtracker.listener.TagButtonOnClickListener;
import me.guillaumin.android.osmtracker.listener.TextNoteOnClickListener;
import me.guillaumin.android.osmtracker.listener.VoiceRecOnClickListener;
import me.guillaumin.android.osmtracker.service.resources.IconResolver;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

/**
 * Reads an user defined layout, using a pull parser,
 * and instantiate corresponding objects (Layouts, Buttons)
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class UserDefinedLayoutReader {

	@SuppressWarnings("unused")
	private static final String TAG = UserDefinedLayoutReader.class.getSimpleName();

	/**
	 * Map containing parsed layouts
	 */
	private HashMap<String, ViewGroup> layouts = new HashMap<String, ViewGroup>();

	/**
	 * Source parser
	 */
	private XmlPullParser parser;

	/**
	 * Context for accessing resources
	 */
	private Context context;

	/**
	 * The user defined Layout
	 */
	private UserDefinedLayout userDefinedLayout;
	
	/**
	 * {@link IconResolver} to retrieve button icons.
	 */
	private IconResolver iconResolver;

	/**
	 * Listener bound to text note buttons
	 */
	private TextNoteOnClickListener textNoteOnClickListener;
	
	/**
	 * Listener bound to voice record buttons
	 */
	private VoiceRecOnClickListener voiceRecordOnClickListener;
	
	/**
	 * Lister bound to picture buttons
	 */
	private StillImageOnClickListener stillImageOnClickListener;
	
	/**
	 * {@link Resources} to retrieve String resources
	 */
	private Resources resources;
	
	/** 
	 * representing ScreenOrientation
	 * see {@link Configuration.orientation}
	 */
	private int orientation;
	
	private static final int ICON_POS_AUTO = 0;
	private static final int ICON_POS_TOP = 1;
	private static final int ICON_POS_RIGHT = 2;
	private static final int ICON_POS_BOTTOM = 3;
	private static final int ICON_POS_LEFT = 4;
	
	/**
	 * the icon position for the current layout
	 */
	private int currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_AUTO;

	/**
	 * Current track id
	 */
	private long currentTrackId;
	
	/**
	 * Constructor
	 * 
	 * @param udl
	 *				User defined layout
	 * @param c
	 *				Context for accessing resources
	 * @param tl
	 *				TrackLogger activity
	 * @param trackId
	 * 			  Current track id
	 * @param input
	 *				Parser for reading layout
	 * @param ir
	 * 			  Icon resolver to use to fetch icons 
	 */
	public UserDefinedLayoutReader(UserDefinedLayout udl, Context c, TrackLogger tl, long trackId, XmlPullParser input, IconResolver ir) {
		parser = input;
		context = c;
		resources = context.getResources();
		userDefinedLayout = udl;
		iconResolver = ir;
		currentTrackId = trackId;
		orientation = resources.getConfiguration().orientation;
		
		// Initialize listeners which will be bound to buttons
		textNoteOnClickListener = new TextNoteOnClickListener(tl);
		voiceRecordOnClickListener = new VoiceRecOnClickListener(tl);
		stillImageOnClickListener = new StillImageOnClickListener(tl);
	}

	/**
	 * Parses an XML layout
	 * 
	 * @return An HashMap of {@link ViewGroup} with layout name as key.
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	public HashMap<String, ViewGroup> parseLayout() throws XmlPullParserException, IOException {
		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();
				if (XmlSchema.TAG_LAYOUT.equals(tagName)) {
					// <layout> tag has been encountered. Inflate this layout
					inflateLayout();
				}
				break;
			case XmlPullParser.END_TAG:
				break;
			}
			eventType = parser.next();
			
		}

		return layouts;
	}

	/**
	 * Inflates a <layout> into a {@link TableLayout}
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private void inflateLayout() throws IOException, XmlPullParserException {
		String layoutName = parser.getAttributeValue(null, XmlSchema.ATTR_NAME);
		String layoutIconPosValue = parser.getAttributeValue(null, XmlSchema.ATTR_ICONPOS);

		// find out the correct icon position for this layout
		if(XmlSchema.ATTR_VAL_ICONPOS_TOP.equals(layoutIconPosValue)){
			// TOP position
			this.currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_TOP;
		} else if (XmlSchema.ATTR_VAL_ICONPOS_RIGHT.equals(layoutIconPosValue)){
			// RIGHT position
			this.currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_RIGHT;
		} else if (XmlSchema.ATTR_VAL_ICONPOS_BOTTOM.equals(layoutIconPosValue)){
			// BOTTOM position
			this.currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_BOTTOM;
		} else if (XmlSchema.ATTR_VAL_ICONPOS_LEFT.equals(layoutIconPosValue)){
			// LEFT position
			this.currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_LEFT;
		} else {
			// if no or an undefined value is given for the current layout
			// AUTO position depending on screen orientation
			this.currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_AUTO;
		}

		// Create a new table layout and set default parameters
		DisablableTableLayout tblLayout = new DisablableTableLayout(context);
		tblLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.FILL_PARENT, 1));

		String currentTagName = null;
		while (!XmlSchema.TAG_LAYOUT.equals(currentTagName)) {
			int eventType = parser.next();
			switch (eventType) {
			case XmlPullParser.START_TAG:
				String name = parser.getName();
				if (XmlSchema.TAG_ROW.equals(name)) {
					// <row> tag has been encountered, inflates it
					inflateRow(tblLayout);
				}
				break;
			case XmlPullParser.END_TAG:
				currentTagName = parser.getName();
				break;
			}
		}

		// Add the new inflated layout to the list
		layouts.put(layoutName, tblLayout);
	}

	/**
	 * Inflates a <row> into a {@link TableRow}
	 * 
	 * @param layout
	 *				{@link TableLayout} to rattach the row to
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private void inflateRow(TableLayout layout) throws XmlPullParserException, IOException {
		TableRow tblRow = new TableRow(layout.getContext());
		tblRow.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT,
				TableLayout.LayoutParams.FILL_PARENT, 1));

		String currentTagName = null;
		// int eventType = parser.next();
		while (!XmlSchema.TAG_ROW.equals(currentTagName)) {
			int eventType = parser.next();
			switch (eventType) {
			case XmlPullParser.START_TAG:
				String name = parser.getName();
				if (XmlSchema.TAG_BUTTON.equals(name)) {
					// <button> tag has been encountered, inflates it.
					inflateButton(tblRow);
				}
				break;
			case XmlPullParser.END_TAG:
				currentTagName = parser.getName();
				break;
			}

		}

		// Add the inflated table row to the current layout
		layout.addView(tblRow);
	}

	/**
	 * Inflates a <button>
	 * 
	 * @param row
	 *				The table row to attach the button to
	 */
	public void inflateButton(TableRow row) {
		Button button = new Button(row.getContext());
		button.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT,
				TableRow.LayoutParams.FILL_PARENT, 1));

		// TODO Use kind of ButtonFactory here

		String buttonType = parser.getAttributeValue(null, XmlSchema.ATTR_TYPE);
		Drawable buttonIcon = null;
		if (XmlSchema.ATTR_VAL_PAGE.equals(buttonType)) {
			// Page button
			button.setText(findLabel(parser.getAttributeValue(null, XmlSchema.ATTR_LABEL), resources));				
			buttonIcon = iconResolver.getIcon(parser.getAttributeValue(null, XmlSchema.ATTR_ICON));
			button.setOnClickListener(new PageButtonOnClickListener(userDefinedLayout, parser.getAttributeValue(null,
					XmlSchema.ATTR_TARGETLAYOUT)));
		} else if (XmlSchema.ATTR_VAL_TAG.equals(buttonType)) {
			// Standard tag button
			button.setText(findLabel(parser.getAttributeValue(null, XmlSchema.ATTR_LABEL), resources));			
			buttonIcon = iconResolver.getIcon(parser.getAttributeValue(null, XmlSchema.ATTR_ICON));
			button.setOnClickListener(new TagButtonOnClickListener(currentTrackId));
		} else if (XmlSchema.ATTR_VAL_VOICEREC.equals(buttonType)) {
			// Voice record button
			button.setText(resources.getString(R.string.gpsstatus_record_voicerec));
			buttonIcon = resources.getDrawable(R.drawable.voice_32x32);
			button.setOnClickListener(voiceRecordOnClickListener);
		} else if (XmlSchema.ATTR_VAL_TEXTNOTE.equals(buttonType)) {
			// Text note button
			button.setText(resources.getString(R.string.gpsstatus_record_textnote));
			buttonIcon = resources.getDrawable(R.drawable.text_32x32);
			button.setOnClickListener(textNoteOnClickListener);
		} else if (XmlSchema.ATTR_VAL_PICTURE.equals(buttonType)) {
			// Picture button
			button.setText(resources.getString(R.string.gpsstatus_record_stillimage));
			buttonIcon = resources.getDrawable(R.drawable.camera_32x32);
			button.setOnClickListener(stillImageOnClickListener);
		}
		
		// Where to draw the button's icon (depending on the current layout)
		switch(this.currentLayoutIconPos){
		case UserDefinedLayoutReader.ICON_POS_TOP:
			// TOP position
			button.setCompoundDrawablesWithIntrinsicBounds(null, buttonIcon, null, null);
			break;
		case UserDefinedLayoutReader.ICON_POS_RIGHT:
			// RIGHT position
			button.setCompoundDrawablesWithIntrinsicBounds(null, null, buttonIcon, null);
			break;
		case UserDefinedLayoutReader.ICON_POS_BOTTOM:
			// BOTTOM position
			button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, buttonIcon);
			break;
		case UserDefinedLayoutReader.ICON_POS_LEFT:
			// LEFT position
			button.setCompoundDrawablesWithIntrinsicBounds(buttonIcon, null, null, null);
			break;
		case UserDefinedLayoutReader.ICON_POS_AUTO:
		default:
			// if no or an undefined value is given for the current layout
			// AUTO position depending on screen orientation
			if(orientation == Configuration.ORIENTATION_LANDSCAPE){
				// in landscape mode draw icon to the LEFT
				button.setCompoundDrawablesWithIntrinsicBounds(buttonIcon, null, null,null);
			}else{
				// in portrait mode draw icon to the TOP
				button.setCompoundDrawablesWithIntrinsicBounds(null, buttonIcon, null, null);
			}
			break;
		}


		row.addView(button);
	}
	
	/**
	 * Finds a label if it's a reference to an internal resource (@string/label) 
	 * @param text Resource reference or plain label
	 * @param r {@link Resources} to lookup from
	 * @return Plain label, or corresponding text extracted from {@link Resources}
	 */
	private String findLabel(String text, Resources r) {
		if (text != null) {
			if (text.startsWith("@")) {
				// Check if it's a resource identifier
				int resId = resources.getIdentifier(text.replace("@", ""), null, OSMTracker.PACKAGE_NAME);
				if (resId != 0) {
					return resources.getString(resId);
				}
			}
		}
		return text;
	}

	/**
	 * XML Schema
	 */
	private static final class XmlSchema {
		public static final String TAG_LAYOUT = "layout";
		public static final String TAG_ROW = "row";
		public static final String TAG_BUTTON = "button";

		public static final String ATTR_NAME = "name";
		public static final String ATTR_TYPE = "type";
		public static final String ATTR_LABEL = "label";
		public static final String ATTR_TARGETLAYOUT = "targetlayout";
		public static final String ATTR_ICON = "icon";
		public static final String ATTR_ICONPOS = "iconpos";

		public static final String ATTR_VAL_TAG = "tag";
		public static final String ATTR_VAL_PAGE = "page";
		public static final String ATTR_VAL_VOICEREC = "voicerec";
		public static final String ATTR_VAL_TEXTNOTE = "textnote";
		public static final String ATTR_VAL_PICTURE = "picture";
		
		public static final String ATTR_VAL_ICONPOS_TOP = "top";
		public static final String ATTR_VAL_ICONPOS_RIGHT = "right";
		public static final String ATTR_VAL_ICONPOS_BOTTOM = "bottom";
		public static final String ATTR_VAL_ICONPOS_LEFT = "left";
	}

}
