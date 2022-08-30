package com.smov.gabriel.orientatree.model;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

public class Activity implements Comparator<Activity>, Serializable {

    private String id;
    private String visible_id;
    private String key;
    private String title;
    private String template;
    private String planner_id;
    private boolean score;
    private boolean location_help;

    private GeoPoint goalLocation;

    private Date startTime;
    private Date finishTime;

    private ArrayList<String> participants;

    public Activity() {

    }

    public Activity(String id, String key, String title, String template, String planner_id,
                    Date startTime, Date finishTime, boolean score, boolean location_help) {
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
    public int compare(Activity o1, Activity o2) {
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
        if (!(obj instanceof Activity)) {
            return false;
        }
        Activity activity = (Activity) obj;
        return this.id.equals(activity.getId());
    }
}
