package com.bonacogo.gameplate.fragment;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.adapter.TicketSliderAdapter;
import com.bonacogo.gameplate.model.TicketRewardObject;
import com.bonacogo.gameplate.viewmodel.TicketRewardViewModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.ViewPager2;

public class AwardFragment extends Fragment {
    private static final String TAG = "AwardFragment";

    private LinearLayout noTickets;

    private TicketRewardViewModel ticketRewardViewModel;
    private ViewPager2 viewPager2;

    private View startView;

    public AwardFragment() {
        super();
    }

    // activity callback
    public interface ActivityCallBack {
        void showTicketDetail(View v, TicketRewardObject ticket);
        void hideTicketDetail(Fragment fragment, View start);
        void hideTicketDetailNow();
    }
    private ActivityCallBack activityCallBack;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallBack = (ActivityCallBack) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ticketRewardViewModel = new ViewModelProvider(requireActivity(),
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())).get(TicketRewardViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myFragment = inflater.inflate(R.layout.fragment_award, container, false);
        ArgbEvaluator argbEvaluator = new ArgbEvaluator();

        // setup view
        viewPager2 = myFragment.findViewById(R.id.viewPagerImageSlider);
        noTickets = myFragment.findViewById(R.id.NoTickets);

        // setup viewPager
        viewPager2.setClipToPadding(false);
        viewPager2.setClipChildren(false);
        viewPager2.setOffscreenPageLimit(3);
        viewPager2.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleX(0.90f + r*0.10f);
            page.setScaleY(0.90f + r*0.10f);
        });

        viewPager2.setPageTransformer(compositePageTransformer);

        // setup get ticket and attach observer
        LiveData<LinkedHashMap<String, TicketRewardObject>> liveData = ticketRewardViewModel.getTickets();

        final Observer<LinkedHashMap<String, TicketRewardObject>> observer = (LinkedHashMap<String, TicketRewardObject> tickets) -> setTicket(tickets, argbEvaluator);

        liveData.observe(getViewLifecycleOwner(), observer);

        return myFragment;
    }

    private void setTicket(LinkedHashMap<String, TicketRewardObject> tickets, ArgbEvaluator argbEvaluator) {

        if(tickets.size() == 0 ) {
            viewPager2.setVisibility(View.GONE);
            viewPager2.setBackgroundColor(Color.TRANSPARENT);
            noTickets.setVisibility(View.VISIBLE);
        }
        else {
            viewPager2.setVisibility(View.VISIBLE);
            noTickets.setVisibility(View.GONE);
        }

        ArrayList<TicketRewardObject> ticketSlider = new ArrayList<>(tickets.values());

        TicketSliderAdapter mAdapter = new TicketSliderAdapter(ticketSlider, this);
        mAdapter.setOnItemClickListener(this::openTicketDetail);
        if(ticketSlider.size() == 1){
            int[] colors = new int[2];
            colors[0] = Color.parseColor(ticketSlider.get(ticketSlider.size() - 1).getBackground());

            colors[1] = getResources().getColor(R.color.lightGray3);

            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM, colors);


            gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            gd.setGradientCenter(0.7f, 0.7f);
            viewPager2.setBackground(gd);
        }

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (ticketSlider.size() > 0) {
                    Log.d(TAG, "onPageScrolled " + position);

                    if (position < ticketSlider.size() - 1) {
                        int[] colors = new int[2];
                        colors[0] = (Integer) argbEvaluator.evaluate(
                                positionOffset,
                                Color.parseColor(ticketSlider.get(position).getBackground()),
                                Color.parseColor(ticketSlider.get(position + 1).getBackground()));

                        colors[1] = getResources().getColor(R.color.lightGray3);

                        GradientDrawable gd = new GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM, colors);

                        gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                        gd.setGradientCenter(0.7f, 0.7f);
                        viewPager2.setBackground(gd);
                    } else {
                        int[] colors = new int[2];
                        colors[0] = Color.parseColor(ticketSlider.get(ticketSlider.size() - 1).getBackground());

                        colors[1] = getResources().getColor(R.color.lightGray3);

                        GradientDrawable gd = new GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM, colors);


                        gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                        gd.setGradientCenter(0.7f, 0.7f);
                        viewPager2.setBackground(gd);
                    }
                }
            }
        });
        viewPager2.setAdapter(mAdapter);

        if(startView != null) {
            startView = null;
            activityCallBack.hideTicketDetailNow();
        }
    }

    private void openTicketDetail(TicketRewardObject ticket , View v) {
        startView = v;
        activityCallBack.showTicketDetail(startView, ticket);
    }

    public void onBackClick(Fragment fragment) {
        activityCallBack.hideTicketDetail(fragment, startView);
        startView = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ticketRewardViewModel.removeListener();
    }

}
