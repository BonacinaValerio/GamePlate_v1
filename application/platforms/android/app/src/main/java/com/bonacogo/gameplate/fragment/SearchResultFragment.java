package com.bonacogo.gameplate.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.adapter.GameAdapter;
import com.bonacogo.gameplate.model.GameObject;
import com.bonacogo.gameplate.model.ListGameObject;
import com.bonacogo.gameplate.viewmodel.SavedStateFragment;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.LinkedHashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SearchResultFragment extends ListGamesFragment {
    private static final String TAG = "SearchResultFragment";
    private static final String SEARCH_NAME = "SEARCH_NAME";

    private TextView title;
    private ShimmerFrameLayout shimmerFrameLayout;
    private LinearLayout noResults;

    private String searchName;
    public String getSearchName() { return searchName; }

    SavedStateFragment savedStateFragment;

    static SearchResultFragment newInstance(ViewModelStoreOwner owner, String search) {
        // creo una nuova istanza con il parametro

        new ViewModelProvider(owner).get(SavedStateFragment.class).clearBundle(TAG);
        SearchResultFragment fragment = new SearchResultFragment();
        Bundle args = new Bundle();
        args.putString(SEARCH_NAME, search);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedStateFragment = new ViewModelProvider(requireActivity()).get(SavedStateFragment.class);
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myFragment = inflater.inflate(R.layout.fragment_game_list, container, false);

        // estraggo il parametro
        if (getArguments() != null) {
            searchName = getArguments().getString(SEARCH_NAME);
        }

        // views
        recyclerResult = myFragment.findViewById(R.id.recycler_result);
        noResults = myFragment.findViewById(R.id.no_results);
        title = myFragment.findViewById(R.id.title);
        shimmerFrameLayout = myFragment.findViewById(R.id.shimmer);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerResult.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerResult.setLayoutManager(layoutManager);
        recyclerResult.setNestedScrollingEnabled(false);

        // specify an adapter
        mAdapter = new GameAdapter(this, GameAdapter.SEARCH_RESULT);
        // listener del click di un gioco
        mAdapter.setAdapterCallBack(this);

        recyclerResult.setAdapter(mAdapter);

        Bundle savedState = savedStateFragment.getBundle(TAG);
        if (savedState == null) {
            // alla prima instanziazione nascondo il recyclerResult e animo lo shimmer
            recyclerResult.setVisibility(View.GONE);
            shimmerFrameLayout.startShimmer();

            // se il search_name è diverso da null vuol dire che la ricerca deriva dalla searchView
            if (searchName!= null) {
                // imposto il titolo della BottomSheet e attivo lo stato di loading
                title.setText(getString(R.string.results_for) + searchName);
            }
            else {
                // imposto LABEL2 come titolo in quanto la ricerca è derivata da SearchHere o FindMe
                title.setText(R.string.results);
            }
        }
        else {
            ListGameObject listGameObject = (ListGameObject) savedState.getSerializable("search_result");

            if (listGameObject != null) {
                // se questo fragment è stato ripristinato mostro la recyclerView, nascondo lo shimmer
                // infine reimposto il titolo e il contenuto dell'adapter
                recyclerResult.setVisibility(View.VISIBLE);
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmer();
                title.setText(savedState.getString("search_title"));
                mAdapter.setType(savedState.getInt("type"));
                mAdapter.setListGameObject(listGameObject);
                if (listGameObject.getGames().isEmpty()) {
                    // se la lista è vuota mostra il label NoResult
                    noResults.setVisibility(View.VISIBLE);
                    recyclerResult.setVisibility(View.GONE);
                } else {
                    // altrimenti mostra la lista
                    noResults.setVisibility(View.GONE);
                    recyclerResult.setVisibility(View.VISIBLE);
                }
                mAdapter.setSavedListGameObject((ListGameObject) savedState.getSerializable("saved_search_result"));
            }
        }

        return myFragment;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        savedStateFragment.setBundle(TAG, saveBundle());
        super.onSaveInstanceState(outState);
    }

    private Bundle saveBundle() {

        Bundle bundle = new Bundle();

        bundle.putString("search_title", title.getText().toString());
        bundle.putSerializable("search_result", mAdapter.getListGameObject());
        bundle.putSerializable("saved_search_result", mAdapter.getSavedListGameObject());
        bundle.putInt("type", mAdapter.getType());
        return bundle;
    }

    void saveState() {
        savedStateFragment.setBundle(TAG, saveBundle());
    }

    public boolean isLoading() {
        return shimmerFrameLayout.getVisibility() == View.VISIBLE;
    }

    public void setResult(LinkedHashMap<String, GameObject> gameList) {
        mAdapter.setListGameObject(new ListGameObject(gameList));
        // nascondi lo shimmer e mostra il recyclerView
        shimmerFrameLayout.setVisibility(View.GONE);
        shimmerFrameLayout.stopShimmer();
        if (gameList.isEmpty()) {
            // se la lista è vuota mostra il label NoResult
            noResults.setVisibility(View.VISIBLE);
            recyclerResult.setVisibility(View.GONE);
        }
        else {
            // altrimenti mostra la lista
            noResults.setVisibility(View.GONE);
            recyclerResult.setVisibility(View.VISIBLE);
        }

        saveState();
    }


}
