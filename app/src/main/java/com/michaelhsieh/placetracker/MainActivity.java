package com.michaelhsieh.placetracker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.michaelhsieh.placetracker.model.PlaceModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.michaelhsieh.placetracker.DetailActivity.DELETE;
import static com.michaelhsieh.placetracker.DetailActivity.EXTRA_BUTTON_TYPE;
import static com.michaelhsieh.placetracker.DetailActivity.EXTRA_SAVED_PLACE;
import static com.michaelhsieh.placetracker.DetailActivity.SAVE;

public class MainActivity extends AppCompatActivity implements PlaceAdapter.ItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // key to get the saved places list when Activity recreated, ex. when device rotated
    private static final String STATE_PLACES = "places";

    // key to get the clicked place's position when Activity recreated, ex. when device rotated
    private static final String STATE_CLICKED_POSITION = "position";

    // PlaceModel key when using Intent
    public static final String EXTRA_PLACE = "PlaceModel";

    // request code when opening DetailActivity
    public static final int DETAIL_ACTIVITY_REQUEST_CODE = 0;

    // list of places user selects from search results
    private List<PlaceModel> places;

    private PlaceAdapter adapter;

    // TextView displaying empty list message
    TextView emptyListDisplay;

    // key of the selected place when user clicks a place in list
    private int clickedPlacePos = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // show a Toast if there's no Internet connection (Wi-Fi or cellular network)
        if (!isNetworkConnected()) {
            Toast.makeText(this, R.string.internet_connection_error, Toast.LENGTH_LONG).show();
        }

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore places list from saved state
            places = savedInstanceState.getParcelableArrayList(STATE_PLACES);

            // restore clicked place position from saved state
            clickedPlacePos = savedInstanceState.getInt(STATE_CLICKED_POSITION);
        } else {
            // initialize places list
            places = new ArrayList<>();
        }

        // get TextView displaying empty list message
        emptyListDisplay = findViewById(R.id.tv_empty_list);

        // Check if empty list message should be displayed.
        // must call checkEmpty() here because adapter is not created yet
        checkEmpty();

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.rv_places);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        // add a divider
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        // use a custom white divider
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.place_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        adapter = new PlaceAdapter(this, places);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        /* Display empty list message if no places have been added yet.
        Register an Observer that checks if an empty list message needs
        to be displayed whenever the RecyclerView updates, inserts items, or removes items.

        Source:
        wonsuc
        https://stackoverflow.com/questions/28217436/how-to-show-an-empty-view-with-a-recyclerview
         */
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onChanged() {
                super.onChanged();
                checkEmpty();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmpty();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmpty();
            }

        });

        // Initialize the SDK
        Places.initialize(getApplicationContext(), getString(R.string.google_places_api_key));

        // Create a new Places client instance
        PlacesClient placesClient = Places.createClient(this);

        // Initialize the AutocompleteSupportFragment.
        final AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                String name = place.getName();
                String id = place.getId();
                String address = place.getAddress();

                PlaceModel newPlace = new PlaceModel(id, name, address);
                //Log.i(TAG, "Place: " + name + ", " + id);
                //Log.i(TAG, "Place address: " + address);

                if (isPlaceInList(newPlace)) {
                    Toast.makeText(getApplicationContext(), R.string.existing_place_message, Toast.LENGTH_LONG).show();
                } else {
                    // add to the list of places
                    insertSingleItem(newPlace);
                }
            }

            @Override
            public void onError(Status status) {
                Log.e(TAG, "An error occurred: " + status);
            }
        });

    }

    @Override
    public void onItemClick(View view, int position) {
        // start DetailActivity
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(EXTRA_PLACE, adapter.getItem(position));
        // get the position that was clicked
        // This will be used to save or delete the place from the DetailActivity buttons
        clickedPlacePos = position;
        startActivityForResult(intent, DETAIL_ACTIVITY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DETAIL_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                String buttonType = data.getStringExtra(EXTRA_BUTTON_TYPE);
                if (buttonType != null && buttonType.equals(DELETE)) {
                    removeSingleItem(clickedPlacePos);
                }
                else if (buttonType != null && buttonType.equals(SAVE)) {
                    PlaceModel updatedPlace = data.getParcelableExtra(EXTRA_SAVED_PLACE);
                    if (updatedPlace != null) {
                        updateSingleItem(clickedPlacePos, updatedPlace);
                    }
                }
            }
        }
    }

    /** Save the state of this Activity, ex. when device rotated
     *
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);

        // Save the user's current list of places
        savedInstanceState.putParcelableArrayList(STATE_PLACES, new ArrayList<>(places));

        // Save the position of a place that's been clicked
        savedInstanceState.putInt(STATE_CLICKED_POSITION, clickedPlacePos);
    }

    /** Check if connected to Wi-Fi or cellular network.

        Source:
        pavelnazimok
        https://stackoverflow.com/questions/32547006/connectivitymanager-getnetworkinfoint-deprecated
     */
    private boolean isNetworkConnected() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT < 23) {
                // use deprecated methods on older devices
                final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                if (networkInfo != null) {
                    return (networkInfo.isConnected() && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE));
                }
            } else {
                // getActiveNetwork() was added in API 23
                final Network network = connectivityManager.getActiveNetwork();

                if (network != null) {
                    final NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);

                    if (networkCapabilities != null) {
                        return (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                    }
                }
            }
        }

        return false;
    }

    /** Display empty list message if list is empty,
            otherwise hide message */
    private void checkEmpty() {
        if (places.size() == 0) {
            emptyListDisplay.setVisibility(View.VISIBLE);
        } else {
            emptyListDisplay.setVisibility(View.GONE);
        }
    }

    /** Checks if user is trying to add a place that already exists in places list
     *
     * @param place The place that is being added
     * @return boolean indicating if the place is already in the list
     */
    private boolean isPlaceInList(PlaceModel place) {
        for (PlaceModel existingPlace: places) {
            // compare existing places with new place by Place ID
            if (existingPlace.getId().equals(place.getId())) {
                return true;
            }
        }
        return false;
    }

    /* Methods to update RecyclerView data.

    Source:
    Suragch
    https://stackoverflow.com/questions/31367599/how-to-update-recyclerview-adapter-data/48959184#48959184  */

    /** Insert an item into the RecyclerView
     * @param place The place being inserted
    */
    private void insertSingleItem(PlaceModel place) {
        // insert at the very end of the list
        int insertIndex = places.size();
        // add place to list
        places.add(insertIndex, place);
        adapter.notifyItemInserted(insertIndex);
    }

    /** Remove an item from the RecyclerView
     * @param removeIndex The index of the place to be removed
     */
    private void removeSingleItem(int removeIndex) {
        // remove place from list
        places.remove(removeIndex);
        adapter.notifyItemRemoved(removeIndex);
    }

    /** Update an item in the RecyclerView
     * @param updateIndex The index of the place which will be updated
     * @param place The new place which will replace the existing place
     */
    private void updateSingleItem(int updateIndex, PlaceModel place) {
        // update place at index in list
        places.set(updateIndex, place);
        adapter.notifyItemChanged(updateIndex);
    }
}
