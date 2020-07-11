package com.michaelhsieh.placetracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.michaelhsieh.placetracker.model.PlaceModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

// Activity where user can enter info to add a place manually. Very similar to DetailActivity.
public class ManualPlaceDetailActivity extends AppCompatActivity {

    private static final String TAG = ManualPlaceDetailActivity.class.getSimpleName();

    // key of place with updated info when add button clicked
    public static final String EXTRA_MANUAL_ADDED_PLACE = "place";

    /* There's one VisitGroup, which is at position 0 of the VisitGroupAdapter.
    The visits list starts at position 1. */
    private static final int NUM_VISIT_GROUPS = 1;

    // the place which the user is adding manually
    private PlaceModel place;

    // custom adapter to display a group of visits using ExpandingRecyclerView
    private VisitGroupAdapter adapter;

    // list of visits from place
    List<Visit> visits;

    TextView numVisitsDisplay;

    // label TextView and TextView of last date visited
    TextView lastVisitLabel;
    TextView lastVisitDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_place_detail);

        // display up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        final EditText nameDisplay = findViewById(R.id.et_manual_name);
        final EditText addressDisplay = findViewById(R.id.et_manual_address);
        numVisitsDisplay = findViewById(R.id.tv_manual_num_visits);
        lastVisitLabel = findViewById(R.id.tv_label_manual_last_visit);
        lastVisitDisplay = findViewById(R.id.tv_manual_last_visit);
        final EditText notesDisplay = findViewById(R.id.et_manual_notes);

        // initialize manual place with empty values and 0 visits
//        String name = place.getName();
//        String address = place.getAddress();
        int numVisits = 0;
//        String notes = place.getNotes();

//        nameDisplay.setText(name);
//        addressDisplay.setText(address);
//        notesDisplay.setText(notes);

        // initialize the visit group and visits
        /*visits = new ArrayList<>();

        // list of visit groups which will only contain one group at position 0
        List<VisitGroup> visitGroupList =
                Arrays.asList
                        (new VisitGroup(getResources().getString(R.string.dates_visited),
                                visits)
                        );*/

        numVisitsDisplay.setText(String.valueOf(numVisits));

        /*// show last visit if PlaceModel already has visits,
        // otherwise hide last visit text and label
        showOrHideLastVisit();

        // initialize expanding RecyclerView
        RecyclerView recyclerView = findViewById(R.id.expanding_rv_manual_visits);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        // add a divider
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        // use custom white divider
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.place_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);

        // instantiate the adapter with the list of visit groups.
        // there's only one visit group
        adapter = new VisitGroupAdapter(visitGroupList);
        // set the click listener for clicks on individual visits
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        // add visit when the add visit button is clicked
        Button addVisitButton = findViewById(R.id.btn_add_visit);
        addVisitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // object whose calendar fields have been initialized with the current date and time
                Calendar rightNow = Calendar.getInstance();
                insertSingleItem(new Visit(rightNow));
            }
        });*/

        Button addManualPlaceButton = findViewById(R.id.btn_manual_add_place);
        addManualPlaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addManualPlaceIntent = new Intent();

                // generate a unique String as the place's Place ID
                String uniqueString = UUID.randomUUID().toString();

                // save the user's current EditText data for name and address
                // visits should already be added
                String name = nameDisplay.getText().toString();
                String address = addressDisplay.getText().toString();

                // create the manual place
                place = new PlaceModel(uniqueString, name, address);
                Log.d(TAG, "Place ID: " + place.getPlaceId());
                Log.d(TAG, "name: " + place.getName());
                Log.d(TAG, "address: " + place.getAddress());
                // save the user's current EditText data for notes
                // visits should already be added
//                place.setName(nameDisplay.getText().toString());
//                place.setAddress(addressDisplay.getText().toString());
                place.setNotes(notesDisplay.getText().toString());
                Log.d(TAG, "notes: " + place.getNotes());
                addManualPlaceIntent.putExtra(EXTRA_MANUAL_ADDED_PLACE, place);
                setResult(RESULT_OK, addManualPlaceIntent);
                finish();
            }
        });
    }
}
