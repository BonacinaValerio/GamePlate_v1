package com.bonacogo.gameplate.viewmodel;

import android.app.Application;

import com.bonacogo.gameplate.repositories.SettingsRepository;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class SettingsViewModel extends AndroidViewModel {
    private static final String TAG = "SettingsViewModel";
    private MutableLiveData<Boolean> enableNotification;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
    }


    public MutableLiveData<Boolean> getEnableNotification() {
        if (enableNotification == null) {
            enableNotification = new MutableLiveData<>();
        }
        SettingsRepository.getInstance().getEnableNotification(enableNotification, getApplication().getApplicationContext());
        return enableNotification;
    }

    public void setEnableNotification(boolean newEnableNotification) {
        if (enableNotification == null) {
            enableNotification = new MutableLiveData<>();
        }
        SettingsRepository.getInstance().setEnableNotification(newEnableNotification, enableNotification, getApplication().getApplicationContext());
    }
}
