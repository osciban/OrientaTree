package com.smov.gabriel.orientatree.model;

import java.util.Comparator;
import java.util.Date;

public class BeaconReachedLOD implements Comparator<BeaconReachedLOD> {

    private Date reachMoment;
    private String beacon_id;
    private int quiz_answer;
    private String written_answer;
    private boolean answer_right;
    private boolean answered;

    public BeaconReachedLOD() {

    }

    public BeaconReachedLOD(Date reachMoment, String beacon_id, boolean answered) {
        this.reachMoment = reachMoment;
        this.beacon_id = beacon_id;
        this.answered = answered;
    }

    public BeaconReachedLOD(Date reachMoment, String beacon_id, int quiz_answer,
                            String written_answer, boolean answer_right, boolean answered) {
        this.reachMoment = reachMoment;
        this.beacon_id = beacon_id;
        this.quiz_answer = quiz_answer;
        this.written_answer = written_answer;
        this.answer_right = answer_right;
        this.answered = answered;
    }

    public Date getReachMoment() {
        return reachMoment;
    }

    public void setReachMoment(Date reachMoment) {
        this.reachMoment = reachMoment;
    }

    public String getBeacon_id() {
        return beacon_id;
    }

    public void setBeacon_id(String beacon_id) {
        this.beacon_id = beacon_id;
    }

    public int getQuiz_answer() {
        return quiz_answer;
    }

    public void setQuiz_answer(int quiz_answer) {
        this.quiz_answer = quiz_answer;
    }

    public String getWritten_answer() {
        return written_answer;
    }

    public void setWritten_answer(String written_answer) {
        this.written_answer = written_answer;
    }

    public boolean isAnswer_right() {
        return answer_right;
    }

    public void setAnswer_right(boolean answer_right) {
        this.answer_right = answer_right;
    }

    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    @Override
    public int compare(BeaconReachedLOD o1, BeaconReachedLOD o2) {
        if(o1.getReachMoment() != null && o2.getReachMoment() != null) {
            return o2.getReachMoment().compareTo(o1.getReachMoment());
        } else {
            return 0;
        }
    }
}
