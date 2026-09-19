package com.wormgpt;

import okhttp3.*;
import okhttp3.sse.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class AtriaClient {
    private static final String API_URL = "https://api.atria-asi.ai/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient.Builder()
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build();

    public interface StreamCallback {
        void onChunk(String text);
        void onComplete();
        void onError(String error);
    }

    public void streamChat(String apiKey, JSONArray messages, double temp, int maxTokens, StreamCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", "Atria-Dawn-Preview");
            body.put("messages", messages);
            body.put("temperature", temp);
            body.put("max_tokens", maxTokens);
            body.put("stream", true);

            Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

            EventSources.createFactory(client).newEventSource(request, new EventSourceListener() {
                @Override
                public void onEvent(EventSource es, String id, String type, String data) {
                    if (data.equals("[DONE]")) {
                        callback.onComplete();
                        return;
                    }
                    try {
                        JSONObject json = new JSONObject(data);
                        String delta = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("delta")
                            .optString("content", "");
                        if (!delta.isEmpty()) callback.onChunk(delta);
                    } catch (Exception e) {}
                }

                @Override
                public void onFailure(EventSource es, Throwable t, Response response) {
                    callback.onError(t != null ? t.getMessage() : "Error");
                }

                @Override
                public void onClosed(EventSource es) {
                    callback.onComplete();
                }
            });
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }
}
