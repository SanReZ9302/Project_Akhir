package com.example.aplikasishalat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Address;
import android.location.Geocoder;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.text.DecimalFormat;

public class KiblatActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView ivCompass, ivQiblaNeedle;
    private TextView tvLocation, tvKiblatDegree;
    private FusedLocationProviderClient fusedLocationClient;

    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float azimuth = 0f;
    private float currentAzimuth = 0f;

    private float kiblatDegree = 0f;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final double MECCA_LATITUDE = 21.4225;
    private static final double MECCA_LONGITUDE = 39.8262;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kiblat);

        ivCompass = findViewById(R.id.iv_compass);
        ivQiblaNeedle = findViewById(R.id.iv_qibla_needle);
        tvLocation = findViewById(R.id.tv_location);
        tvKiblatDegree = findViewById(R.id.tv_kiblat_degree);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        checkLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                tvLocation.setText("Izin lokasi ditolak.");
                tvKiblatDegree.setText("Arah Kiblat: -");
                Toast.makeText(this, "Izin lokasi diperlukan untuk fitur ini", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CurrentLocationRequest request = new CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build();
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(request, cancellationTokenSource.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        updateLocationUI(location);
                        calculateKiblatDirection(location);
                    } else {
                        tvLocation.setText("Gagal mendapatkan lokasi. Pastikan GPS aktif.");
                    }
                });
    }

    private void updateLocationUI(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String cityName = address.getLocality();
                String countryName = address.getCountryName();

                String locationText = "Lokasi: " + cityName + ", " + countryName;
                tvLocation.setText(locationText);

            } else {
                String locationText = "Lokasi: " + new DecimalFormat("0.000").format(location.getLatitude()) + ", " + new DecimalFormat("0.000").format(location.getLongitude());
                tvLocation.setText(locationText);
            }
        } catch (IOException e) {
            tvLocation.setText("Lokasi: Gagal mendapatkan nama lokasi");
        }
    }

    private void calculateKiblatDirection(Location location) {
        double lonDiff = Math.toRadians(MECCA_LONGITUDE - location.getLongitude());
        double latUserRad = Math.toRadians(location.getLatitude());
        double latMeccaRad = Math.toRadians(MECCA_LATITUDE);

        double y = Math.sin(lonDiff) * Math.cos(latMeccaRad);
        double x = Math.cos(latUserRad) * Math.sin(latMeccaRad) - Math.sin(latUserRad) * Math.cos(latMeccaRad) * Math.cos(lonDiff);
        double bearing = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;

        this.kiblatDegree = (float) bearing;
        tvKiblatDegree.setText("Arah Kiblat: " + new DecimalFormat("0.0").format(kiblatDegree) + "° dari Utara");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * event.values[0];
            geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * event.values[1];
            geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * event.values[2];
        }

        float[] R = new float[9];
        float[] I = new float[9];
        boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);

        if (success) {
            float[] orientation = new float[3];
            SensorManager.getOrientation(R, orientation);
            azimuth = (float) Math.toDegrees(orientation[0]);
            azimuth = (azimuth + 360) % 360;
            ivCompass.setRotation(-azimuth);
            ivQiblaNeedle.setRotation(-azimuth + kiblatDegree);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}