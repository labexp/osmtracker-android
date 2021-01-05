package net.osmtracker.db.model;

import android.database.Cursor;

import net.osmtracker.db.TrackContentProvider;

/**
 * Represents a TrackPoint
 */
public class TrackPoint extends Point {

    private Double speed;


    public TrackPoint(Cursor c) {
        super(c);
        if ( ! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_SPEED)) ) {
            speed = c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_SPEED));
        }
    }

    public TrackPoint() {

    }


    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }
}
