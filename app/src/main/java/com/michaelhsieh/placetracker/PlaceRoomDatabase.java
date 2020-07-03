package com.michaelhsieh.placetracker;

import android.content.Context;

import com.michaelhsieh.placetracker.model.PlaceModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PlaceModel.class}, version = 1, exportSchema = false)
public abstract class PlaceRoomDatabase extends RoomDatabase {
    public abstract PlaceDao placeDao();

    private static final String DATABASE_NAME = "place_database";

    private static volatile PlaceRoomDatabase INSTANCE;

    private static final int NUMBER_OF_THREADS = 1;

    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static PlaceRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PlaceRoomDatabase.class) {
                if (INSTANCE == null) {
                    // Creating new database instance
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            PlaceRoomDatabase.class, DATABASE_NAME)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
