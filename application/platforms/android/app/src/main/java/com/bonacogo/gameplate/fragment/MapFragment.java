package com.bonacogo.gameplate.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.interpolator.CustomBounceInterpolator;
import com.bonacogo.gameplate.model.FilterFMObject;
import com.bonacogo.gameplate.model.GameObject;
import com.bonacogo.gameplate.model.MyLastLocationObject;
import com.bonacogo.gameplate.other.CustomInfoWindowAdapter;
import com.bonacogo.gameplate.util.CommonStrings;
import com.bonacogo.gameplate.util.LocationUtil;
import com.bonacogo.gameplate.util.SharedObject;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import br.com.simplepass.loadingbutton.customViews.CircularProgressButton;
import br.com.simplepass.loadingbutton.presentation.State;

import static android.content.Context.MODE_PRIVATE;

public class MapFragment extends BaseMapFragment implements GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    private FirebaseFunctions mFunctions;
    private SharedPreferences prefs;

    private ValueAnimator animation;
    private int ICON_WIDTH = 90;
    private int ICON_HEIGHT = 120;
    private Bitmap bitmap;

    private boolean searchMovement = false;

    public void setSearchHereVisibility(int visibility) {
        searchHere.setVisibility(visibility);
    }

    public String getMarkerChecked() {
        return markerChecked;
    }

    // callback per mainActivity
    public interface ActivityCallBack {
        void onMarkerClick(Marker marker);
        void onInfoWindowClick();

        void requestPermissions(boolean search);
        void onReady(GoogleMap map, Bundle savedState, String markerChecked, HashMap<String, Marker> listMarkers, Bitmap bitmap);

        CircularProgressButton getSearchHere();
        FloatingActionButton getFindMe();
        BottomSheetBehavior getBottomSheetBehavior();
        void setupSearch(SearchResultFragment searchResultFragment, boolean newSearch);
        void setResult(LinkedHashMap<String, GameObject> gameObjects);
        void setCircularProgressActive(boolean active);
        void setGlobalNavigationActive(boolean enable);
    }
    private ActivityCallBack activityCallBack;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        prefs = context.getSharedPreferences(CommonStrings.SHARED_PREF_NAME, MODE_PRIVATE);

        activityCallBack = (ActivityCallBack) context;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirstStart = true;

        if (savedInstanceState != null)
            if (savedInstanceState.keySet().contains(KEY_LAST_LOCATION)) {
                lastLocation = (MyLastLocationObject) savedInstanceState.getSerializable(KEY_LAST_LOCATION);
            }
        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedStateFragment.getBundle(TAG));

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(Objects.requireNonNull(getContext()));
        mSettingsClient = LocationServices.getSettingsClient(getContext());

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationRequest();
        createLocationCallback();
        buildLocationSettingsRequest();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myFragment = inflater.inflate(R.layout.fragment_map, container, false);

        mRequestingSearch = false;

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedStateFragment.getBundle(TAG));

        // Firebase functions
        mFunctions = FirebaseFunctions.getInstance();

        // setup view
        mMapView = myFragment.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        return myFragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchHere = activityCallBack.getSearchHere();
        findMe = activityCallBack.getFindMe();

        // listener onclick
        searchHere.setOnClickListener(v -> {
            setSearchFragment();

            LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
            LatLng northEast = bounds.northeast;
            LatLng southWest = bounds.southwest;
            LatLng center = bounds.getCenter();

            Map<String, Object> data = new HashMap<>();
            data.put("lang", new Locale(Locale.getDefault().getLanguage()).getLanguage());

            data.put("minLat", southWest.latitude);
            data.put("minLng", southWest.longitude);
            data.put("maxLat", northEast.latitude);
            data.put("maxLng", northEast.longitude);

            data.put("centerLat", center.latitude);
            data.put("centerLng", center.longitude);

            data.put("distance", SphericalUtil.computeDistanceBetween(new LatLng(center.latitude, center.longitude), new LatLng(northEast.latitude, northEast.longitude)));

            search(data, "onSearchHere");
        });
        findMe.setOnClickListener(v -> findMeButtonHandler());
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mMap.setMyLocationEnabled(true);
                mCurrentLocation = locationResult.getLastLocation();
                if (mFirstStart) {
                    animateCamera(mMap, mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 10, mMap.getCameraPosition().bearing, mMap.getCameraPosition().tilt);
                    mFirstStart = false;
                }
                // move camera
                if (mRequestingSearch) {
                    mRequestingSearch = false;
                    mMap.clear();
                    LatLng location = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

                    animateCamera(mMap, mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 10, mMap.getCameraPosition().bearing, mMap.getCameraPosition().tilt);
                    FilterFMObject filterFMObject = (FilterFMObject) SharedObject.getObject(prefs, FilterFMObject.DETAILS_STRING);
                    Map<String, Object> data = new HashMap<>();

                    data.put("lang", new Locale(Locale.getDefault().getLanguage()).getLanguage());

                    data.put("lat", location.latitude);
                    data.put("lng", location.longitude);
                    if (filterFMObject != null) {
                        data.put("distance", filterFMObject.getDistance());
                        data.put("orderBy", filterFMObject.getOrder_by());
                    }

                    search(data, "onFindMe");
                }

            }
        };
    }

    /*
     * Check if the device's location settings are adequate for the app's needs using the
     * {@link com.google.android.gms.location.SettingsApi#checkLocationSettings(GoogleApiClient,
     * LocationSettingsRequest)} method, with the results provided through a {@code PendingResult}.
     */
    private void checkLocationSettings() {
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(locationSettingsResponse -> {
                Log.i(TAG, "All location settings are satisfied. ");
                startFindMe();
            })
            .addOnFailureListener(e -> {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                "location settings ");
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the
                            // result in onActivityResult().
                            ResolvableApiException rae = (ResolvableApiException) e;
                            rae.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sie) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        String errorMessage = "Location settings are inadequate, and cannot be " +
                                "fixed here. Fix in Settings.";
                        Log.e(TAG, errorMessage);
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check for the integer request code originally supplied to startResolutionForResult().
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    Log.i(TAG, "All location settings are satisfied. ");
                    startFindMe();
                    break;
                case Activity.RESULT_CANCELED:
                    Log.i(TAG, "User chose not to make required location settings changes.");
                    break;
            }
        }
    }

    private void startFindMe() {
        mRequestingSearch = true;
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback, Looper.myLooper());
        setSearchFragment();
    }

    private void findMeButtonHandler() {
        clearMap();

        if (LocationUtil.checkPermissions(getContext())) {
            // Begin by checking if the device has the necessary location settings.
            checkLocationSettings();
        }
        else {
            activityCallBack.requestPermissions(true);
        }
    }

    public void onRequestPermissionsGranted() {
        checkLocationSettings();
    }

    private void setSearchFragment() {
        clearMap();

        activityCallBack.getBottomSheetBehavior().setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        SearchResultFragment searchResultFragment;
        // disabilito i tasti di navigazione globale (searchbar, navigation bottom)
        activityCallBack.setGlobalNavigationActive(false);
        if (searchHere.getAlpha() != 1) {
            searchHere.animate()
                    .alpha(1f)
                    .setDuration(300);
        }
        // eseguo l'animazione del tasto searchHere
        searchHere.startAnimation();
        // cambio il content bottom sheet con una nuova istanza di searchResultFragment senza paramentri
        // questa verrà inizializzata in stato di loading a tempo indeterminato

        searchResultFragment = SearchResultFragment.newInstance(requireActivity(), null);
        activityCallBack.setupSearch(searchResultFragment, true);
    }

    public void search(Map<String, Object> data, String function) {


        BitmapDescriptor mapIconBitMapDescriptor  = BitmapDescriptorFactory.fromBitmap(bitmap);

        onGetRestaurants(data, function).addOnCompleteListener(task -> {
            FragmentManager fManager = getFragmentManager();
            if (fManager != null) {
                Fragment searchResultFragment = fManager.findFragmentById(R.id.content_bottom_sheet);
                if (searchResultFragment instanceof SearchResultFragment) {
                    LinkedHashMap<String, GameObject> gameObjects = new LinkedHashMap<>();
                    listMarkers = new HashMap<>();

                    if (task.isSuccessful()) {
                        gameObjects = task.getResult();

                        if (!gameObjects.isEmpty()) {
                            Iterator<Map.Entry<String, GameObject>> iterator = gameObjects.entrySet().iterator();
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            while (iterator.hasNext()) {
                                GameObject restaurant = iterator.next().getValue();
                                LatLng position = new LatLng(restaurant.getLat(), restaurant.getLng());
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .icon(mapIconBitMapDescriptor)
                                        .title(restaurant.getGame())
                                        .snippet(restaurant.getRestaurant()));

                                String id = restaurant.getId();
                                marker.setTag(id);

                                listMarkers.put(id, marker);

                                builder.include(position);
                            }

                            LatLngBounds boundsMarkers = builder.build();
                            searchMovement = true;
                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsMarkers, 100), new GoogleMap.CancelableCallback() {
                                @Override
                                public void onFinish() {
                                    Handler handler = new Handler();
                                    handler.postDelayed(() -> searchMovement = false, 200);
                                }

                                @Override
                                public void onCancel() {
                                    Handler handler = new Handler();
                                    handler.postDelayed(() -> searchMovement = false, 200);
                                }
                            });

                        }
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "onGetRestaurants:onFailure", e);
                        String error = "Errore.";
                        Toast.makeText(getContext(), error,
                                Toast.LENGTH_LONG).show();
                    }
                    endRestaurantSearch(gameObjects, function);
                }
            }
        });

    }

    private void endRestaurantSearch(LinkedHashMap<String, GameObject> gameObjects, String function) {
        // imposto il risultato asincrono della chiamata search here
        activityCallBack.setResult(gameObjects);

        if (function.equals("onSearch"))
            activityCallBack.setCircularProgressActive(false);
        else {
            // riattivo i tasti di navigazione globale
            activityCallBack.setGlobalNavigationActive(true);
            // imposto opacità di searchHere a 0 così da nasconderlo e lo reimposto allo stato iniziale
            searchHere.setAlpha(0);
            searchHere.revertAnimation();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        searchHere.setClickable(true);

        Bitmap mapIcon = BitmapFactory.decodeResource(getResources(), R.drawable.panda_marker);
        bitmap = Bitmap.createScaledBitmap(mapIcon, ICON_WIDTH, ICON_HEIGHT, false);

        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(getContext()));

        if (savedState == null) {
            MyLastLocationObject location = (MyLastLocationObject) SharedObject.getObject(prefs, MyLastLocationObject.DETAILS_STRING);
            if (location == null) {
                String locale = Objects.requireNonNull(getContext()).getResources().getConfiguration().locale.getCountry();
                if (locale != null && !locale.isEmpty()) {
                    LatLng pos = SharedObject.getHashMapCountry().get(locale);
                    if (pos != null) {
                        location = new MyLastLocationObject(pos.latitude, pos.longitude, 5, 0, 1);
                        SharedObject.saveObject(location, prefs, MyLastLocationObject.DETAILS_STRING);

                        savedState = new Bundle();
                        savedState.putDouble("LATITUDE", location.getLatitude());
                        savedState.putDouble("LONGITUDE", location.getLongitude());
                        savedState.putFloat("ZOOM", location.getZoom());
                        savedState.putFloat("BEARING", location.getBearing());
                        savedState.putFloat("TILT", location.getTilt());
                    }
                }
            }
            else {
                savedState = new Bundle();
                savedState.putDouble("LATITUDE", location.getLatitude());
                savedState.putDouble("LONGITUDE", location.getLongitude());
                savedState.putFloat("ZOOM", location.getZoom());
                savedState.putFloat("BEARING", location.getBearing());
                savedState.putFloat("TILT", location.getTilt());
            }
        }
        // callback per HomeFragment passando per mainActivity
        activityCallBack.onReady(mMap, savedState, markerChecked, listMarkers, bitmap);

        mMap.setOnCameraIdleListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);

        // Styling the map
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            Objects.requireNonNull(getContext()), R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        //MapUi Settings
        mMap.setBuildingsEnabled(true);
        mMap.setMaxZoomPreference(17f);
        UiSettings mapUiSettings = mMap.getUiSettings();
        mapUiSettings.setCompassEnabled(false);
        mapUiSettings.setTiltGesturesEnabled(false);
        mapUiSettings.setMyLocationButtonEnabled(false);
        mapUiSettings.setMapToolbarEnabled(false);
        mapUiSettings.setRotateGesturesEnabled(false);

    }

    public void clearMap() {
        mMap.clear();
        markerChecked = null;
        animation = null;
        listMarkers = null;
    }

    static void moveCamera(GoogleMap map, double lat, double lon, float zoom, float bearing, float tilt) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(lat, lon))
                .zoom(zoom)
                .bearing(bearing)
                .tilt(tilt)
                .build();
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private static void animateCamera(GoogleMap map, double lat, double lon, float zoom, float bearing, float tilt) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(lat, lon))
                .zoom(zoom)
                .bearing(bearing)
                .tilt(tilt)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private Task<LinkedHashMap<String, GameObject>> onGetRestaurants(Map<String, Object> data, String function) {
        return mFunctions
                .getHttpsCallable(function)
                .call(data)
                .continueWith(task -> {
                    // Questa blocco viene eseguito in caso di esito positivo o negativo
                    // se l'attività non è riuscita, getResult() genererà un'eccezione che verrà propagata verso il basso.

                    HashMap response = (HashMap) task.getResult().getData();
                    ArrayList list = (ArrayList) response.get("result");

                    LinkedHashMap<String, GameObject> games = new LinkedHashMap<>();
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    Gson gson = gsonBuilder.create();
                    if (list != null) {
                        for (Object game : list) {
                            String jsonString = gson.toJson(game);
                            GameObject gameObject = gson.fromJson(jsonString, GameObject.class);
                            games.put(gameObject.getId(), gameObject);
                        }
                    }

                    return games;
                });
    }

    @Override
    public void onCameraIdle() {
        if(firstCameraMovement)
            firstCameraMovement = false;
        else {
            if (searchHere.getAlpha() == 0 && searchHere.getState() == State.IDLE && !searchMovement) {
                searchHere.setBackground(getResources().getDrawable(R.drawable.button_bg_12));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                searchHere.setLayoutParams(layoutParams);
                searchHere.setEnabled(false);
                searchHere.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                searchHere.setEnabled(true);
                            }
                        });
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (this.markerChecked==null || !this.markerChecked.equals(marker.getTag())) {
            if (animation != null){
                animation.setDuration(300);
                animation.setInterpolator(new AnticipateInterpolator());
                animation.reverse();
            }
            this.animation = null;
            this.markerChecked = null;

            ValueAnimator animator = getAnimator(marker);
            animator.start();
            activityCallBack.onMarkerClick(marker);
            mMap.setOnInfoWindowClickListener(marker1 -> activityCallBack.onInfoWindowClick());

            this.animation = animator;
            this.markerChecked = (String) marker.getTag();
        }
        return false;
    }

    public void animateMarker(Marker marker) {
        ValueAnimator animator = getAnimator(marker);
        animator.start();
        marker.showInfoWindow();
        mMap.setOnInfoWindowClickListener(marker1 -> activityCallBack.onInfoWindowClick());

        this.animation = animator;
        this.markerChecked = (String) marker.getTag();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (animation != null) {
            animation.setDuration(300);
            animation.setInterpolator(new AnticipateInterpolator());
            animation.reverse();
            activityCallBack.onMarkerClick(null);
            mMap.setOnInfoWindowClickListener(null);
        }
        animation = null;
        markerChecked = null;
    }

    public void enableFocusMarker(boolean enable, String id) {
        if (!enable) {
            if (listMarkers != null && markerChecked != null)
                Objects.requireNonNull(listMarkers.get(markerChecked)).hideInfoWindow();
            onMapClick(null);
        }
        else {
            if (id != null && listMarkers != null) {
                Marker marker = listMarkers.get(id);
                if (marker != null) {
                    CameraPosition cameraPosition = mMap.getCameraPosition();
                    onMarkerClick(marker);
                    CameraPosition cameraPosition0 = new CameraPosition.Builder()
                                .target(marker.getPosition())
                                .zoom(15)
                                .bearing(cameraPosition.bearing)
                                .tilt(cameraPosition.tilt)
                                .build();

                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition0));
                    marker.showInfoWindow();
                }
            }
        }
    }
    public void enableFocusMarker(boolean enable, GameObject gameObject) {
        if (gameObject != null) {
            mMap.clear();

            listMarkers = new HashMap<>();
            BitmapDescriptor mapIconBitMapDescriptor  = BitmapDescriptorFactory.fromBitmap(bitmap);
            LatLng position = new LatLng(gameObject.getLat(), gameObject.getLng());
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .icon(mapIconBitMapDescriptor)
                    .title(gameObject.getGame())
                    .snippet(gameObject.getRestaurant()));

            String id = gameObject.getId();
            marker.setTag(id);

            listMarkers.put(id, marker);

            enableFocusMarker(enable, id);
        }
    }

    private ValueAnimator getAnimator(Marker marker) {
        ValueAnimator animator = ValueAnimator.ofFloat(1, 1.5f);
        animator.setInterpolator(new CustomBounceInterpolator(0.2, 10));
        animator.setDuration(500);
        animator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            String id = (String) marker.getTag();
            if (listMarkers != null && listMarkers.get(id) != null)
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(getResizedBitmap(bitmap, bitmap.getWidth() * scale, bitmap.getHeight() * scale)));
        });
        return animator;
    }

    public boolean clearMapNotInSearch() {
        if (listMarkers != null) {
            mMap.clear();
            markerChecked = null;
            animation = null;
            listMarkers = null;
            return true;
        }
        return false;
    }

    private static Bitmap getResizedBitmap(Bitmap bm, float newWidth, float newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = newWidth / width;
        float scaleHeight = newHeight / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        return Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
    }
}
