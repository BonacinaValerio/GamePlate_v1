package com.bonacogo.gameplate.viewholder;

import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.adapter.TicketSliderAdapter;
import com.bonacogo.gameplate.other.GlideApp;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;




public class TicketViewHolder extends RecyclerView.ViewHolder {


    private ImageView imageView, imgReward;
    private TextView title, mDays, mHours, mMins, labelReward;
    private MaterialCardView ticketCard;


    public TicketViewHolder(@NonNull View itemView, TicketSliderAdapter.OnItemClickListener listener) {
        super(itemView);
        imageView = itemView.findViewById(R.id.imageSlide);
        title = itemView.findViewById(R.id.title);
        mDays = itemView.findViewById(R.id.days);
        mHours = itemView.findViewById(R.id.hour);
        mMins = itemView.findViewById(R.id.minutes);
        imgReward = itemView.findViewById(R.id.img_reward);
        labelReward = itemView.findViewById(R.id.label_reward);
        ticketCard = itemView.findViewById(R.id.ticket_card);
        /*
        gameTitle = itemView.findViewById(R.id.gameTitle);
        rewardType = itemView.findViewById(R.id.rewardType);*/
    }

        /*void setImage(TicketRewardObject sliderTicket) {
            imageView.setImageResource(setImgGame(fragment, "flapshimi"););
            title.setText(sliderTicket.getRestaurant());
            desc.setText(sliderTicket.getType());
        }*/

    public void setImgGame(Fragment fragment, String url) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("restaurants")
                .child(url)
                .child("logo.jpg");

        RequestOptions options = new RequestOptions().fitCenter();
        GlideApp.with(fragment)
                .load(storageReference)
                .placeholder(R.drawable.image_loading)
                .error(R.drawable.image_error)
                .transition(withCrossFade())
                .apply(options)
                .into(imageView);
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }


    public void setmDays(String days) {
        this.mDays.setText(days);
    }

    public void setmHours(String hours) {
        this.mHours.setText(hours);
    }


    public void setmMins(String mins) {
        this.mMins.setText(mins);
    }


    public void setImgReward(int imgReward) {
        this.imgReward.setBackgroundResource(imgReward);
        this.labelReward.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        this.imgReward.setVisibility(View.VISIBLE);
    }

    public void setLabelReward(String labelReward) {
        this.labelReward.setText(labelReward);
    }


    public ImageView getImageView() { return imageView; }

    public TextView getTitle() { return title; }

    public TextView getmDays() { return mDays; }

    public TextView getmHours() { return mHours; }

    public TextView getmMins() { return mMins; }

    public MaterialCardView getTicketCard() {
        return ticketCard;
    }
}