package net.osmtracker.gpx;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import net.osmtracker.db.DataHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface LongConsumer {
	void accept(long l);
}

interface DoubleConsumer {
	void accept(double d);
}

class InputStreamWithPosition extends FilterInputStream {
	private long position=0;
	private final LongConsumer report;
	
	public InputStreamWithPosition(InputStream in,
				       LongConsumer report) {
		super(in);
		this.report = report;
	}
	
	public boolean markSupported() {
		return false;
	}

	private void advancePosition(long delta) {
		if(delta == -1)
			return;
		position += delta;
		report.accept(position);
	}
	
	public int read() throws IOException {
		int ret = super.read();
		if(ret != -1)
			advancePosition(1);
		return ret;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		int ret = super.read(b, off, len);
		advancePosition(ret);
		return ret;
	}

	public long skip(long n) throws IOException {
		long ret = super.skip(n);
		advancePosition(ret);
		return ret;
	}
	
	public long getPosition() {
		return position;
	}
}

/**
 * Class to import a route
 */
public class ImportRoute {
	private static final String TAG = ImportRoute.class.getSimpleName();

	private final long trackId;
	private final DataHelper dataHelper;
	private final Activity context;

	public ImportRoute(Activity context, long trackId) {
		this.context = context;
		this.trackId = trackId;
		dataHelper = new DataHelper(context);
	}

