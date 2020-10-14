package com.bonacogo.gameplate.model;

import android.content.SharedPreferences;

import com.bonacogo.gameplate.util.SharedObject;

import java.util.ArrayList;

public class HistoryObject {
    public static final String DETAILS_STRING = "HISTORY_DETAILS";

    private ArrayList<String> history;

    public HistoryObject() {
        this.history = new ArrayList<String>();
    }

    public HistoryObject(ArrayList<String> history) {
        this.history = history;
    }

    public void setHistory(ArrayList<String> history) {
        this.history = history;
    }

    public ArrayList<String> getHistory() {
        return history;
    }

    public String getElementHistory(int i) {
        // ritorna l'elemento della lista in senso decrescente
        int index = (history.size()-1) - i;
        return history.get(index);
    }

    public void addElement(String newElement, SharedPreferences prefs) {
        // aggiungi il nuovo elemento e se la lista è >10 rimuovi il più vecchio

        if (newElement == null)
            return;

        // se l'elemento aggiunto è già uguale all'ultimo elemento salvato non lo riaggiungo
        if (this.history.size()>0 && this.getElementHistory(0).equals(newElement))
            return;

        history.add(newElement);
        if (history.size()>10) {
            history.remove(0);
        }

        SharedObject.saveObject(this, prefs, DETAILS_STRING);
    }
}
