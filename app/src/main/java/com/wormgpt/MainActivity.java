package com.wormgpt;

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
    private JSONArray history = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        atria = new AtriaClient();
        settings = new SettingsManager(this);

        chatContainer = findViewById(R.id.chatContainer);
        scrollView = findViewById(R.id.scrollView);
        inputField = findViewById(R.id.inputField);
        ImageButton sendBtn = findViewById(R.id.sendBtn);

        sendBtn.setOnClickListener(v -> sendMessage());

        if (settings.getApiKey().isEmpty()) {
            showSettingsDialog();
        }
    }

    private void sendMessage() {
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) return;

        inputField.setText("");
        addMessage("user", text);

        try {
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", text);
            history.put(msg);
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
                    } catch (Exception e) {}
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> botView.setText("Error: " + error));
                }
            });
    }

    private TextView addMessage(String role, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(24, 24, 24, 24);
        tv.setTextSize(16);
        tv.setTextColor(role.equals("user") ? 0xFF00FF41 : 0xFFE0E0E0);
        tv.setBackgroundColor(role.equals("user") ? 0xFF1A3A1A : 0xFF1A0A2A);
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
        b.setView(input);
        b.setPositiveButton("Save", (d, w) -> {
            settings.setApiKey(input.getText().toString().trim());
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
        b.show();
    }
}
