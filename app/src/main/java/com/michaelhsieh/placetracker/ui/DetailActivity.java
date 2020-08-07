package com.michaelhsieh.placetracker.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
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

public class DetailActivity extends AppCompatActivity implements VisitGroupAdapter.VisitItemClickListener, StartDragListener {

    private static final String TAG = DetailActivity.class.getSimpleName();

    /* There's one VisitGroup, which is at position 0 of the VisitGroupAdapter.
    The visits list starts at position 1.

    Get a clicked visit's adapter position - 1 to get the visit's list position.
    For example first visit is at adapter position 1 but visit list position 0.
    The visit list position is used as a parameter to update, delete, and add visits.

    Get visit's list position + 1 to get the visit's adapter position.
    The adapter position is used to notify the adapter of changes to the visit list.*/
    private static final int NUM_VISIT_GROUPS = 1;

    // maximum number of visits in list to prevent TransactionTooLargeException when rotating device
    public static final int MAX_NUM_VISITS = 150;

    // key to get the visit list after ex. device rotated
    private static final String STATE_VISIT_LIST = "visit_list";

    // key to check whether user was editing before Activity recreated, ex. when device rotated
    private static final String STATE_IS_EDITABLE = "is_editable";

    private PlaceModel place;

    // custom adapter to display a group of visits using ExpandingRecyclerView
    private VisitGroupAdapter adapter;

    // list of visits from place
    List<Visit> visits;

    TextView numVisitsDisplay;

    // label TextView and TextView of last date visited
    TextView lastVisitLabel;
    TextView lastVisitDisplay;

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

    // clickable TextView to edit with drag and drop
    TextView editDisplay;

    // whether the ViewModel is being observed the first time after rotation
    // used to display drag handles after rotation
    private boolean isFirstObservedAfterRotation = true;

    // track whether user can edit visits with drag and drop
    private boolean isEditable = false;

    // ItemTouchHelper to drag and drop visits
    private ItemTouchHelper itemTouchHelper;

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


        numVisitsDisplay = findViewById(R.id.tv_num_visits);
        lastVisitLabel = findViewById(R.id.tv_label_last_visit);
        lastVisitDisplay = findViewById(R.id.tv_last_visit);

        notesDisplay = findViewById(R.id.et_notes);

        // get clickable TextView
        editDisplay = findViewById(R.id.tv_edit_visits);

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
                        place = placeModel;

                        // rest of code using place is put here
                        String name = place.getName();
                        String address = place.getAddress();
                        int numVisits = place.getNumVisits();
                        String notes = place.getNotes();

                        if (savedInstanceState == null) {
                            nameDisplay.setText(name);
                            addressDisplay.setText(address);
                            notesDisplay.setText(notes);
                        }
                        // else, keep the EditText saved automatically by onSaveInstanceState,
                        // ex. user types some text and rotates device


                        // initialize the visit group and visits
                        if (savedInstanceState == null) {
                            visits = place.getVisits();
                        }
                        // else, use the visits list already in use. The user may have
                        // edited this list before rotation, ex. added and deleted visits
                        else {
                            if (savedInstanceState.getParcelableArrayList(STATE_VISIT_LIST) != null) {
                                visits = savedInstanceState.getParcelableArrayList(STATE_VISIT_LIST);
                                place.setVisits(visits);
                                numVisits = place.getNumVisits();
                            } else {
                                visits = place.getVisits();
                            }
                        }

                        // list of visit groups which will only contain one group at position 0
                        visitGroupList =
                                Arrays.asList
                                        (new VisitGroup(getResources().getString(R.string.dates_visited),
                                                visits)
                                        );

                        numVisitsDisplay.setText(String.valueOf(numVisits));

                        // show last visit if PlaceModel already has visits,
                        // otherwise hide last visit text and label
                        showOrHideLastVisit();


                        setUpAdapter();

                        // if ex. device rotated, restore expand or collapse state of adapter
                        if (savedInstanceState != null && adapter != null) {
                            adapter.onRestoreInstanceState(savedInstanceState);
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

        // instantiate the adapter with the list of visit groups and
        // DetailActivity as drag listener.
        // there's only one visit group
        adapter = new VisitGroupAdapter(visitGroupList, this);
        // set the click listener for clicks on individual visits
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        // set up ItemTouchHelper to swipe left to delete visit or drag and drop visits
        setUpItemTouchHelper(recyclerView);
    }

    /** Allow the user to drag and drop visits or update last visit when edit TextView is clicked.
     *
     * @param view The view clicked
     */
    public void editClicked(View view) {
        // TextView editDisplay = findViewById(R.id.tv_edit_visits);
        if (!isEditable) {
            allowEditing();
        } else {
            // since user clicked done, disable drag and drop and update last visit
            editDisplay.setText(getResources().getText(R.string.edit));
            isEditable = false;
            adapter.setHandleVisible(isEditable);
            // force onBindViewHolder again to update holder visibility
            adapter.notifyDataSetChanged();
            if (visits != null) {
                // update last visit
                showOrHideLastVisit();
            }
        }
    }

