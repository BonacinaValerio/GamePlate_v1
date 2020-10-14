package com.bonacogo.gameplate.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.adapter.RankAdapter;
import com.bonacogo.gameplate.model.GameObject;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class RankingDialog extends DialogFragment {
    private static final String TAG = "RankingDialog";
    private FirebaseFunctions mFunctions;
    private GameObject gameObject;
    private boolean close;
    private Dialog d;
    private RankAdapter rankAdapter;

    public RankingDialog() {
    }

    public RankingDialog(GameObject gameObject) {
        this.mFunctions = FirebaseFunctions.getInstance();
        this.gameObject = gameObject;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup view, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.ranking_layout, view, false);

        return v;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        d = super.onCreateDialog(savedInstanceState);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return d;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        ViewPager viewPager = view.findViewById(R.id.view_pager);
        View viewOutside = view.findViewById(R.id.view_outside);
        LinearLayout container = view.findViewById(R.id.container);

        ViewGroup.LayoutParams margin = container.getLayoutParams();

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        margin.height = (int) (0.8*size.y);
        container.requestLayout();

        rankAdapter = new RankAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, getContext());
        viewPager.setAdapter(rankAdapter);
        tabLayout.setupWithViewPager(viewPager);

        viewOutside.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        if (gameObject != null)
            getRank(gameObject);
        else
            dismiss();
    }

    private void getRank(GameObject gameObject) {
        Map<String, Object> data = new HashMap<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            data.put("user", user.getUid());
        if (gameObject != null)
            data.put("game", gameObject.getUrl());

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

    private void error() {
        this.onCancel(d);
        Toast.makeText(getContext(), R.string.error,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        close = true;
    }
}
