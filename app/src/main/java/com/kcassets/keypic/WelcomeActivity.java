package com.kcassets.keypic;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class WelcomeActivity extends AppCompatActivity {


    /***********************************************************
     * Initializations
     **********************************************************/
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    TextView name,email;
    Button signOutBtn;
    Button emcBtn;
    Button amazonBtn;


    /***********************************************************
     * onCreate
     **********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_screen);


        /***********************************************************
         * Local Initializations and Passing Login
         **********************************************************/
        ImageView backArrow = findViewById(R.id.backArrow);
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        signOutBtn = findViewById(R.id.signOutBtn);
        emcBtn = findViewById(R.id.emcBtn);
        amazonBtn = findViewById(R.id.amazonBtn);

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account!=null){
            String personName = account.getDisplayName();
            String personEmail = account.getEmail();
            name.setText(personName);
            email.setText(personEmail);
        }


        /***********************************************************
         * Buttons
         **********************************************************/
        emcBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(WelcomeActivity.this, EmcActivity.class));
            }
        });

        amazonBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(WelcomeActivity.this, AmazonActivity.class));
            }
        });

        signOutBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        backArrow.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
    }


    /***********************************************************
     * Sign Out Command
     **********************************************************/
    void signOut(){
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                finish();
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            }
        });
    }
}
