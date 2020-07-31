package com.michaelhsieh.placetracker;

import android.util.Log;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.michaelhsieh.placetracker.MainActivityScreenTest.atPosition;

/**
 * This test demos a user adding a place manually using the menu item, then
 * opens up the corresponding DetailActivity to check those details are displayed correctly.
 *
 */
@RunWith(AndroidJUnit4.class)
public class AddManualPlaceTest {

    private static final String TAG = AddManualPlaceTest.class.getSimpleName();

    // name and address of the place which will be added manually
    private static final String PLACE_NAME = "Martian Crater";
    private static final String PLACE_ADDRESS = "Olympus Mountain, Mars, The Solar System";

    // position of the manual place in RecyclerView
    private static final int POS_PLACE = 1;
    // position of VisitGroup in ExpandableRecyclerView
    private static final int POS_VISIT_GROUP = 0;

    private static final String NOTES = "Will we land here someday?";

    private static final String AT = " at ";
    // store the expected date and time, which should match date and time of TextViews
    // in visit ExpandableRecyclerView
    private String time;
    private String date;

    private static final int POS_NEW_VISIT = 1;

    // updated date is:
    // Friday, January 1, 2100 at 6:00 pm
    private static final int UPDATED_YEAR = 2100;
    private static final int UPDATED_MONTH = 1;
    private static final int UPDATED_DAY = 1;
    // 24-hour format
    private static final int UPDATED_HOUR = 18;
    private static final int UPDATED_MINUTE = 0;

    @Rule
    public ActivityTestRule activityTestRule = new ActivityTestRule<>(
            MainActivity.class);

    /** Adds a place manually by clicking menu item.
     * Clicks the place and opens the corresponding DetailActivity.
     */
    @Test
    public void addManualPlaceAndCheckDetails() {
        // check the name and address of a place
        onView(withId(R.id.action_add_manual))
                .perform(click());

        // now in ManualPlaceDetailActivity
        onView(withId(R.id.et_manual_name)).perform(typeText(PLACE_NAME));
        onView(withId(R.id.et_manual_address)).perform(typeText(PLACE_ADDRESS));
        // close the keyboard so all Views are visible again
        Espresso.closeSoftKeyboard();

        addAndUpdateManualVisit();

        typeNotes();

        clickAddManualPlaceButton();

        clickPlaceItem_OpensManualDetailActivity();
    }

    private void addAndUpdateManualVisit() {
        // set updated date and time to check if matches RecyclerView TextViews
        setUpdatedDateAndTime();

        // scroll to add visit button and add visit
        onView(withId(R.id.btn_manual_add_visit))
                .perform(scrollTo());
        onView(withId(R.id.btn_manual_add_visit)).perform(click());

        // update the added visit
        updateManualVisit();

        // check visit TextViews are correct before saving
        checkManualVisitBeforeAddingPlace();
    }

    private void updateManualVisit() {
        // scroll to expandable RecyclerView
        onView(withId(R.id.expanding_rv_manual_visits))
                .perform(scrollTo());

        // scroll to visit group and click to expand
        onView(withId(R.id.expanding_rv_manual_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_VISIT_GROUP))
                .perform(click());

        // scroll to bottom of screen to allow visit to be clicked
//        onView(withId(R.id.btn_manual_add_place)).perform(scrollTo());

        // click the visit that was added to update it
        onView(withId(R.id.expanding_rv_manual_visits))
                .perform(RecyclerViewActions.actionOnItemAtPosition(POS_NEW_VISIT, click()));

//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            Log.e(TAG, "updateManualVisit: ", e);
//        }

        // pick updated date
        onView(withId(R.id.date_picker)).perform(PickerActions.setDate(
                UPDATED_YEAR, UPDATED_MONTH, UPDATED_DAY));

        // scroll to time picker
        onView(withId(R.id.time_picker))
                .perform(scrollTo());

        // pick updated time
        onView(withId(R.id.time_picker)).perform(PickerActions.setTime(UPDATED_HOUR, UPDATED_MINUTE));

        // scroll to set button
        onView(withId(R.id.btn_date_time_set))
                .perform(scrollTo());

        // click to set updated date and time
        onView(withId(R.id.btn_date_time_set)).perform(click());
    }

    // check the visit added in ManualPlaceDetailActivity displays correct text
    // This is different from checkManualPlaceUpdatedVisit because that method uses
    // the IDs in DetailActivity, but this uses the IDs in ManualPlaceDetailActivity
    private void checkManualVisitBeforeAddingPlace() {
        // check the newly added visit
        // scroll to number of visits
        onView(withId(R.id.tv_manual_num_visits)).perform(scrollTo());
        // check number of visits is 1
        onView(withId(R.id.tv_manual_num_visits)).check(matches(withText(String.valueOf(1))));

        // scroll to last visit label
        onView(withId(R.id.tv_label_manual_last_visit)).perform(scrollTo());
        // check that the last visit label and date are still visible
        onView(withId(R.id.tv_label_manual_last_visit)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.tv_manual_last_visit)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        // check last visit has updated date and time
        onView(withId(R.id.tv_manual_last_visit)).check(
                matches(withText(date + AT + time)));

        // scroll to expandable RecyclerView
        onView(withId(R.id.expanding_rv_manual_visits))
                .perform(scrollTo());

        // check that the visit has updated date and time
        onView(withId(R.id.expanding_rv_manual_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_NEW_VISIT))
                .check(matches(atPosition(POS_NEW_VISIT, hasDescendant(withText(date)))));
        onView(withId(R.id.expanding_rv_manual_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_NEW_VISIT))
                .check(matches(atPosition(POS_NEW_VISIT, hasDescendant(withText(time)))));
    }

