package com.michaelhsieh.placetracker.database;

import com.michaelhsieh.placetracker.models.PlaceModel;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface PlaceDao {
    // allowing the insert of the same place multiple times by passing a
    // conflict resolution strategy
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PlaceModel place);

    @Delete
    void delete(PlaceModel place);

    @Update
    void update(PlaceModel place);

    // get places sorted by position
    @Query("SELECT * from place_table ORDER BY position ASC")
    LiveData<List<PlaceModel>> getSortedPlaces();

    @Query("SELECT * FROM place_table WHERE place_id =:id")
    LiveData<PlaceModel> getPlaceById(String id);
}
