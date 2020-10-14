package com.bonacogo.gameplate.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.adapter.GameAdapter;
import com.bonacogo.gameplate.model.GameObject;
import com.bonacogo.gameplate.model.ListGameObject;
import com.bonacogo.gameplate.util.CommonStrings;
import com.bonacogo.gameplate.util.SharedObject;
import com.bonacogo.gameplate.viewholder.GameViewHolder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.MODE_PRIVATE;

public class ListGamesFragment extends Fragment implements
        GameAdapter.AdapterCallBack {
    RecyclerView recyclerResult;
    GameAdapter mAdapter;
    SharedPreferences prefs;

    private GameObject selectedGame;

    private View startView;

    // setup activity callback
    public interface ActivityCallBack {
        void onLaunchGame(String url);
        void setGlobalNavigationActive(boolean enable);
        BottomSheetBehavior getBottomSheetBehavior();
        boolean checkChangeStateBottomSheet();
        void enableFocusMarker(int typeAdapter, GameObject gameObject, String focusMarker);

        void showGameViewDetail(View start, GameObject gameObject);
        void hideGameViewDetail(Fragment fragment, View start);
    }
    ActivityCallBack activityCallBack;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallBack = (ActivityCallBack) context;

        prefs = context.getSharedPreferences(CommonStrings.SHARED_PREF_NAME, MODE_PRIVATE);
    }

    // metodo cercato da GameViewDetail passando per MainActivity al click del tasto back
    public void backFromGameDetail(Fragment fragment, String focusMarker, GameObject gameObject) {

        if (focusMarker != null)
            // se ho cliccato mostra sulla mappa
            checkAdapter(fragment, focusMarker, gameObject);
        else
            // se ho cliccato back
            backTransition(fragment);
    }

    private void checkAdapter(Fragment fragment, String focusMarker, GameObject gameObject) {

        if (recyclerResult.getChildCount() == 1 || (mAdapter.getType() == GameAdapter.RECENT_GAMES && mAdapter.isOnTop(focusMarker)))
            checkBottomSheet(fragment);
        else {
            // se la view non è prima nella lista aspetto che si posizioni come tale prima di procedere con l'animazione inversa
            startView.setVisibility(View.VISIBLE);
            recyclerResult.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(@NonNull View view) {
                    recyclerResult.removeOnChildAttachStateChangeListener(this);
                    startView = view;
                    checkBottomSheet(fragment);
                }
                @Override
                public void onChildViewDetachedFromWindow(@NonNull View view) { }
            });
        }

        activityCallBack.enableFocusMarker(mAdapter.getType(), gameObject, focusMarker);
    }

    private void checkBottomSheet(Fragment fragment) {
        BottomSheetBehavior bottomSheetBehavior = activityCallBack.getBottomSheetBehavior();
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HALF_EXPANDED) {
            bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                        bottomSheetBehavior.removeBottomSheetCallback(this);
                        backTransition(fragment);
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) { }
            });
            activityCallBack.checkChangeStateBottomSheet();
        }
        else {
            backTransition(fragment);
        }
    }

    private void backTransition(Fragment fragment) {
        activityCallBack.hideGameViewDetail(fragment, startView);
        startView = null;
    }

    private void setGameViewDetail(FragmentManager fManager, GameObject game) {
        // mostra la GameViewDetail con animazione
        activityCallBack.setGlobalNavigationActive(false);

        FragmentTransaction fTransaction = fManager.beginTransaction();

        GameViewDetail fragment;
        if (this instanceof RecentGamesFragment) {
            if (selectedGame == null || !game.getId().equals(selectedGame.getId())) {
                fragment = GameViewDetail.newInstance(game);
                selectedGame = null;
            }
            else
                fragment = GameViewDetail.newInstance(selectedGame);
        }
        else
            fragment = GameViewDetail.newInstance(game);
        fTransaction.replace(R.id.game_detail, fragment).commit();
    }

    private void setStartView(String id) {
        if (!mAdapter.isOnTop(id)) {
            startView.setVisibility(View.VISIBLE);
            recyclerResult.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(@NonNull View view) {
                    recyclerResult.removeOnChildAttachStateChangeListener(this);
                    startView = view;
                }

                @Override
                public void onChildViewDetachedFromWindow(@NonNull View view) { }
            });
            mAdapter.moveOnTop(prefs, id);
        }
    }

    // START ADAPTER CALLBACK GAMEADAPTER

    // click elemento nell'adapter
    @Override
    public void onItemClick(GameObject game, View v) {
        FragmentManager fManager = getFragmentManager();
        if (fManager == null)
            return;

        startView = v;
        setGameViewDetail(fManager, game);
        activityCallBack.showGameViewDetail(startView, game);
    }

    // click play in adapter
    @Override
    public void onPlayClick(String url) {
        activityCallBack.onLaunchGame(url);
        if (this instanceof RecentGamesFragment) {
            mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    // devo salvare perche quando clicco sul tasto dal RecentGames viene messo onTop nell'adapter
                    // ma non avendo la sharedPreferences devo ricorrere a un salvataggio manuale
                    mAdapter.save(prefs);
                }
            });
        }
    }

    // END ADAPTER CALLBACK GAMEADAPTER

    // START FRAGMENT CALLBACK MAP

    public void onMarkerClick(Marker marker) {
        if (mAdapter.getType() == GameAdapter.SEARCH_RESULT) {
            if (marker == null) {
                mAdapter.restoreState();
            } else {
                activityCallBack.getBottomSheetBehavior().setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                try {
                    mAdapter.setGameSelected((String) marker.getTag());
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            mAdapter.notifyDataSetChanged();
            if (this instanceof SearchResultFragment)
                ((SearchResultFragment) this).saveState();
        }
        else
            if (marker != null) {
                if (!mAdapter.isOnTop((String) marker.getTag())) {
                    mAdapter.moveOnTop(prefs, (String) marker.getTag());
                }
            }
    }

    public void onInfoWindowClick() {
        // al click della infoWindow l'elemento è già il primo della lista
        GameViewHolder viewHolder = (GameViewHolder) recyclerResult.findViewHolderForAdapterPosition(0);
        if (viewHolder != null)
            if (mAdapter.getType() == GameAdapter.RECENT_GAMES)
                viewHolder.getContent().performClick();
            else
                viewHolder.getGameCard().performClick();
    }

    // END FRAGMENT CALLBACK MAP

    // START FRAGMENT CALLBACK GAMEVIEWDEATAIL

    public void onGameObjectDone(GameObject gameObject) {
        this.selectedGame = gameObject;
    }

    public void onPlayClickFromDetail(GameObject gameObject) {
        // click del tasto play
        if (this instanceof RecentGamesFragment)
            // se siamo nella lista dei giochi recenti l'elemento si mette in cima alla lista
            setStartView(gameObject.getId());
        else {
            // altrimenti lo aggiungo alla lista nelle SP
            ListGameObject listGameObject = (ListGameObject) SharedObject.getObject(prefs, ListGameObject.DETAILS_STRING);
            listGameObject.addGame(prefs, gameObject);
        }
        activityCallBack.onLaunchGame(gameObject.getUrl());
    }

    // END FRAGMENT CALLBACK GAMEVIEWDEATAIL
}
