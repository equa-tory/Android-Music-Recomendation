package com.example.test01;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class ReactionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reactions);
    }

    public void ActivityToMain(View v) {
        Intent intent = new Intent(ReactionsActivity.this, MainActivity.class);
        startActivity(intent);
    }
}
