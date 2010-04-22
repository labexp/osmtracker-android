// Created by plusminus on 21:46:22 - 25.09.2008
package org.andnav.osm.views.util;

import java.io.File;
import java.util.HashMap;

import org.andnav.osm.services.IOpenStreetMapTileProviderCallback;
import org.andnav.osm.services.IOpenStreetMapTileProviderService;
import org.andnav.osm.services.util.OpenStreetMapTile;
import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;


/**
 * 
 * @author Nicolas Gramlich
 * 
 */
public class OpenStreetMapTileProvider implements ServiceConnection, OpenStreetMapConstants,
		OpenStreetMapViewConstants {
	// ===========================================================
	// Constants
	// ===========================================================
	
	private static final long ERROR_RESET_PERIOD_MS = 15000; //15 seconds

	// ===========================================================
	// Fields
	// ===========================================================

	/** cache provider */
	protected OpenStreetMapTileCache mTileCache;
	
	private HashMap<OpenStreetMapTile, OpenStreetMapTile> mRequestedTiles;
	private HashMap<OpenStreetMapTile, OpenStreetMapTile> mErrorousTiles;
	
	private long mLastErrorReset;
	
	/**
	 * Service is bound, but maybe not still connected.
	 */
	private boolean mServiceBound;

	private IOpenStreetMapTileProviderService mTileService;
	private Handler mDownloadFinishedHandler;
	
	private Context mContext;

	// ===========================================================
	// Constructors
	// ===========================================================

	public OpenStreetMapTileProvider(final Context ctx,
			final Handler aDownloadFinishedListener) {
		this.mContext = ctx;
		this.mTileCache = new OpenStreetMapTileCache();
		this.mRequestedTiles = new HashMap<OpenStreetMapTile, OpenStreetMapTile>();
		this.mErrorousTiles = new HashMap<OpenStreetMapTile, OpenStreetMapTile>();
		this.mLastErrorReset = 0;
		
		this.bindToService();
		
		this.mDownloadFinishedHandler = aDownloadFinishedListener;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	public void onServiceConnected(final ComponentName name, final IBinder service) {
		mTileService = IOpenStreetMapTileProviderService.Stub.asInterface(service);
		try {
			mDownloadFinishedHandler.sendEmptyMessage(OpenStreetMapTile.MAPTILE_SUCCESS_ID);
		} catch(Exception e) {
			Log.e(DEBUGTAG, "Error sending success message on connect", e);
		}
		Log.d(DEBUGTAG, "connected");
	};
	
	@Override
	public void onServiceDisconnected(final ComponentName name) {
		this.onDisconnect();
		Log.d(DEBUGTAG, "disconnected");
	}
	
	// ===========================================================
	// Methods
	// ===========================================================


	
	private boolean bindToService()
	{
		if (this.mServiceBound)
			return true;
		
		boolean success = this.mContext.bindService(new Intent(IOpenStreetMapTileProviderService.class.getName()), this, Context.BIND_AUTO_CREATE);
		
		if (!success)
			Log.e(DEBUGTAG, "Could not bind to " + IOpenStreetMapTileProviderService.class.getName());
		
		this.mServiceBound = success;
		
		return success;
	}
	

	/***
	 * Disconnects from the tile downloader service.
	 */
	public void disconnectService()
	{
		if (this.mServiceBound)
		{
			if (DEBUGMODE)
				Log.d(DEBUGTAG, "Unbinding service");		
			this.mContext.unbindService(this);
			this.onDisconnect();
		}
	}

	private void onDisconnect()
	{
		this.mServiceBound = false;
		this.mTileService = null;
		this.mTileService = null;
		this.mErrorousTiles = new HashMap<OpenStreetMapTile, OpenStreetMapTile>();
		this.mRequestedTiles = new HashMap<OpenStreetMapTile, OpenStreetMapTile>();
	}
	
	
	/**
	 * Get the tile from the cache.
	 * If it's in the cache then it will be returned.
	 * If not it will return null and request it from the service.
	 * In turn, the service will request it from the file system.
	 * If it's found in the file system it will notify the callback.
	 * If not it will initiate a download.
	 * When the download has finished it will notify the callback.
	 * @param aTile the tile being requested
	 * @return the tile bitmap if found in the cache, null otherwise
	 */
	public Bitmap getMapTile(final OpenStreetMapTile aTile) {
		if (this.mTileCache.containsTile(aTile)) {							// from cache
			if (DEBUGMODE)
				Log.d(DEBUGTAG, "MapTileCache succeeded for: " + aTile);
			return mTileCache.getMapTile(aTile);			
		} else {															// from service
			if (mTileService == null) {
				
				//try to reconnect, but the connection will take time.
				
				if (!this.bindToService())
				{				
					if (DEBUGMODE)
						Log.d(DEBUGTAG, "Cache failed, can't get from FS because no tile service: " + aTile);					
				}
				else
				{
					if (DEBUGMODE)
						Log.d(DEBUGTAG, "Cache failed, tile service still not woken up: " + aTile);
				}
				
				return null;
			} 
			
			if (this.mRequestedTiles.containsKey(aTile))
			{
				//already requested no use to repeat.
				return null;
			}
			else
			{
				this.mRequestedTiles.put(aTile, aTile);
			}

			if (this.mErrorousTiles.containsKey(aTile))
			{
				long curTime = SystemClock.elapsedRealtime();
				
				if (curTime - this.mLastErrorReset > ERROR_RESET_PERIOD_MS )
				{
					//reset errors, try again
					this.mErrorousTiles.clear();
					this.mLastErrorReset = curTime;				
				}
				else
				{				
					//last time this got error, no use to repeat.
					return null;
				}
			}

			if (DEBUGMODE)
					Log.d(DEBUGTAG, "Cache failed, trying from FS: " + aTile);
			try {
				mTileService.requestMapTile(aTile.rendererID, aTile.zoomLevel, aTile.x, aTile.y, mServiceCallback);
			} catch (Throwable e) {
				Log.e(DEBUGTAG, "Error getting map tile from tile service: " + aTile, e);
			}
		
			return null;
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
	
	IOpenStreetMapTileProviderCallback mServiceCallback = new IOpenStreetMapTileProviderCallback.Stub() {

		@Override
		public void mapTileRequestCompleted(int rendererID, int zoomLevel, int tileX, int tileY, String aTilePath) throws RemoteException 
		{
			final OpenStreetMapTile tile = new OpenStreetMapTile(rendererID, zoomLevel, tileX, tileY);
			boolean tileOk = false;
			
			if (aTilePath != null) {
				try {					
					final Bitmap bitmap = BitmapFactory.decodeFile(aTilePath);
					if (bitmap != null) {
						mTileCache.putTile(tile, bitmap);
						tileOk = true;
					} else {
						// if we couldn't load it then it's invalid - delete it
						try {
							new File(aTilePath).delete();
						} catch (Throwable e) {
							Log.e(DEBUGTAG, "Error deleting invalid file: " + aTilePath, e);
						}
					}
				} catch (OutOfMemoryError e) {
					Log.e(DEBUGTAG, "OutOfMemoryError putting tile in cache: " + tile);
				}
			}
			mDownloadFinishedHandler.sendEmptyMessage(OpenStreetMapTile.MAPTILE_SUCCESS_ID);
			if (DEBUGMODE)
				Log.d(DEBUGTAG, "MapTile request complete: " + tile);
			
			if (!tileOk)
				mErrorousTiles.put(tile, tile);
			
			//remove all requested tiles, because we cannot be sure if some of them got removed from queue...
			mRequestedTiles.clear();
							
		}
	};

}