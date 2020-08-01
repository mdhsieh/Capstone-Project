package com.michaelhsieh.placetracker;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import java.util.List;

/** There's only one group that holds all the Visits called VisitGroup.
 * There could be multiple groups, ex. in some other app a list of Genres
 * that each have a list of Artists, but not in this case.
 */
public class VisitGroup extends ExpandableGroup<Visit> {
    public VisitGroup(String title, List<Visit> items) {
        super(title, items);
    }
}
