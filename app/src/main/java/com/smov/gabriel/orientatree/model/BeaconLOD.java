package com.smov.gabriel.orientatree.model;

import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Comparator;

public class BeaconLOD implements Comparator<BeaconLOD> {

    private GeoPoint location;
    private int number;
    private String name;
    private String beacon_id;
    private String template_id;
    private String text;
    private String question;
    private String written_right_answer;
    private ArrayList<String> possible_answers;
    private int quiz_right_answer;
    private String image;
    private String type;

    public BeaconLOD() {

    }

    public BeaconLOD(String beacon_id, GeoPoint location, int number, String name, String template_id,
                     String text, String question, String written_right_answer,
                     int quiz_right_answer) {
        this.beacon_id = beacon_id;
        this.location = location;
        this.number = number;
        this.name = name;
        this.template_id = template_id;
        //this.goal = goal;
        this.text = text;
        this.question = question;
        this.written_right_answer = written_right_answer;
        this.quiz_right_answer = quiz_right_answer;
        this.possible_answers = new ArrayList<>();
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public void setImage(String image){
        this.image=image;
    }

    public String getImage(){
        return image;
    }

    public void setType(String type){
        this.type=type;
    }

    public String getType(){
        return type;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBeacon_id() {
        return beacon_id;
    }

    public void setBeacon_id(String beacon_id) {
        this.beacon_id = beacon_id;
    }

    public String getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(String template_id) {
        this.template_id = template_id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getWritten_right_answer() {
        return written_right_answer;
    }

    public void setWritten_right_answer(String written_right_answer) {
        this.written_right_answer = written_right_answer;
    }

    public ArrayList<String> getPossible_answers() {
        return possible_answers;
    }

    public void setPossible_answers(ArrayList<String> possible_answers) {
        this.possible_answers = possible_answers;
    }

    public int getQuiz_right_answer() {
        return quiz_right_answer;
    }

    public void setQuiz_right_answer(int quiz_right_answer) {
        this.quiz_right_answer = quiz_right_answer;
    }

    @Override
    public int compare(BeaconLOD o1, BeaconLOD o2) {
        return o1.getNumber() - o2.getNumber();
    }
}
