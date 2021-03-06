package com.jlweather.android.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jlweather.android.WeatherActivity;
import com.jlweather.android.gson.AQI;
import com.jlweather.android.gson.Weather;
import com.jlweather.android.util.HttpUtil;
import com.jlweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();
        AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
        int anHour = 8 * 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime()+anHour;
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent,flags,startId);
    }

    private void updateWeather(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString  = prefs.getString("weather",null);

        if(weatherString != null){
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            final String userKey = "3cd2fbbf24684b45acc8527551227cc1";

            String weatherUrl = "https://free-api.heweather.com/s6/weather?"
                    +"location="+weatherId
                    +"&"+"key="+userKey;
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weatherTemp = Utility.handleWeatherResponse(responseText);
                    if(weatherTemp != null && "ok".equals(weatherTemp.status)){
                        SharedPreferences.Editor editor = PreferenceManager
                                .getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather",responseText);
                        editor.apply();
                    }

                }
            });

            String aqiUrl = "https://free-api.heweather.com/s6/air?"
                    +"location="+weatherId
                    +"&"+"key="+userKey;
            HttpUtil.sendOkHttpRequest(aqiUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    AQI aqiTemp = Utility.handleAQIResponse(responseText);
                    if(aqiTemp != null && "ok".equals(aqiTemp.status)){
                        SharedPreferences.Editor editor = PreferenceManager
                                .getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("aqi",responseText);
                        editor.apply();
                    }
                }
            });


        }

    }

    private void updateBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";

        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.d("Bing_Pic//ppz", "AutoUpdate onFailure: --- ");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
            }
        });

    }
}
