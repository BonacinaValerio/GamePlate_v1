package com.bonacogo.gameplate.viewmodel;

import android.os.Bundle;

import java.util.HashMap;

import androidx.lifecycle.ViewModel;

public class SavedStateFragment extends ViewModel {
    private static final String TAG = "SavedStateFragment";
    private HashMap<String, Bundle> savedState;

    public SavedStateFragment() {
        savedState = new HashMap<>();
    }

    public Bundle getBundle(String fragment) {
        return savedState.get(fragment);
    }

    public void setBundle(String fragment, Bundle bundle) {
        savedState.put(fragment, bundle);
    }

    public void clearBundle(String fragment) {
        savedState.remove(fragment);
    }
}
