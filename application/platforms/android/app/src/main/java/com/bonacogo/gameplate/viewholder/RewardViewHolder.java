package com.bonacogo.gameplate.viewholder;

import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bonacogo.gameplate.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RewardViewHolder extends RecyclerView.ViewHolder {
    private TextView title, labelReward, labelTarget, days, hour, minutes;
    private ImageView imgReward;
    private ImageButton infoBtn;

    public RewardViewHolder(@NonNull View v) {
        super(v);
        this.title = v.findViewById(R.id.title);
        this.labelReward = v.findViewById(R.id.label_reward);
        this.labelTarget = v.findViewById(R.id.label_target);
        this.days = v.findViewById(R.id.days);
        this.hour = v.findViewById(R.id.hour);
        this.minutes = v.findViewById(R.id.minutes);
        this.infoBtn = v.findViewById(R.id.info_btn);
        this.imgReward = v.findViewById(R.id.img_reward);
    }

    public TextView getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

    public TextView getLabelReward() {
        return labelReward;
    }

    public void setLabelReward(String labelReward) {
        this.labelReward.setText(labelReward);
    }

    public TextView getLabelTarget() {
        return labelTarget;
    }

    public void setLabelTarget(String labelTarget) {
        this.labelTarget.setText(labelTarget);
    }

    public TextView getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days.setText(days);
    }

    public TextView getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour.setText(hour);
    }

    public TextView getMinutes() {
        return minutes;
    }

    public void setMinutes(String minutes) {
        this.minutes.setText(minutes);
    }

    public ImageView getInfoBtn() {
        return infoBtn;
    }

    public ImageView getImgReward() {
        return imgReward;
    }

    public void setImgReward(int imgReward) {
        this.imgReward.setBackgroundResource(imgReward);
        this.labelReward.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        this.imgReward.setVisibility(View.VISIBLE);
    }
}
