package com.smov.gabriel.orientatree.model;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

public class Location {

    private GeoPoint location;
    private Date time;

    public Location () {

    }

    public Location(GeoPoint location, Date time) {
        this.location = location;
        this.time = time;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
