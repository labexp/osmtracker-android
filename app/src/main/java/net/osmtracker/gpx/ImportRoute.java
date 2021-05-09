package net.osmtracker.gpx;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.util.Log;

import net.osmtracker.db.DataHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.DoubleConsumer;

interface LongConsumer {
	void accept(long l);
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
	private final Context context;

	public ImportRoute(Context context, long trackId) {
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
		if(str == null || "".equals(str))
			return;
		try {
			setter.accept(Double.parseDouble(str));
		} catch(NumberFormatException e) {
			Log.v(TAG, "Bad double "+tag+" :\""+str+"\"");
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

	private void readTime(XmlPullParser parser, String tag,
			      LongConsumer setter)
		throws XmlPullParserException, IOException {
		String str = readString(parser,tag);
		if(str == null || "".equals(str))
			return;

		// first try with timezone...
		try {
			setter.accept(ZonedDateTime
				      .parse(str)
				      .toInstant()
				      .toEpochMilli());
			return;
		} catch(DateTimeParseException e) {
			Log.v(TAG, "Bad zoned time "+tag+" :\""+str+"\":"+e);
		}

		// and then without
		try {
			setter.accept(LocalDateTime
				      .parse(str)
				      .atZone(ZoneId.systemDefault())
				      .toInstant()
				      .toEpochMilli());
		} catch(DateTimeParseException e) {
			Log.v(TAG, "Bad time "+tag+" :\""+str+"\":"+e);
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

	/**
	 * Reads a track point
	 * @param parser the parser
	 * @param tag the tag
	 * @param newSegment true if this point starts a new segment
	 */
	private void readPoint(XmlPullParser parser,
			       String tag,
			       boolean newSegment)
		throws XmlPullParserException, IOException {
		int depth=1;

		float azimuth=-1.0f;
		int compassAccuracy=0;
		float pressure=0.0f;

		/* Ensure we have correct tag */
		parser.require(XmlPullParser.START_TAG, null, tag);

		Location location = new Location("import");
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
			String name = parser.getName();
			switch(name) {
				case "ele":
					readDouble(parser, name, location::setAltitude);
					break;
				case "time":
					readTime(parser, name, location::setTime);
					break;
				case "accuracy":
					readFloat(parser, name, location::setAccuracy);
					break;
				case "speed":
					readFloat(parser, name, location::setSpeed);
					break;
				case "baro":
					pressure = readFloat(parser, name, 0.0f);
					break;
				case "compass":
					azimuth = readFloat(parser, name, -1.0f);
					break;
				case "compassAccuracy":
					compassAccuracy = readInt(parser, name);
					break;
				default:
			       depth++;
			       // ignore all other tags, but still recurse
			       // into them
			}
		}

		parser.require(XmlPullParser.END_TAG, null, tag);

		dataHelper.track(trackId, location, azimuth,
				 compassAccuracy, pressure, newSegment, true);
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
				readPoint(parser, name, segmentStart);
				segmentStart = false;
			} else {
				skip(parser);
			}
		}
		parser.require(XmlPullParser.END_TAG, null, tag);
	}

	public void reportPosition(long position,
				   long totalSize,
				   ProgressDialog pb) {
		if(position > 30 && position <= totalSize)
			pb.setProgress((int)position);
	}

	/**
	 * Import the given input stream into the given track
	 */
	public void doImport(AssetFileDescriptor afd) {
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
					long totSize = afd.getLength();
					if(totSize > 0) {
						pb.setMax((int)totSize);
					}
					InputStreamWithPosition isp =
						new InputStreamWithPosition(is,
									    p->reportPosition(p, totSize, pb));
					doImport(isp);
				} catch(Exception e) {
					new AlertDialog.Builder(context)
					.setTitle("Exception received")
					.setMessage(Log.getStackTraceString(e))
					.setNeutralButton("Ok",
							  (dlg,id)->dlg.dismiss())
					.create()
					.show();
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
				if("trkseg".equals(name) ||
				   "rte".equals(name)) {
					readSegment(p, name);
				}
			}
		}
	}
}
