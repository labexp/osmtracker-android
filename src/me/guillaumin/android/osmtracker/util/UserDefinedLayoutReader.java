package me.guillaumin.android.osmtracker.util;

import java.io.IOException;
import java.util.HashMap;

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
import android.graphics.drawable.Drawable;
import android.util.Log;
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
	 * Constructor
	 * 
	 * @param udl
	 *            User defined layout
	 * @param c
	 *            Context for accessing resources
	 * @param tl
	 *            TrackLogger activity
	 * @param input
	 *            Parser for reading layout
	 */
	public UserDefinedLayoutReader(UserDefinedLayout udl, Context c, TrackLogger tl, XmlPullParser input, IconResolver ir) {
		parser = input;
		context = c;
		userDefinedLayout = udl;
		iconResolver = ir;
		
		// Initialize listeners which will be bound to buttons
		textNoteOnClickListener = new TextNoteOnClickListener();
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
				Log.v(TAG, "start tag <" + tagName + ">");
				if (XmlSchema.TAG_LAYOUT.equals(tagName)) {
					// <layout> tag has been encountered. Inflate this layout
					inflateLayout();
				}
				break;
			case XmlPullParser.END_TAG:
				String name = parser.getName();
				Log.v(TAG, "end tag <" + name + ">");
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
				Log.v(TAG, "start tag <" + name + ">");
				if (XmlSchema.TAG_ROW.equals(name)) {
					// <row> tag has been encountered, inflates it
					inflateRow(tblLayout);
				}
				break;
			case XmlPullParser.END_TAG:
				currentTagName = parser.getName();
				Log.v(TAG, "end tag <" + currentTagName + ">");
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
	 *            {@link TableLayout} to rattach the row to
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
				Log.v(TAG, "start tag <" + name + ">");
				if (XmlSchema.TAG_BUTTON.equals(name)) {
					// <button> tag has been encountered, inflates it.
					inflateButton(tblRow);
				}
				break;
			case XmlPullParser.END_TAG:
				currentTagName = parser.getName();
				Log.v(TAG, "end tag <" + currentTagName + ">");
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
	 *            The table row to attach the button to
	 */
	public void inflateButton(TableRow row) {
		Button button = new Button(row.getContext());
		button.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT,
				TableRow.LayoutParams.FILL_PARENT, 1));

		// TODO Do not instantiate a new listener each time, but reuse them

		String buttonType = parser.getAttributeValue(null, XmlSchema.ATTR_TYPE);
		if (XmlSchema.ATTR_VAL_PAGE.equals(buttonType)) {
			// Page button
			button.setText(parser.getAttributeValue(null, XmlSchema.ATTR_LABEL));
			Drawable icon = iconResolver.getIcon(parser.getAttributeValue(null, XmlSchema.ATTR_ICON));
			button.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
			button.setOnClickListener(new PageButtonOnClickListener(userDefinedLayout, parser.getAttributeValue(null,
					XmlSchema.ATTR_TARGETLAYOUT)));
		} else if (XmlSchema.ATTR_VAL_TAG.equals(buttonType)) {
			// Standard tag button
			button.setText(parser.getAttributeValue(null, XmlSchema.ATTR_LABEL));
			Drawable icon = iconResolver.getIcon(parser.getAttributeValue(null, XmlSchema.ATTR_ICON));
			button.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
			button.setOnClickListener(new TagButtonOnClickListener());
		} else if (XmlSchema.ATTR_VAL_VOICEREC.equals(buttonType)) {
			button.setText(context.getResources().getString(R.string.gpsstatus_record_voicerec));
			button.setCompoundDrawablesWithIntrinsicBounds(null, context.getResources().getDrawable(
					R.drawable.voice_32x32), null, null);
			button.setOnClickListener(voiceRecordOnClickListener);
		} else if (XmlSchema.ATTR_VAL_TEXTNOTE.equals(buttonType)) {
			// Text note button
			button.setText(context.getResources().getString(R.string.gpsstatus_record_textnote));
			button.setCompoundDrawablesWithIntrinsicBounds(null, context.getResources().getDrawable(
					R.drawable.text_32x32), null, null);
			button.setOnClickListener(textNoteOnClickListener);
		} else if (XmlSchema.ATTR_VAL_PICTURE.equals(buttonType)) {
			// Picture button
			button.setText(context.getResources().getString(R.string.gpsstatus_record_stillimage));
			button.setCompoundDrawablesWithIntrinsicBounds(null, context.getResources().getDrawable(
					R.drawable.camera_32x32), null, null);
			button.setOnClickListener(stillImageOnClickListener);
		}

		row.addView(button);

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

		public static final String ATTR_VAL_TAG = "tag";
		public static final String ATTR_VAL_PAGE = "page";
		public static final String ATTR_VAL_VOICEREC = "voicerec";
		public static final String ATTR_VAL_TEXTNOTE = "textnote";
		public static final String ATTR_VAL_PICTURE = "picture";
	}

}
