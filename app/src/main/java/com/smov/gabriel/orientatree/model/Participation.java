package com.smov.gabriel.orientatree.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

public class Participation implements Comparator<Participation>, Serializable {

    private String participant;
    private ParticipationState state = ParticipationState.NOT_YET;
    private Date startTime;
    private Date finishTime;
    private boolean completed;
    private ArrayList<BeaconReached> reaches;

    public Participation() {

    }

    public Participation(String participant) {
        this.participant = participant;
    }

    public String getParticipant() {
        return participant;
    }

    public void setParticipant(String participant) {
        this.participant = participant;
    }

    public ParticipationState getState() {
        return state;
    }

    public void setState(ParticipationState state) {
        this.state = state;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public ArrayList<BeaconReached> getReaches() {
        return reaches;
    }

    public void setReaches(ArrayList<BeaconReached> reaches) {
        this.reaches = reaches;
    }

    @Override
    public int compare(Participation o1, Participation o2) {
        if (o1.isCompleted() && !o2.isCompleted()) {
            return -1;
        } else if (!o1.isCompleted() && o2.isCompleted()) {
            return 1;
        } else if (o1.isCompleted() && o2.isCompleted()) {
            // both are completed, so we look for the total time
            if(o1.startTime != null && o1.finishTime != null
                && o2.startTime != null && o1.finishTime != null) {
                long o1_total = Math.abs(o1.startTime.getTime() - o1.finishTime.getTime());
                long o2_total = Math.abs(o2.startTime.getTime() - o2.finishTime.getTime());
                if(o1_total < o2_total) {
                    return -1;
                } else if(o1_total > o2_total) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                // both are completed but something rare happened with their times
                // this shouldn't be normal. We return 0
                return 0;
            }
        } else {
            // none of them is completed
            // first the one that already started
            if((o1.getState() == ParticipationState.NOW
                    || o1.getState() == ParticipationState.FINISHED)
                    && o2.getState() == ParticipationState.NOT_YET) {
                return -1;
            } else if ((o2.getState() == ParticipationState.NOW
                    || o2.getState() == ParticipationState.FINISHED)
                    && o1.getState() == ParticipationState.NOT_YET) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}

