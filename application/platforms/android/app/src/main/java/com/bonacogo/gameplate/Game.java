package com.bonacogo.gameplate;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.bonacogo.gameplate.dialog.NickChangeDialog;
import com.bonacogo.gameplate.fragment.HomeFragment;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.cordova.CordovaActivity;
import org.json.JSONException;

import androidx.annotation.NonNull;

public class Game extends CordovaActivity {

    private boolean finish = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_game);

        // inizializzazione firebaseauth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        initLaunch(user);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish = true;
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initLaunch(FirebaseUser user) {
        // se l'user non è null esegui un reload e passa alla verifica della mail
        // altrimenti chiudi l'activity con errore generico 0
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String nickname = user.getDisplayName();
                    verifyEmail(user, nickname);
                } else
                    close(0);
            });
        }
        else {
            close(0);
        }
    }

    private void verifyEmail(FirebaseUser user, String nickname) {
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                String provider = task.getResult().getSignInProvider();
                // se il provider è fb
                if (provider.equals("facebook.com")) {
                    // se il displayName è null è quindi verificata anche la mail (operazione eseguita in contemporanea nell'onCreate function) ed
                    // è quindi stato resettato il nickname a fronte del problema "nick già in uso"
                    if (nickname == null)
                        nickFbeExist();
                    // se il displayName non è null non è detto che la mail sia verificata (nel caso in cui si accede prima dell'onCreate function)
                    // quindi se è verificata continua con il controllo sulla sincronia altrimenti segnala errore
                    else if (user.isEmailVerified())
                        checkNick(user.getUid(), nickname);
                    // chiudi con errore -2, stato di loading account
                    else
                        close(-2);
                }
                // se il provider è password
                else
                    // e se la mail non è verificata chiudi con errore -1, email non verificata
                    if (!user.isEmailVerified())
                        close(-1);
                    else
                        // e se la mail è verificata ma il nickname è null chiedi di impostarne uno
                        if (nickname == null)
                            launchChangeDialog(null);
                        // altrimenti fai un ulteriore controllo sulla sincronia di quest'ultimo
                        else
                            checkNick(user.getUid(), nickname);
            }
            else {
                close(0);
            }
        });
    }

    private void launchChangeDialog(String nickUsed) {
        // controllo di chiusura per chiamata asincrona
        if (finish)
            return;

        // mostra la finestra di cambio del nick con errore true nel caso in cui nickUsed è diverso da null, se nickUsed è null non verrà mostrato alcun errore
        NickChangeDialog nickChangeDialog = new NickChangeDialog(this, nickUsed, true, new NickChangeDialog.DialogListener() {
            @Override
            public void onCompleted() {
                launch();
            }

            @Override
            public void onCanceled(boolean error) {
                if (error)
                    close(0);
                else
                    close(1);
            }
        });
        nickChangeDialog.show();
    }

    private void launch() {
        // controllo di chiusura per chiamata asincrona
        if (finish)
            return;

        Intent intent = getIntent();

        // launchUrl è impostato in <content src="..." /> in config.xml
        launchUrl += intent.getStringExtra(HomeFragment.EXTRA_MESSAGE);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        loadUrl(launchUrl);
    }

    private void nickFbeExist() {
        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
                (object, response) -> {
                    try {
                        // recupera il nome da fb e comunica che è già in uso
                        String first_name = object.getString("first_name");
                        String last_name = object.getString("last_name");
                        launchChangeDialog(first_name + " " + last_name);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        close(0);
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "first_name,last_name");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void checkNick(String uid, String nickname) {
        // questo controllo effettua un paragone tra il nickname del modulo di auth di firebase e il nodo nickname
        // scritto sul database perchè, a fronte di una successiva modifica del nickname, la funzione firebase potrebbe
        // non aver concluso il suo compito. quindi per controllare lo stato della funzione viene effettuata da quest'ultima
        // come prima istruzione la modifica del nodo nel DB e come ultima istruzione la modifica del nickname nel modulo
        // di auth di firebase, se il paragone è verificato la funzione è quindi conclusa altrimenti segnala errore
        // di loading
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference nicknameReference = database.getReference()
                .child("users").child(uid).child("nickname");
        nicknameReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String nick = dataSnapshot.getValue(String.class);
                if (nick == null || !nick.equals(nickname))
                    close(-2);
                else {
                    launch();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                close(0);
            }
        });
    }

    private void close(int error) {
        // chiusura con la segnalazione degli errori
        switch (error) {
            case -2:
                Toast.makeText(this, R.string.error_try_later,
                        Toast.LENGTH_LONG).show();
                break;
            case -1:
                Toast.makeText(this, R.string.error_email_not_verified,
                        Toast.LENGTH_LONG).show();
                break;
            case 0:
                Toast.makeText(this, R.string.error,
                        Toast.LENGTH_LONG).show();
                break;
            default:
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        // disabilito la chiusura della activity con la pressione del tasto back
    }
}
