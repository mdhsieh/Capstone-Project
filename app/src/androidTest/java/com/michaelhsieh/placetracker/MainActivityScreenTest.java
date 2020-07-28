package com.michaelhsieh.placetracker;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

/**
 * This test demos a user searching a place and adding it to his or her list, then
 * clicking on the place in MainActivity which opens up the corresponding DetailActivity.
 *
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityScreenTest {

    public static final String PLACE_NAME = "Sweet Tomatoes";

    @Rule
    public ActivityTestRule activityTestRule = new ActivityTestRule<>(
            MainActivity.class);

    // check the search bar, empty list message, and RecyclerView are visible
   /* @Test
    public void mainActivityStarted() {
        onView(withId(R.id.cardView)).check(matches(isDisplayed()));
        onView(withId(R.id.rv_places)).check(matches(isDisplayed()));
        onView(withId(R.id.tv_empty_list)).check(matches(isDisplayed()));
        onView(withId(R.id.tv_empty_list)).check(matches(withText(R.string.empty_list)));
    }*/

    // searches for a place and adds first autocomplete entry to user's places list
    @Test
    public void searchPlace() {
        onView(withId(R.id.cardView)).perform(click());
        onView(withId(R.id.autocomplete_fragment)).perform(typeText(PLACE_NAME));
    }
}
