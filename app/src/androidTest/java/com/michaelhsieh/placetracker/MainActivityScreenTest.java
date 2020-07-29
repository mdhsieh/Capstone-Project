package com.michaelhsieh.placetracker;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * This test demos a user clicking on a place in MainActivity which
 * opens up the corresponding DetailActivity.
 *
 * <p></p>
 *
 * The place must already be added to the RecyclerView,
 * ex. from first running {@link MainActivitySearchPlaceTest}.
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

    private static final String NOTES = "This is a buffet restaurant.\nThey have a salad bar" +
            " and soups.";

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

        checkInitialDetails();

        addNotes();
    }

    /** When a new place's DetailActivity is started, check details are correct.
     *
     */
    private void checkInitialDetails() {
        // now in DetailActivity, check the name and address are correct
        onView(withId(R.id.et_name)).check(matches(withText(PLACE_NAME)));
        onView(withId(R.id.et_address)).check(matches(withText(PLACE_ADDRESS)));

        // check that number of visits label and add visit button are visible to user
        onView(withId(R.id.tv_label_num_visits)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_add_visit)).check(matches(isDisplayed()));

        // check that number of visits label and add visit button display correct text
        onView(withId(R.id.tv_label_num_visits)).check(matches(withText(R.string.num_visits_label)));
        onView(withId(R.id.btn_add_visit)).check(matches(withText(R.string.add_visit)));
        // assuming no visits added yet, check number of visits is 0
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

        // assuming notes empty, check notes EditText displays correct hint
        onView(withId(R.id.et_notes)).check(matches(withHint(R.string.notes_hint)));

        // check save and delete button display correct text
        onView(withId(R.id.btn_delete)).check(matches(withText(R.string.delete)));
        onView(withId(R.id.btn_save)).check(matches(withText(R.string.save)));
    }

    /** Add notes in DetailActivity and save.
     *
     */
    private void addNotes() {
        // scroll to notes EditText to make sure Espresso can type text in it
        onView(withId(R.id.et_notes))
                .perform(scrollTo());
        onView(withId(R.id.et_notes)).perform(typeText(NOTES));

        // scroll to save button to make sure Espresso can click it
        onView(withId(R.id.btn_save))
                .perform(scrollTo());
        onView(withId(R.id.btn_save)).perform(click());
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
