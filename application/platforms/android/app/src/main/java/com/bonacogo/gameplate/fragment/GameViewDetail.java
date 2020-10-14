package com.bonacogo.gameplate.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.adapter.RewardAdapter;
import com.bonacogo.gameplate.dialog.RankingDialog;
import com.bonacogo.gameplate.model.GameObject;
import com.bonacogo.gameplate.model.RewardObject;
import com.bonacogo.gameplate.other.GlideApp;
import com.bonacogo.gameplate.util.GeneralMethod;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class GameViewDetail extends Fragment implements View.OnClickListener {
    static final String TAG = "GameViewDetail";
    private static final String GAME_OBJECT = "GAME_OBJECT";
    private View myFragment;
    private FirebaseFunctions mFunctions;

    private Button playBtn, websiteBtn, mapBtn;
    private ImageView back, rankBtn;
    private RelativeLayout playContainer;
    private LinearLayout headerContainer, content;
    private TextView description, game, restaurant, address, rating, numFeedback;
    private FlexboxLayout category;
    private ImageView header;
    private ArrayList<ImageView> stars;

    private GameObject viewGameObject;

    private ProgressDialog mProgressDialog;

    public interface ActivityCallBack {
        void onBackClick(Fragment fragment, String focusMarker, GameObject gameObject);
        void onGameObjectDone(GameObject gameObject);
        void onPlayClickFromDetail(GameObject gameObject);
    }
    private ActivityCallBack activityCallBack;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallBack = (ActivityCallBack) context;
    }

    static GameViewDetail newInstance(GameObject game) {
        // creo una nuova istanza con il parametro
        GameViewDetail fragment = new GameViewDetail();
        Bundle args = new Bundle();
        args.putSerializable(GAME_OBJECT, game);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myFragment = inflater.inflate(R.layout.fragment_game_view_detail, container, false);

        GameObject gameObject = null;
        // estraggo il parametro
        if (getArguments() != null) {
            gameObject = (GameObject) getArguments().getSerializable(GAME_OBJECT);
        }

        // view
        back = myFragment.findViewById(R.id.back_btn);
        rankBtn = myFragment.findViewById(R.id.rank_btn);
        playBtn = myFragment.findViewById(R.id.play_btn);
        websiteBtn = myFragment.findViewById(R.id.website_btn);
        mapBtn = myFragment.findViewById(R.id.map_btn);

        playContainer = myFragment.findViewById(R.id.play_container);
        headerContainer = myFragment.findViewById(R.id.header_container);
        content = myFragment.findViewById(R.id.content);
        description = myFragment.findViewById(R.id.description);
        game = myFragment.findViewById(R.id.game);
        restaurant = myFragment.findViewById(R.id.restaurant);
        address = myFragment.findViewById(R.id.address);
        category = myFragment.findViewById(R.id.category);
        rating = myFragment.findViewById(R.id.rating);
        numFeedback = myFragment.findViewById(R.id.num_feedback);
        header = myFragment.findViewById(R.id.header);

        ImageView star1 = myFragment.findViewById(R.id.star_1);
        ImageView star2 = myFragment.findViewById(R.id.star_2);
        ImageView star3 = myFragment.findViewById(R.id.star_3);
        ImageView star4 = myFragment.findViewById(R.id.star_4);
        ImageView star5 = myFragment.findViewById(R.id.star_5);

        stars = new ArrayList<>();
        stars.add(star1);
        stars.add(star2);
        stars.add(star3);
        stars.add(star4);
        stars.add(star5);

        if (savedInstanceState != null)
            gameObject = (GameObject) savedInstanceState.getSerializable(GAME_OBJECT);

        if (gameObject != null && gameObject.getDescription() != null)
            setupView(gameObject);
        else {
            if (gameObject != null)
                callFunction(gameObject);
        }

        return myFragment;
    }

    private void callFunction(GameObject gameObject) {
        mFunctions = FirebaseFunctions.getInstance();

        mProgressDialog = GeneralMethod.showProgressDialog(mProgressDialog, getContext(), getString(R.string.loading));

        Map<String, Object> data = new HashMap<>();
        data.put("lang", new Locale(Locale.getDefault().getLanguage()).getLanguage());
        if (gameObject != null)
            data.put("id", gameObject.getId());

        onGetRestaurants(data, "onGetOneRestaurant").addOnCompleteListener(task -> {
            if (mProgressDialog.isShowing()) {
                GeneralMethod.hideProgressDialog(mProgressDialog);
                if (task.isSuccessful()) {
                    GameObject response = task.getResult();
                    if (response != null) {
                        activityCallBack.onGameObjectDone(response);
                        setupView(response);
                    } else
                        error();
                } else {
                    Exception e = task.getException();
                    Log.e(TAG, "onGetRestaurants:onFailure", e);
                    error();
                }
            }
        });
    }

    private void error() {
        String error = "Errore.";
        Toast.makeText(getContext(), error,
                Toast.LENGTH_LONG).show();
        back.setOnClickListener(this);
        back.performClick();
    }

    private Task<GameObject> onGetRestaurants(Map<String, Object> data, String function) {
        return mFunctions
                .getHttpsCallable(function)
                .call(data)
                .continueWith(task -> {
                    // Questa blocco viene eseguito in caso di esito positivo o negativo
                    // se l'attività non è riuscita, getResult() genererà un'eccezione che verrà propagata verso il basso.

                    HashMap response = (HashMap) task.getResult().getData();
                    HashMap game = (HashMap) response.get("result");

                    GsonBuilder gsonBuilder = new GsonBuilder();
                    Gson gson = gsonBuilder.create();

                    String jsonString = gson.toJson(game);
                    Log.i(TAG, "onGetRestaurants: "+jsonString);
                    GameObject gameObject = gson.fromJson(jsonString, GameObject.class);

                    return gameObject;
                });
    }

    private void setupView(GameObject gameObject) {
        game.setText(gameObject.getGame());
        description.setText(gameObject.getDescription());
        restaurant.setText(gameObject.getRestaurant());
        address.setText(gameObject.getAddress().replace("\\n",", "));
        numFeedback.setText(gameObject.getNumFeedback());

        setRating(stars, rating, gameObject.getRating());
        setImgGame(header, gameObject.getUrl());

        Context context = getContext();
        if (context == null)
            return;

        if (gameObject.getWeb().equals("-")) {
            websiteBtn.setEnabled(false);

            int colorStateList = ContextCompat.getColor(context, R.color.gray);
            ((TextView)myFragment.findViewById(R.id.website_label)).setTextColor(colorStateList);
            ((ImageView)myFragment.findViewById(R.id.website_icon)).setColorFilter(colorStateList, android.graphics.PorterDuff.Mode.SRC_IN);

            websiteBtn.setBackgroundResource(R.drawable.button_bg_11);
        }

        String[] types = gameObject.getType();
        for (String type : types) {
            TextView single_type = new TextView(new ContextThemeWrapper(context, R.style.CategoryRestaurant));
            single_type.setText(type);

            LinearLayout linearLayout = new LinearLayout(getContext());
            int padding_in_dp = 10, margin_in_dp_top = 2, margin_in_dp_end = 5;
            final float scale = getResources().getDisplayMetrics().density;
            int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
            int margin_in_px_top = (int) (margin_in_dp_top * scale + 0.5f);
            int margin_in_px_end = (int) (margin_in_dp_end * scale + 0.5f);

            linearLayout.setBackground(ContextCompat.getDrawable(context, (R.drawable.button_bg_07)));
            linearLayout.setPadding(padding_in_px, margin_in_dp_top, padding_in_px, margin_in_dp_top);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, margin_in_px_top, margin_in_px_end, 0);
            linearLayout.setLayoutParams(layoutParams);

            linearLayout.addView(single_type);

            category.addView(linearLayout);
        }


        RecyclerView rewards = myFragment.findViewById(R.id.rewards);

        RewardAdapter adapterReward = new RewardAdapter(cleanReward(gameObject.getRewards()), getContext());
        rewards.setLayoutManager(new GridLayoutManager(getContext(),2));
        rewards.setAdapter(adapterReward);

        // listener on click
        back.setOnClickListener(this);
        rankBtn.setOnClickListener(this);
        playBtn.setOnClickListener(this);
        websiteBtn.setOnClickListener(this);
        mapBtn.setOnClickListener(this);
        ViewTreeObserver vto = headerContainer.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (playContainer.getMeasuredHeight() > 0) {
                    int translate = playContainer.getMeasuredHeight()/2;
                    headerContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) content.getLayoutParams();
                    layoutParams.setMargins(0, -translate, 0, 0);
                    content.setLayoutParams(layoutParams);
                    content.setVisibility(View.VISIBLE);
                }
            }
        });

        this.viewGameObject = gameObject;
    }

    private ArrayList<RewardObject> cleanReward(ArrayList<RewardObject> rewards) {
        ArrayList<RewardObject> newRewards = (ArrayList<RewardObject>) rewards.clone();
        for (int i = 0; i < rewards.size(); i++) {

            Date purchasedDate = new Date(rewards.get(i).getEndAt().getSeconds() *1000);
            //multiply the timestampt with 1000 as java expects the time in milliseconds

            Date currentDate = new Date();
            //To calculate the days difference between two dates
            int diffSecond = (int)(purchasedDate.getTime() - currentDate.getTime());

            if (diffSecond < 0)
                newRewards.remove(i);
        }
        return newRewards;
    }

    private void setImgGame(ImageView imgGame, String url) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("game")
                .child(url)
                .child("logo.jpg");

        RequestOptions options = new RequestOptions().fitCenter();
        GlideApp.with(this)
                .load(storageReference)
                .placeholder(R.drawable.image_loading)
                .error(R.drawable.image_error)
                .transition(withCrossFade())
                .apply(options)
                .into(imgGame);
    }

    private void setRating(ArrayList<ImageView> stars, TextView rating, float rate) {
        Context context = getContext();
        if (context == null)
            return;

        // imposto il rating del ristorante
        rating.setText(String.valueOf(rate).replace('.', ','));

        ColorStateList csl = AppCompatResources.getColorStateList(context, R.color.gold);
        ColorStateList csl2 = AppCompatResources.getColorStateList(context, R.color.lightGray2);
        for (int i=0; i<rate; i++) {
            float waste = (float) (Math.round((rate-i) * 10) / 10.0);
            if (waste >= 0.3 && waste <= 0.7){
                stars.get(i).setImageTintList(null);
                stars.get(i).setImageResource(R.drawable.ic_star_half);
            }
            else if (waste > 0.7) {
                ImageViewCompat.setImageTintList(stars.get(i), csl);
                stars.get(i).setImageResource(R.drawable.ic_star_full);
            }
            if (!((i+1)<rate) && i<4) {
                while (i != 4) {
                    i++;
                    ImageViewCompat.setImageTintList(stars.get(i), csl2);
                    stars.get(i).setImageResource(R.drawable.ic_star_full);
                }
            }
        }
    }


    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.back_btn) {
            disableBtn();
            activityCallBack.onBackClick(this, null, null);
        }
        else if (i == R.id.rank_btn) {
            if (viewGameObject != null) {
                FragmentManager fManager = getFragmentManager();
                if (fManager == null)
                    return;

                RankingDialog rankingDialog = new RankingDialog(viewGameObject);
                rankingDialog.show(fManager, "ranking");
                rankingDialog.setCancelable(true);
            }
        }
        else if (i == R.id.play_btn) {
            if (viewGameObject != null) {
                activityCallBack.onPlayClickFromDetail(viewGameObject);
            }
        }
        else if (i == R.id.website_btn) {
            if (viewGameObject != null) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(viewGameObject.getWeb()));
                startActivity(browserIntent);
            }
        }
        else if (i == R.id.map_btn) {
            if (viewGameObject != null) {
                disableBtn();
                activityCallBack.onBackClick(this, viewGameObject.getId(), viewGameObject);
            }
        }
    }

    private void disableBtn() {
        back.setEnabled(false);
        playBtn.setEnabled(false);
        websiteBtn.setEnabled(false);
        mapBtn.setEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        GeneralMethod.hideProgressDialog(mProgressDialog);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(GAME_OBJECT, viewGameObject);
        super.onSaveInstanceState(outState);
    }

}