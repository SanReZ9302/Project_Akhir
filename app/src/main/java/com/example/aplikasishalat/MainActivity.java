package com.example.aplikasishalat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.aplikasishalat.api.ApiService;
import com.example.aplikasishalat.model.ApiResponse;
import com.example.aplikasishalat.model.PrayerTimes;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private TextView tvCity, tvDate, tvFajr, tvDhuhr, tvAsr, tvMaghrib, tvIsha;
    private Switch switchFajr, switchDhuhr, switchAsr, switchMaghrib, switchIsha;
    private Button btnKiblat;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Izin notifikasi ditolak. Alarm tidak akan muncul.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        askNotificationPermission();

        String currentDate = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID")).format(new Date());
        tvDate.setText(currentDate);

        btnKiblat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, KiblatActivity.class);
            startActivity(intent);
        });

        fetchPrayerTimes("Semarang", "Indonesia");
    }

    private void initViews() {
        tvCity = findViewById(R.id.tv_city);
        tvDate = findViewById(R.id.tv_date);
        tvFajr = findViewById(R.id.tv_fajr);
        tvDhuhr = findViewById(R.id.tv_dhuhr);
        tvAsr = findViewById(R.id.tv_asr);
        tvMaghrib = findViewById(R.id.tv_maghrib);
        tvIsha = findViewById(R.id.tv_isha);
        switchFajr = findViewById(R.id.switch_fajr);
        switchDhuhr = findViewById(R.id.switch_dhuhr);
        switchAsr = findViewById(R.id.switch_asr);
        switchMaghrib = findViewById(R.id.switch_maghrib);
        switchIsha = findViewById(R.id.switch_isha);
        btnKiblat = findViewById(R.id.btn_kiblat);
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void fetchPrayerTimes(String city, String country) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.aladhan.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<ApiResponse> call = apiService.getPrayerTimes(city, country, 20);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PrayerTimes timings = response.body().data.timings;
                    updateUI(timings, city);
                    setupAlarms(timings);
                } else {
                    Toast.makeText(MainActivity.this, "Gagal memuat data. Kode: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Gagal terhubung ke server: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(PrayerTimes timings, String city) {
        tvCity.setText(city);
        tvFajr.setText("Subuh: " + timings.fajr);
        tvDhuhr.setText("Dzuhur: " + timings.dhuhr);
        tvAsr.setText("Ashar: " + timings.asr);
        tvMaghrib.setText("Maghrib: " + timings.maghrib);
        tvIsha.setText("Isya: " + timings.isha);
    }

    private void setupAlarms(PrayerTimes timings) {
        setupAlarmSwitch(switchFajr, "Subuh", timings.fajr, 1);
        setupAlarmSwitch(switchDhuhr, "Dzuhur", timings.dhuhr, 2);
        setupAlarmSwitch(switchAsr, "Ashar", timings.asr, 3);
        setupAlarmSwitch(switchMaghrib, "Maghrib", timings.maghrib, 4);
        setupAlarmSwitch(switchIsha, "Isya", timings.isha, 5);
    }

    private void setupAlarmSwitch(Switch alarmSwitch, String prayerName, String prayerTime, int requestCode) {
        alarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "Izin alarm presisi diperlukan.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);

                    alarmSwitch.setChecked(false);
                } else {
                    // Jika izin ada, setel alarm
                    setAlarm(prayerName, prayerTime, requestCode);
                    Toast.makeText(this, "Alarm " + prayerName + " diaktifkan", Toast.LENGTH_SHORT).show();
                }
            } else {
                cancelAlarm(requestCode);
                Toast.makeText(this, "Alarm " + prayerName + " dinonaktifkan", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setAlarm(String prayerName, String prayerTime, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_PRAYER_NAME, prayerName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String[] timeParts = prayerTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
    private void cancelAlarm(int requestCode) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}