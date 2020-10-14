package com.bonacogo.gameplate.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.adapter.GameAdapter;
import com.bonacogo.gameplate.model.ListGameObject;
import com.bonacogo.gameplate.util.SharedObject;
import com.facebook.shimmer.ShimmerFrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RecentGamesFragment extends ListGamesFragment  {
    private static final String TAG = "RecentGamesFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myFragment = inflater.inflate(R.layout.fragment_game_list, container, false);

        ListGameObject listGameObject = (ListGameObject) SharedObject.getObject(prefs, ListGameObject.DETAILS_STRING);

        // setup views
        TextView title = myFragment.findViewById(R.id.title);
        LinearLayout noResults = myFragment.findViewById(R.id.no_results);
        ShimmerFrameLayout shimmerFrameLayout = myFragment.findViewById(R.id.shimmer);
        recyclerResult = myFragment.findViewById(R.id.recycler_result);

        // setup UI
        shimmerFrameLayout.setVisibility(View.GONE);
        title.setText(R.string.recently_played);

        if (listGameObject.getGames().isEmpty()) {
            // se la lista è vuota mostra il label NoResult
            noResults.setVisibility(View.VISIBLE);
            recyclerResult.setVisibility(View.GONE);
        }
        else {
            // altrimenti mostra la lista
            noResults.setVisibility(View.GONE);
            recyclerResult.setVisibility(View.VISIBLE);
        }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerResult.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerResult.setLayoutManager(layoutManager);
        recyclerResult.setNestedScrollingEnabled(false);

        // specify an adapter
        mAdapter = new GameAdapter(this, listGameObject, GameAdapter.RECENT_GAMES);
        // listener del click di un gioco
        mAdapter.setAdapterCallBack(this);

        recyclerResult.setAdapter(mAdapter);

        return myFragment;
    }
}
