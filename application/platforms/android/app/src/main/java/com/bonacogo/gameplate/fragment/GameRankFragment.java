package com.bonacogo.gameplate.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.adapter.RankAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class GameRankFragment extends Fragment {
    private static final String TAG = "GameRankFragment";
    private static final String GAME_ID = "GAME_ID";

    private ImageButton back;
    private final FirebaseFunctions mFunctions;
    private RankAdapter rankAdapter;
    private boolean close;

    // activity callback
    public interface ActivityCallBack {
        void onBackClick(Fragment fragment);
    }
    private ActivityCallBack activityCallBack;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallBack = (ActivityCallBack) context;
    }

    public GameRankFragment() {
        this.mFunctions = FirebaseFunctions.getInstance();
    }

    public static GameRankFragment newInstance(String gameId) {
        // creo una nuova istanza con il parametro
        GameRankFragment fragment = new GameRankFragment();
        Bundle args = new Bundle();
        args.putString(GAME_ID, gameId);
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myFragment = inflater.inflate(R.layout.fragment_game_rank, container, false);

        String gameId = null;
        // estraggo il parametro
        if (getArguments() != null) {
            gameId = getArguments().getString(GAME_ID);
        }

        // view
        back = myFragment.findViewById(R.id.back_btn);
        back.setOnClickListener(v -> activityCallBack.onBackClick(this));

        View view = myFragment.findViewById(R.id.ranking_view);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        ViewPager viewPager = view.findViewById(R.id.view_pager);
        rankAdapter = new RankAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, getContext());
        viewPager.setAdapter(rankAdapter);
        tabLayout.setupWithViewPager(viewPager);

        getRank(gameId);
        return myFragment;
    }

    private void getRank(String gameId) {
        Map<String, Object> data = new HashMap<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            data.put("user", user.getUid());
        if (gameId != null)
            data.put("game", gameId);

        onGetRank(data).addOnCompleteListener(task -> {
            // stop loading
            if (close)
                return;

            if (task.isSuccessful()) {
                HashMap response = task.getResult();
                Log.i(TAG, "getRank: "+response.toString());
                if (response != null) {
                    rankAdapter.setRanking(response);
                    rankAdapter.notifyDataSetChanged();

                }
                else
                    error();
            }
            else {
                error();
                Exception e = task.getException();
                Log.e(TAG, "onGetRankPosition:onFailure", e);
            }
        });
    }


    private void error() {
        back.performClick();
        Toast.makeText(getContext(), R.string.error,
                Toast.LENGTH_LONG).show();
    }

    private Task<HashMap> onGetRank(Map<String, Object> data) {
        return mFunctions
                .getHttpsCallable("onGetRankPosition")
                .call(data)
                .continueWith(task -> {
                    // Questa blocco viene eseguito in caso di esito positivo o negativo
                    // se l'attività non è riuscita, getResult() genererà un'eccezione che verrà propagata verso il basso.
                    HashMap response = (HashMap) task.getResult().getData();
                    return response;
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        close = true;
    }
}