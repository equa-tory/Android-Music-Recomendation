package com.example.test01;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    public static int savedPage = 1;
    int currentPage = 1;
    int totalPages = 1;
    final int limit = 5;
    Button prevBtn;
    Button nextBtn;
    LinearLayout trackContainer;
    LayoutInflater inflater;
    Retrofit retrofit;
    NetApi api;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // ============================================
        // Проверка логина
        boolean isLoggedIn = getSharedPreferences("prefs", MODE_PRIVATE)
                .getBoolean("isLoggedIn", false);
        if (!isLoggedIn) {
            LogOut(null);
            return;
        }
        SubmitUser(null);

        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt("currentPage", 1);
        } else {
            currentPage = getSharedPreferences("prefs", MODE_PRIVATE)
                .getInt("currentPage", 1);
        }
        currentPage = savedPage;

        trackContainer = findViewById(R.id.grid_list);
        inflater = LayoutInflater.from(MainActivity.this);

        prevBtn = findViewById(R.id.prev_button);
        nextBtn = findViewById(R.id.next_button);
        retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/") // важно! для эмулятора Android = localhost
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(NetApi.class);

        prevBtn.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                loadPage(currentPage);
            }
        });

        nextBtn.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadPage(currentPage);
            }
        });

        loadPage(currentPage);
    }

    public void loadPage(int page) {
        int userId = getSharedPreferences("prefs", MODE_PRIVATE).getInt("user_id", -1);
        api.getTracks(userId, page, limit).enqueue(new Callback<TrackResponse>() {
            @Override
            public void onResponse(Call<TrackResponse> call, Response<TrackResponse> response) {
                if (response.isSuccessful()) {
                    TrackResponse trackResp = response.body();
                    List<Track> tracks = trackResp.data;
                    List<Integer> followedIds = trackResp.followed_ids;
                    totalPages = (int) Math.ceil((double) trackResp.total / limit);

                    trackContainer.removeAllViews();

                    for (Track track : tracks) {
                        View itemView = inflater.inflate(R.layout.item_track, trackContainer, false);
                        ImageButton followBtn = itemView.findViewById(R.id.track_btn_like);
                        TextView title = itemView.findViewById(R.id.title_text);
                        TextView description = itemView.findViewById(R.id.description_text);

                        followBtn.setTag(R.id.track_btn_like, track.id);
                        followBtn.setTag(R.id.followed_flag, followedIds.contains(track.id));
                        title.setText(track.title);
                        description.setText(track.author + " | mood: " + track.mood);

                        // Change like button icon if liked
                        if (followedIds.contains(track.id)) {
                            followBtn.setImageResource(R.drawable.dark_like);
                        }

                        trackContainer.addView(itemView);
                    }

                    updateButtons();
                }
            }

            @Override
            public void onFailure(Call<TrackResponse> call, Throwable t) {
                Log.e("SERVER", "FAIL: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentPage", currentPage);
    }

    private void updateButtons() {
        prevBtn.setEnabled(currentPage > 1);
        nextBtn.setEnabled(currentPage < totalPages);
    }

    public void Like(View v) {
        ImageButton likeBtn = (ImageButton) v;
        Object tag = likeBtn.getTag();
        if (tag == null) {
            Toast.makeText(this, "Ошибка: нет ID трека", Toast.LENGTH_SHORT).show();
            return;
        }

        int trackId = (int) tag;
        int userId = getSharedPreferences("prefs", MODE_PRIVATE).getInt("user_id", -1);
        if (userId == -1) {
            Toast.makeText(this, "Ошибка: не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        Follow follow = new Follow(userId, trackId);

        boolean isFollowed = (boolean) likeBtn.getTag(R.id.followed_flag);

        if (isFollowed) {
            // UNFOLLOW
            api.sendUnfollow(follow).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    Toast.makeText(MainActivity.this, "Удалено из избранного", Toast.LENGTH_SHORT).show();
                    likeBtn.setImageResource(R.drawable.like); // светлая иконка
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // FOLLOW
            api.sendFollow(follow).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    Toast.makeText(MainActivity.this, "Сохранено", Toast.LENGTH_SHORT).show();
                    likeBtn.setImageResource(R.drawable.dark_like); // тёмная иконка
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void ActivityToReactions(View v) {
        savedPage = currentPage;
        getSharedPreferences("prefs", MODE_PRIVATE)
                .edit()
                .putInt("currentPage", currentPage)
                .apply();

        Intent intent = new Intent(MainActivity.this, ReactionsActivity.class);
        startActivity(intent);
    }

    public void ActivityToAdd(View v){
        savedPage = currentPage;
        getSharedPreferences("prefs", MODE_PRIVATE)
                .edit()
                .putInt("currentPage", currentPage)
                .apply();

        Intent intent = new Intent(MainActivity.this, AddActivity.class);
        startActivity(intent);
    }

    public void LogOut(View v){
        getSharedPreferences("prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", false)
                .apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // закрываем MainActivity, чтобы не вернуться назад
    }
    public void SubmitUser(View v){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NetApi api = retrofit.create(NetApi.class);

        String username = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("username", "");
        String password = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("password", "");
        User user = new User(username, password);

        api.sendUser(user).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("SERVER", "Login OK");
                    getSharedPreferences("prefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("isLoggedIn", true)
                            .apply();
                    //ActivityToMain(v);
                } else if (response.code() == 401) {
                    Log.e("SERVER", "Invalid password");
                    // Можно показать Toast или ошибку в UI
                    Toast.makeText(getApplicationContext(), "Неверный пароль", Toast.LENGTH_SHORT).show();
                    LogOut(v);
                } else {
                    Log.e("SERVER", "Unknown error: " + response.code());
                    LogOut(v);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e("SERVER", "FAIL: " + t.getMessage());
                // Можно показать Toast "Нет подключения"
                Toast.makeText(getApplicationContext(), "Ошибка соединения с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }
}