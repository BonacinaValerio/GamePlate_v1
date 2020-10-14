package com.bonacogo.gameplate.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.other.GlideApp;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class TabRankFragment extends Fragment {
    private static final int WEEKLY = 0;
    private static final int GLOBAL = 1;
    private static final String RANKING = "RANKING";

    public static TabRankFragment newInstance(int tabNumber, HashMap ranking) {
        TabRankFragment fragment = new TabRankFragment();
        Bundle bundle = new Bundle();
        if (tabNumber == WEEKLY)
            bundle.putSerializable(RANKING, (Serializable) ranking.get("weekly"));
        else if (tabNumber == GLOBAL)
            bundle.putSerializable(RANKING, (Serializable) ranking.get("global"));
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle arguments = getArguments();

        View view = inflater.inflate(R.layout.tab_rank_fragment, container, false /* attachToRoot */);

        ProgressBar progressBar = view.findViewById(R.id.progress_bar);

        HashMap rank = (HashMap) arguments.getSerializable(RANKING);

        if (rank != null && !rank.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            setupView(rank, view);
        }
        return view;
    }

    @SuppressLint("SetTextI18n")
    private void setupView(HashMap rank, View view) {
        ArrayList ranking = (ArrayList) rank.get("rank");
        if (ranking.isEmpty()) {
            view.findViewById(R.id.no_rank).setVisibility(View.VISIBLE);
        }
        else {
            for (int i = 0; i < ranking.size(); i++) {
                ArrayList userPos = (ArrayList) ranking.get(i);
                int positionReadable = i + 1;
                int nickId = getResId("nickname_" + positionReadable);
                int scoreId = getResId("score_" + positionReadable);
                int posId = getResId("position_" + positionReadable);
                TextView userNick = view.findViewById(nickId);
                userNick.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                TextView userScore = view.findViewById(scoreId);
                LinearLayout userPosition = view.findViewById(posId);

                userNick.setText((String) userPos.get(0));
                userScore.setText((String) userPos.get(1));
                if (userPos.get(2) != null) {
                    int imgUserId = getResId("img_user_" + positionReadable);
                    ImageView imgUser = view.findViewById(imgUserId);
                    imgUser.setImageTintList(null);
                    String photoUrl = userPos.get(2) + "?height=500";
                    setImgGame(photoUrl, imgUser);
                }
                userPosition.setVisibility(View.VISIBLE);
            }
        }

        HashMap user = (HashMap) rank.get("user");
        if (user != null) {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            ImageView imgUser = view.findViewById(R.id.img_user);

            for(UserInfo profile : firebaseUser.getProviderData()) {
                // check if the provider id matches "facebook.com"
                if(FacebookAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                    imgUser.setImageTintList(null);
                    String facebookUserId = profile.getUid();
                    String photoUrl = "https://graph.facebook.com/" + facebookUserId + "/picture?height=500";
                    setImgGame(photoUrl, imgUser);
                }
            }

            TextView userPos = view.findViewById(R.id.user_pos);
            TextView nicknameUser = view.findViewById(R.id.nickname_user);
            nicknameUser.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            TextView scoreUser = view.findViewById(R.id.score_user);
            LinearLayout positionUser = view.findViewById(R.id.position_user);
            userPos.setText((int) user.get("position") +"°");
            nicknameUser.setText(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            scoreUser.setText(String.valueOf((int) user.get("score")));
            positionUser.setVisibility(View.VISIBLE);
        }
    }

    private static int getResId(String resName) {

        try {
            Field idField = R.id.class.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void setImgGame(String url, ImageView imageView) {

        RequestOptions options = new RequestOptions().fitCenter();
        GlideApp.with(this)
                .load(url)
                .placeholder(R.drawable.image_loading)
                .error(R.drawable.image_error)
                .transition(withCrossFade())
                .apply(options)
                .into(imageView);
    }
}
