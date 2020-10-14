package com.bonacogo.gameplate.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.viewholder.GameRankViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public class GameRankingAdapter extends RecyclerView.Adapter<GameRankViewHolder> {
    private final Fragment fragment;

    private LinkedHashMap<String, HashMap<String, String>> gameRanking;
    public void setGameRanking(LinkedHashMap<String, HashMap<String, String>> gameRanking) {
        this.gameRanking = gameRanking;
    }

    // setup adapter callback
    public interface AdapterCallBack {
        void onItemClick(String id, View v);
    }
    private AdapterCallBack adapterCallBack;
    public void setAdapterCallBack(AdapterCallBack adapterCallBack) {
        this.adapterCallBack = adapterCallBack;
    }

    public GameRankingAdapter(LinkedHashMap<String, HashMap<String, String>> gameRanking, Fragment fragment) {
        this.fragment = fragment;
        this.gameRanking = gameRanking;
    }

    @NonNull
    @Override
    public GameRankViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.holder_ranking, parent, false);
        return new GameRankViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GameRankViewHolder holder, int position) {
        HashMap<String, String> gameRank = new ArrayList<>(gameRanking.values()).get(position);
        String id = gameRank.get("id");

        holder.setGame(gameRank.get("name"));
        holder.setGameImg(fragment, id);
        holder.getCardGameCard().setOnClickListener(v -> adapterCallBack.onItemClick(id, holder.getCardGameCard()));
    }

    @Override
    public int getItemCount() {
        return gameRanking.size();
    }
}
