package com.example.test01;

public class Report {
    public int sender_id;
    public int track_id;
    public String category;

    public Report(int sender_id, int track_id, String category) {
        this.sender_id = sender_id;
        this.track_id = track_id;
        this.category = category;
    }
}
