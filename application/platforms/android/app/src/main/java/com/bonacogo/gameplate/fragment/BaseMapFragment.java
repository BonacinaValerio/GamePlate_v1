package com.bonacogo.gameplate.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.bonacogo.gameplate.model.MyLastLocationObject;
import com.bonacogo.gameplate.util.LocationUtil;
import com.bonacogo.gameplate.util.SharedObject;
import com.bonacogo.gameplate.viewmodel.SavedStateFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import br.com.simplepass.loadingbutton.customViews.CircularProgressButton;

public class BaseMapFragment extends Fragment implements
        OnMapReadyCallback,
        GoogleMap.OnCameraIdleListener {
    static final String TAG = "MapFragment";

    boolean firstCameraMovement = true;
    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    Boolean mRequestingSearch;

    /**
     * Constant used in the location settings dialog.
     */
    static final int REQUEST_CHECK_SETTINGS = 0x1;
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 100;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    private static final String KEY_LIST_MARKER = "list-marker";
    private static final String KEY_MARKER_CHECKED = "marker-checked";
    private final static String KEY_REQUESTING_SEARCH = "requesting-search";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_FIRST_START = "first-start";
    final static String KEY_LAST_LOCATION = "last-location";
    private final static String KEY_SAVED_STATE = "saved-state";


    /**
     * Provides access to the Fused Location Provider API.
     */
    FusedLocationProviderClient mFusedLocationClient;

    /**
     * Provides access to the Location Settings API.
     */
    SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    Location mCurrentLocation;

    boolean mFirstStart;
    Bundle savedState;

    MapView mMapView;
    GoogleMap mMap;
    FloatingActionButton findMe;
    CircularProgressButton searchHere;

    String markerChecked;
    HashMap<String, Marker> listMarkers;
    MyLastLocationObject lastLocation;
    SavedStateFragment savedStateFragment;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedStateFragment = new ViewModelProvider(requireActivity()).get(SavedStateFragment.class);
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_SEARCH)) {
                mRequestingSearch = savedInstanceState.getBoolean(
                        KEY_REQUESTING_SEARCH);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }

            if (savedInstanceState.keySet().contains(KEY_FIRST_START)) {
                mFirstStart = savedInstanceState.getBoolean(KEY_FIRST_START);
            }
            if (savedInstanceState.keySet().contains(KEY_SAVED_STATE)) {
                savedState = savedInstanceState.getBundle(KEY_SAVED_STATE);
            }
            if (savedInstanceState.keySet().contains(KEY_MARKER_CHECKED)) {
                markerChecked = savedInstanceState.getString(KEY_MARKER_CHECKED);
            }
            if (savedInstanceState.keySet().contains(KEY_LIST_MARKER)) {
                listMarkers = (HashMap<String, Marker>) savedInstanceState.getSerializable(KEY_LIST_MARKER);
            }
        }
    }

    /*
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }


    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        if (lastLocation != null && mMap != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))
                    .zoom(lastLocation.getZoom())
                    .bearing(lastLocation.getBearing())
                    .tilt(lastLocation.getTilt())
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            lastLocation = null;
        }
        startLocationUpdate();
    }

    private void startLocationUpdate() {
        if (LocationUtil.checkPermissions(getContext())) {
            // Begin by checking if the device has the necessary location settings.
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(locationSettingsResponse -> {
                        Log.i(TAG, "All location settings are satisfied. ");

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());
                    });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        // Remove location updates to save battery.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);

        if (mMap != null) {
            CameraPosition pos = mMap.getCameraPosition();
            SharedPreferences prefs = getContext().getSharedPreferences("com.bonacogo.gameplate", Context.MODE_PRIVATE);
            MyLastLocationObject location = new MyLastLocationObject(
                    pos.target.latitude,
                    pos.target.longitude,
                    pos.zoom,
                    pos.bearing,
                    pos.tilt);
            SharedObject.saveObject(location, prefs, MyLastLocationObject.DETAILS_STRING);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMapView != null)
            mMapView.onDestroy();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mMapView != null)
            mMapView.onLowMemory();
    }

    @Override
    public void onStop() {
        if (mMapView != null)
            mMapView.onStop();
        super.onStop();
    }


    /*
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
        if (mMap != null) {
            CameraPosition pos = mMap.getCameraPosition();
            lastLocation = new MyLastLocationObject(
                    pos.target.latitude,
                    pos.target.longitude,
                    pos.zoom,
                    pos.bearing,
                    pos.tilt);
            savedInstanceState.putSerializable(KEY_LAST_LOCATION, lastLocation);
        }

        if (mMapView != null)
            mMapView.onSaveInstanceState(savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        savedStateFragment.setBundle(TAG, saveBundle());
    }


    private Bundle saveBundle() {
        Bundle bundle = new Bundle();

        bundle.putBoolean(KEY_REQUESTING_SEARCH, mRequestingSearch);
        bundle.putBoolean(KEY_FIRST_START, mFirstStart);
        bundle.putParcelable(KEY_LOCATION, mCurrentLocation);
        bundle.putSerializable(KEY_LIST_MARKER, listMarkers);
        bundle.putString(KEY_MARKER_CHECKED, markerChecked);
        if (mMap != null) {
            Bundle position = new Bundle();
            mMap.setPadding(0,0,0,0);
            CameraPosition pos = mMap.getCameraPosition();
            position.putDouble("LATITUDE", pos.target.latitude);
            position.putDouble("LONGITUDE", pos.target.longitude);
            position.putFloat("ZOOM", pos.zoom);
            position.putFloat("BEARING", pos.bearing);
            position.putFloat("TILT", pos.tilt);
            bundle.putBundle(KEY_SAVED_STATE, position);
        }
        return bundle;
    }

    @Override
    public void onCameraIdle() {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}
