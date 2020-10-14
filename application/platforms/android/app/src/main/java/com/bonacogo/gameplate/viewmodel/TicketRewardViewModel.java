package com.bonacogo.gameplate.viewmodel;

import android.app.Application;

import com.bonacogo.gameplate.model.TicketRewardObject;
import com.bonacogo.gameplate.repositories.TicketsRepository;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class TicketRewardViewModel extends AndroidViewModel {
    private static final String TAG = "TicketRewardViewModel";

    private MutableLiveData<LinkedHashMap<String, TicketRewardObject>> ticketsReward;

    public TicketRewardViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<LinkedHashMap<String, TicketRewardObject>> getTickets() {
        if (ticketsReward == null) {
            ticketsReward = new MutableLiveData<>();
            TicketsRepository.getInstance().getTicketsOnce(ticketsReward, getApplication().getApplicationContext());
        }
        else {
            if (ticketsReward.getValue() != null) {
                LinkedHashMap<String, TicketRewardObject> linkedHashMap = checkValidity(ticketsReward.getValue());
                if (linkedHashMap != null) {
                    ticketsReward.setValue(linkedHashMap);
                }
            }
            TicketsRepository.getInstance().getTicketsOn(ticketsReward, getApplication().getApplicationContext());
        }
        return ticketsReward;
    }

    private LinkedHashMap<String, TicketRewardObject> checkValidity(LinkedHashMap<String, TicketRewardObject> value) {
        boolean changed = false;
        long currentTime = new Date().getTime();
        for (Map.Entry<String, TicketRewardObject> map : value.entrySet()) {
            TicketRewardObject ticketRewardObject = map.getValue();
            if (ticketRewardObject.getDeadline() <= currentTime) {
                value.remove(map.getKey());
                changed = true;
            }
        }
        if (changed)
            return value;
        else
            return null;
    }

    public void removeListener() {
        TicketsRepository.getInstance().removeListener();
    }

    public void init() {
        ticketsReward = null;
        TicketsRepository.getInstance().init();
    }

}
