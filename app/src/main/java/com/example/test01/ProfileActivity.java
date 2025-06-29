package com.example.test01;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileActivity extends AppCompatActivity {

    public static int savedPage = 1;
    int currentPage = 1;
    int totalPages = 1;
    final int limit = 10;
    ImageButton prevBtn;
    ImageButton nextBtn;
    LinearLayout trackContainer;
    LayoutInflater inflater;
    Retrofit retrofit;
    NetApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        //========================================================
        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt("currentPageProfile", 1);
        } else {
            currentPage = getSharedPreferences("prefs", MODE_PRIVATE)
                    .getInt("currentPageProfile", 1);
        }
        currentPage = savedPage;

        inflater = LayoutInflater.from(ProfileActivity.this);

        trackContainer = findViewById(R.id.grid_list);
        prevBtn = findViewById(R.id.prev_button);
        nextBtn = findViewById(R.id.next_button);
        retrofit = new Retrofit.Builder()
                .baseUrl(ServerConfig.SERVER_ADDRESS) // важно! для эмулятора Android = localhost
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

    public void ActivityToMain(View v) {
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void LogOut(View v){
        getSharedPreferences("prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", false)
                .apply();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // закрываем MainActivity, чтобы не вернуться назад
    }

    public void ActivityToAdd(View v){
        savedPage = currentPage;
        getSharedPreferences("prefs", MODE_PRIVATE)
                .edit()
                .putInt("currentPageProfile", currentPage)
                .apply();

        Intent intent = new Intent(ProfileActivity.this, AddActivity.class);
        startActivity(intent);
    }

    public void loadPage(int page) {
        int userId = getSharedPreferences("prefs", MODE_PRIVATE).getInt("user_id", -1);
        api.getTracks(userId, page, limit, "none", true).enqueue(new Callback<TrackResponse>() {
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
                        ImageButton xButton = itemView.findViewById(R.id.track_btn_x);
                        LinearLayout infoBlock = itemView.findViewById(R.id.info_block);
                        TextView title = itemView.findViewById(R.id.title_text);
                        TextView description = itemView.findViewById(R.id.description_text);
                        TextView date = itemView.findViewById(R.id.date_text);

                        followBtn.setTag(track.id);
                        xButton.setTag(track.id);
//                        followBtn.setVisibility(GONE);
                        xButton.setVisibility(VISIBLE);
                        title.setText(track.title);
                        String mood = MoodLabels.getLabelById(track.mood_id);
                        description.setText(track.author + " | " + mood);
                        //
                        SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        SimpleDateFormat displayFormat = new SimpleDateFormat("yy/MM/dd - HH:mm");
                        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMMM, HH:mm", new Locale("ru"));

                        try {
                            Date parsedDate = serverFormat.parse(track.timestamp);
                            String formattedDate = displayFormat.format(parsedDate);
                            date.setText(formattedDate);
                        } catch (ParseException e) {
                            date.setText("invalid date");
                        }

                        infoBlock.setOnClickListener(v -> {
                            try{
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(track.url));
                                v.getContext().startActivity(intent);
                            } catch (Exception e){
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query="+track.title+"+-+"+track.author));
                                v.getContext().startActivity(intent);
//                                    Toast.makeText(itemView.getContext(), "Невозможно открыть ссылку", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // Change like button icon if liked
                        if (followedIds.contains(track.id)) {
                            followBtn.setImageResource(R.drawable.dark_follow);
                            followBtn.setOnClickListener(v -> Unlike(v));
                        }

                        trackContainer.addView(itemView);
                    }

                    updateButtons();
                }
                View list_offset = inflater.inflate(R.layout.list_offset, trackContainer, false);
                trackContainer.addView(list_offset);
            }

            private void updateButtons() {
                if (totalPages <= 1) {
                    prevBtn.setVisibility(GONE);
                    nextBtn.setVisibility(GONE);
                } else {
                    prevBtn.setVisibility(currentPage > 1 ? VISIBLE : View.INVISIBLE);
                    nextBtn.setVisibility(currentPage < totalPages ? VISIBLE : View.INVISIBLE);
                }
            }

            @Override
            public void onFailure(Call<TrackResponse> call, Throwable t) {
                Log.e("SERVER", "FAIL: " + t.getMessage());
            }
        });
    }

    public void Delete(View v) {
        ImageButton btn = (ImageButton) v;

        // Получаем track_id, сохранённый ранее
        Object tag = btn.getTag();
        if (tag == null) {
            Toast.makeText(this, "Ошибка: нет ID трека", Toast.LENGTH_SHORT).show();
            return;
        }

        int trackId = (int) tag;

        int userId = getSharedPreferences("prefs", MODE_PRIVATE)
                .getInt("user_id", -1);
        if (userId == -1) {
            Toast.makeText(ProfileActivity.this, "Ошибка: вы не авторизованы", Toast.LENGTH_SHORT).show();
            return;
        }

//        Track track = new Track(userId, trackId);

        api.deleteTrack(new DeleteRequest(trackId)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Трек удалён", Toast.LENGTH_SHORT).show();
                    loadPage(currentPage); // перезагрузка страницы
                } else {
                    Toast.makeText(ProfileActivity.this, "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void Like(View v) {
        ImageButton likeBtn = (ImageButton) v;

        // Получаем track_id, сохранённый ранее
        Object tag = likeBtn.getTag();
        if (tag == null) {
            Toast.makeText(this, "Ошибка: нет ID трека", Toast.LENGTH_SHORT).show();
            return;
        }

        int trackId = (int) tag;

        int userId = getSharedPreferences("prefs", MODE_PRIVATE)
                .getInt("user_id", -1);
        if (userId == -1) {
            Toast.makeText(ProfileActivity.this, "Ошибка: вы не авторизованы", Toast.LENGTH_SHORT).show();
            return;
        }

        Follow follow = new Follow(userId, trackId);

        api.sendFollow(follow).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
//                    Toast.makeText(MainActivity.this, "Трек сохранён", Toast.LENGTH_SHORT).show();
                    likeBtn.setImageResource(R.drawable.dark_follow);
                    likeBtn.setOnClickListener(v -> Unlike(v));
                } else {
//                    Toast.makeText(MainActivity.this, "Уже добавлено", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void Unlike(View v) {
        ImageButton likeBtn = (ImageButton) v;

        // Получаем track_id, сохранённый ранее
        Object tag = likeBtn.getTag();
        if (tag == null) {
            Toast.makeText(this, "Ошибка: нет ID трека", Toast.LENGTH_SHORT).show();
            return;
        }

        int trackId = (int) tag;

        int userId = getSharedPreferences("prefs", MODE_PRIVATE)
                .getInt("user_id", -1);
        if (userId == -1) {
            Toast.makeText(ProfileActivity.this, "Ошибка: вы не авторизованы", Toast.LENGTH_SHORT).show();
            return;
        }

        Follow follow = new Follow(userId, trackId);

        api.sendUnfollow(follow).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
//                    Toast.makeText(MainActivity.this, "Трек больше не сохранён", Toast.LENGTH_SHORT).show();
                    likeBtn.setImageResource(R.drawable.follow);
                    likeBtn.setOnClickListener(v -> Like(v));
                } else {
                    Toast.makeText(ProfileActivity.this, "Уже добавлено", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