    /** Allow user to drag and drop.
     *
     */
    private void allowEditing() {
        // TextView editDisplay = findViewById(R.id.tv_edit_visits);
        editDisplay.setText(getResources().getText(R.string.done));
        // allow drag and drop
        isEditable = true;
        adapter.setHandleVisible(isEditable);
        // force onBindViewHolder again to update holder visibility
        adapter.notifyDataSetChanged();
    }

    /** Swipe left to delete a visit.
     * Drag and drop to rearrange visit.
     *
     * @param recyclerView The RecyclerView displaying the list of visits
     */
    private void setUpItemTouchHelper(RecyclerView recyclerView) {

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT) {

            // disable long press drag since
            // user can drag by clicking edit and dragging handles instead
            @Override
            public boolean isLongPressDragEnabled() {
                // return super.isLongPressDragEnabled();
                return false;
            }

            // disable swipe for VisitGroup at position 0
            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (viewHolder instanceof VisitGroupAdapter.VisitGroupViewHolder) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {

                // don't allow VisitGroup to be moved
                if (!(viewHolder instanceof VisitGroupAdapter.VisitGroupViewHolder) && !(target instanceof VisitGroupAdapter.VisitGroupViewHolder)) {

                    /* Subtract by 1 to get the correct visit list position of the Visits being moved.
                     Since position 0 is already occupied by the VisitGroup parent, the first Visit
                     is at adapter position 1.
                     Using getAdapterPosition() by itself will cause an IndexOutOfBoundsException. */
                    final int fromPos = viewHolder.getAdapterPosition() - NUM_VISIT_GROUPS;
                    final int toPos = target.getAdapterPosition() - NUM_VISIT_GROUPS;
                    // move item at fromPos to toPos in adapter.
                    Visit visitToMove = visits.get(fromPos);
                    moveSingleItem(fromPos, toPos, visitToMove);
                    // true if moved, false otherwise
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                /* Get adapter position that was swiped and
                subtract 1 because VisitGroup is at position 0, and
                the first visit is at position 1. */
                int posToDelete = viewHolder.getAdapterPosition() - 1;
                // delete place at that position from the database
                Visit visitToDelete = visits.get(posToDelete);

                // create delete visit message
                // Are you sure you want to delete this visit on [date] at [time]?
                String deleteVisitMessage = getResources().getString(R.string.delete_visit_message)
                        + visitToDelete.getDate() + getResources().getString(R.string.at) +
                        visitToDelete.getTime() +
                        getResources().getString(R.string.question_mark);

                new AlertDialog.Builder(DetailActivity.this)
                        .setTitle(R.string.delete_visit_title)
                        .setMessage(deleteVisitMessage)

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Continue with delete operation
                                removeSingleItem(posToDelete);
                            }
                        })

