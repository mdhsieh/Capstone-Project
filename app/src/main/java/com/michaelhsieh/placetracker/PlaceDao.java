package com.michaelhsieh.placetracker;

import com.michaelhsieh.placetracker.model.PlaceModel;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface PlaceDao {
    // allowing the insert of the same word multiple times by passing a
    // conflict resolution strategy
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PlaceModel place);

    @Query("DELETE FROM place_table")
    void deleteAll();

    @Query("SELECT * from place_table")
    List<PlaceModel> getPlaces();
}
