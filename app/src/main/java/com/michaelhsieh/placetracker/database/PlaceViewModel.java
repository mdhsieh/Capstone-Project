package com.michaelhsieh.placetracker.database;

import android.app.Application;

import com.michaelhsieh.placetracker.model.PlaceModel;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class PlaceViewModel extends AndroidViewModel {

    private PlaceRepository repository;

    private LiveData<List<PlaceModel>> allPlaces;


    public PlaceViewModel(@NonNull Application application) {
        super(application);
        repository = new PlaceRepository(application);
        allPlaces = repository.getAllPlaces();
    }

    public LiveData<List<PlaceModel>> getAllPlaces() {
        return allPlaces;
    }

    public void insert(PlaceModel place) {
        repository.insert(place);
    }

    public void delete(PlaceModel place) {
        repository.delete(place);
    }

    public void update(PlaceModel place) {
        repository.update(place);
    }
}
