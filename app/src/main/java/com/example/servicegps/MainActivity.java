package com.example.servicegps;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    private Button btnStart, btnStop;
    private TextView textCoord;
    private BroadcastReceiver broadcastReceiver;
    private double longitude, latitude;

    private static final String TAG = "UsingThingspeakAPI";
    private static final String THINGSPEAK_CHANNEL_ID = "1011235";
    private static final String THINGSPEAK_API_KEY = "VR3D6BWTINF612L0";
    private static final String THINGSPEAK_API_KEY_STRING = "PVA8QRXYMGK5Z2XS";

    private static final String THINGSPEAK_CHANNEL_URL = "https://api.thingspeak.com/channels/";
    private static final String THINGSPEAK_FIELDS = "/fields/";
    private static final String THINGSPEAK_JSON = ".json?api_key";
    private static final String THINGSPEAK_RESULTS = "&results";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.startBtn);
        btnStop = findViewById(R.id.stopBtn);
        textCoord = findViewById(R.id.coordText);
        
        if(!checkPermissions())
            enableButtons();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    longitude = (double) intent.getExtras().get("longitude");
                    latitude = (double) intent.getExtras().get("latitude");

                    try {
                        new FetchThingspeakTask().execute();
                    }
                    catch(Exception e){
                        Log.e("ERROR", e.getMessage(), e);
                    }
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter("locationUpdate"));
    }

    class FetchThingspeakTask extends AsyncTask<Void, Void, String> {
        protected void onPreExecute() {
        }

        protected String doInBackground(Void... urls) {
            try {
                URL url = new URL(THINGSPEAK_CHANNEL_URL + THINGSPEAK_CHANNEL_ID +
                                    THINGSPEAK_FIELDS + 1 + "&" + 2 + THINGSPEAK_JSON + "=" + THINGSPEAK_API_KEY_STRING +
                                    THINGSPEAK_RESULTS + "=" + 1);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response) {
            if(response == null) {
                Toast.makeText(MainActivity.this, "There was an error", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject channel = (JSONObject) new JSONTokener(response).nextValue();
                JSONArray feeds = channel.getJSONArray("feeds");
                JSONObject data = feeds.getJSONObject(0);
                double latitude = data.getDouble("field1");
                double longitude = data.getDouble("field2");
                textCoord.append("\n" + latitude + ", " + longitude);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
    }

    private void enableButtons() {
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ServiceGPS.class);
                startService(intent);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ServiceGPS.class);
                stopService(intent);
            }
        });
    }

    private boolean checkPermissions() {
        if(Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 100) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED &&
            grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                enableButtons();
            } else {
                checkPermissions();
            }
        }
    }
}

