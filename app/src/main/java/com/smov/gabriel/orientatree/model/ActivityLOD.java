package com.smov.gabriel.orientatree.model;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

public class ActivityLOD implements Comparator<ActivityLOD>, Serializable {

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
    private String id;
    private String visible_id;
    private String key;
    private String title;
    private String template;
    private String planner_id;
    private boolean score;
    private boolean location_help;
    private String image;
    private int beaconSize;

    private ArrayList<String> beacons;

    private GeoPoint goalLocation;

    private Date startTime;
    private Date finishTime;

    private ArrayList<String> participants;

    public ActivityLOD() {
        participants=new ArrayList<>();
    }

    public ActivityLOD(String id, String key, String title, String template, String planner_id,
                    Date startTime, Date finishTime, boolean score, boolean location_help,String template_id, String name, TemplateType type, TemplateColor color, String location,
                       String description, String norms, String map_id,
                       double start_lat, double start_lng,
                       double end_lat, double end_lng, String password, String image) {
        this.id = id;
        this.visible_id = id.substring(0, Math.min(id.length(), 4));
        this.key = key;
        this.title = title;
        this.template = template;
        this.planner_id = planner_id;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.participants = new ArrayList<>();
        this.score = score;
        this.location_help = location_help;
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
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVisible_id() {
        return visible_id;
    }

    public void setVisible_id(String visible_id) {
        this.visible_id = visible_id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setImage(String image){
        this.image= image;
    }

    public String getImage(){
        return image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTemplate() {
        return template;
    }

    public String getPlanner_id() {
        return planner_id;
    }

    public void setPlanner_id(String planner_id) {
        this.planner_id = planner_id;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date date) {
        this.startTime = date;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public ArrayList<String> getParticipants() {
        return participants;
    }

    public boolean isScore() {
        return score;
    }

    public void setScore(boolean score) {
        this.score = score;
    }

    public boolean isLocation_help() {
        return location_help;
    }

    public void setLocation_help(boolean location_help) {
        this.location_help = location_help;
    }

    public void setParticipants(ArrayList<String> participants) {
        this.participants = participants;
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

    public void addParticipant(String participant_id) {
        if(participants == null) {
            participants = new ArrayList<>();
        }
        participants.add(participant_id);
    }

    public void removeParticipant(String participant_id) {
        if(participants == null) {
            participants = new ArrayList<>();
        }
        participants.remove(participant_id);
    }

    @Override
    public int compare(ActivityLOD o1, ActivityLOD o2) {
        // order's going to be different for past and for future activities
        // so, first we need the current time
        long millis=System.currentTimeMillis();
        Date date = new Date(millis );
        if(o1.getStartTime().after(date) && o2.getStartTime().after(date)) {
            return o1.getStartTime().compareTo(o2.getStartTime());
        } else {
            return o2.getStartTime().compareTo(o1.getStartTime());
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ActivityLOD)) {
            return false;
        }
        ActivityLOD activity = (ActivityLOD) obj;
        return this.id.equals(activity.getId());
    }


    public int getBeaconSize() {
        return beaconSize;
    }

    public void setBeaconSize(int beaconSize) {
        this.beaconSize = beaconSize;
    }

    @Override
    public int hashCode() {
        return this.hashCode();
    }
}
