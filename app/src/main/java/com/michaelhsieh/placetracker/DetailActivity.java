package com.michaelhsieh.placetracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.michaelhsieh.placetracker.model.PlaceModel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.michaelhsieh.placetracker.MainActivity.EXTRA_PLACE;

public class DetailActivity extends AppCompatActivity implements VisitGroupAdapter.VisitItemClickListener {

    private static final String TAG = DetailActivity.class.getSimpleName();

    private PlaceModel place;

    // custom adapter to display a group of visits using ExpandingRecyclerView
    private VisitGroupAdapter adapter;

    // list of visit groups which will only contain one group at position 0
    List<VisitGroup> visitGroupList;

    TextView numVisitsDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        EditText nameDisplay = findViewById(R.id.et_name);
        EditText addressDisplay = findViewById(R.id.et_address);
        numVisitsDisplay = findViewById(R.id.tv_num_visits);
        EditText notesDisplay = findViewById(R.id.et_notes);

        // get the Place from the Intent that started this Activity
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_PLACE)) {
            place = intent.getParcelableExtra(EXTRA_PLACE);
            if (place != null) {
                Log.d(TAG, "Place ID: " + place.getId());
                Log.d(TAG, "name: " + place.getName());
                Log.d(TAG, "address: " + place.getAddress());
                Log.d(TAG, "number of visits: " + place.getNumVisits());
                // visits and notes should be empty on first startup
                Log.d(TAG, "visits: " + place.getVisits());
                Log.d(TAG, "notes: " + place.getNotes());

                nameDisplay.setText(place.getName());
                addressDisplay.setText(place.getAddress());
                numVisitsDisplay.setText(String.valueOf(place.getNumVisits()));
                notesDisplay.setText(place.getNotes());


                // initialize the visit group and visits
                visitGroupList =
                        Arrays.asList
                                (new VisitGroup(getResources().getString(R.string.dates_visited),
                                        place.getVisits())
                                );

                // initialize expanding RecyclerView
                RecyclerView recyclerView = findViewById(R.id.expanding_rv_visits);
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
                        insertSingleItem(new Visit(getCurrentDate(), getCurrentTime()));
                    }
                });


            } else {
                Log.e(TAG, "place is null");
            }
        }
    }

    /* to save the expand and collapse state of the adapter,
    you have to explicitly call through to the adapter's
    onSaveInstanceState() and onRestoreInstanceState()in the calling Activity */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null) {
            adapter.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (adapter != null) {
            adapter.onRestoreInstanceState(savedInstanceState);
        }
    }

    // method to get the one visit group
    private VisitGroup getVisitGroup() {
        return visitGroupList.get(0);
    }

    // called whenever a visit in the list is clicked
    // position param is the position of the Visit clicked, calculated by getAdapterPosition() - 1
    @Override
    public void onItemClick(View view, int position) {
        Visit visit = getVisitGroup().getItems().get(position);
        Toast.makeText(this, "You clicked " + visit.getDate() + ", " + visit.getTime() + " on row number " + position, Toast.LENGTH_SHORT).show();
    }

    /* Insert an item into the RecyclerView
     */
    private void insertSingleItem(Visit visit) {
        // insert at the very end of the list
        int insertIndex = getVisitGroup().getItems().size();
        // add visit
        getVisitGroup().getItems().add(insertIndex, visit);
        adapter.notifyItemChanged(0);
        // increase number of visits by 1 and display updated text
        place.increaseNumVisits();
        numVisitsDisplay.setText(String.valueOf(place.getNumVisits()));
    }

    // method to get the current week, month, and day as a single String
    private String getCurrentDate() {
        String date = "";
        // object whose calendar fields have been initialized with the current date and time
        Calendar rightNow = Calendar.getInstance();
        // get calendar fields
        int weekField = rightNow.get(Calendar.DAY_OF_WEEK);
        int monthField = rightNow.get(Calendar.MONTH);
        int dayField = rightNow.get(Calendar.DAY_OF_MONTH);
        int yearField = rightNow.get(Calendar.YEAR);

        // get names of week and month fields to show to user
        String week;
        switch (weekField) {
            case Calendar.MONDAY:
                week = "Monday";
                break;
            case Calendar.TUESDAY:
                week = "Tuesday";
                break;
            case Calendar.WEDNESDAY:
                week = "Wednesday";
                break;
            case Calendar.THURSDAY:
                week = "Thursday";
                break;
            case Calendar.FRIDAY:
                week = "Friday";
                break;
            case Calendar.SATURDAY:
                week = "Saturday";
                break;
            case Calendar.SUNDAY:
                week = "Sunday";
                break;
            default:
                week = "Invalid week";
                Log.e(TAG, "Unexpected value for week: " + weekField);
        }
        
        String month;
        switch (monthField) {
            case Calendar.JANUARY:
                month = "January";
                break;
            case Calendar.FEBRUARY:
                month = "February";
                break;
            case Calendar.MARCH:
                month = "March";
                break;
            case Calendar.APRIL:
                month = "April";
                break;
            case Calendar.MAY:
                month = "May";
                break;
            case Calendar.JUNE:
                month = "June";
                break;
            case Calendar.JULY:
                month = "July";
                break;
            case Calendar.AUGUST:
                month = "August";
                break;
            case Calendar.SEPTEMBER:
                month = "September";
                break;
            case Calendar.OCTOBER:
                month = "October";
                break;
            case Calendar.NOVEMBER:
                month = "November";
                break;
            case Calendar.DECEMBER:
                month = "December";
                break;
            default: month = "Invalid month";
                Log.e(TAG, "Unexpected value for month: " + monthField);
        }

        // combine into a single String
        date = week + ", " + month + " " + dayField + ", " + yearField;

        return date;
    }

    // method to get the current hours and minutes as a single String
    private String getCurrentTime() {
        String time = "";
        Calendar rightNow = Calendar.getInstance();
        Date date = rightNow.getTime();
//        Log.d(TAG, "calendar getTime returned Date: " + date);
        time = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
//        Log.d(TAG, "time is: " + time);

        return time;
    }
}
