package com.michaelhsieh.placetracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.michaelhsieh.placetracker.model.PlaceModel;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.michaelhsieh.placetracker.MainActivity.EXTRA_PLACE;

public class DetailActivity extends AppCompatActivity implements VisitGroupAdapter.VisitItemClickListener {

    private static final String TAG = DetailActivity.class.getSimpleName();

    // key of String used to determine which button was clicked
    public static final String EXTRA_BUTTON_TYPE = "button_type";
    // key of place with updated info when save button clicked
    public static final String EXTRA_SAVED_PLACE = "place";

    // Strings to either save or delete the place depending on what button is clicked
    public static final String DELETE = "delete";
    public static final String SAVE = "save";

    // type of button clicked, which is either the save or delete button
    private String buttonType;

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
        setContentView(R.layout.activity_detail);

        final EditText nameDisplay = findViewById(R.id.et_name);
        final EditText addressDisplay = findViewById(R.id.et_address);
        numVisitsDisplay = findViewById(R.id.tv_num_visits);
        lastVisitLabel = findViewById(R.id.tv_label_last_visit);
        lastVisitDisplay = findViewById(R.id.tv_last_visit);
        final EditText notesDisplay = findViewById(R.id.et_notes);

        // get the PlaceModel from the Intent that started this Activity
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_PLACE)) {
            place = intent.getParcelableExtra(EXTRA_PLACE);
            if (place != null) {
//                String placeId = place.getPlaceId();
                String name = place.getName();
                String address = place.getAddress();
                int numVisits = place.getNumVisits();
                String notes = place.getNotes();

//                Log.d(TAG, "Place ID: " + placeId);
//                Log.d(TAG, "name: " + name);
//                Log.d(TAG, "address: " + address);
//                Log.d(TAG, "number of visits: " + numVisits);
                // visits and notes should be empty on first startup
//                Log.d(TAG, "notes: " + notes);

                nameDisplay.setText(name);
                addressDisplay.setText(address);
                notesDisplay.setText(notes);

                // initialize the visit group and visits
                visits = place.getVisits();
//                Log.d(TAG, "visits: " + visits);

                // list of visit groups which will only contain one group at position 0
                List<VisitGroup> visitGroupList =
                        Arrays.asList
                                (new VisitGroup(getResources().getString(R.string.dates_visited),
                                        visits)
                                );

                numVisitsDisplay.setText(String.valueOf(numVisits));

                // show last visit if PlaceModel already has visits,
                // otherwise hide last visit text and label
                showOrHideLastVisit();

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

                Button deleteButton = findViewById(R.id.btn_delete);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        new AlertDialog.Builder(DetailActivity.this)
                                .setTitle(R.string.delete_place_title)
                                .setMessage(R.string.delete_place_message)

                                // Specifying a listener allows you to take an action before dismissing the dialog.
                                // The dialog is automatically dismissed when a dialog button is clicked.
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Continue with delete operation
                                        Intent deletePlaceIntent = new Intent();
                                        // button is used to delete this place from place list
                                        buttonType = DELETE;
                                        deletePlaceIntent.putExtra(EXTRA_BUTTON_TYPE, buttonType);
                                        setResult(RESULT_OK, deletePlaceIntent);
                                        finish();
                                    }
                                })

                                // A null listener allows the button to dismiss the dialog and take no further action.
                                .setNegativeButton(android.R.string.no, null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                });

                Button saveButton = findViewById(R.id.btn_save);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent savePlaceIntent = new Intent();
                        // button is used to save this place's info
                        buttonType = SAVE;
                        savePlaceIntent.putExtra(EXTRA_BUTTON_TYPE, buttonType);
                        // save the user's current EditText data for name, address, and notes
                        // visits should already be added and Place ID should stay the same
                        place.setName(nameDisplay.getText().toString());
                        place.setAddress(addressDisplay.getText().toString());
                        place.setNotes(notesDisplay.getText().toString());
                        savePlaceIntent.putExtra(EXTRA_SAVED_PLACE, place);
                        setResult(RESULT_OK, savePlaceIntent);
                        finish();
                    }
                });


            } else {
                Log.e(TAG, "place is null");
            }
        }
    }

    /* to save the expand and collapse state of the adapter,
    you have to explicitly call through to the adapter's
    onSaveInstanceState() and onRestoreInstanceState() in the calling Activity */
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

    // called whenever a visit in the list is clicked
    // position param is the position of the Visit clicked, calculated by getAdapterPosition() - 1
    @Override
    public void onItemClick(View view, int position) {
        Visit visit = visits.get(position);
        Toast.makeText(this, "You clicked " + visit.getDate() + ", " + visit.getTime() + " on row number " + position, Toast.LENGTH_SHORT).show();
        // show the date and time pickers
        // param is the clicked position so button click can update visit
        showDateTimePicker(position);
    }

    /** Insert an item into the RecyclerView
     * @param visit The visit being inserted
     */
    private void insertSingleItem(Visit visit) {
        // insert at the very end of the list
        int insertIndex = place.getNumVisits();
        // add visit
        visits.add(insertIndex, visit);
        adapter.notifyItemChanged(0);
        // increased number of visits by 1, so
        // display updated text
        numVisitsDisplay.setText(String.valueOf(place.getNumVisits()));

        // update last visit
        showOrHideLastVisit();
    }

    /** Update an item in the RecyclerView
     * @param visit The visit being updated
     */
    private void updateSingleItem(int updateIndex, Visit visit) {
        // add visit
        visits.set(updateIndex, visit);
        // notify adapter that VisitGroup has changed
        // adapter.notifyItemChanged(0);
        Log.d(TAG, "position notify changed: " + updateIndex + 1);
        adapter.notifyItemChanged(updateIndex + 1);
//        Log.d(TAG, "adapter size: " + adapter.getItemCount());

        // update last visit
        // need this method if the visit that was updated was the last visit
        showOrHideLastVisit();
    }

    // show or hide the most recent date visited label and text
    private void showOrHideLastVisit() {
        if (place.getNumVisits() == 0) {
            lastVisitLabel.setVisibility(View.GONE);
            lastVisitDisplay.setVisibility(View.GONE);
        } else {
            lastVisitLabel.setVisibility(View.VISIBLE);
            lastVisitDisplay.setVisibility(View.VISIBLE);
            int lastIndex = place.getNumVisits() - 1;
            Visit lastVisit = visits.get(lastIndex);
            String lastVisitString = lastVisit.getDate() + getString(R.string.at) + lastVisit.getTime();
            lastVisitDisplay.setText(lastVisitString);
        }
    }

    // get the current week, month, and day as a single String
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

    // get the current hours and minutes as a single String
    /* Source:

    Dany Pop
    https://stackoverflow.com/questions/454315/how-to-format-date-and-time-in-android */
    private String getCurrentTime() {
        String time = "";
        Calendar rightNow = Calendar.getInstance();
        // get current date
        Date date = rightNow.getTime();
        // format date to show only hours and minutes
        time = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);

        return time;
    }

    /** Display the date and time picker layout so user can edit a clicked Visit's
     * date and time. Updates the Visit if the user clicks the set button.
     *
     * @param pos The position of the clicked Visit.
     */
    private void showDateTimePicker(int pos) {
        final View dialogView = View.inflate(this, R.layout.date_time_picker, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        DatePicker datePicker = dialogView.findViewById(R.id.date_picker);
        TimePicker timePicker = dialogView.findViewById(R.id.time_picker);

        // Calendar whose Date object will be converted to a readable date String and time String
        // initially set to the current date and time
        Calendar calendar = Calendar.getInstance();

        dialogView.findViewById(R.id.date_time_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int month = datePicker.getMonth();
//                Log.d(TAG, "picked month: " + month);
                int day = datePicker.getDayOfMonth();
//                Log.d(TAG, "picked day of month: " + day);
                int year = datePicker.getYear();
//                Log.d(TAG, "picked year: " + year);
                int hour = timePicker.getCurrentHour();
//                Log.d(TAG, "picked hour: " + hour);
                int minute = timePicker.getCurrentMinute();
//                Log.d(TAG, "picked minute: " + minute);

                // don't retain previous calendar field values from when Calendar was
                // first initialized with getInstance()
                calendar.clear();
                // set Calendar to user's picked date and time
                calendar.set(year, month, day, hour, minute);
                // get picked calendar date
                Date date = calendar.getTime();
                // format date to show only hours and minutes
                String newTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
                // format date to show day of week, month, day, year
                String newDate = DateFormat.getDateInstance(DateFormat.FULL).format(date);
                Log.d(TAG, "time: " + newTime);
                Log.d(TAG, "date: " + newDate);

                // create a new Visit with the user's picked date and time
                Visit updatedVisit = new Visit(newDate, newTime);
                // update the original Visit by replacing it with the new one
                updateSingleItem(pos, updatedVisit);

                alertDialog.dismiss();
            }});
        alertDialog.setView(dialogView);
        alertDialog.show();
    }
}
