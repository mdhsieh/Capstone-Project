package com.michaelhsieh.placetracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.michaelhsieh.placetracker.model.PlaceModel;

import java.util.Arrays;
import java.util.List;

import static com.michaelhsieh.placetracker.MainActivity.EXTRA_PLACE;

public class DetailActivity extends AppCompatActivity {

    private static final String TAG = DetailActivity.class.getSimpleName();

    private PlaceModel place;

    // custom adapter to display a group of visits using ExpandingRecyclerView
    private VisitGroupAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        EditText nameDisplay = findViewById(R.id.et_name);
        EditText addressDisplay = findViewById(R.id.et_address);
        TextView numVisitsDisplay = findViewById(R.id.tv_num_visits);
        EditText notesDisplay = findViewById(R.id.et_notes);

        List<VisitGroup> visitGroupList = makeVisitGroupList(this);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.expanding_rv_visits);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //instantiate your adapter with the list of genres
        VisitGroupAdapter adapter = new VisitGroupAdapter(visitGroupList);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // get the Movie from the Intent that started this Activity
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_PLACE)) {
            place = intent.getParcelableExtra(EXTRA_PLACE);
            if (place != null) {
                Log.d(TAG, "Place ID: " + place.getId());
                Log.d(TAG, "name: " + place.getName());
                Log.d(TAG, "address: " + place.getAddress());
                Log.d(TAG, "number of visits: " + place.getNumVisits());

                nameDisplay.setText(place.getName());
                addressDisplay.setText(place.getAddress());
                numVisitsDisplay.setText(String.valueOf(place.getNumVisits()));
                notesDisplay.setText(place.getNotes());
            } else {
                Log.e(TAG, "place is null");
            }
        }
    }

    public static List<VisitGroup> makeVisitGroupList(Context context) {
        return Arrays.asList(makeVisitGroup(context));
    }

    public static VisitGroup makeVisitGroup(Context context) {
        return new VisitGroup(context.getResources().getString(R.string.last_visit), makeVisits());
    }

    // create test date and times
    public static List<Visit> makeVisits() {
        Visit day1 = new Visit("Wednesday, January 13, 2020", "3:00 pm");
        Visit day2 = new Visit("Friday, June 26, 2020", "12:00 pm");

        return Arrays.asList(day1, day2);
    }
}
