package com.michaelhsieh.placetracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.text.TextUtils;
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
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.michaelhsieh.placetracker.database.PlaceViewModel;
import com.michaelhsieh.placetracker.model.PlaceModel;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.michaelhsieh.placetracker.DetailActivity.DELETE;
import static com.michaelhsieh.placetracker.DetailActivity.EXTRA_BUTTON_TYPE;
import static com.michaelhsieh.placetracker.DetailActivity.EXTRA_SAVED_PLACE;
import static com.michaelhsieh.placetracker.DetailActivity.SAVE;
import static com.michaelhsieh.placetracker.ManualPlaceDetailActivity.EXTRA_MANUAL_ADDED_PLACE;
import static com.michaelhsieh.placetracker.RefreshPlacesListService.EXTRA_REFRESHED_PHOTO_METADATA;
import static com.michaelhsieh.placetracker.RefreshPlacesListService.EXTRA_REFRESHED_PLACE_ADDRESSES;
import static com.michaelhsieh.placetracker.RefreshPlacesListService.EXTRA_REFRESHED_PLACE_IDS;
import static com.michaelhsieh.placetracker.RefreshPlacesListService.EXTRA_REFRESHED_PLACE_NAMES;

public class MainActivity extends AppCompatActivity implements PlaceAdapter.ItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // key to get the clicked place's position when Activity recreated, ex. when device rotated
    private static final String STATE_CLICKED_POSITION = "position";

    // refresh places list notification channel ID
    public static final String CHANNEL_ID = "refresh_places_list_channel";

    // key of all Place IDs in places list to use in RefreshPlaceListService
    public static final String EXTRA_SERVICE_PLACE_IDS = "service_place_ids";

    // PlaceModel key when using Intent
    public static final String EXTRA_PLACE = "PlaceModel";

    // request code when opening DetailActivity
    public static final int DETAIL_ACTIVITY_REQUEST_CODE = 0;

    // request code when opening ManualPlaceDetailActivity
    public static final int MANUAL_PLACE_DETAIL_ACTIVITY_REQUEST_CODE = 1;

    // MainActivity will respond to this action String
    public static final String RECEIVE_REFRESHED_PLACES_INFO = "receive_refreshed_places_info";

    // maximum allowable size, in Kilobytes, of a place's Bitmap encoded as a Base64 String
    private static final int MAX_BASE64_STRING_SIZE_IN_KB = 300;

    // maximum allowable size, in Kilobytes, of data sent to another Activity through Intent
    public static final int MAX_BUNDLE_SIZE_IN_KB = 500;

    // list of places user selects from search results
    private List<PlaceModel> places;

    private PlaceAdapter adapter;

    // TextView displaying empty list message
    TextView emptyListDisplay;

    // key of the selected place when user clicks a place in list
    private int clickedPlacePos = -1;

    private PlaceViewModel placeViewModel;

    private LocalBroadcastManager broadcastManager;

    // banner ad
    private AdView adView;

    /** BroadcastReceiver to get refreshed place info from RefreshPlacesListService.
     *
     * A place in the Room Database is only updated with refreshed info if
     * its Place ID can be found.
     * If the place's photo metadata is available, the place is updated twice.
     *
     * Creates a new Places Client.
     * May contain null elements in photo metadata ArrayList, ex. if places added manually.
     */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() != null && intent.getAction().equals(RECEIVE_REFRESHED_PLACES_INFO)) {
                // get refreshed place IDs, names, addresses, and metadata
                ArrayList<String> updatedPlaceIds = intent.getStringArrayListExtra(EXTRA_REFRESHED_PLACE_IDS);
                ArrayList<String> updatedPlaceNames = intent.getStringArrayListExtra(EXTRA_REFRESHED_PLACE_NAMES);
                ArrayList<String> updatedPlaceAddresses = intent.getStringArrayListExtra(EXTRA_REFRESHED_PLACE_ADDRESSES);
                ArrayList<PhotoMetadata> updatedPhotoMetadata = intent.getParcelableArrayListExtra(EXTRA_REFRESHED_PHOTO_METADATA);

                // Create a new Places client instance
                PlacesClient placesClient = Places.createClient(MainActivity.this);

                if (updatedPlaceIds != null && updatedPlaceNames != null && updatedPlaceAddresses != null && updatedPhotoMetadata != null) {

                    PlaceModel refreshedPlace;
                    String id;
                    String name;
                    String address;
                    PhotoMetadata photoMetadata;
                    // Loop through refreshed Place IDs and find the user's place that
                    // matches this ID. Then update that place with refreshed info.
                    for (int i = 0; i < updatedPlaceIds.size(); i++) {

                        id = updatedPlaceIds.get(i);
                        name = updatedPlaceNames.get(i);
                        address = updatedPlaceAddresses.get(i);

                        if (places != null) {
                            for (PlaceModel originalPlace : places) {
                                if (originalPlace.getPlaceId().equals(id)) {
                                    // set new place to original place with refreshed
                                    // name and address
                                    refreshedPlace = new PlaceModel(id, name, address);
                                    refreshedPlace.setNotes(originalPlace.getNotes());
                                    refreshedPlace.setVisits(originalPlace.getVisits());
                                    refreshedPlace.setBase64String(originalPlace.getBase64String());
                                    refreshedPlace.setAttributions(originalPlace.getAttributions());

                                    // update place in the database with refreshed place info
                                    if (placeViewModel != null) {
                                        placeViewModel.update(refreshedPlace);
                                        // if photo metadata not found, ex. place added manually,
                                        // photo metadata element will be null
                                        if (updatedPhotoMetadata.get(i) != null) {
                                            photoMetadata = updatedPhotoMetadata.get(i);
                                            fetchPhotoAndUpdatePlaceWhenFinished(placesClient, refreshedPlace, photoMetadata);
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }

            Toast.makeText(MainActivity.this, R.string.refresh_finished, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create notification channel
        createNotificationChannel();

        // create broadcast manager and register receiver
        broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIVE_REFRESHED_PLACES_INFO);
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);

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
                refreshPlacesList();
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
        intent.putExtra(EXTRA_PLACE, adapter.getItem(position));
        // get the position that was clicked
        // This will be used to save or delete the place from the DetailActivity buttons
        clickedPlacePos = position;

        // get the place name, address, and number of visits and
        // update the widget using an IntentService
        PlaceTrackerWidgetDisplayService.startActionUpdatePlaceTrackerWidgets(this,
                adapter.getItem(position).getName(), adapter.getItem(position).getAddress(),
                adapter.getItem(position).getNumVisits());

        int intentSizeInKB = getBundleSizeInBytes(intent.getExtras()) / 1000;

        if (intentSizeInKB >= MAX_BUNDLE_SIZE_IN_KB) {
            Log.w(TAG, "onItemClick: size in KB: " + intentSizeInKB);
            Log.w(TAG, "onItemClick: PlaceModel put in Intent may contain too much data.");
        }

        startActivityForResult(intent, DETAIL_ACTIVITY_REQUEST_CODE);
    }

    /** Unregister the BroadcastReceiver
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        broadcastManager.unregisterReceiver(broadcastReceiver);
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

                        // get the saved place's name, address, and number of visits and
                        // update the widget
                        PlaceTrackerWidgetDisplayService.startActionUpdatePlaceTrackerWidgets(this,
                                updatedPlace.getName(), updatedPlace.getAddress(),
                                updatedPlace.getNumVisits());
                    }
                }
            }
        } else if (requestCode == MANUAL_PLACE_DETAIL_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                PlaceModel manualPlace = data.getParcelableExtra(EXTRA_MANUAL_ADDED_PLACE);
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

            // get size of bitmap in kilobytes, which is bytes / 1000
            int bitmapSizeInKB = bitmap.getByteCount() / 1000;

            // convert bitmap to Base64 String
            String base64Image = encodeBitmapToBase64String(bitmap);

            int base64StringSizeInKB = calculateBase64StringSizeInBytes(base64Image) / 1000;

            // uncomment to check that approximate size of Base64String in bytes is correct
            /* try {
                // a Base64 String encodes binary date to ASCII,
                // and Android's default character set, UTF-8, is backwards compatible with ASCII
                // since ASCII is a subset of UTF-8
                int byteLength = base64Image.getBytes("UTF-8").length;
                Log.d(TAG, "bitmap using getBytes() default UTF-8 in KB: " + byteLength / 1000 + " KB");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } */

            /* if Bitmap is too large, the PlaceModel sent to DetailActivity through Intent's
             putExtra() could cause a TransactionTooLargeException and crash the app. */
            if (base64StringSizeInKB >= MAX_BASE64_STRING_SIZE_IN_KB) {
                Log.w(TAG, "Bitmap fetched is too large! Not adding to place. Bitmap size: "
                    + bitmapSizeInKB + " KB, Base64 String size: " + base64StringSizeInKB + " KB");
            } else {
                // update the selected place with the Base64 String
                placeModel.setBase64String(base64Image);
                placeViewModel.update(placeModel);
            }

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
    private void refreshPlacesList() {
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
                            // Continue and start refresh service
                            Toast.makeText(MainActivity.this, R.string.refresh_notification_title, Toast.LENGTH_SHORT).show();

                            Intent serviceIntent = new Intent(MainActivity.this, RefreshPlacesListService.class);

                            ArrayList<String> placeIds = new ArrayList<>();

                            PlaceModel place;
                            for (int i = 0; i < places.size(); i++) {
                                place = places.get(i);
                                placeIds.add(place.getPlaceId());
                            }

                            // put ArrayList of all the user's Place IDs in Intent
                            serviceIntent.putStringArrayListExtra(EXTRA_SERVICE_PLACE_IDS, placeIds);

                            // use startForegroundService in API 26 and higher, otherwise startService on lower API
                            ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                        }
                    })

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

            /*Toast.makeText(this, R.string.refresh_notification_title, Toast.LENGTH_SHORT).show();

            Intent serviceIntent = new Intent(this, RefreshPlacesListService.class);

            ArrayList<String> placeIds = new ArrayList<>();

            PlaceModel place;
            for (int i = 0; i < places.size(); i++) {
                place = places.get(i);
                placeIds.add(place.getPlaceId());
            }

            // put ArrayList of all the user's Place IDs in Intent
            serviceIntent.putStringArrayListExtra(EXTRA_SERVICE_PLACE_IDS, placeIds);

            // use startForegroundService in API 26 and higher, otherwise startService on lower API
            ContextCompat.startForegroundService(this, serviceIntent);*/

        } else {
            Toast.makeText(this, R.string.refresh_internet_connection_error, Toast.LENGTH_LONG).show();

        }
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
            intent.putExtra(EXTRA_PLACE, randPlace);
            // get the position that was clicked
            // This will be used to save or delete the place from the DetailActivity buttons
            clickedPlacePos = randPos;

            Toast.makeText(this, randPlace.getName(), Toast.LENGTH_SHORT).show();

            // get the random place's name, address, and number of visits and
            // update the widget
            PlaceTrackerWidgetDisplayService.startActionUpdatePlaceTrackerWidgets(this,
                    randPlace.getName(), randPlace.getAddress(),
                    randPlace.getNumVisits());

            int intentSizeInKB = getBundleSizeInBytes(intent.getExtras()) / 1000;

            if (intentSizeInKB >= MAX_BUNDLE_SIZE_IN_KB) {
                Log.w(TAG, "pickRandomPlace: size in KB: " + intentSizeInKB);
                Log.w(TAG, "pickRandomPlace: PlaceModel put in Intent may contain too much data.");
            }

            startActivityForResult(intent, DETAIL_ACTIVITY_REQUEST_CODE);
        } else if (places != null && places.isEmpty()) {
            Toast.makeText(this, R.string.random_pick_empty_error, Toast.LENGTH_LONG).show();
        }
    }

    /** Creates a notification channel.
     *
     */
    private void createNotificationChannel() {
        // API 26 and higher require notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.refresh_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            } else {
                Log.e(TAG, "notification manager is null");
            }
        }
    }

    // Methods to check size of objects in bytes. Too many bytes in Intent putExtras() will cause
    // a TransactionTooLargeException and crash the app.

    /** Get approximate size of Base64 String in bytes.
     *
     * Source: Maarten Bodewes
     * https://stackoverflow.com/questions/13378815/base64-length-calculation
     *
     * @param base64String The Base64String
     * @return The size in bytes
     *
     */
    private static int calculateBase64StringSizeInBytes(String base64String) {
        int result = -1;
        if(!TextUtils.isEmpty(base64String)) {
            result = paddedBase64(base64String.length());
        }
        return result;
    }

    private static int ceilDiv(int x, int y) {
        return (x + y - 1) / y;
    }

    private static int paddedBase64(int stringLen) {
        int blocks = ceilDiv(stringLen, 3);
        return blocks * 4;
    }

    /* Get size of a Bundle in bytes.

    Can be used with Intent's getExtras() to check if data sent to an Activity,
    ex. the PlaceModel sent to DetailActivity, is
    using too many bytes, causing a TransactionTooLargeException and crashing the app.

    The maximum amount of bytes the entire Intent's getExtras() Bundle can hold
    seems to be around 500 KB. One PlaceModel Visit is 3 KB.

    Source: ChandraShekhar Kaushik
    https://stackoverflow.com/questions/47633002/how-to-examine-the-size-of-the-bundle-object-in-onsaveinstancestate
     */
    public static int getBundleSizeInBytes(Bundle bundle) {
        Parcel parcel = Parcel.obtain();
        parcel.writeValue(bundle);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes.length;
    }

    // Uncomment to check about how many bytes a String List contains.
    // Can be used to check whether the Place ID List sent to RefreshPlaceListService or
    // the Place ID, name, and address Lists sent back from RefreshPlaceListService
    // will cause a android.os.TransactionTooLargeException and crash the app.
    /*
    // check how many bytes a String List contains
    public static void testObjects(List<String> list) {
        Log.d(TAG, "Objects: " + list.getClass().getSimpleName());
        long size = getBytesFromList(list);
        printInUnits(size);
    }

    public static long getBytesFromList(List list) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(list);
            out.close();
            return baos.toByteArray().length;
        } catch (IOException exception) {
            Log.e(TAG, "getBytesFromList: ", exception);
            return 0;
        }
    }

    public static void printInUnits(long length) {
        Log.d(TAG, "list size is: " + length / 1000000000 + " GB");
        Log.d(TAG, "list size is: " + length / 1000000 + " MB");
        Log.d(TAG, "list size is: " + length / 1000 + " KB");
        Log.d(TAG, "list size is: " + length + " bytes");
    }
    */

}
