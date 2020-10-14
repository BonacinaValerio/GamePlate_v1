package com.bonacogo.gameplate.repositories;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.model.RewardString;
import com.bonacogo.gameplate.model.TicketRewardObject;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

public class TicketsRepository {

    private static final String TAG = "TicketsRepository";

    private static TicketsRepository instance;
    private Query query;
    private ChildEventListener listener;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private boolean firstDownloadDone;
    private HashMap<String, RewardString> stringHashMap;

    private TicketsRepository() {
    }

    public static synchronized TicketsRepository getInstance() {
        if (instance == null) {
            instance = new TicketsRepository();
        }
        return instance;
    }

    public void getTicketsOnce(MutableLiveData<LinkedHashMap<String, TicketRewardObject>> ticketsReward, Context context) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            LinkedHashMap<String, TicketRewardObject> sortedMap = new LinkedHashMap<>();
            Date currentDate = new Date();
            mDatabase.child("users").child(user.getUid()).child("reward").orderByChild("deadline").startAt(currentDate.getTime()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        ArrayList<String> listStrings = new ArrayList<>();
                        HashMap<String, TicketRewardObject> message = new HashMap<>();
                        for(DataSnapshot ticket : dataSnapshot.getChildren()) {
                            TicketRewardObject ticketRewardObject = ticket.getValue(TicketRewardObject.class);
                            if(ticketRewardObject.isEnable()) {
                                message.put(ticket.getKey(), ticketRewardObject);
                                String type = ticketRewardObject.getType();
                                if (!listStrings.contains(type))
                                    listStrings.add(type);
                            }
                        }

                        if(message.isEmpty()){
                            firstDownloadDone = true;
                            ticketsReward.setValue(sortedMap);
                        }

                        String lang = new Locale(Locale.getDefault().getLanguage()).getLanguage().toUpperCase();
                        final int[] countQuery = {0};
                        for (String type : listStrings) {
                            Query stringQuery = mDatabase.child("strings").child(lang).child("reward_"+type);
                            stringQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        countQuery[0]++;
                                        RewardString rewardString = dataSnapshot.getValue(RewardString.class);
                                        stringHashMap.put(type, rewardString);

                                        if (countQuery[0] == listStrings.size()) {

                                            List<Map.Entry<String, TicketRewardObject>> entries = new ArrayList<>(message.entrySet());
                                            Collections.sort(entries, (a, b) -> Long.compare(b.getValue().getStartAt(), a.getValue().getStartAt()));
                                            for (Map.Entry<String, TicketRewardObject> entry : entries) {
                                                TicketRewardObject newTicket = entry.getValue();
                                                newTicket.setRewardString(stringHashMap.get(newTicket.getType()));
                                                sortedMap.put(entry.getKey(), newTicket);
                                            }

                                            ticketsReward.setValue(sortedMap);
                                            firstDownloadDone = true;

                                        }
                                    }
                                    else {
                                        mDatabase.child("strings").child("EN").child("reward_"+type).addListenerForSingleValueEvent(this);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                    ticketsReward.setValue(sortedMap);
                                    firstDownloadDone = true;
                                }
                            });
                        }
                    }
                    else {
                        ticketsReward.setValue(sortedMap);
                        firstDownloadDone = true;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    firstDownloadDone = true;
                    ticketsReward.setValue(sortedMap);
                    Log.e(TAG, "onCancelled: ", databaseError.toException());
                    Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void getTicketsOn(MutableLiveData<LinkedHashMap<String, TicketRewardObject>> ticketsReward, Context context) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Date currentDate = new Date();
            query = mDatabase.child("users").child(user.getUid()).child("reward").orderByChild("deadline").startAt(currentDate.getTime());
            listener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    if (firstDownloadDone) {
                        TicketRewardObject newTicket = dataSnapshot.getValue(TicketRewardObject.class);
                        LinkedHashMap<String, TicketRewardObject> linkedHashMap = ticketsReward.getValue();
                        // se viene aggiunto un ticket che non c'era nella lista lo metto in prima posizione
                        if (newTicket != null && newTicket.isEnable() && linkedHashMap != null && linkedHashMap.get(dataSnapshot.getKey()) == null) {
                            String type = newTicket.getType();
                            RewardString rewardString = stringHashMap.get(newTicket.getType());
                            if (rewardString != null) {
                                newTicket.setRewardString(rewardString);
                                LinkedHashMap <String, TicketRewardObject> newLinkedHashMap = new LinkedHashMap<>();
                                newLinkedHashMap.put(dataSnapshot.getKey(), newTicket);
                                newLinkedHashMap.putAll(linkedHashMap);
                                ticketsReward.setValue(newLinkedHashMap);
                            }
                            else {
                                String lang = new Locale(Locale.getDefault().getLanguage()).getLanguage().toUpperCase();
                                Query stringQuery = mDatabase.child("strings").child(lang).child("reward_"+type);
                                stringQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot stringSnapshot) {
                                        if (stringSnapshot.exists()) {
                                            RewardString rewardString = stringSnapshot.getValue(RewardString.class);
                                            stringHashMap.put(type, rewardString);
                                            newTicket.setRewardString(rewardString);
                                            LinkedHashMap <String, TicketRewardObject> newLinkedHashMap = new LinkedHashMap<>();
                                            newLinkedHashMap.put(dataSnapshot.getKey(), newTicket);
                                            newLinkedHashMap.putAll(linkedHashMap);
                                            ticketsReward.setValue(newLinkedHashMap);
                                        }
                                        else {
                                            mDatabase.child("strings").child("EN").child("reward_"+type).addListenerForSingleValueEvent(this);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Log.e(TAG, "onCancelled: ", databaseError.toException());
                                        Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                        // se viene disabilitato un ticket quando si è negli altri fragment
                        if (newTicket != null && !newTicket.isEnable() && linkedHashMap != null && linkedHashMap.get(dataSnapshot.getKey()) != null) {
                            linkedHashMap.remove(dataSnapshot.getKey());
                            ticketsReward.setValue(linkedHashMap);
                        }
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    if (firstDownloadDone) {
                        TicketRewardObject newTicket = dataSnapshot.getValue(TicketRewardObject.class);
                        LinkedHashMap<String, TicketRewardObject> linkedHashMap = ticketsReward.getValue();
                        // se viene modificato un ticket nella lista e diventa disabilitato lo rimuovo
                        if (newTicket != null && linkedHashMap != null && linkedHashMap.get(dataSnapshot.getKey()) != null) {
                            if (!newTicket.isEnable()) {
                                linkedHashMap.remove(dataSnapshot.getKey());
                                ticketsReward.postValue(linkedHashMap);
                            }
                        }
                    }
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                    if (firstDownloadDone) {
                        TicketRewardObject oldTicket = dataSnapshot.getValue(TicketRewardObject.class);
                        LinkedHashMap<String, TicketRewardObject> linkedHashMap = ticketsReward.getValue();
                        // se viene rimosso un ticket ed era nella lista lo rimuovo
                        if (oldTicket != null && linkedHashMap != null && linkedHashMap.get(dataSnapshot.getKey()) != null) {
                            linkedHashMap.remove(dataSnapshot.getKey());
                            ticketsReward.setValue(linkedHashMap);
                        }
                    }
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "onCancelled: ", databaseError.toException());
                    Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
                }
            };
            query.addChildEventListener(listener);
        }
    }

    public void removeListener() {
        if (query != null)
            query.removeEventListener(listener);
    }

    public void init() {
        removeListener();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        stringHashMap = new HashMap<>();
    }
}