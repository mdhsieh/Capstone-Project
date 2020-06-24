package com.michaelhsieh.placetracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.michaelhsieh.placetracker.model.PlaceModel;

import static com.michaelhsieh.placetracker.MainActivity.EXTRA_PLACE;

public class DetailActivity extends AppCompatActivity {

    private static final String TAG = DetailActivity.class.getSimpleName();

    private PlaceModel place;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // get the Movie from the Intent that started this Activity
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_PLACE)) {
            place = intent.getParcelableExtra(EXTRA_PLACE);
            if (place != null) {
                Log.d(TAG, "Place ID: " + place.getId());
                Log.d(TAG, "name: " + place.getName());
                Log.d(TAG, "address: " + place.getAddress());
                Log.d(TAG, "number of visits: " + place.getNumVisits());
            } else {
                Log.e(TAG, "place is null");
            }
        }
    }
}
