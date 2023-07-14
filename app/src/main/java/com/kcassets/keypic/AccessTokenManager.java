package com.kcassets.keypic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class AccessTokenManager {
    private static final String PREF_ACCESS_TOKEN_EXPIRATION = "access_token_expiration";
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
            // Access token has expired, navigate to login activity
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
            GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
                    .addOnCompleteListener(task ->  {
                        Intent intent = new Intent(context, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("message", "Session has expired. Sign in required.");
                        context.startActivity(intent);
                    });
        }
    }
}