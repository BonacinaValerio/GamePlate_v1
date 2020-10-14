package com.bonacogo.gameplate.adapter;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.model.TicketRewardObject;
import com.bonacogo.gameplate.viewholder.TicketViewHolder;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

public class TicketSliderAdapter extends RecyclerView.Adapter<TicketViewHolder>{

    public interface OnItemClickListener {
        void onItemClick(TicketRewardObject ticket, View v);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    private OnItemClickListener mListener;

    public void setSliderTickets(ArrayList<TicketRewardObject> sliderTickets) {
        this.sliderTickets = sliderTickets;
    }

    private ArrayList<TicketRewardObject> sliderTickets;
    private Fragment fragment;

    public TicketSliderAdapter(ArrayList<TicketRewardObject> sliderTickets, Fragment fragment) {
        this.sliderTickets = sliderTickets;
        this.fragment = fragment;
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, final int position) {

        TicketRewardObject ticketRewardObject = sliderTickets.get(position);


        holder.setImgGame(fragment, ticketRewardObject.getRestaurantId());
        holder.setTitle(ticketRewardObject.getRestaurant());


        String description = ticketRewardObject.getRewardString().getDescription();
        Pattern pattern = Pattern.compile("\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(description);
        if (matcher.find()) {
            String img = matcher.group(1);
            try {
                holder.setImgReward(getResId(img));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            description = description.replace("{"+img+"}", "");
        }
        holder.setLabelReward(description);



        /*
        holder.setGameTitle(ticketRewardObject.getGame());
        holder.setRewardType(ticketRewardObject.getRewardString().getDescription());*/



        // Setting the countdown
        Date date = new Date();
        long countDown = ticketRewardObject.getDeadline() - date.getTime();

        new CountDownTimer(countDown, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                setCountdown(millisUntilFinished/1000, holder);
            }

            @Override
            public void onFinish() {
                holder.setmDays("0");
                holder.setmHours("0");
                holder.setmMins("0");
            }


        }.start();


        holder.getTicketCard().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TicketRewardObject ticket = sliderTickets.get(position);
                mListener.onItemClick(ticket, holder.getTicketCard());
            }
        });




    }

    @Override
    public int getItemCount() {
        return sliderTickets.size();
    }

    public ArrayList<TicketRewardObject> getSliderTickets() {
        return sliderTickets;
    }

    @NotNull
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TicketViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.holder_slide_ticket,
                        parent,
                        false
                ), mListener
        );
    }


    private void setCountdown(long sec, TicketViewHolder holder) {
        // We're forced to use long instead of int : int values->risk of negative results
        long numberOfDays = sec / 86400;
        long numberOfHours = (sec % 86400 ) / 3600;
        long numberOfMinutes = ((sec % 86400 ) % 3600 ) / 60;

        holder.setmDays("" + numberOfDays);
        holder.setmHours("" + numberOfHours);
        holder.setmMins("" + numberOfMinutes);
    }

    private static int getResId(String resName) throws NoSuchFieldException, IllegalAccessException {
        Field idField = R.drawable.class.getDeclaredField(resName);
        return idField.getInt(idField);
    }

}

