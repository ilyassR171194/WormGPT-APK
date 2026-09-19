package com.wormgpt;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences("wormgpt", Context.MODE_PRIVATE);
    }

    public String getApiKey() { return prefs.getString("api_key", ""); }
    public void setApiKey(String key) { prefs.edit().putString("api_key", key).apply(); }
    public double getTemperature() { return prefs.getFloat("temp", 0.7f); }
    public void setTemperature(double t) { prefs.edit().putFloat("temp", (float) t).apply(); }
    public int getMaxTokens() { return prefs.getInt("max_tokens", 1024); }
    public void setMaxTokens(int t) { prefs.edit().putInt("max_tokens", t).apply(); }
}
