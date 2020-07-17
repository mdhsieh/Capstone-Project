package com.michaelhsieh.placetracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class PlaceTrackerAppWidget extends AppWidgetProvider {

    private static final String TAG = PlaceTrackerAppWidget.class.getSimpleName();

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                String name, String address, int appWidgetId) {

        Log.d(TAG, "updateAppWidget: " + name + ", " + address);

        // Create an Intent to launch MainActivity when the widget is clicked
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

//        String widgetPlaceNameText = context.getString(R.string.default_widget_name_text);
//        String widgetPlaceAddressText = context.getString(R.string.default_widget_address_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.place_tracker_app_widget);
        views.setTextViewText(R.id.widget_place_name_text, name);
        views.setTextViewText(R.id.widget_place_address_text, address);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        // There may be multiple widgets active, so update all of them
        /*for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }*/

        /* When a widget is first created, updateAppWidget() hasn't been called yet, so the
        widget will not do anything if clicked.
        The code below enables the user to click the widget.
        It's the same as the code in updateAppWidget() but only opens the Activity on click. */
        // Create an Intent to launch MainActivity when the widget is clicked
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.place_tracker_app_widget);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
        for (int appWidgetId : appWidgetIds) {
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

    }

    /**
     * Updates all widget instances given the widget Ids and display information
     *
     * @param context          The calling context
     * @param appWidgetManager The widget manager
     * @param name       The name of the selected place
     * @param address      The address of that place
     * @param appWidgetIds     Array of widget Ids to be updated
     */
    public static void updatePlaceTrackerWidgets(Context context, AppWidgetManager appWidgetManager,
                                           String name, String address, int[] appWidgetIds)
    {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, name, address, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

