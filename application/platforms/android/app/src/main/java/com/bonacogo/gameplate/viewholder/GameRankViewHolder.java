package com.bonacogo.gameplate.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.other.GlideApp;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class GameRankViewHolder extends RecyclerView.ViewHolder {
    private ImageView gameImg;
    private TextView game;
    private CardView cardGameCard;

    public GameRankViewHolder(@NonNull View v) {
        super(v);
        this.gameImg = v.findViewById(R.id.game_img);
        this.game = v.findViewById(R.id.game);
        this.cardGameCard = v.findViewById(R.id.card_game_rank);
    }

    public CardView getCardGameCard() {
        return cardGameCard;
    }

    public ImageView getGameImg() {
        return gameImg;
    }

    public void setGameImg(Fragment fragment, String url) {
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
                .into(gameImg);
    }

    public TextView getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game.setText(game);
    }
}
