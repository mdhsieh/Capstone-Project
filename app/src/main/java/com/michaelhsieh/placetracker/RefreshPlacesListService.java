package com.michaelhsieh.placetracker;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.SystemClock;
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

/** Gets refreshed info on the user's places in the background.
 *
 * Creates a new Places Client.
 *
 */
public class RefreshPlacesListService extends IntentService {

    private static final String TAG = RefreshPlacesListService.class.getSimpleName();

    // key of all fetched place IDs to send to MainActivity
    public static final String EXTRA_REFRESHED_PLACE_IDS = "refreshed_place_ids";
    // key of all fetched place names to send to MainActivity
    public static final String EXTRA_REFRESHED_PLACE_NAMES = "refreshed_place_names";
    // key of all fetched place addresses to send to MainActivity
    public static final String EXTRA_REFRESHED_PLACE_ADDRESSES = "refreshed_place_addresses";
    // key of all fetched places' first photo metadata to send to MainActivity
    public static final String EXTRA_REFRESHED_PHOTO_METADATA = "refreshed_place_photometadata";

    // notification ID must not be 0 or ANR will occur with log like
    // Reason: Context.startForegroundService() did not then call Service.startForeground()
    private static final int NOTIFICATION_ID = 2;

    // ArrayLists that will contain refreshed place info to be
    // sent to MainActivity through Intent Extras
    private ArrayList<String> refreshedPlaceIds;
    private ArrayList<String> refreshedPlaceNames;
    private ArrayList<String> refreshedPlaceAddresses;
    // This ArrayList will have a null element if a place's first photo metadata can't be found,
    // ex. place added manually
    private ArrayList<PhotoMetadata> refreshedPhotoMetadata;

    // place counter to send refreshed place info to MainActivity once all places fetched
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
        // currently the service finishes too quickly for the notification to be visible
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.refresh_notification_title))
                .setContentText(getString(R.string.refresh_notification_text))
                .setSmallIcon(R.drawable.ic_notification_list_white_24dp)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent");

        // create new ArrayLists to hold refreshed place info
        refreshedPlaceIds = new ArrayList<>();
        refreshedPlaceNames = new ArrayList<>();
        refreshedPlaceAddresses = new ArrayList<>();
        refreshedPhotoMetadata = new ArrayList<>();

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
    private void fetchAllPlacesById( PlacesClient placesClient, String placeId, int listSize) {
        // Specify the fields to return.
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.PHOTO_METADATAS);

        // Construct a request object, passing the place ID and fields array.
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields)
                .build();

        // Add a listener to handle the response.
        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();

            refreshedPlaceIds.add(place.getId());
            refreshedPlaceNames.add(place.getName());
            refreshedPlaceAddresses.add(place.getAddress());
            // add first photo's metadata if available
            if (place.getPhotoMetadatas() != null && !place.getPhotoMetadatas().isEmpty()) {
                refreshedPhotoMetadata.add(place.getPhotoMetadatas().get(0));
            } else {
                // add null element to keep index the same as
                // other refreshed info ArrayList indexes
                refreshedPhotoMetadata.add(null);
            }
            // when last place has been fetched, send the info to MainActivity
            if (placeCounter == listSize - 1) {
                sendInfo();
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
                sendInfo();
            }
            // still increase place counter by 1 because
            // a fetch may fail if ex. place added manually
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

    /** Send all places' refreshed info to MainActivity.
     *
     */
    private void sendInfo() {
        Intent returnIntent = new Intent(MainActivity.RECEIVE_REFRESHED_PLACES_INFO);
        returnIntent.putExtra(EXTRA_REFRESHED_PLACE_IDS, refreshedPlaceIds);
        returnIntent.putExtra(EXTRA_REFRESHED_PLACE_NAMES, refreshedPlaceNames);
        returnIntent.putExtra(EXTRA_REFRESHED_PLACE_ADDRESSES, refreshedPlaceAddresses);
        returnIntent.putExtra(EXTRA_REFRESHED_PHOTO_METADATA, refreshedPhotoMetadata);
        LocalBroadcastManager.getInstance(this).sendBroadcast(returnIntent);
    }
}
