package com.bonacogo.gameplate.viewholder;

import android.view.View;
import android.widget.TextView;

import com.bonacogo.gameplate.R;

import androidx.recyclerview.widget.RecyclerView;

public class HistoryViewHolder extends RecyclerView.ViewHolder {
    public TextView name;

    public HistoryViewHolder(View v) {
        super(v);
        // view
        TextView text_element = v.findViewById(R.id.text_element);
        name = text_element;
    }

    public void setName(String name) {
        this.name.setText(name);
    }
}