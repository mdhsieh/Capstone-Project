package com.michaelhsieh.placetracker.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.michaelhsieh.placetracker.R;
import com.michaelhsieh.placetracker.database.PlaceViewModel;
import com.michaelhsieh.placetracker.models.expandablegroup.VisitGroup;
import com.michaelhsieh.placetracker.models.PlaceModel;
import com.michaelhsieh.placetracker.models.expandablegroup.Visit;
import com.michaelhsieh.placetracker.widget.PlaceTrackerWidgetDisplayService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static com.michaelhsieh.placetracker.ui.MainActivity.EXTRA_PLACE_ID;

public class DetailActivity extends AbstractDetailActivity {

    private static final String TAG = DetailActivity.class.getSimpleName();

    // maximum number of visits in list to prevent TransactionTooLargeException when rotating device
    public static final int MAX_NUM_VISITS = 150;

    // key to get the visit list after ex. device rotated
    private static final String STATE_VISIT_LIST = "visit_list";

    // key to check whether user was editing before Activity recreated, ex. when device rotated
    private static final String STATE_IS_EDITABLE = "is_editable";

    // ImageViews to display place's photos
    ImageView photo;
    // TextView to display photo's attributions text
    TextView attributionsText;

    // PlaceViewModel to get clicked place by Place ID
    PlaceViewModel viewModel;

    // list of visit groups which has only one visit group
    List<VisitGroup> visitGroupList;

    // TextViews to display name, address, and notes
    EditText nameDisplay;
    EditText addressDisplay;
    EditText notesDisplay;

    // Whether the ViewModel is being observed the first time after rotation.
    // Used to display drag handles after rotation.
    private boolean isFirstObservedAfterRotation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // display up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        nameDisplay = findViewById(R.id.et_name);
        addressDisplay = findViewById(R.id.et_address);


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

        // Set Activity to DetailActivity
        setActivity(this);
        setNumVisitsDisplay(findViewById(R.id.tv_num_visits));
        setLastVisitLabel(findViewById(R.id.tv_label_last_visit));
        setLastVisitDisplay(findViewById(R.id.tv_last_visit));

        notesDisplay = findViewById(R.id.et_notes);

        // get clickable TextView
        setEditDisplay(findViewById(R.id.tv_edit_visits));

        // find ImageView that display photo
        photo = findViewById(R.id.iv_photo);
        // fine TextView that displays attribution text
        attributionsText = findViewById(R.id.tv_attributions);

        viewModel = new ViewModelProvider(this).get(PlaceViewModel.class);