                        // if user cancels delete, then refresh the visit that was supposed to be swiped so
                        // it doesn't get swiped off screen
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // make the visit visible again
                                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    /** Implement start dragging on a given ViewHolder.
     *
     * @param viewHolder The ViewHolder of the drag handle
     */
    @Override
    public void requestDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }

    /** Display place photo and attribution text, if available.
     *
     */
    private void setUpPhoto() {
        // display bitmap photo if available
        String base64String = place.getBase64String();
        // display photo's attribution text if available
        String attributions = place.getAttributions();
        if (base64String != null && !base64String.isEmpty()) {
            // decode Base64 String to bitmap
            Bitmap bitmap = decodeBase64StringToBitmap(base64String);

            photo.setVisibility(View.VISIBLE);
            photo.setImageBitmap(bitmap);

            // make attributions text visible and display
            if (attributions != null && !attributions.isEmpty()) {
                attributionsText.setVisibility(View.VISIBLE);
                // setText(Html.fromHtml(bodyData)) is deprecated after api 24
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
                        + place.getName() + getResources().getString(R.string.at) +
                        place.getAddress() +
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
                    // save the user's current EditText data for name, address, and notes
                    // visits should already be added and Place ID should stay the same
                    place.setName(nameDisplay.getText().toString());
                    place.setAddress(addressDisplay.getText().toString());
                    place.setNotes(notesDisplay.getText().toString());

                    // update place in the database
                    viewModel.update(place);
                    // get the saved place's name, address, and number of visits and
                    // update the widget
                    PlaceTrackerWidgetDisplayService.startActionUpdatePlaceTrackerWidgets(DetailActivity.this,
                            place.getName(), place.getAddress(),
                            place.getNumVisits());

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
        lastVisitDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lastVisitDisplay.getVisibility() == View.VISIBLE) {
                    // label is only used by developer, can retrieve by using clip.getDescription()
                    String label = getString(R.string.visit_date_time_copy_label);
                    String text = lastVisitDisplay.getText().toString();
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
        if (adapter != null) {
            adapter.onSaveInstanceState(outState);
        }

        // save the visit list on configuration change, ex. device rotated
        if (visits != null && visits.size() < MAX_NUM_VISITS) {
            outState.putParcelableArrayList(STATE_VISIT_LIST, new ArrayList<>(visits));
        } else if (visits != null) {
            Log.e(TAG, "visit list is too large. Not placing in outState bundle.");
        }

        // save whether the user was editing visits or not
        outState.putBoolean(STATE_IS_EDITABLE, isEditable);
    }

    /** Called whenever a visit in the list is clicked. Show the date and time picker dialog.
     *
     * @param view The View displaying this visit's date and time
     * @param position The adapter position of the Visit clicked, calculated by getAdapterPosition()
     */
    @Override
    public void onItemClick(View view, int position) {
        /* Subtract by 1 to get the correct visit list position of the Visit clicked.
         * Since position 0 is already occupied by the VisitGroup parent, the first Visit
         * is at adapter position 1.
         * Using getAdapterPosition() by itself will cause an IndexOutOfBoundsException. */
        int positionInVisitList = position - NUM_VISIT_GROUPS;

        Visit visit = visits.get(positionInVisitList);

        // show the date and time pickers
        // param is the clicked position so button click can update visit
        showDateTimePicker(positionInVisitList, visit);
    }

    /** When user touches outside an EditText, clear that EditText's focus and close keyboard.
     * <p></p>
     * Used for name, address, and notes EditText
     * to stop screen from jumping to focused EditText when ex. expanding visits or adding visits.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            int x = (int) event.getRawX();
            int y = (int) event.getRawY();

            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains(x, y)) {
                    // clear focus
                    v.clearFocus();

                    // without this part of code, if the user clicks on another EditText then
                    // the keyboard closes and reopens immediately
                    boolean touchTargetIsEditText = false;
                    // Check if another EditText has been touched
                    for (View vi : v.getRootView().getTouchables()) {
                        if (vi instanceof EditText) {
                            Rect clickedViewRect = new Rect();
                            vi.getGlobalVisibleRect(clickedViewRect);
                            if (clickedViewRect.contains(x, y)) {
                                touchTargetIsEditText = true;
                                break;
                            }
                        }
                    }

                    if (!touchTargetIsEditText) {
                        // hide keyboard
                        hideSoftKeyboard(v);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    /** Hide soft keyboard.
     * @param view Used to retrieve the token identifying the window this view is attached to.
     */
    private void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /** Insert an item into the RecyclerView
     * @param visit The visit being inserted
     */
    private void insertSingleItem(Visit visit) {
        // insert at the very end of the list
        int insertIndex = place.getNumVisits();
        // add visit
        visits.add(insertIndex, visit);
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

    /** Move an item from one position to another in the RecyclerView.
     * @param fromPosition The starting visit list position of the visit
     * @param toPosition The ending visit list position of the visit
     * @param visit The visit being moved
     */
    private void moveSingleItem(int fromPosition, int toPosition, Visit visit) {
        // update visits list
        visits.remove(fromPosition);
        visits.add(toPosition, visit);

        // notify adapter
        // add 1 to get the correct adapter position of the visit,
        // since the visit list starts at position 1 of the adapter
        adapter.notifyItemMoved(fromPosition + NUM_VISIT_GROUPS, toPosition + NUM_VISIT_GROUPS);
    }

    /** Show or hide the most recent date visited label and text.
     *
     */
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
    /** Display the date and time picker dialog so user can edit a clicked Visit's
     * date and time. Updates the Visit if the user clicks the set button.
     * Deletes the Visit if the user clicks the delete visit button.
     *
     * @param pos The visit list position of the clicked Visit. Used to update or delete the Visit.
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

        // copy visit date and time if copy button clicked
        dialogView.findViewById(R.id.btn_copy_visit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // label is only used by developer, can retrieve by using clip.getDescription()
                String label = getString(R.string.visit_date_time_copy_label);
                String text = visit.getDate() + getApplicationContext().getResources().getString(R.string.at) + visit.getTime();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(label, text);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getApplicationContext(), R.string.visit_date_time_copy_confirm_message, Toast.LENGTH_LONG).show();
                }
                alertDialog.dismiss();
            }
        });

        // show confirmation dialog and delete visit if delete button clicked
        dialogView.findViewById(R.id.btn_delete_visit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // create delete visit message
                // Are you sure you want to delete this visit on [date] at [time]?
                String deleteVisitMessage = getResources().getString(R.string.delete_visit_message)
                        + visit.getDate() + getResources().getString(R.string.at) +
                        visit.getTime() +
                        getResources().getString(R.string.question_mark);

                new AlertDialog.Builder(DetailActivity.this)
                        .setTitle(R.string.delete_visit_title)
                        .setMessage(deleteVisitMessage)

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
