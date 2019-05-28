package com.example.moodroad;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

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

        currentMood = this.getArguments().getStringArray("mood");

        acceptBtn = view.findViewById(R.id.fragmentAcceptBtn);
        acceptBtn.setOnClickListener(v -> acceptBtnClickListener());

        placesSuggestionListView = view.findViewById(R.id.placesSuggestListView);
        placesSuggestionListView.setOnItemClickListener(this::placesSuggestionOnItemClickListener);
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
