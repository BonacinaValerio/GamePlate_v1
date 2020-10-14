package com.bonacogo.gameplate.adapter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.model.RewardObject;
import com.bonacogo.gameplate.viewholder.RewardViewHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RewardAdapter extends RecyclerView.Adapter<RewardViewHolder> {
    private ArrayList<RewardObject> rewards;
    private Context context;

    public RewardAdapter(ArrayList<RewardObject> rewards, Context context) {
        this.context = context;
        this.rewards = rewards;
    }

    @NonNull
    @Override
    public RewardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.holder_reward, parent, false);
        return new RewardViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RewardViewHolder holder, int position) {
        RewardObject reward = rewards.get(position);

        String description = reward.getDescription();
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
        holder.setTitle(reward.getType());
        holder.setLabelTarget(reward.getTarget());

        Date purchasedDate = new Date(reward.getEndAt().getSeconds() *1000);
        //multiply the timestampt with 1000 as java expects the time in milliseconds

        Date currentDate = new Date();
        //To calculate the days difference between two dates
        int diffSecond = (int)((purchasedDate.getTime() - currentDate.getTime()) / 1000);

        int numberOfDays = diffSecond / 86400;
        int numberOfHours = (diffSecond % 86400 ) / 3600;
        int numberOfMinutes = ((diffSecond % 86400 ) % 3600 ) / 60;

        holder.setDays(String.valueOf(numberOfDays));
        holder.setHour(String.valueOf(numberOfHours));
        holder.setMinutes(String.valueOf(numberOfMinutes));

        holder.getInfoBtn().setOnClickListener(v -> showDialog(reward.getTerms(), reward.getType()));
    }

    private void showDialog(String detailTxt, String titleTxt) {
        // creo la dialog
        Dialog dialog = new Dialog(context, R.style.Theme_Dialog);


        // setup
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.terms_layout);

        TextView detail = dialog.findViewById(R.id.terms);
        TextView title = dialog.findViewById(R.id.title);

        View outsideOfDialog = dialog.findViewById(R.id.outside_of_dialog);

        detail.setText(detailTxt);
        title.setText(titleTxt);
        dialog.show();

        outsideOfDialog.setOnClickListener(v -> dialog.dismiss());
    }

    @Override
    public int getItemCount() {
        return rewards.size();
    }

    private static int getResId(String resName) throws NoSuchFieldException, IllegalAccessException {
        Field idField = R.drawable.class.getDeclaredField(resName);
        return idField.getInt(idField);
    }
}
