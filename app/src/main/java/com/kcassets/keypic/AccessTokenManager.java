package com.kcassets.keypic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AccessTokenManager {
    private static final String PREF_ACCESS_TOKEN_EXPIRATION = "access_token_expiration";
    private static final String PREF_REFRESH_TOKEN = "refresh_token";
    private static final String PREF_ACCESS_TOKEN = "access_token";
    private Context context;
    private SharedPreferences sharedPreferences;

    public AccessTokenManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
    }

    public void checkAccessTokenExpiration() {
        long expirationTime = sharedPreferences.getLong(PREF_ACCESS_TOKEN_EXPIRATION, 0);
        long currentTime = System.currentTimeMillis() / 1000;

        // Check if the current time is within a threshold of the expiration time
        long threshold = 60; // in seconds, adjust as needed

        if (currentTime >= expirationTime - threshold) {
            // Access token has expired, refresh the access token using the refresh token
            String refreshToken = sharedPreferences.getString(PREF_REFRESH_TOKEN, null);
            if (refreshToken != null) {
                refreshAccessToken(refreshToken);
            } else {
                Intent intent = new Intent(context, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("message", "Session has expired. Sign in required.");
                context.startActivity(intent);
            }
        }
    }

    private void refreshAccessToken(String refreshToken) {
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new FormBody.Builder()
                .add("client_id", "307019473468-vig8i8bli5iff5ss9lrq2mn67dt6rl31.apps.googleusercontent.com")
                .add("client_secret", "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")
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();

        Request request = new Request.Builder()
                .url("https://www.googleapis.com/oauth2/v4/token")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
                // Handle error case, such as displaying a message to the user
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Gson gson = new Gson();
                    JsonObject jsonObject;

                    try {
                        jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

                        JsonElement accessTokenElement = jsonObject.get("access_token");
                        JsonElement expiresInElement = jsonObject.get("expires_in");

                        String newAccessToken = accessTokenElement != null ? accessTokenElement.getAsString() : null;
                        long expiresIn = expiresInElement != null ? expiresInElement.getAsLong() : 0;

                        long newExpirationTime = System.currentTimeMillis() / 1000 + expiresIn;

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(PREF_ACCESS_TOKEN, newAccessToken);
                        editor.putLong(PREF_ACCESS_TOKEN_EXPIRATION, newExpirationTime);
                        editor.apply();

                    } catch (JsonParseException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Handle error response
                }
            }
        });
    }
}