package net.osmtracker.receiver;

import android.content.BroadcastReceiver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import net.osmtracker.activity.TrackManager;

import java.util.Objects;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private final TrackManager myTrackManager;

    public NetworkChangeReceiver(TrackManager trackManager) {
        this.myTrackManager = trackManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String currentSSID = wifiInfo.getSSID();

        String homeWiFiName = "Hanh Phuc";
        if (
                !Objects.equals(currentSSID, homeWiFiName) &&
                        myTrackManager.currentTrackId == TrackManager.TRACK_ID_NO_TRACK
        ) {
            myTrackManager.startTrackLoggerForNewTrack();
        } else if (
                Objects.equals(currentSSID, homeWiFiName) &&
                        myTrackManager.currentTrackId != TrackManager.TRACK_ID_NO_TRACK
        ) {
            myTrackManager.stopActiveTrack();
        }
    }
}