	private void skip(XmlPullParser parser)
		throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}

	// For getting the text of a tag.
	private String readText(XmlPullParser parser)
		throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

	private String readString(XmlPullParser parser, String tag)
		throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, tag);
		String result = readText(parser);
		parser.require(XmlPullParser.END_TAG, null, tag);
		return result;
	}

	
	private void readDouble(XmlPullParser parser, String tag,
							DoubleConsumer setter)
		throws XmlPullParserException, IOException {
		String str = readString(parser,tag);
		storeDouble(str, setter);
	}

	private void storeDouble(String str, DoubleConsumer setter) {
		if(str == null || "".equals(str))
			return;
		try {
			setter.accept(Double.parseDouble(str));
		} catch(NumberFormatException e) {
			Log.v(TAG, "Bad double :\""+str+"\"");
		}
	}

	private interface FloatConsumer {
		void accept(float f);
	}

	private void readFloat(XmlPullParser parser, String tag,
						   FloatConsumer setter)
			throws XmlPullParserException, IOException {
		String str = readString(parser,tag);
		if(str == null || "".equals(str))
			return;
		try {
			setter.accept(Float.parseFloat(str));
		} catch(NumberFormatException e) {
			Log.v(TAG, "Bad float "+tag+" :\""+str+"\"");
		}
	}

	private float readFloat(XmlPullParser parser, String tag,
							float dflt)
			throws XmlPullParserException, IOException {
		String str = readString(parser,tag);
		if(str == null || "".equals(str))
			return dflt;
		try {
			return Float.parseFloat(str);
		} catch(NumberFormatException e) {
			Log.v(TAG, "Bad float "+tag+" :\""+str+"\"");
			return dflt;
		}
	}

	private int readInt(XmlPullParser parser, String tag)
		throws XmlPullParserException, IOException {
		String str = readString(parser,tag);
		if(str == null || "".equals(str))
			return 0;
		try {
			return Integer.parseInt(str);
		} catch(NumberFormatException e) {
			Log.v(TAG, "Bad integer "+tag+" :\""+str+"\"");
			return 0;
		}
	}

	private static final DateFormat dfz = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz");
	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	private static final Pattern p =
		Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+)(\\.\\d+)?(Z|(\\+\\d{2}):?(\\d{2})?)?");
	//

	private static String or(String first, String second) {
		return (first != null) ? first : second;
	}
	
	private void readTime(XmlPullParser parser, String tag,
			      LongConsumer setter)
		throws XmlPullParserException, IOException {
		String str = readString(parser,tag);
		if(str == null || "".equals(str))
			return;
		
		Matcher m = p.matcher(str);
		if(!m.find()) {
			Log.v(TAG, "Bad date string \""+str+"\"");
		}

		StringBuilder sb = new StringBuilder();

		sb.append(m.group(1)); // date and time
		sb.append(or(m.group(2),".000")); // milliseconds

		String tz = m.group(3);
		if("Z".equals(tz))
			sb.append("+0000");
		else if(tz != null) {
			sb.append(m.group(4));
			sb.append(or(m.group(5),"00"));
		}
		String dstr = sb.toString();
		try {
			Date d = (tz != null) ? dfz.parse(dstr) : df.parse(dstr);
			if(d!=null)
				setter.accept(d.getTime());
		} catch(ParseException e) {
			Log.v(TAG, "Bad date string \""+str+"\" => \""+
			      dstr+"\"");
		}
	}

	private void readDoubleFromAttribute(XmlPullParser parser,
					     String attribute,
					     DoubleConsumer setter) {
		String str = parser.getAttributeValue(null, attribute);
		if(str == null || "".equals(str))
			return;
		try {
			setter.accept(Double.parseDouble(str));
		} catch(NumberFormatException e) {
			Log.v(TAG, "Bad double attribute "+attribute+
			      " :\""+str+"\"");
		}
	}

	// GPX
	
	/**
	 * Reads a track point
	 * @param parser the parser
	 * @param tag the tag
	 * @param newSegment true if this point starts a new segment
	 */
	private void readPoint(XmlPullParser parser,
			       String tag,
			       boolean newSegment,
			       boolean isWaypoint)
		throws XmlPullParserException, IOException {
		int depth=1;

		float azimuth=-1.0f;
		int compassAccuracy=0;
		float pressure=0.0f;

		String name=null;
		
		/* Ensure we have correct tag */
		parser.require(XmlPullParser.START_TAG, null, tag);

		Location location = new Location("import");
		if(isWaypoint)
			location.setExtras(new Bundle());
		readDoubleFromAttribute(parser, "lat", location::setLatitude);
		readDoubleFromAttribute(parser, "lon", location::setLongitude);
		
		/* Process attributes */
		while (depth > 0) {
			switch(parser.next()) {
			case XmlPullParser.END_TAG:
			       depth--;
			       continue;
			case XmlPullParser.START_TAG:
			       break;
			default:
				continue; /* All other tags => ignore */
			}
			
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String subTag = parser.getName();
			switch(subTag) {
			case "ele":
				readDouble(parser, subTag, location::setAltitude);
				break;
			case "time":
				readTime(parser, subTag, location::setTime);
				break;
			case "accuracy":
				readFloat(parser, subTag, location::setAccuracy);
				break;
			case "speed":
				readFloat(parser, subTag, location::setSpeed);
				break;
			case "baro":
				pressure = readFloat(parser, subTag, 0.0f);
				break;
			case "compass":
				azimuth = readFloat(parser, subTag, -1.0f);
				break;
			case "compassAccuracy":
				compassAccuracy = readInt(parser, subTag);
				break;
			case "name":
				name = readString(parser, subTag);
				break;
			default:
				depth++;
				// ignore all other tags, but still recurse
				// into them
			}
		}

		parser.require(XmlPullParser.END_TAG, null, tag);

		if(isWaypoint) {
			dataHelper.wayPoint(trackId, location, name, null, null, azimuth, compassAccuracy, pressure);
		} else {
			dataHelper.track(trackId, location, azimuth,
					 compassAccuracy, pressure, newSegment, true);
		}
	}
	
	// Parses the contents of a track or route segment. If it encounters a
	// trkpt tag, hands it off to readPoint
	private void readSegment(XmlPullParser parser, String tag)
		throws XmlPullParserException, IOException {
		boolean segmentStart = true;
		parser.require(XmlPullParser.START_TAG, null, tag);
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("trkpt")||
			    name.equals("rtept")) {
				readPoint(parser, name, segmentStart,false);
				segmentStart = false;
			} else {
				skip(parser);
			}
		}
		parser.require(XmlPullParser.END_TAG, null, tag);
	}


	// KML
	private static final Pattern pat =
			Pattern.compile("\\G\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*(?:,\\s*([0-9.]+)\\s*)?");
	
	private Location nextCoordinate(Matcher m) {
		Location location = new Location("import");
		storeDouble(m.group(1), location::setLongitude);
		storeDouble(m.group(2), location::setLatitude);
		storeDouble(m.group(3), location::setAltitude);
		return location;
	}

	private Matcher parseCoordinates(XmlPullParser parser, String tag)
		throws XmlPullParserException, IOException
	{
		return pat.matcher(readString(parser, tag));
	}

	private void readKmlTrackSegment(XmlPullParser parser, String tag)
		throws XmlPullParserException, IOException
	{
		parser.require(XmlPullParser.START_TAG, null, tag);
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}

			String subTag = parser.getName();
			if("coordinates".equals(subTag)) {
				boolean newSegment = true;
				Matcher m = parseCoordinates(parser, subTag);
				while (m.find()) {
					Location point = nextCoordinate(m);
					dataHelper.track(trackId, point,
							-1.0f, 0, 0.0f,
							newSegment, true);
					newSegment = false;
				}
			} else
				skip(parser);
		}
		parser.require(XmlPullParser.END_TAG, null, tag);
	}
	
	private Location readKmlWayPoint(XmlPullParser parser, String tag)
		throws XmlPullParserException, IOException
	{
		Location point=null;
		parser.require(XmlPullParser.START_TAG, null, tag);
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String subTag = parser.getName();
			if("coordinates".equals(subTag)) {
				Matcher m = parseCoordinates(parser, subTag);
				if (!m.find())
					return null;
				point = nextCoordinate(m);
			} else {
				skip(parser);
			}
		}
		parser.require(XmlPullParser.END_TAG, null, tag);
		return point;
	}
	
	private void readPlacemark(XmlPullParser parser, String tag)
		throws XmlPullParserException, IOException {

		Location waypoint=null;
		String wptName=null;

		parser.require(XmlPullParser.START_TAG, null, tag);
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String subTag = parser.getName();
			switch(subTag) {
			case "Point":
				// a waypoint
				waypoint = readKmlWayPoint(parser, subTag);
				break;
			case "name":
				// the name of the waypoint or track
				wptName = readString(parser, subTag);
				break;
			case "LineString":
				// a track segment
				readKmlTrackSegment(parser, subTag);
				break;
			default:
				skip(parser);
			}
		}
		parser.require(XmlPullParser.END_TAG, null, tag);

		if(waypoint != null) {
			waypoint.setExtras(new Bundle());
			dataHelper.wayPoint(trackId, waypoint, wptName,
					    null, null, -1.0f, 0, 0.0f);
		}
	}

	public void reportPosition(long position,
				   long totalSize,
				   ProgressDialog pb) {
		if(position > 30 && position <= totalSize)
			pb.setProgress((int)position);
	}


	private void showException(String msg, Exception e) {
		try {
			new AlertDialog.Builder(context)
				.setTitle("Exception received while "+msg)
				.setMessage(Log.getStackTraceString(e))
				.setNeutralButton("Ok",
						  (dlg,id)->dlg.dismiss())
				.create()
				.show();
		} catch(Exception e2) {
			Log.v(TAG, "Exception while showing exception "+
			      Log.getStackTraceString(e2));
								
		}
	}
	
	/**
	 * Import the given input stream into the given track
	 */
	public void doImport(AssetFileDescriptor afd,
			     Runnable completion) {
		ProgressDialog pb = new ProgressDialog(context);
		pb.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pb.setIndeterminate(false);
		pb.setCancelable(false);
		pb.setProgress(0);
		pb.setMax(100);
		pb.setTitle("Import");
		pb.show();

		new Thread(()-> {
				try(InputStream is = afd.createInputStream()){
					Looper.prepare();
					long totSize = afd.getLength();
					if(totSize > 0) {
						pb.setMax((int)totSize);
					}
					InputStreamWithPosition isp =
						new InputStreamWithPosition(is,
									    p->reportPosition(p, totSize, pb));
					doImport(isp);
					context.runOnUiThread(completion);
				} catch(Exception e) {
					Log.v(TAG, "Exception during import "+
					      Log.getStackTraceString(e));
					context.runOnUiThread(()->showException("importing route", e));
				} finally {
					pb.dismiss();
				}
		}).start();
	}
		
	/**
	 * Import the given input stream into the given track
	 */
	private void doImport(InputStream is)
		throws IOException, XmlPullParserException {
		int event;
		// Xml Pull parser
		// https://www.tutorialspoint.com/android/android_xml_parsers.htm
		// https://developer.android.com/reference/org/xmlpull/v1/XmlPullParser
		// https://developer.android.com/training/basics/network-ops/xml
		XmlPullParserFactory xmlFactoryObject =
			XmlPullParserFactory.newInstance();
		XmlPullParser p = xmlFactoryObject.newPullParser();
		p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		p.setInput(is, null);

		while ((event=p.next()) != XmlPullParser.END_DOCUMENT) {
			if(event == XmlPullParser.START_TAG) {
				String name=p.getName();

				// GPX
				if("trkseg".equals(name) ||
				   "rte".equals(name)) {
					readSegment(p, name);
				} else if (name.equals("wpt")) {
					readPoint(p, name, false, true);

					// KML
				} else if (name.equals("Placemark")) {
					readPlacemark(p, name);
				}
			}
		}
	}
}
