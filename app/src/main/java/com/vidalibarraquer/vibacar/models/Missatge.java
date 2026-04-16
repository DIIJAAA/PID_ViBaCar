package com.vidalibarraquer.vibacar.models;

public class Missatge {

    private String id;
    private String emissorId;
    private String text;
    private long dataMillis;

    public Missatge() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmissorId() {
        return emissorId;
    }

    public void setEmissorId(String emissorId) {
        this.emissorId = emissorId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getDataMillis() {
        return dataMillis;
    }

    public void setDataMillis(long dataMillis) {
        this.dataMillis = dataMillis;
    }
}
