package com.bonacogo.gameplate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        // inizializza firebaseapp
        FirebaseApp.initializeApp(this);
        // inizializza FirebaseAuth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // se l'utente non è null ricaricalo e vai alla mainactivity
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    goToMainActivity();
                }
                else
                    // DA FARE - controllo connessione internet
                    Toast.makeText(this, R.string.error_try_later,
                            Toast.LENGTH_LONG).show();
            });
        }
        else
            goToMainActivity();

    }

    private void goToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
