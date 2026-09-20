package com.wormgpt;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private AtriaClient atria;
    private SettingsManager settings;
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private EditText inputField;
    private JSONArray history;
    private SharedPreferences prefs;
    private static final String PREF_NAME = "wormgpt_history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        atria = new AtriaClient();
        settings = new SettingsManager(this);
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        chatContainer = findViewById(R.id.chatContainer);
        scrollView = findViewById(R.id.scrollView);
        inputField = findViewById(R.id.inputField);
        ImageButton sendBtn = findViewById(R.id.sendBtn);
        Button settingsBtn = findViewById(R.id.settingsBtn);

        sendBtn.setOnClickListener(v -> sendMessage());
        settingsBtn.setOnClickListener(v -> showSettingsDialog());

        // تحميل المحادثة السابقة
        loadHistory();

        if (settings.getApiKey().isEmpty()) {
            showSettingsDialog();
        }
    }

    private void loadHistory() {
        String saved = prefs.getString("history", "[]");
        try {
            history = new JSONArray(saved);
            for (int i = 0; i < history.length(); i++) {
                JSONObject msg = history.getJSONObject(i);
                addMessage(msg.getString("role"), msg.getString("content"));
            }
            if (history.length() == 0) {
                addSystemMessage("🐉 WormGPT جاهز");
            }
        } catch (Exception e) {
            history = new JSONArray();
            addSystemMessage("🐉 WormGPT جاهز");
        }
    }

    private void saveHistory() {
        prefs.edit().putString("history", history.toString()).apply();
    }

    private void clearHistory() {
        history = new JSONArray();
        chatContainer.removeAllViews();
        prefs.edit().remove("history").apply();
        addSystemMessage("تم مسح المحادثة");
    }

    private void addSystemMessage(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(24, 24, 24, 24);
        tv.setTextSize(14);
        tv.setTextColor(0xFFFF0040);
        tv.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);
        tv.setLayoutParams(params);
        chatContainer.addView(tv);
    }

    private void sendMessage() {
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) return;
        if (settings.getApiKey().isEmpty()) {
            Toast.makeText(this, "أضف مفتاح API أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        inputField.setText("");
        addMessage("user", text);

        try {
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", text);
            history.put(msg);
            saveHistory();
        } catch (Exception e) {}

        final TextView botView = addMessage("bot", "");

        atria.streamChat(settings.getApiKey(), history, settings.getTemperature(), settings.getMaxTokens(),
            new AtriaClient.StreamCallback() {
                StringBuilder fullText = new StringBuilder();
                @Override
                public void onChunk(String chunk) {
                    fullText.append(chunk);
                    runOnUiThread(() -> {
                        botView.setText(fullText.toString());
                        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                    });
                }
                @Override
                public void onComplete() {
                    try {
                        JSONObject msg = new JSONObject();
                        msg.put("role", "assistant");
                        msg.put("content", fullText.toString());
                        history.put(msg);
                        saveHistory();
                    } catch (Exception e) {}
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> botView.setText("خطأ: " + error));
                }
            });
    }

    private TextView addMessage(String role, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(24, 24, 24, 24);
        tv.setTextSize(15);
        tv.setTextColor(role.equals("user") ? 0xFFFF6666 : 0xFFFFFFFF);
        tv.setBackgroundColor(role.equals("user") ? 0xFF2A0A0A : 0xFF1A0000);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 8, 16, 8);
        tv.setLayoutParams(params);
        chatContainer.addView(tv);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        return tv;
    }

    private void showSettingsDialog() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("Atria API Key");
        final EditText input = new EditText(this);
        input.setHint("atr_...");
        input.setText(settings.getApiKey());
        input.setTextColor(0xFFFFFFFF);
        b.setView(input);

        b.setPositiveButton("حفظ", (d, w) -> {
            settings.setApiKey(input.getText().toString().trim());
            Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show();
        });

        b.setNeutralButton("مسح المحادثة", (d, w) -> clearHistory());
        b.setNegativeButton("إلغاء", null);
        b.show();
    }
}
