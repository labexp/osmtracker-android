package org.andnav.osm.services.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.andnav.osm.services.IOpenStreetMapTileProviderCallback;
import org.andnav.osm.services.util.constants.OpenStreetMapServiceConstants;

import android.os.DeadObjectException;
import android.os.RemoteException;
import android.util.Log;

public abstract class OpenStreetMapAsyncTileProvider implements OpenStreetMapServiceConstants {

	protected String PATH_NOREPLY ="NOREPLY";
	
	private final int mThreadPoolSize;
	private final int mPendingQueueSize;
	private final ThreadGroup mThreadPool = new ThreadGroup(debugtag());
	private final LinkedHashMap<OpenStreetMapTile, IOpenStreetMapTileProviderCallback> mPending;
	private final HashMap<OpenStreetMapTile, Object> mWorking;

	public OpenStreetMapAsyncTileProvider(final int aThreadPoolSize, final int aPendingQueueSize) {
		mThreadPoolSize = aThreadPoolSize;
		mPendingQueueSize = aPendingQueueSize;
		mWorking = new HashMap<OpenStreetMapTile, Object>();
		mPending = new LinkedHashMap<OpenStreetMapTile, IOpenStreetMapTileProviderCallback>(aPendingQueueSize + 2, 0.1f, true)
		{
			private static final long serialVersionUID = 1L;
			@Override
			protected boolean removeEldestEntry(Entry<OpenStreetMapTile, IOpenStreetMapTileProviderCallback> pEldest) {
				final boolean max = size() > mPendingQueueSize;
				return max;
			}
		};
	}
	
	public boolean loadMapTileAsync(final OpenStreetMapTile aTile,
			final IOpenStreetMapTileProviderCallback aCallback) {
		
		boolean newEntry = false;
		final int activeCount = mThreadPool.activeCount(); 

		synchronized(mPending)
		{
			// sanity check
			if (activeCount == 0 && !mPending.isEmpty()) {
				Log.w(debugtag(), "Unexpected - no active threads but pending queue not empty");
				mPending.clear();
			}			
			
			newEntry = !mPending.containsKey(aTile);
			
			// this will put the tile in the queue, or move it to the BACK of the
			// queue if it's already present
			mPending.put(aTile, aCallback);
		}
		
		if (!newEntry)
			return false;
		
		if (DEBUGMODE)
			Log.d(debugtag(), activeCount + " active threads");
		if (activeCount < mThreadPoolSize) {
			final Thread t = new Thread(mThreadPool, getTileLoader(aCallback));
			t.start();
		}
		
		return true;
	}
	
	/**
	 * Stops all workers, the service is shutting down.
	 */
	public void stopWorkers()
	{
		this.clearQueue();
		this.mThreadPool.interrupt();
	}
	
	private OpenStreetMapTile startTileLoad() 
	{
		
		synchronized (mPending) 
		{
			if (mPending.size() == 0)
				return null;				
			
			//find LAST tile that is not taken.
			Iterator<OpenStreetMapTile> iterator = mPending.keySet().iterator();
			OpenStreetMapTile result = null;
			
			while(iterator.hasNext())
			{
				OpenStreetMapTile tile = iterator.next();
				
				if (!mWorking.containsKey(tile))
				{
					result = tile;
				}
			}
			
			if (result != null)
			{
				mWorking.put(result, result);					
			}
			
			return result;					
		}
	}
	
	
	/**
	 * 
	 * @param tile
	 * @param path
	 * @param success
	 */
	private void finishTileLoad(OpenStreetMapTile tile, final IOpenStreetMapTileProviderCallback aCallback, String path, boolean success)
	{
		synchronized (mPending) 
		{
			mPending.remove(tile);
			mWorking.remove(tile);
		}
		
		if (success && aCallback != null && path != PATH_NOREPLY)
		{
			try {
				aCallback.mapTileRequestCompleted(tile.rendererID, tile.zoomLevel, tile.x, tile.y, path);
			} catch (DeadObjectException e) {
				// our caller has died so there's not much point
				// carrying on
				Log.e(debugtag(), "Caller has died");
				clearQueue();
			} catch (RemoteException e) {
				Log.e(debugtag(), "Service failed", e);
			}			
		}
	}
	
	private void clearQueue()
	{
		synchronized (mPending) 
		{
			mPending.clear();
			mWorking.clear();
		}
	}
	
	/**
	 * The debug tag.
	 * Because the tag of the abstract class is not so interesting.
	 * @return
	 */
	protected abstract String debugtag();
	
	protected abstract Runnable getTileLoader(final IOpenStreetMapTileProviderCallback aCallback);

	protected abstract class TileLoader implements Runnable {
		final IOpenStreetMapTileProviderCallback mCallback;

		public TileLoader(final IOpenStreetMapTileProviderCallback aCallback) {
			mCallback = aCallback;
		}
		
		/**
		 * Load the requested tile.
		 * @param aTile the tile to load
		 * @return the path of the requested tile
		 * @throws CantContinueException if it is not possible to continue with processing the queue
		 */
		protected abstract String loadTile(OpenStreetMapTile aTile) throws CantContinueException;

		
		@Override
		final public void run() {

			OpenStreetMapTile tile;
			while ((tile = startTileLoad()) != null) {
				if (DEBUGMODE)
					Log.d(debugtag(), "Next tile: " + tile);
				String path = null;
				try {
					path = loadTile(tile);
				} catch (final CantContinueException e) {					
					Log.i(debugtag(), "Tile loader can't continue");
					clearQueue();
				} catch (final Throwable e) {
					Log.e(debugtag(), "Error downloading tile: " + tile, e);
					finishTileLoad(tile, mCallback, path, false);
				} finally {
					finishTileLoad(tile, mCallback, path, true);
				}
			}
			if (DEBUGMODE)
				Log.d(debugtag(), "No more tiles");
		}
	}
	
	protected class CantContinueException extends Exception {
		private static final long serialVersionUID = 146526524087765133L;
	}
}