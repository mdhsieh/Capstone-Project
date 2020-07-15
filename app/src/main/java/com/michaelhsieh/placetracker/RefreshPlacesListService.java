package com.michaelhsieh.placetracker;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
//import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.michaelhsieh.placetracker.MainActivity.CHANNEL_ID;
import static com.michaelhsieh.placetracker.MainActivity.EXTRA_SERVICE_PLACE_IDS;

/** An IntentService that refreshes the user's places list with new place info in the background.
 * A place is only refreshed if its Place ID can be found.
 *
 */
public class RefreshPlacesListService extends IntentService {

    private static final String TAG = RefreshPlacesListService.class.getSimpleName();

    // key of all fetched place IDs to send to MainActivity
    public static final String EXTRA_UPDATED_PLACE_IDS = "updated_place_ids";
    // key of all fetched place names to send to MainActivity
    public static final String EXTRA_UPDATED_PLACE_NAMES = "updated_place_names";
    // key of all fetched place addresses to send to MainActivity
    public static final String EXTRA_UPDATED_PLACE_ADDRESSES = "updated_place_addresses";
    // key of all fetched places' first photo metadata to send to MainActivity
    public static final String EXTRA_UPDATED_PHOTO_METADATA = "updated_place_photometadata";

    // notification ID must not be 0 or ANR will occur with log like
    // Reason: Context.startForegroundService() did not then call Service.startForeground()
    private static final int NOTIFICATION_ID = 2;

    // ArrayLists that will contain updated place info to be sent to MainActivity through Intent Extras
    private ArrayList<String> updatedPlaceIds;
    private ArrayList<String> updatedPlaceNames;
    private ArrayList<String> updatedPlaceAddresses;
    private ArrayList<PhotoMetadata> updatedPhotoMetadata;

    // place counter to send updated place info to MainActivity once all places fetched
    private int placeCounter = 0;

    public RefreshPlacesListService() {
        super("RefreshPlacesListService");
        // false means when system kills service, it won't be created again.
        // true means the last service will be started again and the last Intent will be
        // delivered to onHandleIntent
        setIntentRedelivery(false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // open MainActivity when service notification clicked
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        // display notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.refresh_notification_title))
                .setContentText(getString(R.string.refresh_notification_text))
                .setSmallIcon(R.drawable.ic_notification_list_orange_24dp)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent");

        // create new ArrayLists to hold updated place info
        updatedPlaceIds = new ArrayList<>();
        updatedPlaceNames = new ArrayList<>();
        updatedPlaceAddresses = new ArrayList<>();
        updatedPhotoMetadata = new ArrayList<>();

        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);

        if (intent != null) {

            ArrayList<String> allPlaceIds = intent.getStringArrayListExtra(EXTRA_SERVICE_PLACE_IDS);
            if (allPlaceIds != null && !allPlaceIds.isEmpty()) {
                int allPlaceIdsSize = allPlaceIds.size();
                for (int i = 0; i < allPlaceIdsSize; i++) {
                    fetchAllPlacesById(placesClient, allPlaceIds.get(i), allPlaceIdsSize);
                }
            }


            // test, freeze thread to simulate work for 10 seconds
//            for (int i = 0; i < 10; i++) {
//                Log.d(TAG, " - " + i);
//                SystemClock.sleep(1000); // 1 second
//            }
        }

    }

    /** Fetch up-to-date place info using the Place ID,
     * or just log message if the Place ID can't be found.
     *
     * @param placesClient The places client, initialized by the Service
     * @param placeId The Place ID of a place in user's places list
     * @param listSize The size of the List of Place IDs
     */
    private void fetchAllPlacesById( PlacesClient placesClient, String placeId, int listSize) {
        // Specify the fields to return.
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.PHOTO_METADATAS);

        // Construct a request object, passing the place ID and fields array.
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields)
                .build();

        // Add a listener to handle the response.
        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            Log.i(TAG, "Place found: " + place.getName() + ", " + place.getId());
            updatedPlaceIds.add(place.getId());
            updatedPlaceNames.add(place.getName());
            updatedPlaceAddresses.add(place.getAddress());
            // add first photo metadata if available
            if (place.getPhotoMetadatas() != null && !place.getPhotoMetadatas().isEmpty()) {
                updatedPhotoMetadata.add(place.getPhotoMetadatas().get(0));
                Log.d(TAG, "added metadata");
            } else {
                // add null data to keep index the same as other updated place ArrayList indexes
                updatedPhotoMetadata.add(null);
                Log.d(TAG, "added null");
            }
            if (placeCounter == listSize - 1) {
                sendInfo();
            }
            incrementPlaceCounter();
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                int statusCode = apiException.getStatusCode();
                // Handle error with given status code.
                Log.e(TAG, "Place not found: " + exception.getMessage());
                Log.e(TAG, "status code: " +  statusCode);
            }
            if (placeCounter == listSize - 1) {
                sendInfo();
            }
            incrementPlaceCounter();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private void incrementPlaceCounter() {
        placeCounter += 1;
    }

    private void sendInfo() {
        Log.d(TAG, "finished getting updated place info");

        Intent returnIntent = new Intent(MainActivity.RECEIVE_REFRESHED_PLACES_INFO);
        returnIntent.putExtra(EXTRA_UPDATED_PLACE_IDS, updatedPlaceIds);
        returnIntent.putExtra(EXTRA_UPDATED_PLACE_NAMES, updatedPlaceNames);
        returnIntent.putExtra(EXTRA_UPDATED_PLACE_ADDRESSES, updatedPlaceAddresses);
        returnIntent.putExtra(EXTRA_UPDATED_PHOTO_METADATA, updatedPhotoMetadata);
        LocalBroadcastManager.getInstance(this).sendBroadcast(returnIntent);
    }
}
