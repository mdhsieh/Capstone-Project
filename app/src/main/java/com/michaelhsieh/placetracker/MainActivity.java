package com.michaelhsieh.placetracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.michaelhsieh.placetracker.database.PlaceViewModel;
import com.michaelhsieh.placetracker.model.PlaceModel;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import static com.michaelhsieh.placetracker.DetailActivity.DELETE;
import static com.michaelhsieh.placetracker.DetailActivity.EXTRA_BUTTON_TYPE;
import static com.michaelhsieh.placetracker.DetailActivity.EXTRA_SAVED_PLACE;
import static com.michaelhsieh.placetracker.DetailActivity.SAVE;

public class MainActivity extends AppCompatActivity implements PlaceAdapter.ItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

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

    private PlaceViewModel placeViewModel;

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
            // restore clicked place position from saved state
            clickedPlacePos = savedInstanceState.getInt(STATE_CLICKED_POSITION);
        }

        // get TextView displaying empty list message
        emptyListDisplay = findViewById(R.id.tv_empty_list);

        // place can be null before first LiveData update
        // ex. when app first started or screen rotated, places is null in onCreate

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

        placeViewModel = new ViewModelProvider(this).get(PlaceViewModel.class);

        // add an observer for the LiveData returned by getAllPlaces()
        // The onChanged() method fires when the observed data changes and the activity is in the foreground
        placeViewModel.getAllPlaces().observe(this, new Observer<List<PlaceModel>>() {
            @Override
            public void onChanged(@Nullable final List<PlaceModel> updatedPlaces) {

                // updatedPlaces should be an empty list in onCreate, ex. when app first
                // starts up and after rotation, not null
                if (updatedPlaces != null) {
                    // set places list to updated places list
                    places = updatedPlaces;

                    // Update the cached copy of the places in the adapter.
                    adapter.setPlaces(places);

                    // Display empty list message if list is empty.
                    checkEmpty();
                } else {
                    Log.e(TAG, "updated places list is null!");
                }

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
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.PHOTO_METADATAS));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {

                String name = place.getName();
                String id = place.getId();
                String address = place.getAddress();

                PlaceModel newPlace = new PlaceModel(id, name, address);

                // Get the photo metadata.
                final List<PhotoMetadata> metadata = place.getPhotoMetadatas();

                if (isPlaceInList(newPlace)) {
                    Toast.makeText(getApplicationContext(), R.string.existing_place_message, Toast.LENGTH_LONG).show();
                } else {

                    if (metadata == null || metadata.isEmpty()) {
                        Log.v(TAG, "No photo metadata.");
                    } else {
                        // get the photo's metadata,
                        // which will be used to get a bitmap and attribution text
                        final PhotoMetadata photoMetadata = metadata.get(0);
                        /* This method uses fetchPhoto(), an asynchronous method.
                        The method will finish after the place has already been inserted, so
                        update the place once all photos have been fetched. */
                        fetchPhotoAndUpdatePlaceWhenFinished(placesClient, newPlace, photoMetadata);
                    }

                    // insert place into the database
                    placeViewModel.insert(newPlace);
                    // Observer's onChanged() method updates the adapter
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.e(TAG, "An error occurred: " + status);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_manual:
                // Toast.makeText(this, "add a place manually selected", Toast.LENGTH_LONG).show();
                // start ManualPlaceDetailsActivity
                Intent intent = new Intent(this, ManualPlaceDetailActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    // delete place from the database
                    PlaceModel placeToDelete = places.get(clickedPlacePos);
                    placeViewModel.delete(placeToDelete);
                    // Observer's onChanged() method updates the adapter
                }
                else if (buttonType != null && buttonType.equals(SAVE)) {
                    PlaceModel updatedPlace = data.getParcelableExtra(EXTRA_SAVED_PLACE);
                    if (updatedPlace != null) {
                        // update() in dao uses the primary key of the original clicked place,
                        // which is the Place ID

                        // update place in the database
                        placeViewModel.update(updatedPlace);
                        // Observer's onChanged() method updates the adapter
                    }
                }
            }
        }
    }

    /** Save the state of this Activity, ex. when device rotated.
     *
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);

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

    /** Display empty list message if list is empty, otherwise hide message.
     *
     */
    private void checkEmpty() {
        if (places.size() == 0) {
            emptyListDisplay.setVisibility(View.VISIBLE);
        } else {
            emptyListDisplay.setVisibility(View.GONE);
        }
    }

    /** Checks if user is trying to add a place that already exists in places list.
     *
     * @param place The place that is being added
     * @return boolean indicating if the place is already in the list
     */
    private boolean isPlaceInList(PlaceModel place) {
        for (PlaceModel existingPlace: places) {
            // compare existing places with new place by Place ID
            if (existingPlace.getPlaceId().equals(place.getPlaceId())) {
                return true;
            }
        }
        return false;
    }

    /** Get the photo from place metadata as a Bitmap encoded as Base64 String and
     * set as place Base64 String. This uses an asynchronous method fetchPhoto(), so
     * by the time it finishes the place has already been inserted.
     * When the Base64 String has been added the place should be updated.
     *
     * @param placesClient The places client required to initialize the Google Places SDK
     * @param placeModel The selected place
     * @param photoMetadata The photo metadata of a place, used to get a single
     *                      Bitmap and attribution text
     */
    private void fetchPhotoAndUpdatePlaceWhenFinished(PlacesClient placesClient, PlaceModel placeModel, PhotoMetadata photoMetadata) {
        // Get the attribution text.
        final String attributions = photoMetadata.getAttributions();
        Log.d(TAG, "attributions: " + attributions);

        // must set max width and height in pixels. The image's default width and height
        // causes a TransactionTooLargeException and the app crashes

        // Create a FetchPhotoRequest.
        final FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                .setMaxWidth(500)
                .setMaxHeight(300)
                .build();
        placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {

            Bitmap bitmap = fetchPhotoResponse.getBitmap();

            // convert bitmap to Base64 String
            String base64Image = encodeBitmapToBase64String(bitmap);

            // update the selected place with the Base64 String
            placeModel.setBase64String(base64Image);
            placeViewModel.update(placeModel);

        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                final ApiException apiException = (ApiException) exception;
                Log.e(TAG, "Place not found: " + exception.getMessage());
                final int statusCode = apiException.getStatusCode();
                Log.e(TAG, "Status code: " + statusCode);
            }
        });
    }

    /** Encode Bitmap to Base64 String.
     *
     * @param bitmap The Bitmap to be encoded into a String
     * @return A String that's in Base64 format. Used to store Bitmap into Room database
     */
    private String encodeBitmapToBase64String(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream); // Could be Bitmap.CompressFormat.PNG or Bitmap.CompressFormat.WEBP
        byte[] byteArrayInput = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArrayInput, Base64.DEFAULT);
    }
}
