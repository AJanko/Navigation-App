package com.example.moodroad;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geocoder.service.models.GeocoderFeature;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    private DirectionsRoute currentRoute;
    private NavigationMapRoute navigationMapRoute;

    private Button startNavigation;
    private MultiAutoCompleteTextView humourSuggest;
    private AutoCompleteTextView geocoderCompletion;

    private static final String[] STATUS = new String[] {
            "Sad", "Hungry", "Bored", "Sleepy", "Tired", "Sick",
    };
    private String[] actualMoods;
    private static final String TAG = "DirectionsActivity";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new ReleaseTree());
        }

        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.auto_dropdown_list, R.id.tvHintCompletion, STATUS);
        humourSuggest = findViewById(R.id.humourSuggest);
        humourSuggest.setAdapter(adapter);
        humourSuggest.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        humourSuggest.setOnItemClickListener((parent, view, position, id) -> {
            actualMoods = getMoods();
            arrayToString(actualMoods);
        });

        final GeocoderAdapter geocoderAdapter = new GeocoderAdapter(this);
        geocoderCompletion = findViewById(R.id.destinationACTV);
        geocoderCompletion.setLines(1);
        geocoderCompletion.setAdapter(geocoderAdapter);
        geocoderCompletion.setOnItemClickListener((parent, view, position, id) -> {
            GeocoderFeature result = geocoderAdapter.getItem(position);
            geocoderCompletion.setText(result.getText());
            onMapClick(new LatLng(result.getLatitude(), result.getLongitude()));
        });

        final Drawable imgClearButton = getResources().getDrawable(R.drawable.ic_clear);
//        geocoderCompletion.setCompoundDrawablesWithIntrinsicBounds(null, null, imgClearButton, null);
        geocoderCompletion.setOnTouchListener((v, event) -> {
            AutoCompleteTextView et = (AutoCompleteTextView) v;
            if (et.getCompoundDrawables()[2] == null)
                return false;
            if (event.getAction() != MotionEvent.ACTION_UP)
                return false;
            if (event.getX() > et.getWidth() - et.getPaddingRight() - imgClearButton.getIntrinsicWidth()) {
                geocoderCompletion.setText("");
            }
            return false;
        });

        startNavigation = findViewById(R.id.startButton);
        startNavigation.setOnClickListener(this::navigationStartOnClickListener);
    }

    public void changeVisibility(int invisible) {

        startNavigation.setVisibility(invisible);
        mapView.setVisibility(invisible);
        humourSuggest.setVisibility(invisible);
        geocoderCompletion.setVisibility(invisible);
    }

    //for now string but will be LatLng type
    public void calculateFinalRoad(ArrayList additionalPlacesList) {
        //add additionalplaces from additionalplaceslist to currentroute and call navigationlauncher

        if (additionalPlacesList.size() > 0) {
            for(int i = 0; i< additionalPlacesList.size(); i++) {
                Timber.d("this is: "+additionalPlacesList.get(i));
            }
        }

        //            NavigationLauncherOptions options = NavigationLauncherOptions.builder()
//                    .directionsRoute(currentRoute)
//                    .shouldSimulateRoute(false)
//                    .build();
//            NavigationLauncher.startNavigation(MainActivity.this, options);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day), style -> {
            enableLocationComponent(style);
            addDestinationIconSymbolLayer(style);

            mapboxMap.addOnMapClickListener(MainActivity.this);

        });
    }

    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        Point destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }

        getRoute(originPoint, destinationPoint);
        startNavigation.setEnabled(true);
        startNavigation.setBackgroundResource(R.color.mapboxBlue);
        return true;
    }

    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
// You can get the generic HTTP info about the response
                        Timber.d("Response code: " + response.code());
                        if (response.body() == null) {
                            Timber.e("No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Timber.e("No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

// Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        Timber.d(currentRoute+"");
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Timber.e("Error: " + throwable.getMessage());
                    }
                });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationComponent = mapboxMap.getLocationComponent(); // Activate the MapboxMap LocationComponent to show user location / Adding in LocationComponentOptions is also an optional parameter
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    protected String[] getMoods() {

        String input = humourSuggest.getText().toString().trim();
        String[] singleInputs = new HashSet<String>(Arrays.asList(input.split("\\s*,\\s*"))).toArray(new String[0]);

        return singleInputs;
    }

    private String arrayToString(String[] array) {

        String log = " ";
        for (String anArray : array) log += anArray + ' ';
        return log;
    }

    private void navigationStartOnClickListener(View v) {
        if (actualMoods.length > 0) {
            changeVisibility(View.INVISIBLE);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            Fragment placesFragment = new PlacesFragment();

            Bundle bundle = new Bundle();
            bundle.putStringArray("mood", actualMoods);
            placesFragment.setArguments(bundle);

            transaction.replace(R.id.placesSuggestFrameLayout, placesFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        } else {
            calculateFinalRoad(new ArrayList());
        }
    }
}