package com.michaelhsieh.placetracker.ui;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.michaelhsieh.placetracker.R;
import com.michaelhsieh.placetracker.models.expandablegroup.VisitGroup;
import com.michaelhsieh.placetracker.models.PlaceModel;
import com.michaelhsieh.placetracker.models.expandablegroup.Visit;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/** Activity where user can enter info to add a place manually. Very similar to DetailActivity.
 *
 */
public class ManualPlaceDetailActivity extends AbstractDetailActivity {

    private static final String TAG = ManualPlaceDetailActivity.class.getSimpleName();

    // key to get manually added place when Activity recreated, ex. device rotated
    private static final String STATE_MANUAL_PLACE = "manual_place";

    // key to check whether user was editing before Activity recreated, ex. when device rotated
    private static final String STATE_IS_EDITABLE = "is_editable";

    // key of place with user's manually entered info when add button clicked
    // Used to send place to MainActivity
    public static final String EXTRA_MANUAL_ADDED_PLACE = "place";

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
        // Set Activity to ManualPlaceDetailActivity
        setActivity(this);
        setNumVisitsDisplay(findViewById(R.id.tv_manual_num_visits));
        setLastVisitLabel(findViewById(R.id.tv_label_manual_last_visit));
        setLastVisitDisplay(findViewById(R.id.tv_manual_last_visit));
        final EditText notesDisplay = findViewById(R.id.et_manual_notes);

        // make name and address multi-line EditTexts with done button
        nameDisplay.setHorizontallyScrolling(false);
        nameDisplay.setMaxLines(getResources().getInteger(R.integer.max_num_lines));
        addressDisplay.setHorizontallyScrolling(false);
        addressDisplay.setMaxLines(getResources().getInteger(R.integer.max_num_lines));

        // clear focus when done pressed on soft keyboard
        nameDisplay.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    // Clear focus here from EditText
                    nameDisplay.clearFocus();
                    // hide keyboard
                    hideSoftKeyboard(nameDisplay);
                }
                return false;
            }
        });
        addressDisplay.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    // Clear focus here from EditText
                    addressDisplay.clearFocus();
                    // hide keyboard
                    hideSoftKeyboard(addressDisplay);
                }
                return false;
            }
        });

        // get clickable TextView
        setEditDisplay(findViewById(R.id.tv_manual_edit_visits));

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore place from saved state
            setPlace(savedInstanceState.getParcelable(STATE_MANUAL_PLACE));

        } else {
            // generate a unique String as the place's Place ID
            String uniqueString = UUID.randomUUID().toString();

            // Create the manual place with randomly generated String PlaceID,
            // empty name, and empty address.
            // Initialize place here because visit methods will need to use place's getNumVisits,
            // avoid NullPointerException
            setPlace(new PlaceModel(uniqueString, "", ""));

            // initialize layout with empty texts and 0 visits
            int numVisits = 0;
            getNumVisitsDisplay().setText(String.valueOf(numVisits));
        }

        if (getPlace() != null) {

            // initialize visits to place's list of visits
            setVisits(getPlace().getVisits());

            // initialize list of visit groups which will only contain one group at position 0
            List<VisitGroup> visitGroupList =
                    Arrays.asList
                            (new VisitGroup(getResources().getString(R.string.dates_visited),
                                    getVisits())
                            );

            // show last visit if PlaceModel already has visits,
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

            // Instantiate the adapter with the list of visit groups and
            // ManualPlaceDetailActivity as drag listener.
            // There's only one visit group.
            setAdapter(new VisitGroupAdapter(visitGroupList, this));
            // set the click listener for clicks on individual visits
            getAdapter().setClickListener(this);
            recyclerView.setAdapter(getAdapter());

            // set up ItemTouchHelper to swipe left to delete visit or drag and drop visits
            setUpItemTouchHelper(recyclerView);

            // if user was editing before ex. device rotation, click edit button to
            // set isEditable to true and display drag handles
            if (savedInstanceState != null) {
                // when Activity is recreated, isEditable is false
                if (savedInstanceState.getBoolean(STATE_IS_EDITABLE)) {
                    allowEditing();
                }
            }

            // add visit when the add visit button is clicked
            Button addVisitButton = findViewById(R.id.btn_manual_add_visit);
            addVisitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // object whose calendar fields have been initialized with the current date and time
                    Calendar rightNow = Calendar.getInstance();
                    insertSingleItem(new Visit(rightNow));
                }
            });

            Button addManualPlaceButton = findViewById(R.id.btn_manual_add_place);
            addManualPlaceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent addManualPlaceIntent = new Intent();

                    // show Toast and don't save place if user didn't enter a valid name
                    if (nameDisplay.getText().toString().isEmpty()) {
                        Toast.makeText(ManualPlaceDetailActivity.this, R.string.empty_place_name_error, Toast.LENGTH_LONG).show();
                    } else {
                        // Save the user's current EditText data for name, address, and notes.
                        // Visits should already be added.
                        // This should not crash with TransactionTooLargeException unless user adds 150+ visits.
                        getPlace().setName(nameDisplay.getText().toString());
                        getPlace().setAddress(addressDisplay.getText().toString());
                        getPlace().setNotes(notesDisplay.getText().toString());
                        addManualPlaceIntent.putExtra(EXTRA_MANUAL_ADDED_PLACE, getPlace());
                        setResult(RESULT_OK, addManualPlaceIntent);
                        finish();
                    }
                }
            });

        } else {
            Log.e(TAG, "manual place is null");
        }
    }

    /** Save the state of the adapter and the manually entered place on configuration change,
     * ex. device rotation
     *
     * @param outState The Bundle that will store the adapter state and place
     */
    /* To save the expand and collapse state of the adapter,
    you have to explicitly call through to the adapter's
    onSaveInstanceState() and onRestoreInstanceState() in the calling Activity

    Source: https://github.com/thoughtbot/expandable-recycler-view */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState);
        if (getAdapter() != null) {
            getAdapter().onSaveInstanceState(outState);
        }

        // save whether the user was editing visits or not
        outState.putBoolean(STATE_IS_EDITABLE, isEditable());

        // save the place that the user is manually adding
        // This should not crash with TransactionTooLargeException unless user adds 150+ visits
        outState.putParcelable(STATE_MANUAL_PLACE, getPlace());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (getAdapter() != null) {
            getAdapter().onRestoreInstanceState(savedInstanceState);
        }
    }
}
