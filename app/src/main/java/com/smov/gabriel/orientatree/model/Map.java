package com.smov.gabriel.orientatree.model;

import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;

public class Map {

    private ArrayList<GeoPoint> map_corners;
    private ArrayList<GeoPoint> overlay_corners;
    private GeoPoint centering_point;
    private float max_zoom;
    private float min_zoom;
    private float initial_zoom;
    private String map_id;

    public Map () {

    }

    public Map(ArrayList<GeoPoint> map_corners, ArrayList<GeoPoint> overlay_corners,
               GeoPoint centering_point, float max_zoom, float min_zoom, float initial_zoom, String map_id) {
        this.map_corners = map_corners;
        this.overlay_corners = overlay_corners;
        this.centering_point = centering_point;
        this.max_zoom = max_zoom;
        this.min_zoom = min_zoom;
        this.initial_zoom = initial_zoom;
        this.map_id = map_id;
    }

    public ArrayList<GeoPoint> getMap_corners() {
        return map_corners;
    }

    public void setMap_corners(ArrayList<GeoPoint> map_corners) {
        this.map_corners = map_corners;
    }

    public ArrayList<GeoPoint> getOverlay_corners() {
        return overlay_corners;
    }

    public void setOverlay_corners(ArrayList<GeoPoint> overlay_corners) {
        this.overlay_corners = overlay_corners;
    }

    public GeoPoint getCentering_point() {
        return centering_point;
    }

    public void setCentering_point(GeoPoint centering_point) {
        this.centering_point = centering_point;
    }

    public float getMax_zoom() {
        return max_zoom;
    }

    public void setMax_zoom(float max_zoom) {
        this.max_zoom = max_zoom;
    }

    public float getMin_zoom() {
        return min_zoom;
    }

    public void setMin_zoom(float min_zoom) {
        this.min_zoom = min_zoom;
    }

    public float getInitial_zoom() {
        return initial_zoom;
    }

    public void setInitial_zoom(float initial_zoom) {
        this.initial_zoom = initial_zoom;
    }

    public String getMap_id() {
        return map_id;
    }

    public void setMap_id(String map_id) {
        this.map_id = map_id;
    }
}
