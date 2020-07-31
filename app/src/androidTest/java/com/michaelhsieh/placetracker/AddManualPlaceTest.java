package com.michaelhsieh.placetracker;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
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
    // January 1, 2100 at 6:00 pm
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

        typeNotes();

        clickAddManualPlaceButton();

        clickPlaceItem_OpensManualDetailActivity();
    }

    private void addAndUpdateManualVisit() {
        /*// scroll to add visit button and add visit
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
                .check(matches(atPosition(POS_NEW_VISIT, hasDescendant(withText(time)))));*/
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

        // scroll to notes EditText
        onView(withId(R.id.et_notes))
                .perform(scrollTo());
        onView(withId(R.id.et_notes)).check(matches(isDisplayed()));
        // check notes displays correct text
        onView(withId(R.id.et_notes)).check(matches(withText(NOTES)));

        // go back
        Espresso.pressBack();
    }
}
