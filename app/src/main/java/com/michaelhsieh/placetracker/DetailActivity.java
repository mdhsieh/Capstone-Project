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

import java.util.Arrays;
import java.util.List;

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
                // add a divider
                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                        layoutManager.getOrientation());
                // use custom white divider
                dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.place_divider));
                recyclerView.addItemDecoration(dividerItemDecoration);

                // instantiate the adapter with the list of visit groups.
                // there's only one visit group
                adapter = new VisitGroupAdapter(visitGroupList);
                recyclerView.setLayoutManager(layoutManager);
                // set the click listener for clicks on individual visits
                adapter.setClickListener(this);
                recyclerView.setAdapter(adapter);

                // show the expanded dates when the edit dates button is clicked and
                // the group is not already expanded
                Button editDatesButton = findViewById(R.id.btn_edit_dates);
                editDatesButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!adapter.isGroupExpanded(visitGroupList.get(0))) {
                            adapter.toggleGroup(visitGroupList.get(0));
                        }
                    }
                });

                // add visit when the add visit button is clicked
                // TODO: change visit to current date and time
                Button addVisitButton = findViewById(R.id.btn_add_visit);
                addVisitButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        insertSingleItem(new Visit("Saturday, June 27, 2020", "1:15 pm"));
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
    /*private VisitGroup getVisitGroup() {
        if (visitGroupList != null) {
            return visitGroupList.get(0);
        }
        else {
            Log.e(TAG, "visit group list is null");
            return null;
        }
    }*/

    // called whenever a visit in the list is clicked
    // position param is the position of the Visit clicked, calculated by getAdapterPosition() - 1
    @Override
    public void onItemClick(View view, int position) {
        Log.d(TAG, "visit group items: " + visitGroupList.get(0).getItems());
        Log.d(TAG, "position is: " + position);
        Visit visit = visitGroupList.get(0).getItems().get(position);
        Toast.makeText(this, "You clicked " + visit.getDate() + ", " + visit.getTime() + " on row number " + position, Toast.LENGTH_SHORT).show();
    }

    /* Insert an item into the RecyclerView
     */
    private void insertSingleItem(Visit visit) {
        // insert at the very end of the list
        int insertIndex = visitGroupList.get(0).getItems().size();
        // add visit
        visitGroupList.get(0).getItems().add(insertIndex, visit);
        adapter.notifyItemChanged(0);
        // increase number of visits by 1 and display updated text
        place.increaseNumVisits();
        numVisitsDisplay.setText(String.valueOf(place.getNumVisits()));
    }
}
