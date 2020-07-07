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

import java.util.Arrays;
import java.util.Calendar;
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

    /* There's one VisitGroup, which is at position 0 of the VisitGroupAdapter.
     The visits list starts at position 1. */
    private static final int NUM_VISIT_GROUPS = 1;

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

                String name = place.getName();
                String address = place.getAddress();
                int numVisits = place.getNumVisits();
                String notes = place.getNotes();

                nameDisplay.setText(name);
                addressDisplay.setText(address);
                notesDisplay.setText(notes);

                // initialize the visit group and visits
                visits = place.getVisits();

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
                        // object whose calendar fields have been initialized with the current date and time
                        Calendar rightNow = Calendar.getInstance();
                        insertSingleItem(new Visit(rightNow));
                    }
                });

                Button deleteButton = findViewById(R.id.btn_delete);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // alert dialog to confirm user wants to delete this place
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

    /* To save the expand and collapse state of the adapter,
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
    // position param is the adapter position of the Visit clicked, calculated by getAdapterPosition()
    @Override
    public void onItemClick(View view, int position) {
        // Visit visit = visits.get(position);

        /* Subtract by 1 to get the correct visit list position of the Visit clicked.
         * Since position 0 is already occupied by the VisitGroup parent, the first Visit
         * is really at adapter position 1.
         * Using getAdapterPosition() by itself will cause an IndexOutOfBoundsException. */
        int positionInVisitList = position - NUM_VISIT_GROUPS;

        Visit visit = visits.get(positionInVisitList);
        Toast.makeText(this, "You clicked " + visit.getDate() + ", " + visit.getTime() + " on row number " + positionInVisitList, Toast.LENGTH_LONG).show();

        // Toast.makeText(this, "You clicked " + visit.getDate() + ", " + visit.getTime() + " on row number " + position, Toast.LENGTH_SHORT).show();
        // show the date and time pickers
        // param is the clicked position so button click can update visit
        // showDateTimePicker(position, visit);
        showDateTimePicker(positionInVisitList, visit);
    }

    /** Insert an item into the RecyclerView
     * @param visit The visit being inserted
     */
    private void insertSingleItem(Visit visit) {
        // insert at the very end of the list
        int insertIndex = place.getNumVisits();
        // add visit
        visits.add(insertIndex, visit);
        // adapter.notifyItemChanged(0);
        // notify adapter that visit has been added
        // add 1 to get the correct adapter position of the visit,
        // since the visit list starts at position 1 of the adapter
        adapter.notifyItemInserted(insertIndex + NUM_VISIT_GROUPS);
        // increased number of visits by 1, so
        // display updated text
        numVisitsDisplay.setText(String.valueOf(place.getNumVisits()));

        // update last visit
        showOrHideLastVisit();
    }

    /** Update an item in the RecyclerView
     *
     * @param updateIndex The visit list index of the visit being updated
     * @param visit The visit being updated
     */
    private void updateSingleItem(int updateIndex, Visit visit) {
        // update visit
        visits.set(updateIndex, visit);
        // notify adapter that visit has changed
        // add 1 to get the correct adapter position of the visit,
        // since the visit list starts at position 1 of the adapter
        adapter.notifyItemChanged(updateIndex + NUM_VISIT_GROUPS);

        // update last visit
        // need this method if the visit that was updated was the last visit
        showOrHideLastVisit();
    }

    /** Delete an item from the RecyclerView
     * @param removeIndex The visit list index of the visit being removed
     */
    private void removeSingleItem(int removeIndex) {
        // remove visit
        visits.remove(removeIndex);
        // notify adapter that visit has been removed
        // add 1 to get the correct adapter position of the visit, since the visit list
        // starts at position 1 of the adapter
        adapter.notifyItemRemoved(removeIndex + NUM_VISIT_GROUPS);
        // decreased number of visits by 1, so
        // display updated text
        numVisitsDisplay.setText(String.valueOf(place.getNumVisits()));

        // update last visit
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
    /** Display the date and time picker layout so user can edit a clicked Visit's
     * date and time. Updates the Visit if the user clicks the set button.
     * Deletes the Visit if the user clicks the delete visit button.
     *
     * @param pos The position of the clicked Visit. Used to update or delete the Visit.
     * @param visit The visit that was clicked. Used to update the Visit.
     */
    private void showDateTimePicker(int pos, Visit visit) {
        final View dialogView = View.inflate(this, R.layout.date_time_picker, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        DatePicker datePicker = dialogView.findViewById(R.id.date_picker);
        TimePicker timePicker = dialogView.findViewById(R.id.time_picker);

        // update date and time pickers by setting them to current Visit's date and time
        Calendar visitCalendar = visit.getCalendar();
        int yearField = visitCalendar.get(Calendar.YEAR);
        int monthField = visitCalendar.get(Calendar.MONTH);
        int dayField = visitCalendar.get(Calendar.DAY_OF_MONTH);
        datePicker.updateDate(yearField, monthField, dayField);
        // get hours in 24-hour format
        int hourField = visitCalendar.get(Calendar.HOUR_OF_DAY);
        timePicker.setCurrentHour(hourField);
        int minuteField = visitCalendar.get(Calendar.MINUTE);
        timePicker.setCurrentMinute(minuteField);

        // update Visit with user's picked date and time when set button clicked
        dialogView.findViewById(R.id.btn_date_time_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int month = datePicker.getMonth();
                int day = datePicker.getDayOfMonth();
                int year = datePicker.getYear();
                int hour = timePicker.getCurrentHour();
                int minute = timePicker.getCurrentMinute();

                // Calendar whose Date object will be converted to a readable date String and time String
                // initially set to the current date and time
                Calendar calendar = Calendar.getInstance();

                // don't retain previous calendar field values from when Calendar was
                // first initialized with getInstance()
                calendar.clear();
                // set Calendar to user's picked date and time
                calendar.set(year, month, day, hour, minute);

                // create a new Visit with the Calendar matching user's picked date and time
                Visit updatedVisit = new Visit(calendar);
                // update the original Visit by replacing it with the new one
                updateSingleItem(pos, updatedVisit);

                alertDialog.dismiss();
            }});

        // cancel dialog if cancel button clicked
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        // show confirmation dialog and delete visit if delete button clicked
        dialogView.findViewById(R.id.btn_delete_visit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(DetailActivity.this)
                        .setTitle(R.string.delete_visit_title)
                        .setMessage(R.string.delete_visit_message)

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Continue with delete operation
                                removeSingleItem(pos);
                                // dismiss date and time picker dialog
                                alertDialog.dismiss();
                            }
                        })

                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        alertDialog.setView(dialogView);
        alertDialog.show();
    }
}
