package com.bonacogo.gameplate.adapter;


import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.model.GameObject;
import com.bonacogo.gameplate.model.ListGameObject;
import com.bonacogo.gameplate.viewholder.GameViewHolder;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public class GameAdapter extends RecyclerView.Adapter<GameViewHolder> {
    public static final int RECENT_GAMES = 0;
    public static final int SEARCH_RESULT = 1;
    private int type;
    private Fragment fragment;
    private ListGameObject listGameObject;
    private ListGameObject savedListGameObject;

    public interface AdapterCallBack {
        void onPlayClick(String url);
        void onItemClick(GameObject game, View v);
    }
    private AdapterCallBack adapterCallBack;
    public void setAdapterCallBack(AdapterCallBack adapterCallBack) {
        this.adapterCallBack = adapterCallBack;
    }

    public GameAdapter(Fragment fragment, int type) {
        this.type = type;
        this.fragment = fragment;
    }

    public GameAdapter(Fragment fragment, ListGameObject listGameObject, int type) {
        this.listGameObject = listGameObject;
        this.type = type;
        this.fragment = fragment;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setGameSelected(String id) throws CloneNotSupportedException {
        if (savedListGameObject == null)
            this.savedListGameObject = (ListGameObject) listGameObject.clone();

        LinkedHashMap<String, GameObject> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put(id, savedListGameObject.getGame(id));
        this.listGameObject = new ListGameObject(linkedHashMap);
    }

    public void moveOnTop(SharedPreferences pref, String id) {
        moveOnTop(id);
        this.listGameObject.save(pref);
    }
    public void save(SharedPreferences pref) {
        this.listGameObject.save(pref);
    }
    private void moveOnTop(String id) {
        GameObject gameObject = listGameObject.getGames().get(id);
        this.listGameObject.getGames().remove(id);
        this.listGameObject.getGames().put(gameObject.getId(), gameObject);

        notifyDataSetChanged();
    }

    public boolean isOnTop(String id) {
        ArrayList<String> arrayList = new ArrayList<String>(listGameObject.getGames().keySet());
        int pos = arrayList.indexOf(id);
        return pos == arrayList.size() - 1;
    }

    public ListGameObject getSavedListGameObject() {
        return savedListGameObject;
    }

    public void setSavedListGameObject(ListGameObject savedListGameObject) {
        this.savedListGameObject = savedListGameObject;
    }

    public void restoreState() {
        if (this.savedListGameObject != null) {
            this.listGameObject = new ListGameObject(savedListGameObject.getGames());
            this.savedListGameObject = null;
        }
    }

    public void setListGameObject(ListGameObject listGameObject) {
        this.listGameObject = listGameObject;
    }

    public ListGameObject getListGameObject() {
        return listGameObject;
    }

    @NotNull
    @Override
    public GameViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if (type == SEARCH_RESULT)
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.holder_search_result, parent, false);
        else
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.holder_recent_game, parent, false);
        return new GameViewHolder(v, parent.getContext(), this.type);
    }

    @Override
    public void onBindViewHolder(@NotNull GameViewHolder holder, int position) {
        GameObject gameObject = listGameObject.getGame(position);

        if (type == SEARCH_RESULT) {
            holder.setRating(gameObject.getRating());
            holder.getGameCard().setOnClickListener(v -> adapterCallBack.onItemClick(gameObject, holder.getView()));
            holder.setAddress(gameObject.getAddress().replace("\\n","\n"));
            holder.setNumFeedback(gameObject.getNumFeedback());
        }
        else {
            holder.getContent().setOnClickListener(v -> adapterCallBack.onItemClick(gameObject, holder.getView()));
            holder.getPlay().setOnClickListener(v -> {
                adapterCallBack.onPlayClick(gameObject.getUrl());
                if (!isOnTop(gameObject.getId())) {
                    moveOnTop(gameObject.getId());
                }
            });
        }

        holder.setGame(gameObject.getGame());
        holder.setRestaurant(gameObject.getRestaurant());
        holder.setImgGame(fragment, gameObject.getUrl());
    }

    @Override
    public int getItemCount() {
        return listGameObject.getGames().size();
    }
}