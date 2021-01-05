package net.osmtracker.db.model;

import android.database.Cursor;

import net.osmtracker.db.TrackContentProvider;

/**
 * Represents a WayPoint
 */
public class WayPoint extends Point {

    private String uuid;
    private Integer numberOfSatellites;
    private String name;
    private String link;


    public WayPoint(Cursor c) {
        super(c);
        uuid = c.getString(c.getColumnIndex(TrackContentProvider.Schema.COL_UUID));
        numberOfSatellites = c.getInt(c.getColumnIndex(TrackContentProvider.Schema.COL_NBSATELLITES));
        name = c.getString(c.getColumnIndex(TrackContentProvider.Schema.COL_NAME));
    }

    public WayPoint() {
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Integer getNumberOfSatellites() {
        return numberOfSatellites;
    }

    public void setNumberOfSatellites(Integer numberOfSatellites) {
        this.numberOfSatellites = numberOfSatellites;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
