package me.guillaumin.android.osmtracker.osm;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import me.guillaumin.android.osmtracker.db.model.Track.OSMVisibility;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public class UploadToOsmTask extends AsyncTask<Void, Integer, Integer> {

	private static final String TAG = UploadToOsmTask.class.getSimpleName();
	
	private final CommonsHttpOAuthConsumer oAuthConsumer;
	private final File gpxFile;	
	private final String description;
	private final String tags;
	private final OSMVisibility visibility;
	
	private HttpPost request;
	
	public UploadToOsmTask(CommonsHttpOAuthConsumer oAuthConsumer,
			File gpxFile, String description, String tags, OSMVisibility visibility) {
		this.oAuthConsumer = oAuthConsumer;
		this.gpxFile = gpxFile;
		this.description = (description == null) ? "test" : description;
		this.tags = (tags == null) ? "test" : tags;
		this.visibility = (visibility == null) ? OSMVisibility.Private : visibility;
	}
	
	@Override
	protected void onPreExecute() {
		try {
			request = new HttpPost(OSMConstants.Api.Gpx.CREATE	);
			oAuthConsumer.sign(request);
			
            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            entity.addPart(OSMConstants.Api.Gpx.Parameters.FILE, new FileBody(gpxFile));
            entity.addPart(OSMConstants.Api.Gpx.Parameters.DESCRIPTION, new StringBody(description));
            entity.addPart(OSMConstants.Api.Gpx.Parameters.TAGS, new StringBody(tags));
            entity.addPart(OSMConstants.Api.Gpx.Parameters.VISIBILITY, new StringBody(visibility.toString().toLowerCase()));
            request.setEntity(entity);
            
		} catch (Exception e) {
			Log.e(TAG, "onPreExecute failed", e);
			cancel(true);
		}
		
		
	}
	
	@Override
	protected Integer doInBackground(Void... params) {
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);
			
			BufferedReader r= new BufferedReader(new InputStreamReader(new BufferedInputStream(response.getEntity().getContent())));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ( (line=r.readLine()) != null) {
				sb.append(line).append(System.getProperty("line.separator"));
			}
			r.close();
			Log.e(TAG, "Response: " + sb.toString());
			
			return response.getStatusLine().getStatusCode();
		} catch (Exception e) {
			Log.e(TAG, "doInBackground failed", e);
			return -1;
		}
	}

}