    /** Set the expected date and time to updated values.
     * These should match the date and time set in dialog when Espresso updates visit.
     *
     */
    private void setUpdatedDateAndTime() {
        // Calendar whose Date object will be converted to a readable date String and time String
        // initially set to the current date and time
        Calendar updatedCalendar = Calendar.getInstance();
        updatedCalendar.clear();
        // subtract 1 because Calendar month numbering is 0-based, ex. January is 0
        updatedCalendar.set(UPDATED_YEAR, UPDATED_MONTH-1, UPDATED_DAY, UPDATED_HOUR, UPDATED_MINUTE);

        Date updatedTime = updatedCalendar.getTime();
        time = DateFormat.getTimeInstance(DateFormat.SHORT).format(updatedTime);
        // format date to show day of week, month, day, year
        date = DateFormat.getDateInstance(DateFormat.FULL).format(updatedTime);
    }

    private void typeNotes() {
        // scroll to notes EditText to make sure Espresso can type text in it
        onView(withId(R.id.et_manual_notes))
                .perform(scrollTo());
        onView(withId(R.id.et_manual_notes)).perform(typeText(NOTES));
        // close soft keyboard when done typing
        Espresso.closeSoftKeyboard();
    }

    private void clickAddManualPlaceButton() {
        // scroll to save button to make sure Espresso can click it
        onView(withId(R.id.btn_manual_add_place))
                .perform(scrollTo());
        // save manual place
        onView(withId(R.id.btn_manual_add_place)).perform(click());
    }

    private void clickPlaceItem_OpensManualDetailActivity() {
        // check the name and address of the manual place
        onView(withId(R.id.rv_places))
                .perform(RecyclerViewActions.scrollToPosition(POS_PLACE))
                .check(matches(atPosition(POS_PLACE, hasDescendant(withText(PLACE_NAME)))))
                .check(matches(atPosition(POS_PLACE, hasDescendant(withText(PLACE_ADDRESS)))));
        // click on the manual place
        onView(withId(R.id.rv_places))
                .perform(RecyclerViewActions.actionOnItemAtPosition(POS_PLACE, click()));

        // check manual place details
        checkManualPlaceDetails();
    }

    private void checkManualPlaceDetails() {
        // now in DetailActivity, check the name and address are correct
        onView(withId(R.id.et_name)).check(matches(withText(PLACE_NAME)));
        onView(withId(R.id.et_address)).check(matches(withText(PLACE_ADDRESS)));

        checkManualPlaceUpdatedVisit();

        // scroll to notes EditText
        onView(withId(R.id.et_notes))
                .perform(scrollTo());
        onView(withId(R.id.et_notes)).check(matches(isDisplayed()));
        // check notes displays correct text
        onView(withId(R.id.et_notes)).check(matches(withText(NOTES)));

        // go back
        Espresso.pressBack();
    }

    // check manual place's DetailActivity has correct visit
    private void checkManualPlaceUpdatedVisit() {
        // scroll to number of visits
        onView(withId(R.id.tv_num_visits)).perform(scrollTo());
        // check number of visits is 1
        onView(withId(R.id.tv_num_visits)).check(matches(withText(String.valueOf(1))));

        checkLastVisitLabelAndText(date, time);

        // scroll to expandable RecyclerView
        onView(withId(R.id.expanding_rv_visits))
                .perform(scrollTo());

        // scroll to visit group and click to expand
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_VISIT_GROUP))
                .perform(click());

        // check that the visit has updated date and time
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_NEW_VISIT))
                .check(matches(atPosition(POS_NEW_VISIT, hasDescendant(withText(date)))));
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_NEW_VISIT))
                .check(matches(atPosition(POS_NEW_VISIT, hasDescendant(withText(time)))));
    }

    /** Check last visit label and TextView are visible and TextView matches the
     * expected date and time.
     * @param date The expected date
     * @param time The expected time
     */
    private void checkLastVisitLabelAndText(String date, String time) {
        // scroll to last visit label
        onView(withId(R.id.tv_label_last_visit)).perform(scrollTo());
        // check that the last visit label and date are still visible
        onView(withId(R.id.tv_label_last_visit)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.tv_last_visit)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        // check last visit has updated date and time
        onView(withId(R.id.tv_last_visit)).check(
                matches(withText(date + AT + time)));
    }
}
