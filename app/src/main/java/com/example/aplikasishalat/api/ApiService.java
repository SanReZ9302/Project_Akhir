package com.example.aplikasishalat.api;

import com.example.aplikasishalat.model.ApiResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("v1/timingsByCity")
    Call<ApiResponse> getPrayerTimes(
            @Query("city") String city,
            @Query("country") String country,
            @Query("method") int method
    );
}