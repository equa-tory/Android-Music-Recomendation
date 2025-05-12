package com.example.test01;

import android.content.Intent;
import android.os.Bundle;
import android.util.AndroidException;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        // ============================================
        View fieldLogin = findViewById(R.id.item_field_01);
        View fieldPass = findViewById(R.id.item_field_02);

        TextView fi_login = fieldLogin.findViewById(R.id.field_title);
        fi_login.setText("Login");

        TextView fi_pass = fieldPass.findViewById(R.id.field_title);
        fi_pass.setText("Password");
    }

    public void SubmitUser(View v){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ServerConfig.SERVER_ADDRESS)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NetApi api = retrofit.create(NetApi.class);

        View fieldItemLogin = findViewById(R.id.item_field_01);
        View fieldItemPassword = findViewById(R.id.item_field_02);

        EditText fi_login = fieldItemLogin.findViewById(R.id.field_edittext);
        EditText fi_password = fieldItemPassword.findViewById(R.id.field_edittext);

        String login = fi_login.getText().toString();
        String password = fi_password.getText().toString();

        User user = new User(login, password);

        api.sendUser(user).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int userId = response.body().user_id;

                    getSharedPreferences("prefs", MODE_PRIVATE)
                            .edit()
                            .putInt("user_id", userId)
                            .putString("username", login)
                            .putString("password", password)
                            .putBoolean("isLoggedIn", true)
                            .apply();

                    ActivityToMain(v);
                } else if (response.code() == 401) {
                    Log.e("SERVER", "Invalid password");
                    Toast.makeText(getApplicationContext(), "Неверный пароль", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("SERVER", "Unknown error: " + response.code());
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

    public void ActivityToMain(View v) {
        getSharedPreferences("prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", true)
                .apply();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
    }
}
