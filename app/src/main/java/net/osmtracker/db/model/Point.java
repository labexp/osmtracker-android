package net.osmtracker.db.model;

import android.database.Cursor;

import net.osmtracker.db.TrackContentProvider;

/**
 * WayPoint and TrackPoint inherit from this object Point
 */
public abstract class Point {

    protected Integer id;
    protected Integer trackId;
    protected double latitude;
    protected double longitude;
    protected long pointTimestamp;

    protected Double elevation;
    protected Double accuracy;
    protected Double compassHeading;
    protected Double compassAccuracy;
    protected Double atmosphericPressure;


    protected Point(Cursor c) {
        id = c.getInt(c.getColumnIndex(TrackContentProvider.Schema.COL_ID));
        trackId = c.getInt(c.getColumnIndex(TrackContentProvider.Schema.COL_TRACK_ID));
        latitude = c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_LATITUDE));
        longitude = c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_LONGITUDE));
        pointTimestamp = c.getLong(c.getColumnIndex(TrackContentProvider.Schema.COL_TIMESTAMP));

        if ( ! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_ELEVATION)) ) {
            elevation = c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_ELEVATION));
        }
        if ( ! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY)) ) {
            accuracy = c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY));
        }
        if ( ! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS)) ) {
            compassHeading = c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS));
        }
        if ( ! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS_ACCURACY)) ) {
            compassAccuracy = c.getDouble(
                    c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS_ACCURACY));
        }
        if ( ! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_ATMOSPHERIC_PRESSURE)) ) {
            atmosphericPressure = c.getDouble(
                    c.getColumnIndex(TrackContentProvider.Schema.COL_ATMOSPHERIC_PRESSURE));
        }
    }

    public Point() {

    }


    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public long getPointTimestamp() {
        return pointTimestamp;
    }

    public void setPointTimestamp(long pointTimestamp) {
        this.pointTimestamp = pointTimestamp;
    }


    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Double getCompassHeading() {
        return compassHeading;
    }

    public void setCompassHeading(Double compassHeading) {
        this.compassHeading = compassHeading;
    }

    public Double getCompassAccuracy() {
        return compassAccuracy;
    }

    public void setCompassAccuracy(Double compassAccuracy) {
        this.compassAccuracy = compassAccuracy;
    }

    public Double getAtmosphericPressure() {
        return atmosphericPressure;
    }

    public void setAtmosphericPressure(Double atmosphericPressure) {
        this.atmosphericPressure = atmosphericPressure;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTrackId() {
        return trackId;
    }

    public void setTrackId(Integer trackId) {
        this.trackId = trackId;
    }

}
