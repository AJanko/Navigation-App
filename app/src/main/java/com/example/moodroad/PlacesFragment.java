package com.example.moodroad;

import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;

import timber.log.Timber;

public class PlacesFragment extends Fragment {

    Button acceptBtn;
    ListView placesSuggestionListView;

    ArrayList<String> listItems;
    ArrayList<String> choosedItems;
    String[] currentMood;

    public PlacesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_places, container, false);

        prepareArguments(view);

        ArrayAdapter<String> placesAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_list_item_multiple_choice,
                listItems
        );

        placesSuggestionListView.setAdapter(placesAdapter);

        return view;
    }

    private void prepareArguments(View view) {

        choosedItems = new ArrayList<>();
        listItems = new ArrayList<>();

        // Here will be suggest places
        listItems.add("First item");
        listItems.add("Second item");
        listItems.add("Third item");

        Timber.d( "first");
        if ((currentMood = this.getArguments().getStringArray("mood")) != null) {
            Timber.d( "second");
            getPlaces(); //add Places if user add his mood
        }

        acceptBtn = view.findViewById(R.id.fragmentAcceptBtn);
        acceptBtn.setOnClickListener(v -> acceptBtnClickListener());

        placesSuggestionListView = view.findViewById(R.id.placesSuggestListView);
        placesSuggestionListView.setOnItemClickListener(this::placesSuggestionOnItemClickListener);
    }

    private void getPlaces() {

        Timber.d( "third");

        Places.initialize(getActivity(), getString(R.string.GOOGLE_API_KEY));
        PlacesClient placesClient = Places.createClient(getActivity());

        //build RectangularBounds object - should be calculated by route
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(-33.880490, 151.184363),
                new LatLng(-33.858754, 151.229596)
        ); //be careful LatLng is mapbox object type and rectangulatbounds is google object type



        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setLocationBias(bounds)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener((response) -> {

            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                Timber.d( prediction.getPlaceId());
                Timber.d( prediction.getPrimaryText(null).toString());
            }
        })
                .addOnFailureListener((exception) -> {
                    Timber.d( "place not found"+exception.toString());
                });
    }

    public void acceptBtnClickListener() {
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.calculateFinalRoad(choosedItems);
        mainActivity.changeVisibility(View.VISIBLE);

        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }

    public void placesSuggestionOnItemClickListener(AdapterView<?> parent, View view1, int position, long id) {
        if (placesSuggestionListView.isItemChecked(position)) {
            choosedItems.add(listItems.get(position));
        }
        else {
            choosedItems.remove(listItems.get(position));
        }
    }

}