        // get the PlaceModel ID from the Intent that started this Activity
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_PLACE_ID)) {
            String id = intent.getStringExtra(EXTRA_PLACE_ID);

            viewModel.getPlaceById(id).observe(this, new Observer<PlaceModel>() {
                @Override
                public void onChanged(PlaceModel placeModel) {
                    // placeModel will be null if place is deleted
                    if (placeModel != null) {

                        // change place to observed place from database
                        setPlace(placeModel);

                        // rest of code using place is put here
                        String name = getPlace().getName();
                        String address = getPlace().getAddress();
                        int numVisits = getPlace().getNumVisits();
                        String notes = getPlace().getNotes();

                        if (savedInstanceState == null) {
                            nameDisplay.setText(name);
                            addressDisplay.setText(address);
                            notesDisplay.setText(notes);
                        }
                        // else, keep the EditText saved automatically by onSaveInstanceState,
                        // ex. user types some text and rotates device


                        // initialize the visit group and visits
                        if (savedInstanceState == null) {
                            setVisits(getPlace().getVisits());
                        }
                        // else, use the visits list already in use. The user may have
                        // edited this list before rotation, ex. added and deleted visits
                        else {
                            if (savedInstanceState.getParcelableArrayList(STATE_VISIT_LIST) != null) {
                                setVisits(savedInstanceState.getParcelableArrayList(STATE_VISIT_LIST));
                                getPlace().setVisits(getVisits());
                                numVisits = getPlace().getNumVisits();
                            } else {
                                setVisits(getPlace().getVisits());
                            }
                        }

                        // list of visit groups which will only contain one group at position 0
                        visitGroupList =
                                Arrays.asList
                                        (new VisitGroup(getResources().getString(R.string.dates_visited),
                                                getVisits())
                                        );

                        getNumVisitsDisplay().setText(String.valueOf(numVisits));

                        // show last visit if PlaceModel already has visits,
                        // otherwise hide last visit text and label
                        showOrHideLastVisit();


                        setUpAdapter();

                        // if ex. device rotated, restore expand or collapse state of adapter
                        if (savedInstanceState != null && getAdapter() != null) {
                            getAdapter().onRestoreInstanceState(savedInstanceState);
                        }

                        // if user was editing before ex. device rotation,
                        // set isEditable to true and display drag handles
                        if (isFirstObservedAfterRotation && savedInstanceState != null) {
                            // when Activity is recreated, isEditable is false
                            if (savedInstanceState.getBoolean(STATE_IS_EDITABLE)) {
                                allowEditing();
                                // ViewModel will observe place again if user ex.
                                // clicks save button, so don't display drag handles after this
                                isFirstObservedAfterRotation = false;
                            }
                        }

                        setUpPhoto();

                        setUpAddVisitButton();

                        setUpDeleteButton();

                        setUpSaveButton();

                        setUpLastVisitDisplay();
                    }
                }
            });

        }

    }

    /** Set up the adapter, RecyclerView, and ItemTouchHelper.
     *
     */
    private void setUpAdapter() {
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

        // Instantiate the adapter with the list of visit groups and
        // DetailActivity as drag listener.
        // There's only one visit group.
        setAdapter(new VisitGroupAdapter(visitGroupList, this));
        // set the click listener for clicks on individual visits
        getAdapter().setClickListener(this);
        recyclerView.setAdapter(getAdapter());

        // set up ItemTouchHelper to swipe left to delete visit or drag and drop visits
        setUpItemTouchHelper(recyclerView);
    }

    /** Display place photo and attribution text, if available.
     *
     */
    private void setUpPhoto() {
        // display bitmap photo if available
        String base64String = getPlace().getBase64String();
        // display photo's attribution text if available
        String attributions = getPlace().getAttributions();
        if (base64String != null && !base64String.isEmpty()) {
            // decode Base64 String to bitmap
            Bitmap bitmap = decodeBase64StringToBitmap(base64String);

            photo.setVisibility(View.VISIBLE);
            photo.setImageBitmap(bitmap);

            // make attributions text visible and display
            if (attributions != null && !attributions.isEmpty()) {
                attributionsText.setVisibility(View.VISIBLE);
                // setText(Html.fromHtml(bodyData)) is deprecated after API 24
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    attributionsText.setText(Html.fromHtml(attributions, Html.FROM_HTML_MODE_COMPACT));
                } else {
                    attributionsText.setText(Html.fromHtml(attributions));
                }
            } else {
                attributionsText.setVisibility(View.GONE);
            }
        } else {
            photo.setVisibility(View.GONE);
            attributionsText.setVisibility(View.GONE);
        }
    }

    /** Set click listener for add visit button.
     *
     */
    private void setUpAddVisitButton() {
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
    }

    /** Set click listener for delete place button.
     *
     */
    private void setUpDeleteButton() {
        Button deleteButton = findViewById(R.id.btn_delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // create delete place message
                // Are you sure you want to delete [place] at [address]?
                String deleteMessage = getResources().getString(R.string.delete_place_message)
                        + getPlace().getName() + getResources().getString(R.string.at) +
                        getPlace().getAddress() +
                        getResources().getString(R.string.question_mark);

                // alert dialog to confirm user wants to delete this place
                new AlertDialog.Builder(DetailActivity.this)
                        .setTitle(R.string.delete_place_title)
                        .setMessage(deleteMessage)

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Continue with delete operation
                                Intent deletePlaceIntent = new Intent();
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
    }

    /** Set click listener for save button.
     *
     */
    private void setUpSaveButton() {
        Button saveButton = findViewById(R.id.btn_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // show Toast and don't save place if user didn't enter a valid name
                if (nameDisplay.getText().toString().isEmpty()) {
                    Toast.makeText(DetailActivity.this, R.string.empty_place_name_error, Toast.LENGTH_LONG).show();
                } else {
                    // Save the user's current EditText data for name, address, and notes.
                    // Visits should already be added and Place ID should stay the same.
                    getPlace().setName(nameDisplay.getText().toString());
                    getPlace().setAddress(addressDisplay.getText().toString());
                    getPlace().setNotes(notesDisplay.getText().toString());

                    // update place in the database
                    viewModel.update(getPlace());
                    // get the saved place's name, address, and number of visits and
                    // update the widget
                    PlaceTrackerWidgetDisplayService.startActionUpdatePlaceTrackerWidgets(DetailActivity.this,
                            getPlace().getName(), getPlace().getAddress(),
                            getPlace().getNumVisits());

                    finish();
                }
            }
        });
    }

    /** Set up click listener for last visit text.
     *
     */
    private void setUpLastVisitDisplay() {
        // if last visit TextView clicked, copy the date and time to clipboard
        getLastVisitDisplay().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getLastVisitDisplay().getVisibility() == View.VISIBLE) {
                    // label is only used by developer, can retrieve by using clip.getDescription()
                    String label = getString(R.string.visit_date_time_copy_label);
                    String text = getLastVisitDisplay().getText().toString();
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(label, text);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getApplicationContext(), R.string.visit_date_time_copy_confirm_message, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    /** To save the expand and collapse state of the adapter,
    you have to explicitly call through to the adapter's
    onSaveInstanceState() and onRestoreInstanceState() in the calling Activity. */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (getAdapter() != null) {
            getAdapter().onSaveInstanceState(outState);
        }

        // save the visit list on configuration change, ex. device rotated
        if (getVisits() != null && getVisits().size() < MAX_NUM_VISITS) {
            outState.putParcelableArrayList(STATE_VISIT_LIST, new ArrayList<>(getVisits()));
        } else if (getVisits() != null) {
            Log.e(TAG, "visit list is too large. Not placing in outState bundle.");
        }

        // save whether the user was editing visits or not
        outState.putBoolean(STATE_IS_EDITABLE, isEditable());
    }

    /** Decode Base64 String to Bitmap
     *
     * @param base64Image The Base64 String to be decoded into a Bitmap
     * @return A Bitmap
     */
    private Bitmap decodeBase64StringToBitmap(String base64Image) {
        byte[] data = Base64.decode(base64Image, Base64.DEFAULT);
        Bitmap bitmap;
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;
        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opt);
        return bitmap;
    }
}
