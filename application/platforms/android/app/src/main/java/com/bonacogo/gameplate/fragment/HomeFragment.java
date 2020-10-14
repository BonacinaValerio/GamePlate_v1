package com.bonacogo.gameplate.fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bonacogo.gameplate.Game;
import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.bottomsheet.CustomBottomSheet;
import com.bonacogo.gameplate.model.FilterFMObject;
import com.bonacogo.gameplate.model.FilterSObject;
import com.bonacogo.gameplate.model.HistoryObject;
import com.bonacogo.gameplate.outlineprovider.CustomOutlineProvider;
import com.bonacogo.gameplate.util.CommonStrings;
import com.bonacogo.gameplate.util.ReverseGeocode;
import com.bonacogo.gameplate.util.SharedObject;
import com.bonacogo.gameplate.viewmodel.SavedStateFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.transition.MaterialArcMotion;
import com.google.android.material.transition.MaterialContainerTransform;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.maps.android.SphericalUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import br.com.simplepass.loadingbutton.customViews.CircularProgressButton;

import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment implements
        View.OnClickListener {
    private static final String TAG = "HomeFragment";
    public static String EXTRA_MESSAGE = null;
    private int startColor, endColor;
    private int spaceLimit, slideSheetSpace = 0, peekHeight, filterCardHeightMin = 0, filterCardHeightMax = 0;

    private RelativeLayout header, background, filterScrim;
    private LinearLayout fabContainer, tabContainer;
    private LinearLayout searchContainer, searchMask;
    private ImageView tab;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private NestedScrollView standardBottomSheet;
    private SearchView searchView;
    private ImageButton searchButton;
    private FloatingActionButton findMe, filter;
    private CircularProgressButton searchHere;

    private SharedPreferences prefs;
    private FilterFMObject filterFMObject;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private SavedStateFragment savedStateFragment;
    private Bundle savedState;
    private boolean isSearchChecked;

    public boolean getIsSearchChecked() { return isSearchChecked; }

    private MapSlideListener mapSlideListener;
    private FabSlideListener fabSlideListener;

    public interface MapSlideListener {
        void onSlide(int spaceSlide, int spaceLimit);
    }
    public interface FabSlideListener {
        void onSlide(int spaceSlide, int spaceLimit);
    }
    private void setMapSlideListener(MapSlideListener mapSlideListener) {
        this.mapSlideListener = mapSlideListener;
    }
    private void setFabSlideListener(FabSlideListener fabSlideListener) {
        this.fabSlideListener = fabSlideListener;
    }

    // callback per mainActivity
    public interface ActivityCallback {
        RelativeLayout getFilterScrim();

        int getHeightDisplay();
        void setFilterCardMargin(int bottomMargin, int topMargin);
        void showFilterScreen(MaterialContainerTransform transition, int distance, int orderBy);
        int [] hideFilterScreen(MaterialContainerTransform transition);
        void setItemMenuEnable(boolean sx, boolean dx);

        void enableFocusMarker(boolean enable, String id);
        String getMarkerChecked();
        boolean isLoading();

        void search(Map<String, Object> data, String function);
        void clearMap();
        void setSearchHereVisibility(int visibility);
        void animateMarker(Marker newMarker);

        void launchGameUserNull();
        String getSearchName();
    }
    private ActivityCallback activityCallback;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallback = (ActivityCallback)context;

        prefs = context.getSharedPreferences(CommonStrings.SHARED_PREF_NAME, MODE_PRIVATE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedStateFragment = new ViewModelProvider(requireActivity()).get(SavedStateFragment.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myFragment = inflater.inflate(R.layout.fragment_home, container, false);

        filterFMObject = (FilterFMObject) SharedObject.getObject(prefs, FilterFMObject.DETAILS_STRING);

        mAuth = FirebaseAuth.getInstance();

        // setup views
        header = myFragment.findViewById(R.id.header);
        background = myFragment.findViewById(R.id.white_background);
        tab = myFragment.findViewById(R.id.tab);
        fabContainer = myFragment.findViewById(R.id.fab_container);
        searchContainer = myFragment.findViewById(R.id.search_container);
        LinearLayout searchHereContainer = myFragment.findViewById(R.id.search_here_container);
        tabContainer = myFragment.findViewById(R.id.tab_container);
        searchMask = myFragment.findViewById(R.id.search_mask);
        standardBottomSheet = myFragment.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet);
        searchView = myFragment.findViewById(R.id.search_view);
        searchButton = myFragment.findViewById(R.id.search_button);
        findMe = myFragment.findViewById(R.id.find_me);
        searchHere = myFragment.findViewById(R.id.search_here);
        filter = myFragment.findViewById(R.id.filter);
        filterScrim = activityCallback.getFilterScrim();

        // setup button
        findMe.setClickable(false);
        searchHere.setClickable(false);
        filter.setClickable(false);
        filterScrim.setClickable(false);
        searchView.setClickable(false);
        searchButton.setClickable(false);
        searchHere.setAlpha(0);
        filterScrim.setAlpha(0);


        // setup searchview
        searchView.setIconifiedByDefault(false);
        TextView searchText = searchView.findViewById(R.id.search_src_text);
        Typeface typeface = ResourcesCompat.getFont(Objects.requireNonNull(getContext()), R.font.poppins);
        searchText.setTypeface(typeface);
        searchText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        // setup color
        startColor = getResources().getColor(android.R.color.transparent);
        endColor = getResources().getColor(R.color.white);

        // setup bottomsheet
        // imposto l'ombra sulla bottomsheet così che venga direzionata verso l'alto
        float corner = getResources().getDimensionPixelSize(R.dimen.bottom_sheet_corner_material);
        final float scale = Objects.requireNonNull(getContext()).getResources().getDisplayMetrics().density;
        int yShift = (int) (-3 * scale + 0.5f);
        CustomOutlineProvider customOutlineProvider = new CustomOutlineProvider(yShift, corner);
        standardBottomSheet.setOutlineProvider(customOutlineProvider);

        // imposto l'ombra della search view quando la bottom sheet scrolla oltre lo stato expanded
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            standardBottomSheet.setOnScrollChangeListener((View.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                    header.setSelected(standardBottomSheet.canScrollVertically(-1)));
        }

        // listener onclick
        searchButton.setOnClickListener(this);
        searchView.setOnClickListener(this);
        filterScrim.setOnClickListener(this);
        filter.setOnClickListener(this);

        // listener fab container per impostare il padding dinamico con la bottomsheet
        setFabSlideListener((spaceSlide, spaceLimit) -> fabContainer.setPadding(0, 0, 0, Math.min(spaceSlide, spaceLimit)));

        // listener del cambio di stato della searchview [attiva/disattiva]
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            // se viene attivata e lo stato attuale di ricerca è false allora si cambia lo stato di
            // isSearchChecked e si imposta il fragment di cronologia
            if (hasFocus && !isSearchChecked) {
                onFocusTextSearch(false);
            }
            // se viene attivata e lo stato attuale è già quello di ricerca e il contenuto della bottom sheet
            // è un SearchResulFragment allora si imposta il fragment di cronologia ma senza cambiare lo stato di
            // isSearchChecked
            else if (hasFocus && getFragmentFromId(R.id.content_bottom_sheet) instanceof SearchResultFragment) {
                onFocusTextSearch(true);
            }
        });
        // listener della searchview [submit, modifica edittext]
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                search(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // listener bottomsheet
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            // listener cambio di stato della bottom sheet
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

                // imposto il fab del filtro cliccabile SOLO quando è nei due stati collapsed o half_expanded e in questi casi
                // setto alla filter card la dimensione corretta
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    if (filterCardHeightMax > 0) {
                        if (!filter.isClickable())
                            filter.setClickable(true);
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                            activityCallback.setFilterCardMargin(filterCardHeightMin, header.getMeasuredHeight());
                        else
                            activityCallback.setFilterCardMargin(filterCardHeightMax, header.getMeasuredHeight());
                    }
                }
                else
                    filter.setClickable(false);
            }
            // listener trascinamento della bottom sheet
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // slideoffset indica un valore da 0 a 1 che indica la posizione della bottomsheet
                // dal basso verso l'alto

                // passo al listener dei fab i valori per impostare il loro padding dal fondo dello schermo
                if (slideSheetSpace > 0) {
                    int spaceSlide = (int) (slideOffset * slideSheetSpace) + peekHeight;
                    if (mapSlideListener != null)
                        mapSlideListener.onSlide(spaceSlide, spaceLimit);
                    if (fabSlideListener != null)
                        fabSlideListener.onSlide(spaceSlide, spaceLimit);
                }
                // imposto il colore del linearlayout header perche col background trasparente non mostra
                // l'ombra sulla bottomsheet espansa, mentre il tabContainer imposta si sovrappone al
                // background della bottomsheet e quindi nasconde i bordi smussati in alto
                if (slideOffset == 1) {
                    header.setBackgroundColor(endColor);
                    tabContainer.setBackgroundColor(endColor);
                }
                else {
                    // in caso non sia al massimo della sua espansione resetto i valori predefiniti
                    if (tabContainer.getBackground() instanceof ColorDrawable)
                        tabContainer.setBackgroundResource(R.drawable.tab_bottom_sheet_bg);
                    if (((ColorDrawable)header.getBackground()).getColor() == endColor)
                        header.setBackgroundColor(startColor);
                }

                // animazione fusione bottomsheet con searchview
                if (slideOffset>=0.9f) {
                    // funzione [0.9 : 1] -> [0 : 255]
                    float fraction = (slideOffset - 0.9f)/0.1f;
                    int alpha = (int) Math.ceil(fraction*255);
                    // funzione [0.9 : 1] -> [1 - 0]
                    float alpha2 =  ((1-slideOffset)*10);
                    // funzione [0.9 : 1] -> [7 - 0]
                    int elevation = (int) (alpha2*7);
                    // imposto l'alpha dello sfondo e della search mask (strato che mostra il bordo nella searchbar) e dell'ombra della BS
                    background.getBackground().mutate().setAlpha(alpha);
                    searchMask.getBackground().mutate().setAlpha(alpha);
                    ((CustomOutlineProvider) standardBottomSheet.getOutlineProvider()).setAlpha(alpha2);
                    standardBottomSheet.invalidateOutline();
                    // imposto l'elevazione del search container e mask
                    searchContainer.setElevation(elevation);
                    searchMask.setElevation(elevation);
                }
                else {
                    // reset dei colori nel caso in cui lo slideoffset sia rapido e salti dei valori
                    if (background.getBackground().mutate().getAlpha() != 0)
                        background.getBackground().mutate().setAlpha(0);
                    if (searchMask.getBackground().mutate().getAlpha() != 0)
                        searchMask.getBackground().mutate().setAlpha(0);
                    if (((CustomOutlineProvider) standardBottomSheet.getOutlineProvider()).getAlpha() != 1)
                        ((CustomOutlineProvider) standardBottomSheet.getOutlineProvider()).setAlpha(1);
                    standardBottomSheet.invalidateOutline();
                    if (searchContainer.getElevation() != 7)
                        searchContainer.setElevation(7);
                    if (searchMask.getElevation() != 7)
                        searchMask.setElevation(7);
                }
            }
        });

        // imposta offset bottomSheet
        // osservo il linearlayout header
        ViewTreeObserver vto = header.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (header.getMeasuredHeight()>0) {
                    int headerHeight = header.getMeasuredHeight();
                    header.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    // offset bottomsheet
                    bottomSheetBehavior.setExpandedOffset(headerHeight);
                    // offset del cerca qui
                    searchHereContainer.setTranslationY(headerHeight);
                    // reimposto i tasti cliccabili
                    searchButton.setClickable(true);
                    searchView.setClickable(true);

                    int height = activityCallback.getHeightDisplay();
                    // calcolo il limite in px dal fondo dello schermo per il padding massimo dei tasti fab
                    spaceLimit = (int) (bottomSheetBehavior.getHalfExpandedRatio() * height);
                    // calcolo in px dal fondo dello schermo il padding minimo dei tasti fab
                    peekHeight = bottomSheetBehavior.getPeekHeight();
                    // calcolo in px lo spazio di slide della bottomsheet
                    slideSheetSpace = height - peekHeight - headerHeight;

                    // converto 90dp in px, 90dp è la distanda dal basso del filter container fino al fab del filtro
                    final float scale = getResources().getDisplayMetrics().density;
                    int heightFilterPx = (int) (90 * scale + 0.5f);
                    // distanza in px dal basso dello schermo fino al fab del filtro nei due stati della bottomSheet (collapsed/half_expanded)
                    filterCardHeightMin = peekHeight + heightFilterPx;
                    filterCardHeightMax = spaceLimit + heightFilterPx;

                    // imposto un minimo valore di altezza della filter card per essere responsiva e visibile per ogni schermo
                    int cardHeightCollapsedPx = height - headerHeight - filterCardHeightMax - (int) (10 * scale + 0.5f);
                    int limitPx = (int) (250 * scale + 0.5f);
                    if (cardHeightCollapsedPx < limitPx) {
                        filterCardHeightMax = filterCardHeightMax - (limitPx - cardHeightCollapsedPx);
                    }

                    setInitialState();

                    savedState = savedStateFragment.getBundle(TAG);
                    // reimposta lo stato solo DOPO aver impostato l'offset della bottomsheet e settato le variabili varie
                    if (savedState != null) {
                        isSearchChecked = savedState.getBoolean("STATE_IS_SEARCH");
                        //reimposta lo stato
                        restoreState(savedState);
                    }
                    else {
                        RecentGamesFragment recentGamesFragment = new RecentGamesFragment();
                        // imposta il fragment nella bottom sheet
                        changeContentBottomSheet(recentGamesFragment, true);
                    }

                    setupFabButton();
                    setupMap();
                }
            }
        });


        return myFragment;
    }

    private void setInitialState() {
        // setup views
        background.getBackground().mutate().setAlpha(0);
        searchMask.getBackground().mutate().setAlpha(0);
        header.setBackgroundColor(startColor);
        searchContainer.setElevation(7);
        searchMask.setElevation(7);
        standardBottomSheet.setElevation(6);
    }

    private void setExpandedState() {
        background.getBackground().mutate().setAlpha(255);
        searchMask.getBackground().mutate().setAlpha(255);
        header.setBackgroundColor(endColor);
        tabContainer.setBackgroundColor(endColor);
        searchContainer.setElevation(0);
        searchMask.setElevation(0);
    }

    // imposta le dimensioni iniziali per i fab button e filter card
    private void setupFabButton() {
        int state = bottomSheetBehavior.getState();

        // setup fab_container per posizionarlo all'altezza della bottomsheet
        if (state == BottomSheetBehavior.STATE_COLLAPSED) {
            activityCallback.setFilterCardMargin(filterCardHeightMin, header.getMeasuredHeight());
            fabContainer.setPadding(0, 0, 0, peekHeight);
        }
        else {
            activityCallback.setFilterCardMargin(filterCardHeightMax, header.getMeasuredHeight());
            fabContainer.setPadding(0, 0, 0, spaceLimit);
        }

        // reimposto il filtro cliccabile
        filter.setClickable(true);
    }

    // inserisci il fragment della mappa preservandone l'istanza
    private void setupMap() {
        FragmentManager fManager = getFragmentManager();
        if (fManager == null)
            return;

        FragmentTransaction fTransaction = fManager.beginTransaction();
        Fragment fragment = fManager.findFragmentById(R.id.map);
        if(fragment == null) {
            fragment = new MapFragment();
            fTransaction.addToBackStack("map");
        }
        fTransaction.replace(R.id.map, fragment).commit();
    }

    // attiva/disattiva lo stato di loading
    public void setCircularProgressActive(boolean active) {
        if (active) {
            searchHere.setAlpha(0);
            searchHere.startAnimation();
            searchHere.animate()
                    .alpha(1f)
                    .setDuration(300);
            searchHere.setVisibility(View.VISIBLE);
            setGlobalNavigationActive(false);
        }
        else {
            searchHere.setVisibility(View.INVISIBLE);
            setGlobalNavigationActive(true);
            searchHere.revertAnimation();
        }
    }

    // attiva/disattiva la navigazione globale (navigationView, searchbar, fab)
    public void setGlobalNavigationActive(boolean active) {
        if (active) {
            findMe.setClickable(true);
            searchHere.setEnabled(true);
            searchMask.setClickable(false);
            activityCallback.setItemMenuEnable(true, true);
        }
        else {
            findMe.setClickable(false);
            searchHere.setEnabled(false);
            searchMask.setClickable(true);
            activityCallback.setItemMenuEnable(false, false);
        }
    }

    private void search(String query) {
        // disattiva la search view, quindi togli la tastiera e il cursore
        searchView.clearFocus();

        // salva la ricerca nella cronologia
        HistoryObject historyObject = (HistoryObject) SharedObject.getObject(prefs, HistoryObject.DETAILS_STRING);
        historyObject.addElement(query, prefs);

        // cambio lo stato della bottomsheet in half_expanded
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        if (bottomSheetBehavior instanceof CustomBottomSheet)
            // sblocco la bottomsheet
            ((CustomBottomSheet) bottomSheetBehavior).setLocked(false);
        header.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getContext(), R.animator.scroll_elevation));
        tab.setVisibility(View.VISIBLE);

        SearchResultFragment searchResultFragment = SearchResultFragment.newInstance(requireActivity(), query);
        activityCallback.clearMap();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        setupSearch(searchResultFragment, false);

        // nascondi il tasto searchHere e tutto il fabContainer (perchè in caso di ricerca da searchView, questi tasti non
        // servono all'utente in quanto l'unica cosa importante è il risultato della ricerca)
        fabContainer.setVisibility(View.INVISIBLE);

        setCircularProgressActive(true);

        FilterSObject filterSObject = (FilterSObject) SharedObject.getObject(prefs, FilterSObject.DETAILS_STRING);
        Map<String, Object> data = new HashMap<>();

        data.put("lang", new Locale(Locale.getDefault().getLanguage()).getLanguage());
        data.put("textQuery", query);
        data.put("orderBy", filterSObject.getOrder_by());

        RequestQueue queue = Volley.newRequestQueue(Objects.requireNonNull(getContext()));
        String url = null;
        try {
            url = ReverseGeocode.API_URL + "?" +
                    ReverseGeocode.KEY_API_KEY + getResources().getString(R.string.open_cage_api_key) +
                    ReverseGeocode.KEY_POSITION +
                    URLEncoder.encode(query, "UTF-8") +
                    ReverseGeocode.NO_DEDUPE + ReverseGeocode.NO_ANNOTATION +
                    ReverseGeocode.LIMIT_1 + ReverseGeocode.LANGUAGE_EN;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, response -> {
                    try {
                        JSONObject result = response.getJSONArray("results").getJSONObject(0);

                        JSONObject bounds = result.getJSONObject("bounds");
                        LatLng southwest = new LatLng(bounds.getJSONObject("southwest").getDouble("lat"), bounds.getJSONObject("southwest").getDouble("lng"));
                        LatLng northeast = new LatLng(bounds.getJSONObject("northeast").getDouble("lat"), bounds.getJSONObject("northeast").getDouble("lng"));
                        data.put("minLat", southwest.latitude);
                        data.put("minLng", southwest.longitude);
                        data.put("maxLat", northeast.latitude);
                        data.put("maxLng", northeast.longitude);

                        LatLng center = SphericalUtil.interpolate(southwest, northeast, 0.5);
                        data.put("centerLat", center.latitude);
                        data.put("centerLng", center.longitude);
                        data.put("distance", SphericalUtil.computeDistanceBetween(center, northeast));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        setDefaultValue(data);
                    }

                    activityCallback.search(data, "onSearch");

                }, error -> {
                    setDefaultValue(data);
                    activityCallback.search(data, "onSearch");
                });

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }

    private void setDefaultValue(Map<String, Object> data) {
        data.put("minLat", -1);
        data.put("minLng", -1);
        data.put("maxLat", -1);
        data.put("maxLng", -1);
        data.put("centerLat", -1);
        data.put("centerLng", -1);
        data.put("distance", -1);
    }

    // avvia gioco
    private void launchGame(String game) {
        FirebaseUser user = mAuth.getCurrentUser();
        // se l'utente è loggato avvia l'attività game con l'id del gioco
        // altrimenti mostra la schermata di login
        if (user != null) {
            Intent intent = new Intent(getContext(), Game.class);
            intent.putExtra(EXTRA_MESSAGE, game);
            startActivity(intent);

            checkChangeStateBottomSheet();
        }
        else {
            activityCallback.launchGameUserNull();
        }
    }

    // cambia lo stato isSearchChecked
    private void changeSearchChecked(){
        isSearchChecked = !isSearchChecked;
        // cambio lo stato di animazione del tasto
        final int[] stateSet = {android.R.attr.state_checked * (isSearchChecked ? 1 : -1)};
        searchButton.setImageState(stateSet, true);
    }

    // cambio di UI all'attivazione della searchView
    private void onFocusTextSearch(boolean restore) {
        // se devo ripristinare il fragment non cambio lo stato isSearchChecked
        if (!restore)
            changeSearchChecked();

        if (isSearchChecked) {
            HistorySearchFragment historySearchFragment = new HistorySearchFragment();
            // cambia il fragment nella bottomsheet con quello della cronologia di ricerca
            changeContentBottomSheet(historySearchFragment, true);
            // tolgo il listAnimator dell'header che ha comando sull'elevazione e quindi abbasso l'elevazione
            // manualmente per portarlo sullo stesso piano della bottomSheet e non mostrare l'ombra
            header.setStateListAnimator(null);
            header.setElevation(6);
            // espando la bottomsheet
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            if (bottomSheetBehavior instanceof CustomBottomSheet)
                // blocco la bottomsheet
                ((CustomBottomSheet) bottomSheetBehavior).setLocked(true);
            // infine elimino il tab
            tab.setVisibility(View.GONE);
        }
        else {
            // reimposto la visualizzazione del tasto SearchHere e i fab
            fabContainer.setVisibility(View.VISIBLE);
            activityCallback.setSearchHereVisibility(View.VISIBLE);
            if (getFragmentFromId(R.id.content_bottom_sheet) instanceof SearchResultFragment) {
                activityCallback.clearMap();
            }
            setViewsNotInSearch(null);
            // cambia il fragment nella bottomsheet con quello dei giochi recenti
            RecentGamesFragment recentGamesFragment = new RecentGamesFragment();
            changeContentBottomSheet(recentGamesFragment, false);
        }
    }

    // imposto l'UI COME non in stato di ricerca, questo non presuppone, nè modifica, isSearchChecked in false
    private void setViewsNotInSearch(String nameRestored) {
        header.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getContext(), R.animator.scroll_elevation));
        if (nameRestored == null) {
            // cancello il testo nella edittext della searchview
            searchView.setQuery("", false);
            // cambio lo stato della bottomsheet in collassato
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        else {
            // reimposto il testo nella edittext della searchview
            searchView.setQuery(nameRestored, false);
            // cambio lo stato della bottomsheet in half_expanded
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        }
        // tolgo il focus alla searchview
        searchView.clearFocus();
        if (bottomSheetBehavior instanceof CustomBottomSheet)
            // sblocco la bottomsheet
            ((CustomBottomSheet) bottomSheetBehavior).setLocked(false);
        // reimposto visibile il tab
        tab.setVisibility(View.VISIBLE);
    }

    private void changeContentBottomSheet(Fragment fragment, boolean commitNow) {
        changeContentBottomSheet(fragment, true, false, commitNow);
    }
    // cambia il fragment nella bottomsheet
    private void changeContentBottomSheet(Fragment fragment, boolean firstInstance, boolean popStack, boolean commitNow) {
        FragmentManager fManager = getFragmentManager();
        if (fManager == null)
            return;

        FragmentTransaction fTransaction = fManager.beginTransaction();

        if (fragment instanceof SearchResultFragment) {

            if (popStack)
                // si effettua un pop nello stack del fragment saarch, con il flag inclusivo
                fManager.popBackStack("search", FragmentManager.POP_BACK_STACK_INCLUSIVE);

            if (firstInstance)
                // alla prima istanza si aggiunge allo stack con il tag search
                fTransaction.addToBackStack("search");

            if (commitNow)
                fTransaction.replace(R.id.content_bottom_sheet, fragment, "search").commitNow();
            else
                fTransaction.replace(R.id.content_bottom_sheet, fragment, "search").commit();
        }
        else {
            if (fragment instanceof RecentGamesFragment)
                if (commitNow)
                    fTransaction.replace(R.id.content_bottom_sheet, fragment, "rg").commitNow();
                else
                    fTransaction.replace(R.id.content_bottom_sheet, fragment, "rg").commit();
            else if (fragment instanceof HistorySearchFragment)
                fTransaction.replace(R.id.content_bottom_sheet, fragment, "hs").commitNow();
        }

    }

    private Fragment getFragmentFromId(int id) {
        FragmentManager fManager = getFragmentManager();
        if (fManager != null)
            return fManager.findFragmentById(id);
        return null;
    }
    private Fragment getFragmentFromTag(String tag) {
        FragmentManager fManager = getFragmentManager();
        if (fManager != null)
            return fManager.findFragmentByTag(tag);
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        savedStateFragment.setBundle(TAG, saveBundle());
    }

    private Bundle saveBundle() {
        Bundle bundle = new Bundle();

        bundle.putBoolean("STATE_IS_SEARCH", isSearchChecked);
        FragmentManager fManager = getFragmentManager();
        if (fManager != null) {
            Fragment search = fManager.findFragmentByTag("search");
            if (search instanceof SearchResultFragment)
                bundle.putBoolean("IS_IN_LOADING", activityCallback.isLoading());
        }
        // salvo nel bundle per il restore lo stato della bottomSheet
        bundle.putInt("bottom_sheet_state", bottomSheetBehavior.getState());

        Fragment fragment = getFragmentFromId(R.id.content_bottom_sheet);
        // e salvo il contenuto della bottomSheet
        if (fragment != null) {
            String nameContentBS = fragment.getClass().getName();
            bundle.putString("content_bottom_sheet", nameContentBS);
        }
        return bundle;
    }

    // rimposto lo stato precendete
    private void restoreState(Bundle savedState) {
        // rimuovo il vecchio fragment perchè attaccato a container obsoleto
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null)
            return;

        Fragment f = fragmentManager.findFragmentById(R.id.content_bottom_sheet);
        if (f != null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(f).commitNow();
        }

        boolean isInLoading = savedState.getBoolean("IS_IN_LOADING", false);

        // se lo stato di ricerca era vero
        if (isSearchChecked && !isInLoading) {
            // cambio lo stato di animazione del tasto
            final int[] stateSet = {android.R.attr.state_checked};
            searchButton.setImageState(stateSet, true);

            String oldContentBS = savedState.getString("content_bottom_sheet");
            if (oldContentBS != null) {
                if (oldContentBS.equals(HistorySearchFragment.class.getName())) {
                    // se la UI era sulla HistorySearch nascondi i fab e la reimposto non cambiando lo stato di
                    // isSearchChecked
                    setExpandedState();
                    fabContainer.setVisibility(View.INVISIBLE);
                    onFocusTextSearch(true);
                }
                else if (oldContentBS.equals(SearchResultFragment.class.getName())) {
                    // se la UI era sulla SearchResult allora
                    // se è derivata da una ricerca da searchView nascondi i fab
                    if (activityCallback.getSearchName() != null) {
                        searchHere.setVisibility(View.INVISIBLE);
                        fabContainer.setVisibility(View.INVISIBLE);
                    }
                    // infine reiserisco il fragment
                    changeContentBottomSheet(getFragmentFromTag("search"), false, false, true);
                }
            }
        }
        else {
            if (isSearchChecked) {
                FragmentManager fManager = getFragmentManager();
                if (fManager != null)
                    fManager.popBackStackImmediate("search", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                searchView.setQuery("", false);
                isSearchChecked = false;

            }
            RecentGamesFragment recentGamesFragment = new RecentGamesFragment();
            // imposta il fragment nella bottom sheet
            changeContentBottomSheet(recentGamesFragment, true);
        }
    }

    @NonNull
    private MaterialContainerTransform buildContainerTransform() {
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setScrimColor(Color.TRANSPARENT);
        transform.setPathMotion(new MaterialArcMotion());
        return transform;
    }

    // interfacce
    @Override
    public void onClick(View v) {
        int i = v.getId();
        // click del search_button
        if (i == R.id.search_button) {
            Fragment fragment = getFragmentFromId(R.id.content_bottom_sheet);
            SearchResultFragment searchResultFragment = (SearchResultFragment) getFragmentFromTag("search");

            if (!isSearchChecked) {
                // simula il click della searchview se non era attivo isSearchChecked
                searchView.performClick();
            }
            else if (fragment instanceof HistorySearchFragment && searchResultFragment == null) {
                // se si è in historySearch e non c'è nello stack il fragment search allora imposta lo
                // stato e la UI non in ricerca
                onFocusTextSearch(false);
            }
            else if (fragment instanceof HistorySearchFragment) {
                // se si è in historySearch e c'è nello stack il fragment search allora imposta la UI
                // non in ricerca e inserisci il fragment nello stack
                String searchName = activityCallback.getSearchName();
                // se è derivata da una ricerca da searchView nascondi i fab
                if (searchName != null) {
                    fabContainer.setVisibility(View.INVISIBLE);
                }
                setViewsNotInSearch(searchName);
                changeContentBottomSheet(searchResultFragment, false, false, false);
            }
            else {
                if (fragment instanceof SearchResultFragment) {
                        Fragment mapFragment = getFragmentFromId(R.id.map);
                        if (mapFragment instanceof MapFragment) {
                            if (checkChangeStateBottomSheet())
                                return;
                            if (activityCallback.getMarkerChecked() != null)
                                activityCallback.enableFocusMarker(false, "");
                            else if (!activityCallback.isLoading()){
                                // l'ultimo caso possibile è quello di essere in SearchResult e nessun marker selezionato quindi si esegue un pop dello stack
                                // e si imposta lo stato e l'UI non in ricerca
                                FragmentManager fragmentManager = getFragmentManager();
                                if (fragmentManager != null)
                                    fragmentManager.popBackStack("search", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                onFocusTextSearch(false);
                            }
                        }
                }
            }
        }
        else if (i == R.id.search_view)
            searchView.setIconified(false);
        else if (i == R.id.filter) {
            // abilito il click della filterScrim
            filterScrim.setClickable(true);
            // reimposta i valori salvati
            int distanceValue = filterFMObject.getDistance();
            int orderBy = filterFMObject.getOrder_by();

            // Construct a container transform transition between two views.
            MaterialContainerTransform transition = buildContainerTransform();
            transition.setStartView(filter);

            // disabilito il click del filter e animo la filterScrim per fare l'effetto fadeIn
            filter.setClickable(false);
            filterScrim.setVisibility(View.VISIBLE);
            filterScrim.animate()
                    .alpha(1f)
                    .setDuration(300).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    filterScrim.setAlpha(1f);
                }
            });

            activityCallback.showFilterScreen(transition, distanceValue, orderBy);

            // nascondo il fab filter
            filter.setVisibility(View.INVISIBLE);
        }
        else if (i == R.id.scrim) {
            // se l'animazione fadeIn della filterScrim non è terminata ritorna
            if (filterScrim.getAlpha() != 1f)
                return;

            // Construct a container transform transition between two views.
            MaterialContainerTransform transition = buildContainerTransform();
            transition.setEndView(filter);

            filterScrim.setClickable(false);
            filterScrim.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            // alla fine dell'animazione elimino la filterScrim e riabilito il click del fab filter
                            filterScrim.setVisibility(View.GONE);
                            filter.setClickable(true);
                        }
                    });

            int[] values = activityCallback.hideFilterScreen(transition);

            // mostro la fab filter
            filter.setVisibility(View.VISIBLE);
            // salva i valori impostati
            filterFMObject.setDistance(values[0], prefs);
            filterFMObject.setOrder_by(values[1], prefs);
        }
    }

    public boolean checkChangeStateBottomSheet() {
        Fragment fragment = getFragmentFromId(R.id.content_bottom_sheet);
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            if ((fragment instanceof RecentGamesFragment) || fragment instanceof SearchResultFragment) {
                standardBottomSheet.smoothScrollTo(0, 0);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                return true;
            }
        }
        return false;
    }

    public void clickSearchButton() {
        searchButton.performClick();
    }

    // ------ START HISTORY SEARCH FRAGMENT CALLBACK ------

    // click elemento nella history search
    public void onItemClick(String name) {
        // scrivi l'elemento nella searchview e fai il submit
        searchView.setQuery(name, true);
    }

    // ------ END HISTORY SEARCH FRAGMENT CALLBACK ------

    // ------ START LIST GAME FRAGMENT CALLBACK ------

    public void onLaunchGame(String url) {
        launchGame(url);
    }

    public BottomSheetBehavior getBottomSheetBehavior() {
        return bottomSheetBehavior;
    }

    // ------ END LIST GAME FRAGMENT CALLBACK ------


    // ------ START MAP FRAGMENT CALLBACK ------

    // aggiora UI sulla base del caricamento della mappa
    public void updateMap(GoogleMap map, Bundle mapSavedState, String markerChecked, HashMap<String, Marker> listMarkers, Bitmap bitmap) {
        // listener mappa per impostare il padding dinamico con la bottomsheet
        setMapSlideListener((spaceSlide, spaceLimit) -> map.setPadding(0, header.getMeasuredHeight(), 0, Math.min(spaceSlide, spaceLimit)));
        if (mapSavedState != null) {
            Log.i(TAG, "updateMap: savedstate");
            // ripristina la vecchia visuale della camera
            MapFragment.moveCamera(map,
                    mapSavedState.getDouble("LATITUDE"),
                    mapSavedState.getDouble("LONGITUDE"),
                    mapSavedState.getFloat("ZOOM"),
                    mapSavedState.getFloat("BEARING"),
                    mapSavedState.getFloat("TILT"));
        }

        if (savedState == null) {
            // se non è un ripristino imposta il padding come minimo possibile
            map.setPadding(0, header.getMeasuredHeight(), 0, peekHeight);
        }
        else {
            if (mapSavedState != null) {
                if (listMarkers != null && !listMarkers.isEmpty()) {
                    Log.i(TAG, "updateMap: restore");

                    for (Map.Entry<String, Marker> stringMarkerEntry : listMarkers.entrySet()) {
                        Marker marker = stringMarkerEntry.getValue();
                        String id = (String) marker.getTag();
                        BitmapDescriptor mapIconBitMapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
                        Marker newMarker = map.addMarker(new MarkerOptions()
                                .position(marker.getPosition())
                                .icon(mapIconBitMapDescriptor)
                                .title(marker.getTitle())
                                .snippet(marker.getSnippet()));
                        newMarker.setTag(id);

                        listMarkers.put(id, newMarker);
                        if (markerChecked != null && markerChecked.equals(id)) {
                            activityCallback.animateMarker(newMarker);
                        }
                    }
                }
            }

            String oldContentBS = savedState.getString("content_bottom_sheet");
            // solo se lo stato NON era nella HistorySearch
            if (oldContentBS != null && !oldContentBS.equals(HistorySearchFragment.class.getName())) {
                if (savedState.getInt("bottom_sheet_state") == BottomSheetBehavior.STATE_COLLAPSED)
                    // imposta il padding come minimo possibile se si era nello stato collapsed
                    map.setPadding(0, header.getMeasuredHeight(), 0, peekHeight);
                else {
                    // altrimenti si cambia lo stato della bottomSheet che triggera il listener
                    // impostato in precedenza e quindi cambia il padding della mappa
                    if (slideSheetSpace > 0)
                        map.setPadding(0, header.getMeasuredHeight(), 0, spaceLimit);
                    int state = savedState.getInt("bottom_sheet_state");
                    if (state == BottomSheetBehavior.STATE_HALF_EXPANDED || state == BottomSheetBehavior.STATE_EXPANDED) {
                        bottomSheetBehavior.setState(state);
                        if (state == BottomSheetBehavior.STATE_EXPANDED) {
                            setExpandedState();
                        }

                    }
                    else {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                    }
                }
            }
        }
    }

    public void setupSearch(SearchResultFragment searchResultFragment, boolean newSearch) {

        if (getFragmentFromTag("search") == null) {
            if (newSearch)
                // unico caso in cui si effettua una ricerca *SEARCH_HERE* oppure *FIND_ME* e non si hanno ricerche nello stack
                // entro nello stato searchChecked
                changeSearchChecked();
            changeContentBottomSheet(searchResultFragment, false);
        }
        else
            changeContentBottomSheet(searchResultFragment, true, true, false);
    }

    public CircularProgressButton getSearchHere() {
        return searchHere;
    }

    public FloatingActionButton getFindMe() {
        return findMe;
    }

    // ------ END MAP FRAGMENT CALLBACK ------
}
