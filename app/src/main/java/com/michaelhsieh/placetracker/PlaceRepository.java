package com.michaelhsieh.placetracker;

import android.app.Application;

import com.michaelhsieh.placetracker.model.PlaceModel;

import java.util.List;

import androidx.lifecycle.LiveData;

public class PlaceRepository {

    private PlaceDao placeDao;
    private LiveData<List<PlaceModel>> allPlaces;

    PlaceRepository(Application application) {
        PlaceRoomDatabase database = PlaceRoomDatabase.getDatabase(application);
        placeDao = database.placeDao();
        allPlaces = placeDao.getPlaces();
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    LiveData<List<PlaceModel>> getAllPlaces() {
        return allPlaces;
    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    void insert(PlaceModel place) {
        PlaceRoomDatabase.databaseWriteExecutor.execute(() -> {
            placeDao.insert(place);
        });
    }
}
