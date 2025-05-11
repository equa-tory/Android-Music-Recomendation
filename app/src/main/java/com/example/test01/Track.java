package com.example.test01;

import java.util.List;

public class Track {
    public int id;
    public String title;
    public String author;
    public String url;
    public int mood;
    public String comment;
    public int user_id;

    public Track(int id, String title, String author, String url, int mood, String comment, int user_id) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.url = url;
        this.mood = mood;
        this.comment = comment;
        this.user_id = user_id;
    }
}

