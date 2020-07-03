package com.michaelhsieh.placetracker.database;

import android.content.Context;

import com.michaelhsieh.placetracker.VisitTypeConverter;
import com.michaelhsieh.placetracker.model.PlaceModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {PlaceModel.class}, version = 1, exportSchema = false)
@TypeConverters({VisitTypeConverter.class})
public abstract class PlaceRoomDatabase extends RoomDatabase {
    public abstract PlaceDao placeDao();

    private static final String DATABASE_NAME = "place_database";

    // an instance of the PlaceRoomDatabase
    private static volatile PlaceRoomDatabase placeRoomInstance;

    private static final int NUMBER_OF_THREADS = 1;

    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static PlaceRoomDatabase getDatabase(final Context context) {
        if (placeRoomInstance == null) {
            synchronized (PlaceRoomDatabase.class) {
                if (placeRoomInstance == null) {
                    // Creating new database instance
                    placeRoomInstance = Room.databaseBuilder(context.getApplicationContext(),
                            PlaceRoomDatabase.class, DATABASE_NAME)
                            .build();
                }
            }
        }
        return placeRoomInstance;
    }
}
