/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.bonacogo.gameplate;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.bonacogo.gameplate.adapter.GameAdapter;
import com.bonacogo.gameplate.bottomnav.CustomBottomNavigationView;
import com.bonacogo.gameplate.fragment.AwardFragment;
import com.bonacogo.gameplate.fragment.BaseFragment;
import com.bonacogo.gameplate.fragment.GameRankFragment;
import com.bonacogo.gameplate.fragment.GameViewDetail;
import com.bonacogo.gameplate.fragment.HistorySearchFragment;
import com.bonacogo.gameplate.fragment.HomeFragment;
import com.bonacogo.gameplate.fragment.ListGamesFragment;
import com.bonacogo.gameplate.fragment.LoginFragment;
import com.bonacogo.gameplate.fragment.LoginScreen;
import com.bonacogo.gameplate.fragment.MapFragment;
import com.bonacogo.gameplate.fragment.ProfileFragment;
import com.bonacogo.gameplate.fragment.RegisterFragment;
import com.bonacogo.gameplate.fragment.SearchResultFragment;
import com.bonacogo.gameplate.fragment.SettingsFragment;
import com.bonacogo.gameplate.fragment.TicketDetailsFragment;
import com.bonacogo.gameplate.fragment.Tutorial01;
import com.bonacogo.gameplate.interpolator.CustomBounceInterpolator;
import com.bonacogo.gameplate.model.GameObject;
import com.bonacogo.gameplate.model.TicketRewardObject;
import com.bonacogo.gameplate.radiobutton.PresetRadioGroup;
import com.bonacogo.gameplate.slider.CustomSlider;
import com.bonacogo.gameplate.util.LocationUtil;
import com.bonacogo.gameplate.viewmodel.GameRankingViewModel;
import com.bonacogo.gameplate.viewmodel.SettingsViewModel;
import com.bonacogo.gameplate.viewmodel.TicketRewardViewModel;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.transition.MaterialArcMotion;
import com.google.android.material.transition.MaterialContainerTransform;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import br.com.simplepass.loadingbutton.customViews.CircularProgressButton;

