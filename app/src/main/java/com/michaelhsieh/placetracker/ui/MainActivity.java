package com.michaelhsieh.placetracker.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.michaelhsieh.placetracker.R;
import com.michaelhsieh.placetracker.database.PlaceViewModel;
import com.michaelhsieh.placetracker.models.PlaceModel;
import com.michaelhsieh.placetracker.widget.PlaceTrackerWidgetDisplayService;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.michaelhsieh.placetracker.ui.ManualPlaceDetailActivity.EXTRA_MANUAL_ADDED_PLACE;

public class MainActivity extends AppCompatActivity implements PlaceAdapter.ItemClickListener, StartDragListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // key to get the clicked place's position when Activity recreated, ex. when device rotated
    private static final String STATE_CLICKED_POSITION = "position";

    // PlaceModel ID key when using Intent
    public static final String EXTRA_PLACE_ID = "PlaceModel_ID";

    // request code when opening DetailActivity
    public static final int DETAIL_ACTIVITY_REQUEST_CODE = 0;

    // request code when opening ManualPlaceDetailActivity
    public static final int MANUAL_PLACE_DETAIL_ACTIVITY_REQUEST_CODE = 1;

    // list of places user selects from search results
    private List<PlaceModel> places;

    private PlaceAdapter adapter;

    // TextView displaying empty list message
    TextView emptyListDisplay;

    // key of the selected place when user clicks a place in list
    private int clickedPlacePos = -1;

    private PlaceViewModel placeViewModel;


    PlacesClient placesClient;

    // place counter to send refreshed place info to MainActivity once all places fetched
    private int placeCounter = 0;

    private ArrayList<Place> refreshedPlaces;

    // can the user edit places with drag and drop
    private boolean isEditable = false;

    // ItemTouchHelper to drag and drop places
    private ItemTouchHelper itemTouchHelper;

    // banner ad
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // show a Toast if there's no Internet connection (Wi-Fi or cellular network)
        if (!isNetworkConnected()) {
            Toast.makeText(this, R.string.internet_connection_error, Toast.LENGTH_LONG).show();
        }

        // initialize Mobile Ads SDK
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Log.v(TAG, "onInitializationComplete: Ads SDK initialized");
            }
        });
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // restore clicked place position from saved state
            clickedPlacePos = savedInstanceState.getInt(STATE_CLICKED_POSITION);
        }

        // get TextView displaying empty list message
        emptyListDisplay = findViewById(R.id.tv_empty_list);

        // places can be null before first LiveData update
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
        adapter = new PlaceAdapter(this, places, this);
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

                    Log.d(TAG, "onChanged");

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

        // swipe left to delete a place
        // drag and drop to rearrange place
        setUpItemTouchHelper(recyclerView);

        // Initialize the SDK
        Places.initialize(getApplicationContext(), getString(R.string.google_places_api_key));

        // Create a new Places client instance
        placesClient = Places.createClient(this);

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

                    // set sorted position of the place in Room Database
                    int sortPosition = 0;
                    if (places != null && !places.isEmpty()) {
                        // place will be at last position in list,
                        // which is size of list before adding this place
                        sortPosition = places.size();
                    }
                    Log.d(TAG, "onPlaceSelected: sort position " + sortPosition);
                    newPlace.setPosition(sortPosition);

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

    /** Create menu with items, including items to add place manually and refresh places list.
     *
     * @param menu The options menu in which you place your items
     * @return True for the menu to be displayed
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    /**  Called whenever an item in the options menu is selected.
     *
     * @param item The menu item that was selected
     * @return False to allow normal menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_manual:
                // start ManualPlaceDetailsActivity
                Intent intent = new Intent(this, ManualPlaceDetailActivity.class);
                // get result when Activity finishes
                startActivityForResult(intent, MANUAL_PLACE_DETAIL_ACTIVITY_REQUEST_CODE);
                return true;
            case R.id.action_refresh:
                // refresh places list with up-to-date place info
                refreshPlacesList(placesClient);
                return true;
            case R.id.action_random_picker:
                // pick a random place and start its DetailActivity
                pickRandomPlace();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** Called when an item in the places list is clicked.
     *
     * @param view The view displaying place name and address
     * @param position The position of the place item
     */
    @Override
    public void onItemClick(View view, int position) {
        // start DetailActivity
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(EXTRA_PLACE_ID, adapter.getItem(position).getPlaceId());
        // get the position that was clicked
        // This will be used to save or delete the place from the DetailActivity buttons
        clickedPlacePos = position;

        // get the place name, address, and number of visits and
        // update the widget using an IntentService
        PlaceTrackerWidgetDisplayService.startActionUpdatePlaceTrackerWidgets(this,
                adapter.getItem(position).getName(), adapter.getItem(position).getAddress(),
                adapter.getItem(position).getNumVisits());

        startActivityForResult(intent, DETAIL_ACTIVITY_REQUEST_CODE);
    }

    /** Allow the user to drag and drop places or save positions when edit TextView is clicked.
     *
     * @param view The view clicked
     */
    public void editClicked(View view) {
        TextView editDisplay = findViewById(R.id.tv_edit);
        if (!isEditable) {
            editDisplay.setText(getResources().getText(R.string.done));
            // allow drag and drop
            isEditable = true;
            adapter.setHandleVisible(isEditable);
            // force onBindViewHolder again to update holder visibility
            adapter.notifyDataSetChanged();
        } else {
            // since user clicked done, disable drag and drop and save positions
            editDisplay.setText(getResources().getText(R.string.edit));
            isEditable = false;
            adapter.setHandleVisible(isEditable);
            // force onBindViewHolder again to update holder visibility
            adapter.notifyDataSetChanged();
            Log.d(TAG, "editClicked: done rearranging places");
            // update all sorted place positions in Room Database
            setSortPositionsInDatabase();
        }
    }

    /** Swipe left to delete a place.
     * Drag and drop to rearrange place.
     */
    private void setUpItemTouchHelper(RecyclerView recyclerView) {
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT) {

            // disable long press drag since
            // user can drag by clicking edit and dragging handles instead
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                final int fromPos = viewHolder.getAdapterPosition();
                final int toPos = target.getAdapterPosition();
                // move item at fromPos to toPos in adapter.
                PlaceModel placeToMove = places.get(viewHolder.getAdapterPosition());
                moveSingleItem(fromPos, toPos, placeToMove);
                // true if moved, false otherwise
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // get adapter position that was swiped
                // delete place at that position from the database
                PlaceModel placeToDelete = places.get(viewHolder.getAdapterPosition());

                // create delete place message
                // Are you sure you want to delete [place] at [address]?
                String deleteMessage = getResources().getString(R.string.delete_place_message)
                        + placeToDelete.getName() + getResources().getString(R.string.at) +
                        placeToDelete.getAddress() +
                        getResources().getString(R.string.question_mark);

                // alert dialog to confirm user wants to delete this place
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.delete_place_title)
                        .setMessage(deleteMessage)

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Continue with delete operation
                                placeViewModel.delete(placeToDelete);
                                // Toast.makeText(MainActivity.this, "Place deleted.", Toast.LENGTH_LONG).show();
                            }
                        })

                        // If user cancels delete, then refresh the place that was supposed to be swiped so
                        // it doesn't get swiped off screen
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // make the place visible again
                                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    /** Implement start dragging on a given ViewHolder.
     *
     * @param viewHolder The ViewHolder of the drag handle
     */
    @Override
    public void requestDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }

    /** Update sort positions in Room Database.
     * Used to get all places in the order the user rearranged them.
     * <p></p>
     * The positions will not match the places' list indexes if
     * a place is deleted.
     * <p></p>
     * Source: Diana Szczepankowska
     * https://stackoverflow.com/questions/55949538/update-onmove-changes-in-recycler-view-data-to-room-database
     *
     */
    private void setSortPositionsInDatabase() {
        for (PlaceModel place : places) {
            place.setPosition(places.indexOf(place));
            placeViewModel.update(place);
        }
    }

    /** Move an item from one position to another in the RecyclerView.
     * @param fromPosition The starting position of the place
     * @param toPosition The ending position of the place
     * @param place The place being moved
     */
    private void moveSingleItem(int fromPosition, int toPosition, PlaceModel place) {
        // update places list
        places.remove(fromPosition);
        places.add(toPosition, place);

        // notify adapter
        adapter.notifyItemMoved(fromPosition, toPosition);
    }

    /** Delete a place or add a manual place when returning to MainActivity.
     *
     * @param requestCode The request code of the Activity that called startActivityForResult
     * @param resultCode Whether the result was okay or not
     * @param data The data sent to MainActivity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DETAIL_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                // delete place from the database
                PlaceModel placeToDelete = places.get(clickedPlacePos);
                placeViewModel.delete(placeToDelete);
                // Observer's onChanged() method updates the adapter
            }
        } else if (requestCode == MANUAL_PLACE_DETAIL_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                PlaceModel manualPlace = data.getParcelableExtra(EXTRA_MANUAL_ADDED_PLACE);

                // set sorted position of the manual place in Room Database
                if (manualPlace != null) {
                    int sortPosition = 0;
                    if (places != null && !places.isEmpty()) {
                        // place will be at last position in list,
                        // which is size of list before adding this place
                        sortPosition = places.size();
                    }
                    Log.d(TAG, "onActivityResult: sort position " + sortPosition);
                    manualPlace.setPosition(sortPosition);
                }

                // insert manually added place into the database
                placeViewModel.insert(manualPlace);
                // Observer's onChanged() method updates the adapter
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
        <p></p>
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
     * When the Base64 String has been added the place is updated.
     *
     * @param placesClient The places client required to initialize the Google Places SDK
     * @param placeModel The selected place
     * @param photoMetadata The photo metadata of a place, used to get a single
     *                      Bitmap and attribution text
     */
    private void fetchPhotoAndUpdatePlaceWhenFinished(PlacesClient placesClient, PlaceModel placeModel, PhotoMetadata photoMetadata) {
        // Get the attribution text.
        final String attributions = photoMetadata.getAttributions();
        placeModel.setAttributions(attributions);

        // Must set max width and height in pixels. The image's default width and height
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

    /** Starts an IntentService to get refreshed info on user's places in the background.
     *
     * The IntentService gets refreshed place info from the Google Places SDK using the Place ID
     * of each place in the places list. The IntentService sends this info back to MainActivity.
     * Then the Room Database is updated with the refreshed info.
     *
     */
    private void refreshPlacesList(PlacesClient client) {
        // only refresh if Internet is connected
        if (isNetworkConnected()) {

            // show confirmation dialog and refresh if user confirms
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.refresh_places_title)
                    .setMessage(R.string.refresh_places_message)

                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            // continue and refresh places
                            Toast.makeText(MainActivity.this, R.string.refresh_notification_title, Toast.LENGTH_SHORT).show();

                            ArrayList<String> placeIds = new ArrayList<>();

                            PlaceModel place;
                            for (int i = 0; i < places.size(); i++) {
                                place = places.get(i);
                                placeIds.add(place.getPlaceId());
                            }

                            // reset place counter to 0, ex. when doing second refresh
                            placeCounter = 0;

                            // initialize refreshed places list
                            refreshedPlaces = new ArrayList<>();

                            if (!placeIds.isEmpty()) {
                                int placeIdsSize = placeIds.size();
                                for (int i = 0; i < placeIdsSize; i++) {
                                    fetchAllPlacesById(client, placeIds.get(i), placeIdsSize);
                                }
                            }

                        }
                    })

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        } else {
            Toast.makeText(this, R.string.refresh_internet_connection_error, Toast.LENGTH_LONG).show();

        }
    }

    /** Fetch refreshed place info using the Place ID,
     * or just log message if the Place ID can't be found.
     *
     * The info consists of a place's ID, name, address, and first photo's metadata.
     *
     * @param placesClient The places client, initialized by the IntentService
     * @param placeId The Place ID of a place in user's places list
     * @param listSize The size of the List of Place IDs
     */
    private void fetchAllPlacesById(PlacesClient placesClient, String placeId, int listSize) {

        // Specify the fields to return.
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.PHOTO_METADATAS);

        // Construct a request object, passing the place ID and fields array.
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields)
                .build();

        // Add a listener to handle the response.
        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();

            refreshedPlaces.add(place);

            // when last place has been fetched, send the info to MainActivity
            if (placeCounter == listSize - 1) {
                updatePlacesWithRefreshedInfo(refreshedPlaces);
            }
            // increase place counter by 1
            incrementPlaceCounter();
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                int statusCode = apiException.getStatusCode();
                // Handle error with given status code.
                Log.e(TAG, "Place not found: " + exception.getMessage());
                Log.e(TAG, "status code: " +  statusCode);
            }
            // when last place has been fetched, send the info to MainActivity
            if (placeCounter == listSize - 1) {
                updatePlacesWithRefreshedInfo(refreshedPlaces);
            }
            // still increase place counter by 1 because
            // a fetch may fail if ex. place added manually
            incrementPlaceCounter();
        });
    }

    private void incrementPlaceCounter() {
        placeCounter += 1;
    }

    private void updatePlacesWithRefreshedInfo(List<Place> refreshedPlaces) {

        PlaceModel refreshedPlace;
        String id;
        String name;
        String address;
        PhotoMetadata photoMetadata;

        int sortPosition;

        // Loop through refreshed Place IDs and find the user's place that
        // matches this ID. Then update that place with refreshed info.
        for (int i = 0; i < refreshedPlaces.size(); i++) {

            id = refreshedPlaces.get(i).getId();
            name = refreshedPlaces.get(i).getName();
            address = refreshedPlaces.get(i).getAddress();

            if (places != null) {
                for (PlaceModel originalPlace : places) {
                    if (originalPlace.getPlaceId().equals(id)) {
                        // set new place to original place with refreshed
                        // name and address
                        refreshedPlace = new PlaceModel(id, name, address);

                        // set sort position of refreshed place
                        sortPosition = originalPlace.getPosition();
                        Log.d(TAG, "updatePlacesWithRefreshedInfo: sort position " + sortPosition);
                        refreshedPlace.setPosition(sortPosition);

                        refreshedPlace.setNotes(originalPlace.getNotes());
                        refreshedPlace.setVisits(originalPlace.getVisits());
                        refreshedPlace.setBase64String(originalPlace.getBase64String());
                        refreshedPlace.setAttributions(originalPlace.getAttributions());

                        // update place in the database with refreshed place info
                        if (placeViewModel != null) {
                            placeViewModel.update(refreshedPlace);
                            // if photo metadata not found, ex. place added manually,
                            // photo metadata element will be null
                            List<PhotoMetadata> refreshedPhotoMetadata = refreshedPlaces.get(i).getPhotoMetadatas();
                            if (refreshedPhotoMetadata != null && !refreshedPhotoMetadata.isEmpty()) {
                                photoMetadata = refreshedPhotoMetadata.get(0);
                                fetchPhotoAndUpdatePlaceWhenFinished(placesClient, refreshedPlace, photoMetadata);
                            }
                        }
                    }
                }
            }

        }

        Toast.makeText(MainActivity.this, R.string.refresh_finished, Toast.LENGTH_SHORT).show();
    }


    /** Start a random place's DetailActivity. This may be useful if ex. the user
     * wants to pick a random restaurant. Similar to onItemClick().
     */
    private void pickRandomPlace() {
        // don't pick a random place if there are no places
        if (adapter != null && places != null && !places.isEmpty()) {
            // pick a random number between 0 and places.size() - 1
            // this will be the position of the place in the user's list
            int randPos = new Random().nextInt(places.size());
            PlaceModel randPlace = adapter.getItem(randPos);
            // start DetailActivity
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(EXTRA_PLACE_ID, randPlace.getPlaceId());
            // get the position that was clicked
            // This will be used to save or delete the place from the DetailActivity buttons
            clickedPlacePos = randPos;

            Toast.makeText(this, randPlace.getName(), Toast.LENGTH_SHORT).show();

            // get the random place's name, address, and number of visits and
            // update the widget
            PlaceTrackerWidgetDisplayService.startActionUpdatePlaceTrackerWidgets(this,
                    randPlace.getName(), randPlace.getAddress(),
                    randPlace.getNumVisits());

            startActivityForResult(intent, DETAIL_ACTIVITY_REQUEST_CODE);
        } else if (places != null && places.isEmpty()) {
            Toast.makeText(this, R.string.random_pick_empty_error, Toast.LENGTH_LONG).show();
        }
    }
}
