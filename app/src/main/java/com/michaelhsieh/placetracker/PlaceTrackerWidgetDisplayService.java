package com.michaelhsieh.placetracker;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

/** Updates the name and address of place tracker widgets
 * to the user's clicked place.
 *
 * An {@link IntentService} subclass so actions are done in the background on a separate thread.
 *
 */
public class PlaceTrackerWidgetDisplayService extends IntentService {

    public static final String ACTION_UPDATE_PLACE_TRACKER_WIDGETS =
            "update_place_tracker_widgets";

    public static final String EXTRA_WIDGET_PLACE_NAME = "widget_place_name";
    public static final String EXTRA_WIDGET_PLACE_ADDRESS = "widget_place_address";

    public PlaceTrackerWidgetDisplayService() {
        super("PlaceTrackerWidgetDisplayService");
    }

    /**
     * Starts this service to perform UpdatePlaceTrackerWidgets action with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdatePlaceTrackerWidgets(Context context, String name, String address) {
        Intent intent = new Intent(context, PlaceTrackerWidgetDisplayService.class);
        intent.setAction(ACTION_UPDATE_PLACE_TRACKER_WIDGETS);
        intent.putExtra(EXTRA_WIDGET_PLACE_NAME, name);
        intent.putExtra(EXTRA_WIDGET_PLACE_ADDRESS, address);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_UPDATE_PLACE_TRACKER_WIDGETS.equals(action)) {
                final String placeName = intent.getStringExtra(EXTRA_WIDGET_PLACE_NAME);
                final String placeAddress = intent.getStringExtra(EXTRA_WIDGET_PLACE_ADDRESS);
                // Context is this IntentService
                handleActionUpdatePlaceTrackerWidgets(this, placeName, placeAddress);
            }
        }
    }

    /**
     * Update widget name and address to match the selected place
     */
    private static void handleActionUpdatePlaceTrackerWidgets(Context context, String name, String address)
    {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, PlaceTrackerAppWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        // Trigger the data update to handle the widget ingredient text and force a data refresh
        //appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.appwidget_text);

        // Update all widgets
        PlaceTrackerAppWidget.updatePlaceTrackerWidgets(context, appWidgetManager,
                name, address, appWidgetIds);
    }
}
