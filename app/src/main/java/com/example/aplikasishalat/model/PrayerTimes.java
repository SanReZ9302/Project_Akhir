package com.example.aplikasishalat.model;

import com.google.gson.annotations.SerializedName;

public class PrayerTimes {
    @SerializedName("Fajr") public String fajr;
    @SerializedName("Dhuhr") public String dhuhr;
    @SerializedName("Asr") public String asr;
    @SerializedName("Maghrib") public String maghrib;
    @SerializedName("Isha") public String isha;
}