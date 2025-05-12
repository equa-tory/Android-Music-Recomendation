package com.example.test01;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddActivity extends AppCompatActivity {

    View fieldItemTitle;
    View fieldItemAuthor;
    View fieldItemUrl;
    View fieldItemMood;
    View fieldItemComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        fieldItemTitle = findViewById(R.id.item_field_01);
        fieldItemAuthor = findViewById(R.id.item_field_02);
        fieldItemUrl = findViewById(R.id.item_field_03);
        fieldItemMood = findViewById(R.id.item_field_04);
        fieldItemComment = findViewById(R.id.item_field_05);

        TextView fi_title_title = fieldItemTitle.findViewById(R.id.field_title);
        fi_title_title.setText("Song Name");

        TextView fi_author_title = fieldItemAuthor.findViewById(R.id.field_title);
        fi_author_title.setText("Author");

        TextView fi_url_title = fieldItemUrl.findViewById(R.id.field_title);
        fi_url_title.setText("Url*");

        TextView fi_mood_title = fieldItemMood.findViewById(R.id.field_title);
        fi_mood_title.setText("Mood*");
        //
        Spinner spinner = fieldItemMood.findViewById(R.id.field_spinner);
        fieldItemMood.findViewById(R.id.field_edittext).setVisibility(GONE);
        spinner.setVisibility(VISIBLE);
        //
        String[] moodLabels = {"Chill", "Drive", "Casual", "Rock", "Christmas"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, moodLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        TextView fi_comment_title = fieldItemComment.findViewById(R.id.field_title);
        fi_comment_title.setText("Comment*");
    }

    public void ActivityToMain(View v) {
        Intent intent = new Intent(AddActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void SubmitTrack(View v){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ServerConfig.SERVER_ADDRESS) // важно! для эмулятора Android = localhost
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NetApi api = retrofit.create(NetApi.class);

//        Track testTrack = new Track("Test Song", "Anon", "https://youtube.com/xyz", 3, "cool mood");

        EditText fi_title = fieldItemTitle.findViewById(R.id.field_edittext);
        EditText fi_author = fieldItemAuthor.findViewById(R.id.field_edittext);
        EditText fi_url = fieldItemUrl.findViewById(R.id.field_edittext);
        Spinner fi_mood = fieldItemMood.findViewById(R.id.field_spinner);
        EditText fi_comment = fieldItemComment.findViewById(R.id.field_edittext);

        int userId = getSharedPreferences("prefs", MODE_PRIVATE)
                .getInt("user_id", -1);
        if (userId == -1) {
            Log.e("TRACK", "No user_id found in SharedPreferences");
            return; // не продолжаем без пользователя
        }

        Track track = new Track(
            -1,
            fi_title.getText().toString(),
            fi_author.getText().toString(),
            fi_url.getText().toString(),
//            Integer.parseInt(fi_mood.getText().toString()),
            fi_mood.getSelectedItemPosition(),
            fi_comment.getText().toString(),
            userId
        );

        api.sendTrack(track).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("SERVER", "OK");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("SERVER", "FAIL: " + t.getMessage());
            }
        });

        ActivityToMain(v);
    }
}
