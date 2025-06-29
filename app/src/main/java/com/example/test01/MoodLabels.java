package com.example.test01;

import java.util.ArrayList;
import java.util.List;

public class MoodLabels {
    public static List<Mood> moods = new ArrayList<>();

//    public static final String[] labels = {
//            "\uD83D\uDCA5 Drive",
//            "\uD83D\uDECB\uFE0F Chill",
//            "\uD83D\uDC94 Sad",
//            "❄ Christmas",
//            "\uD83D\uDC7B Other"
//    };

    public static String getLabelById(int id) {
        for (Mood mood : moods) {
            if (mood.id == id) return mood.name;
        }
        return "";
    }

    public static List<String> getLabels() {
         List<String> moodNames = new ArrayList<>();
         for(Mood mood : moods) {
             moodNames.add(mood.name);
         }
         return moodNames;
    }
}