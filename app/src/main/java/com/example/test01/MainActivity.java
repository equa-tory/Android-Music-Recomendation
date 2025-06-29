package com.example.test01;


import com.example.test01.PasswordUtil;

import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.test01.MoodLabels;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    public static int savedPage = 1;
    int currentPage = 1;
    int totalPages = 1;
    final int limit = 10;
    String currentSort = "none";
    ImageButton prevBtn;
    ImageButton nextBtn;
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
                .baseUrl(ServerConfig.SERVER_ADDRESS) // важно! для эмулятора Android = localhost
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(NetApi.class);

        prevBtn.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                loadPage(currentPage, currentSort);
            }
        });

        nextBtn.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadPage(currentPage, currentSort);
            }
        });

        ImageButton sortButton = findViewById(R.id.sort_button);
        sortButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, v);
            popup.getMenu().add("Creation Date");
//            popup.getMenu().add("By mood");// TODO redo
            popup.getMenu().add("Followed");
            popup.getMenu().add("Most popular");
            popup.getMenu().add("Music of the week");
            for (String moodString :
                    MoodLabels.labels) {
                popup.getMenu().add(moodString);
            }

            popup.setOnMenuItemClickListener(item -> {
                String selected = item.getTitle().toString().toLowerCase();

                // fixing the duplicates
                String lastSort = currentSort;
                switch (selected){
                    case "creation date": {
                        currentSort = "none";
                        break;
                    }
                    case "by mood": {
                        currentSort = "mood";
                        break;
                    }
                    case "followed": {
                        currentSort = "followed";
                        break;
                    }
                    case "most popular": {
                        currentSort = "popular";
                        break;
                    }
                    case "music of the week": {
                        currentSort = "week";
                        break;
                    }
                    case "\uD83D\uDCA5 drive": {
                        currentSort = "mood:0";
                        break;
                    }
                    case "🛋️ chill": {
                        currentSort = "mood:1";
                        break;
                    }
                    case "\uD83D\uDC94 sad": {
                        currentSort = "mood:2";
                        break;
                    }
                    case "❄ christmas": {
                        currentSort = "mood:3";
                        break;
                    }
                    case "\uD83D\uDC7B other": {
                        currentSort = "mood:4";
                        break;
                    }
                }
//                currentSort = selected; // переменная сортировки
                if(!Objects.equals(lastSort, currentSort)) loadPage(1, currentSort); // загружаем с первой страницы
                return true;
            });

            popup.show();
        });

        loadPage(currentPage, currentSort);
    }

    public void loadPage(int page, String sortingType) {
        int userId = getSharedPreferences("prefs", MODE_PRIVATE).getInt("user_id", -1);
        api.getTracks(userId, page, limit, sortingType, false).enqueue(new Callback<TrackResponse>() {
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
                        LinearLayout infoBlock = itemView.findViewById(R.id.info_block);
                        TextView title = itemView.findViewById(R.id.title_text);
                        TextView description = itemView.findViewById(R.id.description_text);
                        TextView date = itemView.findViewById(R.id.date_text);

                        followBtn.setTag(track.id);
                        title.setText(track.title);
//                        description.setText(track.author + " | mood: " + track.mood);
                        String mood = "";
                        mood = MoodLabels.labels[track.mood];
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
//                                Toast.makeText(itemView.getContext(), "Невозможно открыть ссылку", Toast.LENGTH_SHORT).show();
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
        if (totalPages <= 1) {
            prevBtn.setVisibility(View.GONE);
            nextBtn.setVisibility(View.GONE);
        } else {
            prevBtn.setVisibility(currentPage > 1 ? View.VISIBLE : View.INVISIBLE);
            nextBtn.setVisibility(currentPage < totalPages ? View.VISIBLE : View.INVISIBLE);
        }
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
            Toast.makeText(MainActivity.this, "Ошибка: вы не авторизованы", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(MainActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(MainActivity.this, "Ошибка: вы не авторизованы", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MainActivity.this, "Уже добавлено", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show();
            }
        });
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

    public void Profile(View v){
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
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
                .baseUrl(ServerConfig.SERVER_ADDRESS)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NetApi api = retrofit.create(NetApi.class);

        String username = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("username", "");
        String password = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("password", "");
        User user = new User(username, PasswordUtil.hashPassword(password));

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