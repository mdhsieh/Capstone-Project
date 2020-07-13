package com.michaelhsieh.placetracker;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.michaelhsieh.placetracker.MainActivity.CHANNEL_ID;
import static com.michaelhsieh.placetracker.MainActivity.EXTRA_SERVICE_PLACE_IDS;
//import static com.michaelhsieh.placetracker.MainActivity.EXTRA_SERVICE_PLACE_NAMES;

/** An IntentService that refreshes the user's places list with new place info in the background.
 * A place is only refreshed if its Place ID can be found.
 *
 */
public class RefreshPlacesListService extends IntentService {

    private static final String TAG = RefreshPlacesListService.class.getSimpleName();

    // notification ID must not be 0 or ANR will occur with log like
    // Reason: Context.startForegroundService() did not then call Service.startForeground()
    private static final int NOTIFICATION_ID = 2;

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
        /*Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.refresh_notification_title))
                .setContentText(getString(R.string.refresh_notification_text))
                .setSmallIcon(R.drawable.ic_notification_list_orange_24dp)
                .setContentIntent(pendingIntent)
                .build();*/

        /*startForeground(NOTIFICATION_ID, notification);*/

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.refresh_notification_title))
                .setContentText(getString(R.string.refresh_notification_text))
                .setSmallIcon(R.drawable.ic_notification_list_orange_24dp)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setContentIntent(pendingIntent);

        // make sure notification is shown and sound plays on older devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
//            builder.setDefaults(Notification.DEFAULT_SOUND);
        }


        startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent");

        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);

        if (intent != null) {

            ArrayList<String> allPlaceIds = intent.getStringArrayListExtra(EXTRA_SERVICE_PLACE_IDS);
//            if (allPlaceIds != null) {
//                for (int i = 0; i < allPlaceIds.size(); i++) {
//                    Log.d(TAG, "id: " + allPlaceIds.get(i));
//                }
//            }

//            ArrayList<String> allPlaceNames = intent.getStringArrayListExtra(EXTRA_SERVICE_PLACE_NAMES);
//            if (allPlaceNames != null) {
//                for (int i = 0; i < allPlaceNames.size(); i++) {
//                    Log.d(TAG, "name: " + allPlaceNames.get(i));
//                }
//            }

            if (allPlaceIds != null && !allPlaceIds.isEmpty()) {

                for (int i = 0; i < allPlaceIds.size(); i++) {
                    fetchAllPlacesById(placesClient, allPlaceIds.get(i));
                }

                /*// Define a Place ID.
//            String placeId = "INSERT_PLACE_ID_HERE";
                String placeId = allPlaceIds.get(0);

                // Specify the fields to return.
                List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME);

                // Construct a request object, passing the place ID and fields array.
                FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields)
                        .build();

                // Add a listener to handle the response.
                placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
                    Place place = response.getPlace();
                    // Log.i(TAG, "Place found: " + place.getName());
                    Log.i(TAG, "Place found: " + place.getName() + ", " + place.getId());
                }).addOnFailureListener((exception) -> {
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        int statusCode = apiException.getStatusCode();
                        // Handle error with given status code.
                        Log.e(TAG, "Place not found: " + exception.getMessage());
                    }
                });*/
            }


            // test, freeze thread to simulate work for 10 seconds
            for (int i = 0; i < 10; i++) {
                Log.d(TAG, " - " + i);
                SystemClock.sleep(1000); // 1 second
            }
        }

    }

    private void fetchAllPlacesById( PlacesClient placesClient, String placeId) {
        // Specify the fields to return.
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME);

        // Construct a request object, passing the place ID and fields array.
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields)
                .build();

        // Add a listener to handle the response.
        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            // Log.i(TAG, "Place found: " + place.getName());
            Log.i(TAG, "Place found: " + place.getName() + ", " + place.getId());
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                int statusCode = apiException.getStatusCode();
                // Handle error with given status code.
                Log.e(TAG, "Place not found: " + exception.getMessage());
                Log.e(TAG, "status code: " +  statusCode);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
