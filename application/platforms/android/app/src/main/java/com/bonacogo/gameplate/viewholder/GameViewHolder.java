package com.bonacogo.gameplate.viewholder;


import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.adapter.GameAdapter;
import com.bonacogo.gameplate.other.GlideApp;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class GameViewHolder extends RecyclerView.ViewHolder {
    private final ColorStateList csl, csl2;
    private MaterialCardView gameCard;
    private LinearLayout content;
    private TextView game, restaurant, address, numFeedback, rating;
    private ArrayList<ImageView> stars;
    private ImageView imgGame;
    private Button play;
    private View view;

    public GameViewHolder(View v, Context context, int type) {
        super(v);
        csl = AppCompatResources.getColorStateList(context, R.color.gold);
        csl2 = AppCompatResources.getColorStateList(context, R.color.lightGray2);
        this.game = v.findViewById(R.id.game);
        this.restaurant = v.findViewById(R.id.restaurant);
        this.address = v.findViewById(R.id.address);
        this.numFeedback = v.findViewById(R.id.num_feedback);
        this.gameCard = v.findViewById(R.id.game_card);
        this.imgGame = v.findViewById(R.id.img_game);
        if (type == GameAdapter.RECENT_GAMES) {
            this.play = v.findViewById(R.id.play_btn);
            this.content = v.findViewById(R.id.content);
            RelativeLayout playContainer = v.findViewById(R.id.play_container);
            LinearLayout contentContainer = v.findViewById(R.id.container_content);
            RelativeLayout cardContainer = v.findViewById(R.id.card_container);

            ViewTreeObserver vto = playContainer.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (playContainer.getMeasuredHeight() > 0) {
                        playContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int translation = playContainer.getMeasuredHeight()/2;
                        contentContainer.setPadding(
                                contentContainer.getPaddingLeft(),
                                contentContainer.getPaddingTop(),
                                contentContainer.getPaddingRight(),
                                contentContainer.getPaddingBottom()+translation);
                        cardContainer.setPadding(0, 0, 0, translation);
                    }
                }
            });
        }
        else {
            this.rating = v.findViewById(R.id.rating);
            ImageView star1 = v.findViewById(R.id.star_1);
            ImageView star2 = v.findViewById(R.id.star_2);
            ImageView star3 = v.findViewById(R.id.star_3);
            ImageView star4 = v.findViewById(R.id.star_4);
            ImageView star5 = v.findViewById(R.id.star_5);

            this.stars = new ArrayList<>();
            this.stars.add(star1);
            this.stars.add(star2);
            this.stars.add(star3);
            this.stars.add(star4);
            this.stars.add(star5);
        }


        this.view = v;
    }

    public LinearLayout getContent() {
        return content;
    }

    public void setGame(String game) {
        this.game.setText(game);
    }

    public void setRestaurant(String restaurant) {
        this.restaurant.setText(restaurant);
    }

    public void setAddress(String address) {
        this.address.setText(address);
    }

    public void setNumFeedback(String numFeedback) {
        this.numFeedback.setText(numFeedback);
    }

    public void setRating(float rating) {
        // imposto il rating del ristorante
        this.rating.setText(String.valueOf(rating).replace('.', ','));

        for (int i=0; i<rating; i++) {
            float waste = (float) (Math.round((rating-i) * 10) / 10.0);
            if (waste >= 0.3 && waste <= 0.7){
                this.stars.get(i).setImageTintList(null);
                this.stars.get(i).setImageResource(R.drawable.ic_star_half);
            }
            else if (waste > 0.7) {
                ImageViewCompat.setImageTintList(this.stars.get(i), csl);
                this.stars.get(i).setImageResource(R.drawable.ic_star_full);
            }
            if (!((i+1)<rating) && i<4) {
                while (i != 4) {
                    i++;
                    ImageViewCompat.setImageTintList(this.stars.get(i), csl2);
                    this.stars.get(i).setImageResource(R.drawable.ic_star_full);
                }
            }
        }
    }

    public MaterialCardView getGameCard() {
        return gameCard;
    }

    public View getView() {
        return view;
    }

    public Button getPlay() {
        return play;
    }

    public ImageView getImgGame() {
        return imgGame;
    }

    public void setImgGame(Fragment fragment, String url) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("game")
                .child(url)
                .child("logo.jpg");

        RequestOptions options = new RequestOptions().fitCenter();
        GlideApp.with(fragment)
                .load(storageReference)
                .placeholder(R.drawable.image_loading)
                .error(R.drawable.image_error)
                .transition(withCrossFade())
                .apply(options)
                .into(imgGame);
    }

}