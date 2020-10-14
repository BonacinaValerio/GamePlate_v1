package com.bonacogo.gameplate.adapter;

import android.content.Context;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.fragment.TabRankFragment;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class RankAdapter extends FragmentStatePagerAdapter {
    private HashMap ranking;
    private Context context;

    public void setRanking(HashMap ranking) {
        this.ranking = ranking;
    }

    public RankAdapter(@NonNull FragmentManager fm, int behavior, Context context) {
        super(fm, behavior);
        this.ranking = new HashMap();
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return TabRankFragment.newInstance(position, ranking);
    }


    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0)
            return context.getString(R.string.weekly);
        else
            return context.getString(R.string.global);
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }
}
