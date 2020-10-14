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
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

public class SettingsRepository {
    private static final String TAG = "SettingsRepository";
    private static SettingsRepository instance;
    private final DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private SettingsRepository() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized SettingsRepository getInstance() {
        if (instance == null) {
            instance = new SettingsRepository();
        }
        return instance;
    }

    public void getEnableNotification(MutableLiveData<Boolean> enableNotification, Context context) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mDatabase.child("users").child(user.getUid()).child("enableNotification").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        enableNotification.setValue((Boolean) dataSnapshot.getValue());
                    }
                    else {
                        enableNotification.setValue(true);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    enableNotification.setValue(true);
                    Log.e(TAG, "onCancelled: ", databaseError.toException());
                    Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void setEnableNotification(boolean newEnableNotification, MutableLiveData<Boolean> enableNotification, Context context) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mDatabase.child("users").child(user.getUid()).child("enableNotification").setValue(newEnableNotification, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if (databaseError == null) {
                        enableNotification.setValue(newEnableNotification);
                        if (newEnableNotification)
                            Toast.makeText(context, R.string.notifications_enabled, Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(context, R.string.notifications_disabled, Toast.LENGTH_LONG).show();
                    }
                    else {
                        Log.e(TAG, "onCancelled: ", databaseError.toException());
                        Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
}
