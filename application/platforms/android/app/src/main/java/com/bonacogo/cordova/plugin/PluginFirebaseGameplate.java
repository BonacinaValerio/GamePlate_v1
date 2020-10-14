package com.bonacogo.cordova.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bonacogo.gameplate.MainActivity;
import com.bonacogo.gameplate.fragment.HomeFragment;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * This class echoes a string called from JavaScript.
 */
public class PluginFirebaseGameplate extends CordovaPlugin {
    private static final String TAG = "PluginFirebaseGameplate";

    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private FirebaseFunctions mFunctions;

    @Override
    protected void pluginInitialize() {
        // istanzio le variabili del plugin
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mFunctions = FirebaseFunctions.getInstance();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        switch (action) {
            case "saveScore":
                return saveScore(args, callbackContext);
            case "getBest":
                return getBest(callbackContext);
            case "getRank":
                return getRank(args, callbackContext);
            case "exit":
                try {
                    // avvia l'activity MainActivity
                    Context context = cordova.getActivity().getApplicationContext();
                    Intent intent = new Intent(context, MainActivity.class);
                    this.cordova.getActivity().startActivity(intent);
                    // chiudi l'activity Game
                    Activity activity = this.cordova.getActivity();
                    activity.finish();
                    // ritorna stato di OK
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, 0));
                } catch (Exception e) {
                    // ritorna stato di ERRORE
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, 1));
                }
                return true;
            default:
                return false;
        }
    }

    private boolean saveScore(JSONArray args, final CallbackContext callbackContext) {
        // estrazione parametri
        final String game = this.cordova.getActivity().getIntent().getStringExtra(HomeFragment.EXTRA_MESSAGE);
        final int score = args.optInt(0);
        // creo l'oggetto da dare alla funzione Firebase
        final Map<String, Object> data = new HashMap<>();
        data.put("game", game);
        data.put("score", score);
        // chiamo il metodo per chiamare la funzione
        callFunction(data, "onCreateSession")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                        // se il task ha avuto successo risponde alla WebView con la risposta della funzione
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, task.getResult()));
                    else {
                        // altrimenti fai un print dell'errore e rispondi alla WebView con quell'errore
                        Exception e = task.getException();
                        Log.e(TAG, "onCreateSession:onFailure", e);
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            callbackContext.sendPluginResult(convertToPluginResult(ffe));
                        }
                    }
                });
        return true;
    }


    private boolean getBest(final CallbackContext callbackContext) {
        // estrazione parametri
        final String game = this.cordova.getActivity().getIntent().getStringExtra(HomeFragment.EXTRA_MESSAGE);
        final String userId = mAuth.getUid();
        // prendo il DBReference del numero week del gioco e lo estraggo
        DatabaseReference mWeekReference = database.getReference()
                .child("game").child(game).child("week");
        mWeekReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot week) {
                String weekly = "weekly"+week.getValue();
                // prendo il DBReference dei punteggi dell'utente nel gioco e li estraggo
                DatabaseReference mBestScoreReference = database.getReference()
                        .child("users").child(userId).child("games").child(game);
                mBestScoreReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot bestScore) {
                        int i2 = -1, i1 = -1;
                        // imposto dei valori di default nel caso il campo estratto sia null
                        if (bestScore.child("global").getValue() != null) {
                            i1 = bestScore.child("global").getValue(Integer.class);
                            if (bestScore.child(weekly).getValue() != null)
                                i2 = bestScore.child(weekly).getValue(Integer.class);
                        }

                        JSONObject data = new JSONObject();
                        try {
                            data.put("global", i1);
                            data.put("weekly", i2);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // restituisco alla webview cordova lo stato di OK con i punteggi trovati
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, data));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callbackContext.sendPluginResult(convertToPluginResult(databaseError));
                    }
                });
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                callbackContext.sendPluginResult(convertToPluginResult(databaseError));
            }
        });

        return true;
    }

    private boolean getRank(JSONArray args, final CallbackContext callbackContext) {
        // estrazione parametri
        final String game = this.cordova.getActivity().getIntent().getStringExtra(HomeFragment.EXTRA_MESSAGE);
        final String score = args.optString(0);
        final Integer crc32GlobalSaved = args.optInt(1);
        final Integer crc32WeeklySaved = args.optInt(2);

        final Map<String, Object> data = new HashMap<>();
        data.put("game", game);
        if (score != null && !score.equals("null"))
            data.put("score", Integer.valueOf(score));
        data.put("crc32Global", crc32GlobalSaved);
        data.put("crc32Weekly", crc32WeeklySaved);
        // chiamo il metodo per chiamare la funzione
        callFunction(data, "onGetRank")
                .addOnCompleteListener(new OnCompleteListener<JSONObject>() {
                    @Override
                    public void onComplete(@NonNull Task<JSONObject> task) {
                        if (task.isSuccessful())
                            // se il task ha avuto successo risponde alla WebView con la risposta della funzione
                            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, task.getResult()));
                        else {
                            // altrimenti fai un print dell'errore e rispondi alla WebView con quell'errore
                            Exception e = task.getException();
                            Log.e(TAG, "onGetRank:onFailure", e);
                            if (e instanceof FirebaseFunctionsException) {
                                FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                callbackContext.sendPluginResult(convertToPluginResult(ffe));
                            }
                        }
                    }
                });
        return true;
    }

    private Task<JSONObject> callFunction(Map<String, Object> data, String function) {
        // chiama la funzione Firebase chiamata "onCreateSession"
        return mFunctions
                .getHttpsCallable(function)
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, JSONObject>() {
                    @Override
                    public JSONObject then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // Questa blocco viene eseguito in caso di esito positivo o negativo
                        // se l'attività non è riuscita, getResult() genererà un'eccezione che verrà propagata verso il basso.
                        HashMap result = (HashMap) task.getResult().getData();
                        // converto da HashMap in JSONObject per agevolare la manipolazione da javascript nella WebView
                        JSONObject json = new JSONObject(result);
                        return json;
                    }
                });
    }

    // converti in oggetto manipolabile da javascript nella WebView
    private PluginResult convertToPluginResult(Object error) {
        String code;
        String message, detail = "-";
        if (error instanceof DatabaseError) {
            code = String.valueOf(((DatabaseError) error).getCode());
            message = ((DatabaseError) error).getMessage();
            detail = ((DatabaseError) error).getDetails();
        }
        else {
            code = ((FirebaseFunctionsException) error).getCode().name();
            message = ((FirebaseFunctionsException) error).getMessage();
        }
        JSONObject data = new JSONObject();
        try {
            data.put("code", code);
            data.put("message", message);
            data.put("details", detail);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        PluginResult result = new PluginResult(PluginResult.Status.ERROR, data);

        return result;
    }

}