public class MainActivity extends BaseActivity implements
        BaseFragment.ActivityCallback,
        ProfileFragment.ActivityCallback,
        GameRankFragment.ActivityCallBack,
        SettingsFragment.ActivityCallBack,
        AwardFragment.ActivityCallBack,
        TicketDetailsFragment.ActivityCallBack,
        HomeFragment.ActivityCallback,
        MapFragment.ActivityCallBack,
        HistorySearchFragment.ActivityCallBack,
        ListGamesFragment.ActivityCallBack,
        GameViewDetail.ActivityCallBack {
    private static final String TAG = "MainActivity";
    private static final int MARGIN_FAB = 22;

    CustomBottomNavigationView bottomNavigation;
    FloatingActionButton fabButton;

    private LinearLayout containerNav;
    private RelativeLayout contentUp;
    private RelativeLayout gameDetail;
    private RelativeLayout containerGame;
    private RelativeLayout filterContainer;
    private MaterialCardView filterCard;
    private CustomSlider sliderDistance;
    private PresetRadioGroup radioOrderBy;

    Animation animFabZoomIn, animFabZoomOut;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!LocationUtil.checkPermissions(this)) {
            requestPermissions(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH));
        }

        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // setup views
        contentUp = findViewById(R.id.content_up);
        containerGame = findViewById(R.id.container_game);
        gameDetail = findViewById(R.id.game_detail);
        containerNav = findViewById(R.id.container_nav);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        fabButton = findViewById(R.id.navigation_home);
        LinearLayout fabContainer = findViewById(R.id.fab_container);
        filterCard = findViewById(R.id.filter_card);
        sliderDistance = findViewById(R.id.slider_distance);
        radioOrderBy = findViewById(R.id.radio_order_by);
        filterContainer = findViewById(R.id.filter_container);

        // setup slider distance
        sliderDistance.setLabelBehavior(Slider.LABEL_WITHIN_BOUNDS);
        sliderDistance.setTrackColorActive(getResources().getColorStateList(R.color.primaryColor));
        sliderDistance.setTrackColorInactive(getResources().getColorStateList(R.color.secondaryLightColor));
        sliderDistance.setTickColor(ColorStateList.valueOf(Color.TRANSPARENT));
        sliderDistance.setThumbColor(getResources().getColorStateList(R.color.secondaryColor));
        sliderDistance.setValueFrom(10);
        sliderDistance.setValueTo(50);

        ScrollView scrollView = findViewById(R.id.filter_list);
        LinearLayout distanceContainer = findViewById(R.id.distance_container);
        // imposto l'ombra della slider distance quando scrollo i filtri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                    distanceContainer.setSelected(scrollView.canScrollVertically(-1)));
        }

        // diametro della conca nel bottomNavigation
        int diameterArc = bottomNavigation.getCURVE_CIRCLE_RADIUS()*2;
        // diametro del fabButton
        int customSize = diameterArc - MARGIN_FAB*2;
        // setup diametro del fab
        fabButton.setCustomSize(customSize);

        // imposto il margine del fabButton per evitare che con l'animazione vada fuori dal margine del layout al livello superiore
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(MARGIN_FAB,MARGIN_FAB,MARGIN_FAB,MARGIN_FAB);
        fabButton.setLayoutParams(layoutParams);
        // traslo il fabContainer e non il fabbutton così che resti centrato nel suo container
        fabContainer.setTranslationY(diameterArc/2f);

        // Use bounce_in interpolator with amplitude 0.2 and frequency 10
        CustomBounceInterpolator interpolator = new CustomBounceInterpolator(0.2, 10);

        // setup animazioni
        animFabZoomIn = AnimationUtils.loadAnimation(this, R.anim.bounce_in);
        animFabZoomIn.setFillAfter(true);
        animFabZoomIn.setInterpolator(interpolator);

        animFabZoomOut = AnimationUtils.loadAnimation(this, R.anim.bounce_out);
        animFabZoomOut.setFillAfter(true);
        animFabZoomOut.setInterpolator(interpolator);


        // listener onclick
        fabButton.setOnClickListener(v -> toggleAnimFab());

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (savedInstanceState == null) {
            updateUI(currentUser);
        }
        else {
            restoreUI(currentUser, savedInstanceState);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String action = bundle.getString(MyFirebaseMessagingService.NOTIFICATION_KEY);
            if (action != null && action.equals(MyFirebaseMessagingService.NOTIFICATION_MESSAGE)) {
                Log.i(TAG, "onNewIntent: " + action);

                FragmentManager fManager = getSupportFragmentManager();
                Fragment fragment = fManager.findFragmentById(R.id.game_detail);
                Fragment fragment1 = fManager.findFragmentById(R.id.content_up);

                if (fragment1 instanceof SettingsFragment)
                    removeFragment(fragment1);

                if (fragment != null) {
                    containerNav.setVisibility(View.VISIBLE);
                    showBottomNav(() -> {
                        FragmentTransaction fTransaction = fManager.beginTransaction();
                        fTransaction.remove(fragment).commit();
                        findViewById(R.id.container_game).setClickable(false);
                        findViewById(R.id.game_detail).setVisibility(View.GONE);

                        setItemMenuEnable(false, true);
                        fabButton.setEnabled(true);
                    });
                }

                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    openFragment("award");
                    bottomNavigation.setSelectedItemId(R.id.navigation_award);
                }
                else
                    showLogin();
            }
        }

    }

    private void restoreUI(FirebaseUser user, Bundle savedInstanceState) {
        boolean stateFab = savedInstanceState.getBoolean("STATE_FAB");
        fabButton.setActivated(stateFab);
        if (!stateFab)
            fabButton.startAnimation(animFabZoomOut);
        if (user == null) {
            bottomNavigation.getMenu().getItem(1).setChecked(true);
            bottomNavigation.getMenu().setGroupCheckable(0, false,true);
            // cambia il listener del bottomNavigation così che apra la loginScreen al click
            bottomNavigation.setOnNavigationItemSelectedListener(navigationItemSelectedListenerNotLogged);
        }
        else {
            bottomNavigation.getMenu().setGroupCheckable(0, true, true);
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_layout);
            if (fragment instanceof ProfileFragment)
                setItemMenuEnable(true, false);
            else if (fragment instanceof AwardFragment)
                setItemMenuEnable(false, true);
            // cambia il listener del bottomNavigation così che apra i vari fragment
            bottomNavigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener);
        }
    }

    private void toggleAnimFab() {
        // se il fabButton è attivato e viene premuto esegui l'animazione zoomIn solo per effetto visivo
        if (fabButton.isActivated())
            fabButton.startAnimation(animFabZoomIn);
        // in caso contrario esegui l'animazione zoomIn e cambia il fragment con quello di home
        else {
            fabButton.startAnimation(animFabZoomIn);
            openFragment("home");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean("STATE_FAB", fabButton.isActivated());
        super.onSaveInstanceState(outState);
    }

    // aggiorna l'ui a fronte di un cambiamento dello stato del FirebaseUser oppure al primo avvio dell'activity
    public void updateUI(FirebaseUser user) {
        // se il fab non è attivato e si esegue un updateUi vuol dire che è stato effettuato un logOut
        // quindi attiva il fab con l'animazione e continua con le istruzioni
        if (!fabButton.isActivated()) {
            fabButton.setActivated(true);
            toggleAnimFab();
        }
        // attiva il click dei due tasti dx e sx nel bottomNavigation
        setItemMenuEnable(true, true);
        // imposta su checked il tasto centrale (invisibile) così che gli altri due tolgano il loro check
        bottomNavigation.getMenu().getItem(1).setChecked(true);

        if (user == null) {
            // disabila il check di tutti i tasti
            bottomNavigation.getMenu().setGroupCheckable(0, false,true);
            // cambia il listener del bottomNavigation così che apra la loginScreen al click
            bottomNavigation.setOnNavigationItemSelectedListener(navigationItemSelectedListenerNotLogged);
            // apri il fragment home SENZA refresh così che mantenga lo stato
            openFragment("home");
            // se è il primo avvio "firstrun" non esiste e quindi è true
            SharedPreferences prefs = getSharedPreferences("com.bonacogo.gameplate", MODE_PRIVATE);
            if (prefs.getBoolean("firstRun", true)) {
                // imposta visibile il content_up layout e cambia il fragment con Tutorial01
                RelativeLayout content_tutorial = findViewById(R.id.content_up);
                content_tutorial.setVisibility(View.VISIBLE);
                FragmentTransaction fTransaction = getSupportFragmentManager().beginTransaction();
                Fragment fragment = new Tutorial01();
                fTransaction.add(R.id.content_up, fragment).commit();
            }
        }
        else{
            TicketRewardViewModel ticketRewardViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(TicketRewardViewModel.class);
            ticketRewardViewModel.init();
            ticketRewardViewModel.getTickets();

            GameRankingViewModel gameRankingViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(GameRankingViewModel.class);
            gameRankingViewModel.getGames();

            SettingsViewModel settingsViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(SettingsViewModel.class);
            settingsViewModel.getEnableNotification();

            setUserVariable();
            // abilita il check di tutti gli stati
            bottomNavigation.getMenu().setGroupCheckable(0, true, true);
            // cambia il listener del bottomNavigation così che apra i vari fragment
            bottomNavigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener);
            // apri il fragment home CON refresh
            openFragment("home");
        }
    }

    private void setUserVariable() {
        String uid = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("lang")
                .setValue(new Locale(Locale.getDefault().getLanguage()).getLanguage().toUpperCase());
        saveToken(uid);
    }

    private void saveToken(String uid) {
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult().getToken();
                FirebaseDatabase.getInstance().getReference("users").child(uid)
                        .child("notificationTokens").child(token).setValue(true);
            }
            else {
                Toast.makeText(MainActivity.this, R.string.error_set_token, Toast.LENGTH_LONG).show();
            }

        });
    }

    // listener BottomNavigation di utente loggato
    BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener =
            bottomNavigationView -> {
                switch (bottomNavigationView.getItemId()) {
                    case R.id.navigation_profile:
                        openFragment("profile");
                        return true;
                    case R.id.navigation_award:
                        openFragment("award");
                        return true;
                }
                return false;
            };

    // listener BottomNavigation di utente non loggato
    BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListenerNotLogged =
            bottomNavigationView -> {
                showLogin();
                return true;
            };

    public void showLogin() {
        // mostra la loginScreen con animazione
        FragmentManager fManager = getSupportFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();
        MaterialFadeThrough fadeThrough = MaterialFadeThrough.create();

        Fragment fragment0 = fManager.findFragmentById(R.id.content_up);
        if (fragment0 != null)
            fManager.beginTransaction().remove(fragment0).commitNow();

        RelativeLayout content_tutorial = findViewById(R.id.content_up);
        content_tutorial.setVisibility(View.VISIBLE);

        Fragment fragment = new LoginScreen();
        fragment.setEnterTransition(fadeThrough);
        fTransaction.replace(R.id.content_up, fragment).commit();
    }

    public void openFragment(String fragmentId) {
        // apri il fragment dato l'id
        FragmentManager fManager = getSupportFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();

        MapFragment map = (MapFragment) fManager.findFragmentById(R.id.map);

        Fragment fragment;
        MaterialFadeThrough fadeThrough = MaterialFadeThrough.create();
        if(fragmentId.equals("home")) {
            // se si apre il fragment home attiva il fabbutton nel caso in cui non sia attivato
            if (!fabButton.isActivated())
                fabButton.setActivated(true);
            // abilita il click dei tasti dx e sx
            setItemMenuEnable(true, true);
            // esegui il check del tasto centrale così che gli altri tolgano il loro
            bottomNavigation.getMenu().getItem(1).setChecked(true);
            // apri il fragment home senza crearne una nuova istanza a meno che sia la prima oppure si forza il refresh
            fragment = fManager.findFragmentByTag(fragmentId);
            if(fragment == null) {
                fragment = new HomeFragment();
                fTransaction.addToBackStack("home");
            }
            fragment.setEnterTransition(fadeThrough);
            fTransaction.replace(R.id.content_layout, fragment, fragmentId).commit();
        }
        else {
            // rimuovi la mappa perchè attaccata a view obsoleta
            if (map != null)
                removeFragment(fManager, map, false, false);
            // se il fab era attivo lo si disattiva con animazione
            if (fabButton.isActivated()) {
                fabButton.startAnimation(animFabZoomOut);
                fabButton.setActivated(false);
            }
            // cambia il fragment
            switch (fragmentId) {
                case "award":
                    fragment = new AwardFragment();
                    setItemMenuEnable(false, true);
                    break;
                case "profile":
                    fragment = new ProfileFragment();
                    setItemMenuEnable(true, false);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + fragmentId);
            }
            fragment.setEnterTransition(fadeThrough);
            fTransaction.replace(R.id.content_layout, fragment).commit();

        }
    }

    private void removeFragment(FragmentManager fManager, Fragment fragment, boolean popSearch, boolean popMap) {
        if (popSearch)
            fManager.popBackStack("search", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction fTransaction = fManager.beginTransaction();
        fTransaction.remove(fragment).commit();

        if (popMap) {
            fManager.popBackStack("map", FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fManager.popBackStack("home", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    public void setItemMenuEnable(boolean sx, boolean dx) {
        // attiva o disattiva il check dei tasti sx e dx
        bottomNavigation.getMenu().getItem(0).setEnabled(sx);
        bottomNavigation.getMenu().getItem(2).setEnabled(dx);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment_content = fragmentManager.findFragmentById(R.id.content_layout);

        // quando si preme il tasto back verifica cosa si sta visualizzando
        Fragment fragment_up = fragmentManager.findFragmentById(R.id.content_up);
        // se si è in RegisterFragment o LoginFragment torna nel loginScreen
        if (fragment_up instanceof RegisterFragment || fragment_up instanceof LoginFragment) {
            backPressedInScreenLogin(fragmentManager);
            return;
        }
        else if (fragment_up instanceof SettingsFragment) {
            FragmentTransaction fTransaction = fragmentManager.beginTransaction();
            fTransaction.remove(fragment_up).commit();
            return;
        }
        Fragment fragment_game = fragmentManager.findFragmentById(R.id.game_detail);
        if (fragment_game instanceof GameViewDetail && gameDetail.getVisibility() == View.VISIBLE) {
            GameViewDetail gameViewDetail = (GameViewDetail) fragment_game;
            this.onBackClick(gameViewDetail, null, null);
            return;
        }else if (fragment_game instanceof GameRankFragment && gameDetail.getVisibility() == View.VISIBLE) {
            if (fragment_content instanceof ProfileFragment)
                ((ProfileFragment) fragment_content).onBackClick(fragment_game);
            return;
        }else if (fragment_game instanceof  TicketDetailsFragment && gameDetail.getVisibility() == View.VISIBLE) {
            if (fragment_content instanceof AwardFragment)
                ((AwardFragment) fragment_content).onBackClick(fragment_game);
            return;
        }

        // se si è in HomeFragment e lo stato di ricerca è attivo allora è come premere searchButton
        // altrimenti si chiude l'app
        if (fragment_content instanceof HomeFragment) {
            HomeFragment homeFragment = (HomeFragment) fragment_content;
            boolean isSearchChecked = homeFragment.getIsSearchChecked();
            // effettuo un controllo che non sia visibile il filtro di ricerca oppure che la bottomsheet sia alzata
            if (findViewById(R.id.scrim).getVisibility()==View.VISIBLE)
                findViewById(R.id.scrim).performClick();
            else if (homeFragment.checkChangeStateBottomSheet()) {
                return;
            }
            else if (isSearchChecked) {
                homeFragment.clickSearchButton();
            }
            else {
                // se non si è in ricerca controllo che non vi siano marker sulla mappa (il controllo li rimuove)
                // se non ci sono marker chiudo l'app
                Fragment map = fragmentManager.findFragmentById(R.id.map);
                if(map instanceof MapFragment) {
                    if (!((MapFragment) map).clearMapNotInSearch())
                        finish();
                }
            }
        }
        else
            finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment frg = getSupportFragmentManager().findFragmentById(R.id.map);
        if (frg != null) {
            frg.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void hideBottomNav(Runnable runnable) {
        containerNav.animate()
                .alpha(0f)
                .translationY(80)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        runnable.run();
                    }
                });
    }

    public void showBottomNav(Runnable runnable) {
        containerNav.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        runnable.run();
                    }
                });
    }

    private Fragment getFragment(String tag, Class<?> type) {
        return getFragment(-1, tag, type);
    }
    private Fragment getFragment(int id, Class<?> type) {
        return getFragment(id, null, type);
    }
    private Fragment getFragment(int id, String tag, Class<?> type) {
        FragmentManager fManager = getSupportFragmentManager();
        Fragment fragment;
        if (tag != null)
            fragment = fManager.findFragmentByTag(tag);
        else {
            fragment = fManager.findFragmentById(id);
        }

        if(type.isInstance(fragment))
            return fragment;
        else
            return null;
    }

    // ------ START MAP FRAGMENT CALLBACK ------
    @Override
    public void onReady(GoogleMap map, Bundle savedState, String markerChecked, HashMap<String, Marker> listMarkers, Bitmap bitmap) {
        // comunicazione MapFragment - HomeFragment
        Fragment home = getFragment("home", HomeFragment.class);
        if (home != null)
            ((HomeFragment) home).updateMap(map, savedState, markerChecked, listMarkers, bitmap);
    }

    @Override
    public void onMarkerClick(Marker marker) {
        Fragment listGames = getFragment(R.id.content_bottom_sheet, ListGamesFragment.class);
        if (listGames != null)
            ((ListGamesFragment) listGames).onMarkerClick(marker);
    }

    @Override
    public void onInfoWindowClick() {
        Fragment listGames = getFragment(R.id.content_bottom_sheet, ListGamesFragment.class);
        if (listGames != null)
            ((ListGamesFragment) listGames).onInfoWindowClick();
    }

    @Override
    public CircularProgressButton getSearchHere() {
        Fragment home = getFragment("home", HomeFragment.class);
        if (home != null)
            return ((HomeFragment) home).getSearchHere();
        return null;
    }

    @Override
    public FloatingActionButton getFindMe() {
        Fragment home = getFragment("home", HomeFragment.class);
        if (home != null)
            return ((HomeFragment) home).getFindMe();
        return null;
    }

    @Override
    public void setupSearch(SearchResultFragment searchResultFragment, boolean newSearch) {
        Fragment home = getFragment("home", HomeFragment.class);
        if (home != null)
            ((HomeFragment) home).setupSearch(searchResultFragment, newSearch);
    }

    @Override
    public void setResult(LinkedHashMap<String, GameObject> gameObjects) {
        Fragment search = getFragment("search", SearchResultFragment.class);
        if (search != null)
            ((SearchResultFragment) search).setResult(gameObjects);
    }

    @Override
    public void setCircularProgressActive(boolean active) {
        Fragment home = getFragment("home", HomeFragment.class);
        if (home != null)
            ((HomeFragment) home).setCircularProgressActive(active);
    }

    // ------ END MAP FRAGMENT CALLBACK ------

    // ------ START GAMEVIEWDETAIL FRAGMENT CALLBACK ------
    @Override
    public void onBackClick(Fragment fragment0, String focusMarker, GameObject gameObject) {
        // comunicazione GameViewDetail - RecentGamesFragment/SearchResultFragment
        FragmentManager fManager = getSupportFragmentManager();
        Fragment fragment1 = fManager.findFragmentById(R.id.content_bottom_sheet);
        if (!(fragment1 instanceof ListGamesFragment)) {
            // per bug dovuto a click rapidi in fase di animazione, bug molto raro
            showBottomNav(() -> {
                FragmentTransaction fTransaction = fManager.beginTransaction();
                fTransaction.remove(fragment0).commit();
                findViewById(R.id.container_game).setClickable(false);
                findViewById(R.id.game_detail).setVisibility(View.GONE);

                Fragment home = fManager.findFragmentByTag("home");
                if (home instanceof HomeFragment)
                    ((HomeFragment) home).setGlobalNavigationActive(true);
            });
        }
        else {
            ((ListGamesFragment) fragment1).backFromGameDetail(fragment0, focusMarker, gameObject);
        }
    }

    @Override
    public void onGameObjectDone(GameObject gameObject) {
        Fragment listGames = getFragment(R.id.content_bottom_sheet, ListGamesFragment.class);
        if (listGames != null)
            ((ListGamesFragment) listGames).onGameObjectDone(gameObject);
    }

    @Override
    public void onPlayClickFromDetail(GameObject gameObject) {
        Fragment listGames = getFragment(R.id.content_bottom_sheet, ListGamesFragment.class);
        if (listGames != null)
            ((ListGamesFragment) listGames).onPlayClickFromDetail(gameObject);
    }

    @Override
    public void enableFocusMarker(int typeAdapter, GameObject gameObject, String focusMarker) {
        Fragment map = getFragment(R.id.map, MapFragment.class);
        if (map != null) {
            if (typeAdapter == GameAdapter.RECENT_GAMES)
                ((MapFragment) map).enableFocusMarker(true, gameObject);
            else
                ((MapFragment) map).enableFocusMarker(true, focusMarker);
        }
    }

    // ------ END GAMEVIEWDETAIL FRAGMENT CALLBACK ------

    // ------ START PROFILE FRAGMENT CALLBACK ------

    @Override
    public void setContentUpVisibility(int visibility) {
        contentUp.setVisibility(visibility);
    }

    @Override
    public void showGameRank(ProfileFragment fragment, View start, String detail) {

        setGameRankViewDetail(detail);

        // Construct a container transform transition between two views.
        MaterialContainerTransform transition = buildContainerTransform(true, 1);
        transition.setStartView(start);
        transition.setEndView(gameDetail);

        hideBottomNav(() -> {
            containerGame.setClickable(true);
            containerNav.setVisibility(View.GONE);
        });

        // Trigger the container transform transition.
        TransitionManager.beginDelayedTransition(containerGame, transition);
        start.setVisibility(View.INVISIBLE);
        gameDetail.setVisibility(View.VISIBLE);
    }

    @NonNull
    private MaterialContainerTransform buildContainerTransform(boolean start, int type) {
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setScrimColor(Color.TRANSPARENT);
        transform.setPathMotion(new MaterialArcMotion());
        if (start)
            transform.setDrawingViewId(R.id.game_detail);
        else {
            if (type == 0)
                transform.setDrawingViewId(R.id.coordinator);
            else if (type == 1)
                transform.setDrawingViewId(R.id.main_container);
            else
                transform.setDrawingViewId(R.id.main_container2);
        }
        return transform;
    }

    private void setGameRankViewDetail(String game) {

        // mostra la GameRank con animazione

        setItemMenuEnable(false, false);
        fabButton.setEnabled(false);

        FragmentTransaction fTransaction = getSupportFragmentManager().beginTransaction();

        GameRankFragment fragment = GameRankFragment.newInstance(game);
        fTransaction.replace(R.id.game_detail, fragment).commit();
    }

    @Override
    public void hideGameRank(Fragment fragment, View start) {
        // Construct a container transform transition between two views.
        MaterialContainerTransform transition = buildContainerTransform(false, 1);
        transition.setStartView(gameDetail);
        transition.setEndView(start);


        containerNav.setVisibility(View.VISIBLE);
        showBottomNav(() -> {
            FragmentTransaction fTransaction = getSupportFragmentManager().beginTransaction();
            fTransaction.remove(fragment).commit();
            containerGame.setClickable(false);

            setItemMenuEnable(true, false);
            fabButton.setEnabled(true);
        });

        // Trigger the container transform transition.
        TransitionManager.beginDelayedTransition(containerGame, transition);
        gameDetail.setVisibility(View.GONE);
        start.setVisibility(View.VISIBLE);
    }



    // ------ END PROFILE FRAGMENT CALLBACK ------

    // ------ START LOGIN SCREEN FRAGMENT CALLBACK ------

    @Override
    public void okBackClick() {
        backPressedInScreenLogin(getSupportFragmentManager());
    }

    private void backPressedInScreenLogin(FragmentManager fragmentManager) {
        Fragment loginScreen = new LoginScreen();
        FragmentTransaction fTransaction1 = fragmentManager.beginTransaction();
        fTransaction1
                .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.content_up, loginScreen).commit();
    }

    @Override
    public void removeFragment(Fragment fragment) {

        // rimuovi il frammento on top e nascondi la view
        FragmentTransaction fTransaction = getSupportFragmentManager().beginTransaction();
        fTransaction.remove(fragment).commit();
        contentUp.setVisibility(View.GONE);
    }


    // ------ END LOGIN SCREEN FRAGMENT CALLBACK ------

    // ------ START HOME FRAGMENT CALLBACK ------

    @Override
    public RelativeLayout getFilterScrim() {
        return findViewById(R.id.scrim);
    }

    @Override
    public void launchGameUserNull() {
        updateUI(null);
        showLogin();
    }

    @Override
    public void setFilterCardMargin(int bottomMargin, int topMargin) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) filterCard.getLayoutParams();
        layoutParams.setMargins(0, topMargin, 0, bottomMargin);
        filterCard.setLayoutParams(layoutParams);
    }

    @Override
    public int getHeightDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        // altezza in px dello schermo
        return size.y;
    }

    @Override
    public void showFilterScreen(MaterialContainerTransform transition, int distance, int orderBy) {
        sliderDistance.setValue(distance);
        radioOrderBy.setChecked(orderBy);
        transition.setEndView(filterCard);
        transition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) { }

            @Override
            public void onTransitionEnd(Transition transition) {
                // alla fine dell'animazione imposto il focus della slider che rende visibile
                // il cursore
                sliderDistance.setFocus(true, distance);
            }

            @Override
            public void onTransitionCancel(Transition transition) { }
            @Override
            public void onTransitionPause(Transition transition) { }
            @Override
            public void onTransitionResume(Transition transition) { }
        });
        // Trigger the container transform transition.
        TransitionManager.beginDelayedTransition(filterContainer, transition);
        filterCard.setVisibility(View.VISIBLE);
    }

    @Override
    public int[] hideFilterScreen(MaterialContainerTransform transition) {
        transition.setStartView(filterCard);
        // Trigger the container transform transition.
        TransitionManager.beginDelayedTransition(filterContainer, transition);

        // disabilito il click del filterScrim e animo la filterScrim per fare l'effetto fadeOut
        // disabilito il focus della slider che rende visibile il cursore
        sliderDistance.setFocus(false, (int) sliderDistance.getValue());
        filterCard.setVisibility(View.INVISIBLE);
        return new int[] {(int) sliderDistance.getValue(), radioOrderBy.getChecked()};
    }

    @Override
    public String getSearchName() {
        Fragment searchResult = getFragment("search", SearchResultFragment.class);
        if (searchResult != null)
            return ((SearchResultFragment) searchResult).getSearchName();
        return null;
    }

    @Override
    public void enableFocusMarker(boolean enable, String id) {
        Fragment map = getFragment(R.id.map, MapFragment.class);
        if (map != null)
            ((MapFragment) map).enableFocusMarker(enable, id);
    }

    @Override
    public String getMarkerChecked() {
        Fragment map = getFragment(R.id.map, MapFragment.class);
        if (map != null)
            return ((MapFragment) map).getMarkerChecked();
        return null;
    }

    @Override
    public boolean isLoading() {
        Fragment searchResult = getFragment("search", SearchResultFragment.class);
        if (searchResult != null)
            return ((SearchResultFragment) searchResult).isLoading();
        return false;
    }

    @Override
    public void search(Map<String, Object> data, String function) {
        Fragment map = getFragment(R.id.map, MapFragment.class);
        if (map != null)
            ((MapFragment) map).search(data, function);
    }

    @Override
    public void clearMap() {
        Fragment map = getFragment(R.id.map, MapFragment.class);
        if (map != null)
            ((MapFragment) map).clearMap();
    }

    @Override
    public void setSearchHereVisibility(int visibility) {
        Fragment map = getFragment(R.id.map, MapFragment.class);
        if (map != null)
            ((MapFragment) map).setSearchHereVisibility(visibility);
    }

    @Override
    public void animateMarker(Marker newMarker) {
        Fragment map = getFragment(R.id.map, MapFragment.class);
        if (map != null)
            ((MapFragment) map).animateMarker(newMarker);
    }

    // ------ END HOME FRAGMENT CALLBACK ------

    // ------ START LIST GAME FRAGMENT CALLBACK ------

    @Override
    public void showGameViewDetail(View start, GameObject gameObject) {

        // Construct a container transform transition between two views.
        MaterialContainerTransform transition = buildContainerTransform(true, 0);
        transition.setStartView(start);
        transition.setEndView(gameDetail);

        hideBottomNav(() -> {
            containerGame.setClickable(true);
            containerNav.setVisibility(View.GONE);
        });

        // Trigger the container transform transition.
        TransitionManager.beginDelayedTransition(containerGame, transition);
        start.setVisibility(View.INVISIBLE);
        gameDetail.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideGameViewDetail(Fragment fragment, View start) {
        // Construct a container transform transition between two views.
        MaterialContainerTransform transition = buildContainerTransform(false, 0);
        transition.setStartView(gameDetail);
        transition.setEndView(start);
        containerNav.setVisibility(View.VISIBLE);

        showBottomNav(() -> {
            FragmentTransaction fTransaction = getSupportFragmentManager().beginTransaction();
            fTransaction.remove(fragment).commit();
            containerGame.setClickable(false);

            Fragment home = getFragment("home", HomeFragment.class);
            if (home != null)
                ((HomeFragment) home).setGlobalNavigationActive(true);
        });

        // Trigger the container transform transition.
        TransitionManager.beginDelayedTransition(containerGame, transition);
        gameDetail.setVisibility(View.GONE);
        start.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLaunchGame(String url) {
        Fragment home = getFragment("home", HomeFragment.class);
        if (home != null)
            ((HomeFragment) home).onLaunchGame(url);
    }

    @Override
    public void setGlobalNavigationActive(boolean enable) {
        Fragment home = getFragment("home", HomeFragment.class);
        if (home != null)
            ((HomeFragment) home).setGlobalNavigationActive(enable);
    }

    @Override
    public BottomSheetBehavior getBottomSheetBehavior() {
        Fragment home = getFragment("home", HomeFragment.class);
        if (home != null)
            return ((HomeFragment) home).getBottomSheetBehavior();
        return null;
    }

    @Override
    public boolean checkChangeStateBottomSheet() {
        Fragment home = getFragment("home", HomeFragment.class);
        if (home != null)
            return ((HomeFragment) home).checkChangeStateBottomSheet();
        return false;
    }

    // ------ END LIST GAME FRAGMENT CALLBACK ------

    // ------ START AWARD FRAGMENT CALLBACK ------

    @Override
    public void showTicketDetail(View v, TicketRewardObject ticket) {
        setTicketViewDetail(ticket);

        // Construct a container transform transition between two views.
        MaterialContainerTransform transition = buildContainerTransform(true, 2);
        transition.setStartView(v);
        transition.setEndView(gameDetail);

        hideBottomNav(() -> {
            containerNav.setVisibility(View.GONE);
            containerGame.setClickable(true);


        });

        // Trigger the container transform transition.
        TransitionManager.beginDelayedTransition(containerGame, transition);
        v.setVisibility(View.INVISIBLE);
        gameDetail.setVisibility(View.VISIBLE);
    }


    private void setTicketViewDetail(TicketRewardObject ticket) {
        // mostra la GameViewDetail con animazione

        setItemMenuEnable(false, false);
        fabButton.setEnabled(false);

        FragmentTransaction fTransaction = getSupportFragmentManager().beginTransaction();

        TicketDetailsFragment fragment = TicketDetailsFragment.newInstance(ticket);
        fTransaction.replace(R.id.game_detail, fragment).commit();
    }

    @Override
    public void hideTicketDetail(Fragment fragment, View start) {
        // Construct a container transform transition between two views.
        MaterialContainerTransform transition = buildContainerTransform(false, 2);
        transition.setStartView(gameDetail);
        transition.setEndView(start);

        containerNav.setVisibility(View.VISIBLE);

        showBottomNav(() -> {
            FragmentTransaction fTransaction = getSupportFragmentManager().beginTransaction();
            fTransaction.remove(fragment).commit();
            containerGame.setClickable(false);

            setItemMenuEnable(false, true);
            fabButton.setEnabled(true);
        });

        // Trigger the container transform transition.
        TransitionManager.beginDelayedTransition(containerGame, transition);
        gameDetail.setVisibility(View.GONE);
        start.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideTicketDetailNow() {
        if(gameDetail.getVisibility() == View.VISIBLE) {
            FragmentManager fragmentManager = getSupportFragmentManager();

            Fragment ticketDetailsFragment = fragmentManager.findFragmentById(R.id.game_detail);
            if(ticketDetailsFragment instanceof TicketDetailsFragment) {
                FragmentTransaction fTransaction = fragmentManager.beginTransaction();
                fTransaction.remove(ticketDetailsFragment).commit();
                containerGame.setClickable(false);
                setItemMenuEnable(true, false);
                fabButton.setEnabled(true);
            }

            containerNav.setVisibility(View.VISIBLE);
            showBottomNav(() -> {});

            gameDetail.setVisibility(View.GONE);
        }
    }

    // ------ END AWARD FRAGMENT CALLBACK ------

    // ------ START SETTINGS FRAGMENT CALLBACK ------

    @Override
    public void onLogout() {
        updateUI(null);
    }


    // ------ END SETTINGS FRAGMENT CALLBACK ------

    // ------ START HISTORY SEARCH FRAGMENT CALLBACK ------

    @Override
    public void onItemClick(String name) {
        // comunicazione HistorySearchFragment - HomeFragment
        Fragment home = getFragment("home", HomeFragment.class);
        if (home != null)
            ((HomeFragment) home).onItemClick(name);
    }

    // ------ END HISTORY SEARCH FRAGMENT CALLBACK ------

    // ------ START TICKET DETAIL/GAME RANK FRAGMENT CALLBACK ------

    @Override
    public void onBackClick(Fragment fragment) {
        if (fragment instanceof TicketDetailsFragment) {
            Fragment award = getFragment(R.id.content_layout, AwardFragment.class);
            if (award != null)
                ((AwardFragment) award).onBackClick(fragment);
        } else if (fragment instanceof GameRankFragment) {
            Fragment profile = getFragment(R.id.content_layout, ProfileFragment.class);
            if (profile != null)
                ((ProfileFragment) profile).onBackClick(fragment);
        }
    }

    // ------ END TICKET DETAIL FRAGMENT CALLBACK ------

}
