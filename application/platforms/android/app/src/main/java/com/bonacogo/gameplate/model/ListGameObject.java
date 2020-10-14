package com.bonacogo.gameplate.model;

import android.content.SharedPreferences;

import com.bonacogo.gameplate.util.SharedObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ListGameObject implements Serializable, Cloneable {
    public static final String DETAILS_STRING = "RECENT_GAME_DETAILS";
    private LinkedHashMap<String, GameObject> games;

    public ListGameObject() {
        this.games = new LinkedHashMap<>();
    }

    public ListGameObject(LinkedHashMap<String, GameObject> games) {
        this.games = games;
    }

    public LinkedHashMap<String, GameObject> getGames() {
        return games;
    }

    public void setGames(LinkedHashMap<String, GameObject> games) {
        this.games = games;
    }

    public GameObject getGame(int i) {
        // ritorna l'elemento della lista in senso decrescente
        int index = (games.size()-1) - i;
        List<GameObject> l = new ArrayList<>(games.values());
        return l.get(index);
    }

    public GameObject getGame(String key) {
        return games.get(key);
    }

    private void removeGame(int i) {
        List<String> l = new ArrayList<>(games.keySet());
        this.games.remove(l.get(i));
    }

    public void addGame(SharedPreferences prefs, GameObject game) {
        if (game == null)
            return;

        String id = game.getId();
        // se il gioco era già nella lista lo rimuovo
        if (games.containsKey(id))
            this.games.remove(id);

        // aggiungo il gioco e imposto un limite di 10
        this.games.put(game.getId(), new GameObject(
                game.getGame(),
                game.getRestaurant(),
                game.getUrl(),
                game.getId(),
                game.getLat(),
                game.getLng()));
        if (games.size()>10) {
            removeGame(0);
        }

        SharedObject.saveObject(this, prefs, DETAILS_STRING);
    }

    public void save(SharedPreferences prefs) {
        SharedObject.saveObject(this, prefs, DETAILS_STRING);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
