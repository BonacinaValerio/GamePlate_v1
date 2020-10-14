package com.bonacogo.gameplate.repositories;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.bonacogo.gameplate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.LinkedHashMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

public class GamesRankRepository {
    private static final String TAG = "GamesRankRepository";
    private static GamesRankRepository instance;
    private final DatabaseReference mDatabase;
    private FirebaseAuth mAuth;


    private GamesRankRepository() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized GamesRankRepository getInstance() {
        if (instance == null) {
            instance = new GamesRankRepository();
        }
        return instance;
    }


    public void getGamesOnce(MutableLiveData<LinkedHashMap<String, HashMap<String, String>>> savedRanking, Context context) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            LinkedHashMap<String, HashMap<String, String>> allGameRank = new LinkedHashMap<>();
            mDatabase.child("users").child(user.getUid()).child("games").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.i(TAG, "onDataChange: ");
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot game : dataSnapshot.getChildren()) {
                            HashMap<String, String> gameRank = new HashMap<>();
                            gameRank.put("id", game.getKey());
                            allGameRank.put(game.getKey(), gameRank);
                        }

                        final int[] countQuery = {0};
                        for (String gameId : allGameRank.keySet()) {
                            Query stringQuery = mDatabase.child("game").child(gameId).child("name");
                            stringQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        countQuery[0]++;
                                        String gameName = (String) dataSnapshot.getValue();

                                        HashMap<String, String> oldGameRank = allGameRank.get(gameId);
                                        oldGameRank.put("name", gameName);
                                        allGameRank.put(gameId, oldGameRank);

                                        if (countQuery[0] == allGameRank.size()) {
                                            savedRanking.setValue(allGameRank);

                                        }
                                    }
                                    else {
                                        savedRanking.setValue(allGameRank);
                                        Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    savedRanking.setValue(allGameRank);
                                    Log.e(TAG, "onCancelled: ", databaseError.toException());
                                    Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                    else {
                        savedRanking.setValue(allGameRank);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    savedRanking.setValue(allGameRank);
                    Log.e(TAG, "onCancelled: ", databaseError.toException());
                    Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
