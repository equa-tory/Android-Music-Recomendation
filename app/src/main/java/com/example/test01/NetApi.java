package com.example.test01;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface NetApi {
    @POST("/submit")
    Call<Void> sendTrack(@Body Track track);

    @POST("/user")
    Call<LoginResponse> sendUser(@Body User user);

    @POST("/follow")
    Call<Void> sendFollow(@Body Follow follow);

    @POST("/unfollow")
    Call<Void> sendUnfollow(@Body Follow follow);

    @GET("/tracks")
    Call<TrackResponse> getTracks(
            @Query("user_id") int userId,
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("sort") String sort,
            @Query("profile") Boolean profile
    );

    @POST("/delete")
    Call<Void> deleteTrack(@Body DeleteRequest request);

    @GET("/moods")
    Call<List<Mood>> getMoods();
}