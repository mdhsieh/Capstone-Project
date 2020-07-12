package com.michaelhsieh.placetracker;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
//import android.os.Build;
//import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.michaelhsieh.placetracker.model.PlaceModel;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.michaelhsieh.placetracker.MainActivity.CHANNEL_ID;
import static com.michaelhsieh.placetracker.MainActivity.EXTRA_SERVICE_PLACES;

/** An IntentService that refreshes the user's places list with new place info in the background.
 *
 */
public class RefreshPlacesListService extends IntentService {

    private static final String TAG = RefreshPlacesListService.class.getSimpleName();

    // notification ID must not be 0 or ANR will occur with log like
    // Reason: Context.startForegroundService() did not then call Service.startForeground()
    private static final int NOTIFICATION_ID = 2;

    /*private PowerManager.WakeLock wakeLock;*/

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

        /*// wakelock to keep phone CPU awake
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        // partial to allow screen to go dark
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "PlaceTracker:WakeLock");
        wakeLock.acquire(10*60*1000L *//*10 minutes*//*);
        Log.d(TAG, "Wakelock acquired");*/

        // open MainActivity when service notification clicked
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        // display notification on API 26 or higher devices
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // display notification
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.refresh_notification_title))
                    .setContentText(getString(R.string.refresh_notification_text))
                    .setSmallIcon(R.drawable.ic_notification_list_orange_24dp)
                    .setContentIntent(pendingIntent)
                    .build();



            startForeground(NOTIFICATION_ID, notification);
//        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent");

        if (intent != null) {
            String input = intent.getStringExtra(EXTRA_SERVICE_PLACES);
            // test, freeze thread to simulate work for 10 seconds
            for (int i = 0; i < 10; i++) {
                Log.d(TAG, input + " - " + i);
                SystemClock.sleep(1000); // 1 second
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        /*wakeLock.release();
        Log.d(TAG, "Wakelock released");*/
    }
}
