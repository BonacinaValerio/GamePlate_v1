package com.bonacogo.gameplate.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.model.HistoryObject;
import com.bonacogo.gameplate.viewholder.HistoryViewHolder;

import org.jetbrains.annotations.NotNull;

import androidx.recyclerview.widget.RecyclerView;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> implements View.OnClickListener {
    private HistoryObject history;

    // setup adapter callback
    public interface AdapterCallBack {
        void onItemClick(String name);
    }
    private AdapterCallBack adapterCallBack;
    public void setAdapterCallBack(AdapterCallBack adapterCallBack) {
        this.adapterCallBack = adapterCallBack;
    }

    public HistoryAdapter(HistoryObject historyObject) {
        history = historyObject;
    }

    @NotNull
    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.holder_history_element, parent, false);
        view.setOnClickListener(this);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {
        // imposto il nome dell'holder con l'elemento del'HistoryObject nella posizione corrente
        holder.setName(history.getElementHistory(position));
    }

    @Override
    public int getItemCount() {
        return history.getHistory().size();
    }

    @Override
    public void onClick(View v) {
        // estraggo il nome e lo ritorno al callback
        TextView textView = v.findViewById(R.id.text_element);
        String name = textView.getText().toString();
        adapterCallBack.onItemClick(name);
    }
}