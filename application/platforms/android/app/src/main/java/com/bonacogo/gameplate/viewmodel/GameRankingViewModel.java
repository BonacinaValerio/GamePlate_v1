package com.bonacogo.gameplate.viewmodel;

import android.app.Application;

import com.bonacogo.gameplate.repositories.GamesRankRepository;

import java.util.HashMap;
import java.util.LinkedHashMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class GameRankingViewModel extends AndroidViewModel {
    private static final String TAG = "GameRankingViewModel";
    private MutableLiveData<LinkedHashMap<String, HashMap<String, String>>> savedRanking;

    public GameRankingViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<LinkedHashMap<String, HashMap<String, String>>> getGames() {
        if (savedRanking == null) {
            savedRanking = new MutableLiveData<>();
        }
        GamesRankRepository.getInstance().getGamesOnce(savedRanking, getApplication().getApplicationContext());
        return savedRanking;
    }
}
