package com.michaelhsieh.placetracker;

import android.view.View;

import com.michaelhsieh.placetracker.ui.MainActivity;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;

/**
 * This test demos a user clicking on a place in MainActivity which
 * opens up the corresponding DetailActivity.
 *
 * <p></p>
 *
 * The place must already be added to the RecyclerView,
 * ex. from first running {@link MainActivitySearchPlaceTest}.
 *
 * <p></p>
 *
 * This test assumes the place is at the first position of the RecyclerView and
 * has no visits or notes.
 *
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityScreenTest {

    // name and address of the place which will be clicked
    private static final String PLACE_NAME = "Sweet Tomatoes";
    private static final String PLACE_ADDRESS = "4501 Hopyard Rd, Pleasanton, CA 94588, USA";
    // position of the place in RecyclerView
    private static final int POS_PLACE = 0;
    // position of VisitGroup in ExpandableRecyclerView
    private static final int POS_VISIT_GROUP = 0;

    private static final String NOTES = "This is a buffet restaurant.\nIt has a salad bar" +
            " and soups.";

    private static final String AT = " at ";
    // store the expected date and time, which should match date and time of TextViews
    // in visit ExpandableRecyclerView
    private String time;
    private String date;
    // expected number of visits
    private int numVisits = 0;
    private static final int POS_NEW_VISIT = 1;

    // updated date is:
    // Tuesday, February 14, 2017 at 3:25 pm
    private static final int UPDATED_YEAR = 2017;
    private static final int UPDATED_MONTH = 2;
    private static final int UPDATED_DAY = 14;
    // 24-hour format
    private static final int UPDATED_HOUR = 15;
    private static final int UPDATED_MINUTE = 25;

    @Rule
    public ActivityTestRule activityTestRule = new ActivityTestRule<>(
            MainActivity.class);

    /** Checks a place at a given position has the correct name and address.
     * Clicks the place and opens the corresponding DetailActivity.
     */
    @Test
    public void clickPlaceItem_OpensDetailActivity() {
        // check the name and address of a place
        onView(withId(R.id.rv_places))
                .perform(RecyclerViewActions.scrollToPosition(POS_PLACE))
                .check(matches(atPosition(POS_PLACE, hasDescendant(withText(PLACE_NAME)))))
                .check(matches(atPosition(POS_PLACE, hasDescendant(withText(PLACE_ADDRESS)))));
        // click on that place
        onView(withId(R.id.rv_places))
                .perform(RecyclerViewActions.actionOnItemAtPosition(POS_PLACE, click()));

        // check details are correct and go back to MainActivity
        checkDetails();
    }

    /** When a place's DetailActivity is started, check details are correct. Then go back.
     * <p></p>
     * Checks name, address, and number of visits have correct text.
     * Check labels and buttons are displayed on screen to user and have correct text.
     *
     */
    private void checkDetails() {
        // now in DetailActivity, check the name and address are correct
        onView(withId(R.id.et_name)).check(matches(withText(PLACE_NAME)));
        onView(withId(R.id.et_address)).check(matches(withText(PLACE_ADDRESS)));

        // scroll to save button to make sure number of visits label is visible to user,
        // is visible on phone but not tablet
        onView(withId(R.id.tv_label_num_visits))
                .perform(scrollTo());

        // check that number of visits label and add visit button are visible to user
        onView(withId(R.id.tv_label_num_visits)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_add_visit)).check(matches(isDisplayed()));

        // check that number of visits label and add visit button display correct text
        onView(withId(R.id.tv_label_num_visits)).check(matches(withText(R.string.num_visits_label)));
        onView(withId(R.id.btn_add_visit)).check(matches(withText(R.string.add_visit)));
        // assuming no visits, check number of visits is 0
        onView(withId(R.id.tv_num_visits)).check(matches(withText(String.valueOf(0))));

        // scroll to save button to make sure Views are displayed on screen to user
        onView(withId(R.id.btn_save))
                .perform(scrollTo());

        // check expandable visit group displays correct label
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_VISIT_GROUP))
                .check(matches(atPosition(POS_VISIT_GROUP, hasDescendant(withText(R.string.dates_visited)))));

        // check notes label is visible to user
        onView(withId(R.id.tv_label_notes)).check(matches(isDisplayed()));

        // check notes label displays correct text
        onView(withId(R.id.tv_label_notes)).check(matches(withText(R.string.notes_label)));

        // check notes EditText has correct hint
        onView(withId(R.id.et_notes)).check(matches(withHint(R.string.notes_hint)));

        // check save and delete button display correct text
        onView(withId(R.id.btn_delete)).check(matches(withText(R.string.delete)));
        onView(withId(R.id.btn_save)).check(matches(withText(R.string.save)));

        // go back
        Espresso.pressBack();
    }

    /** Start DetailActivity, add notes, and save.
     *
     */
    @Test
    public void addNotes() {
        // click a place to start DetailActivity
        clickPlace();

        // scroll to notes EditText to make sure Espresso can type text in it
        onView(withId(R.id.et_notes))
                .perform(scrollTo());
        onView(withId(R.id.et_notes)).perform(typeText(NOTES));
        // close soft keyboard when done typing
        Espresso.closeSoftKeyboard();

        // scroll to save button to make sure Espresso can click it
        onView(withId(R.id.btn_save))
                .perform(scrollTo());
        // save notes
        onView(withId(R.id.btn_save)).perform(click());

        // re-open details screen and check notes text is still there
        checkNotes();

        // clears notes to make repeat testing easier
        clearNotes();
    }

    /** Check notes were saved.
     *
     */
    private void checkNotes() {
        // start DetailActivity again
        clickPlace();
        // scroll to notes EditText
        onView(withId(R.id.et_notes))
                .perform(scrollTo());
        onView(withId(R.id.et_notes)).check(matches(isDisplayed()));
        // check notes displays correct text
        onView(withId(R.id.et_notes)).check(matches(withText(NOTES)));

        // go back
        Espresso.pressBack();
    }

    /** Clear notes by replacing with empty text.
     *  Used to make testing easier.
     */
    private void clearNotes() {
        // click a place to start DetailActivity
        clickPlace();

        // scroll to notes EditText to make sure Espresso can type text in it
        onView(withId(R.id.et_notes))
                .perform(scrollTo());
        onView(withId(R.id.et_notes)).perform(replaceText(""));
        // close soft keyboard when done typing
        Espresso.closeSoftKeyboard();

        // scroll to save button to make sure Espresso can click it
        onView(withId(R.id.btn_save))
                .perform(scrollTo());
        // save notes
        onView(withId(R.id.btn_save)).perform(click());
    }

    /** Add, update, and delete one visit.
     *
     */
    @Test
    public void editVisits() {

        // add one because expecting number of visits to increase by 1 when adding visit
        increaseNumVisits();
        // set current date and time to check if matches RecyclerView TextViews and last visit date
        setCurrentDateAndTime();

        // add a visit and save
        addVisit();

        // set updated date and time to check if matches RecyclerView TextViews
        setUpdatedDateAndTime();

        // update the visit and save
        updateVisit();

        // subtract one because expecting number of visits to decrease by 1 when deleting visit
        decreaseNumVisits();

        // delete the visit and save
        deleteVisit();
    }

    private void increaseNumVisits() {
        numVisits += 1;
    }

    private void decreaseNumVisits() {
        numVisits -= 1;
    }

    /** Set the expected date and time to the current date and time.
     * These should match the default date and time when user adds visit.
     *
     */
    private void setCurrentDateAndTime() {
        Date currentTime = Calendar.getInstance().getTime();
        time = DateFormat.getTimeInstance(DateFormat.SHORT).format(currentTime);
        // format date to show day of week, month, day, year
        date = DateFormat.getDateInstance(DateFormat.FULL).format(currentTime);
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

    /** Add a visit and check TextViews are correct.
     *
     */
    private void addVisit() {
        // click place to start DetailActivity
        clickPlace();

        // scroll to add visit button and add visit
        onView(withId(R.id.btn_add_visit))
                .perform(scrollTo());
        onView(withId(R.id.btn_add_visit)).perform(click());

        // check number of visits increased by 1
        onView(withId(R.id.tv_num_visits)).check(matches(withText(String.valueOf(numVisits))));

        // check last visit label and text are visible and match current date and time
        checkLastVisitLabelAndText(date, time);

        // scroll to expandable RecyclerView
        onView(withId(R.id.expanding_rv_visits))
                .perform(scrollTo());

        // scroll to visit group and click to expand
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_VISIT_GROUP))
                .perform(click());

        // check that the new visit has current date and time
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_NEW_VISIT))
                .check(matches(atPosition(POS_NEW_VISIT, hasDescendant(withText(date)))));
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_NEW_VISIT))
                .check(matches(atPosition(POS_NEW_VISIT, hasDescendant(withText(time)))));

        // scroll to save button
        onView(withId(R.id.btn_save))
                .perform(scrollTo());
        // save the visit
        onView(withId(R.id.btn_save)).perform(click());
    }

    /** Update an existing visit and check TextViews are correct.
     *
     */
    private void updateVisit() {
        // click place to start DetailActivity
        clickPlace();

        // scroll to expandable RecyclerView
        onView(withId(R.id.expanding_rv_visits))
                .perform(scrollTo());

        // scroll to visit group and click to expand
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_VISIT_GROUP))
                .perform(click());

        // scroll to bottom of screen to allow visit to be clicked
        onView(withId(R.id.btn_save)).perform(scrollTo());

        // click the visit that was added to update it
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.actionOnItemAtPosition(POS_NEW_VISIT, click()));

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

        // scroll to number of visits
        onView(withId(R.id.tv_num_visits)).perform(scrollTo());
        // check number of visits is unchanged
        onView(withId(R.id.tv_num_visits)).check(matches(withText(String.valueOf(numVisits))));

        // check last visit label visible and text has latest date and time TextViews
        checkLastVisitLabelAndText(date, time);

        // scroll to expandable RecyclerView
        onView(withId(R.id.expanding_rv_visits))
                .perform(scrollTo());

        // check that the visit has updated date and time
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_NEW_VISIT))
                .check(matches(atPosition(POS_NEW_VISIT, hasDescendant(withText(date)))));
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_NEW_VISIT))
                .check(matches(atPosition(POS_NEW_VISIT, hasDescendant(withText(time)))));

        // scroll to save button and save the updated visit
        onView(withId(R.id.btn_save))
                .perform(scrollTo(), click());
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

    /** Delete an existing visit and check TextViews are correct.
     *
     */
    private void deleteVisit() {
        // click place to start DetailActivity
        clickPlace();

        // scroll to expandable RecyclerView
        onView(withId(R.id.expanding_rv_visits))
                .perform(scrollTo());

        // scroll to visit group and click to expand
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_VISIT_GROUP))
                .perform(click());

        // scroll to bottom of screen to allow visit to be clicked
        onView(withId(R.id.btn_save)).perform(scrollTo());

        // click the visit to delete it
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.actionOnItemAtPosition(POS_NEW_VISIT, click()));

        // scroll to delete button and click
        onView(withId(R.id.btn_delete_visit))
                .perform(scrollTo(), click());

        // click confirmation dialog to delete
        onView(withText(android.R.string.yes)).check(matches(isDisplayed()));
        onView(withText(android.R.string.yes)).perform(click());

        // check number of visits decreased by 1
        onView(withId(R.id.tv_num_visits)).check(matches(withText(String.valueOf(numVisits))));

        // check that the last visit label and date are gone
        onView(withId(R.id.tv_label_last_visit)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.tv_last_visit)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // check visit has been deleted
        // visit group label should remain
        onView(withId(R.id.expanding_rv_visits))
                .perform(RecyclerViewActions.scrollToPosition(POS_VISIT_GROUP))
                .check(matches(atPosition(POS_VISIT_GROUP, hasDescendant(withText(R.string.dates_visited)))));
        // visit date and time TextViews should not exist
        onView(withId(R.id.expanding_rv_visits))
                .check(matches(not(atPosition(POS_NEW_VISIT, hasDescendant(withText(date))))));
        onView(withId(R.id.expanding_rv_visits))
                .check(matches(not(atPosition(POS_NEW_VISIT, hasDescendant(withText(time))))));

        // scroll to save button
        onView(withId(R.id.btn_save))
                .perform(scrollTo());
        // save
        onView(withId(R.id.btn_save)).perform(click());
    }

    /** Click place at position in list.
     *
     */
    private void clickPlace() {
        onView(withId(R.id.rv_places))
                .perform(RecyclerViewActions.actionOnItemAtPosition(POS_PLACE, click()));
    }

    /** Method to test item in RecyclerView at a given position.
     * <p></p>
     * Source: https://stackoverflow.com/questions/31394569/how-to-assert-inside-a-recyclerview-in-espresso
     */
    public static Matcher<View> atPosition(final int position, @NonNull final Matcher<View> itemMatcher) {
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("has item at position " + position + ": ");
                itemMatcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(final RecyclerView view) {
                RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);
                if (viewHolder == null) {
                    // has no item on such position
                    return false;
                }
                return itemMatcher.matches(viewHolder.itemView);
            }
        };
    }
}
