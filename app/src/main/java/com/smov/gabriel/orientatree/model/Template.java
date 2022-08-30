package com.smov.gabriel.orientatree.model;

import java.io.Serializable;
import java.util.ArrayList;

public class Template implements Serializable {

    private String template_id;
    private String name;
    private TemplateType type;
    private TemplateColor color;
    private String location;
    private String description;
    private String norms;
    private String map_id;
    private double start_lat;
    private double start_lng;
    private double end_lat;
    private double end_lng;
    private String password;

    private ArrayList<String> beacons;

    public Template() {

    }

    public Template(String template_id, String name, TemplateType type, TemplateColor color, String location,
                    String description, String norms, String map_id,
                    double start_lat, double start_lng,
                    double end_lat, double end_lng, String password) {
        this.template_id = template_id;
        this.name = name;
        this.type = type;
        this.color = color;
        this.location = location;
        this.description = description;
        this.norms = norms;
        beacons = new ArrayList<>();
        this.map_id = map_id;
        this.start_lat = start_lat;
        this.start_lng = start_lng;
        this.end_lat = end_lat;
        this.end_lng = end_lng;
        this.password = password;
    }

    public TemplateType getType() {
        return type;
    }

    public void setType(TemplateType type) {
        this.type = type;
    }

    public TemplateColor getColor() {
        return color;
    }

    public void setColor(TemplateColor color) {
        this.color = color;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNorms() {
        return norms;
    }

    public void setNorms(String norms) {
        this.norms = norms;
    }

    public ArrayList<String> getBeacons() {
        return beacons;
    }

    public void setBeacons(ArrayList<String> beacons) {
        this.beacons = beacons;
    }

    public String getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(String template_id) {
        this.template_id = template_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMap_id() {
        return map_id;
    }

    public void setMap_id(String map_id) {
        this.map_id = map_id;
    }

    public double getStart_lat() {
        return start_lat;
    }

    public void setStart_lat(double start_lat) {
        this.start_lat = start_lat;
    }

    public double getStart_lng() {
        return start_lng;
    }

    public void setStart_lng(double start_lng) {
        this.start_lng = start_lng;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public double getEnd_lat() {
        return end_lat;
    }

    public void setEnd_lat(double end_lat) {
        this.end_lat = end_lat;
    }

    public double getEnd_lng() {
        return end_lng;
    }

    public void setEnd_lng(double end_lng) {
        this.end_lng = end_lng;
    }
}
