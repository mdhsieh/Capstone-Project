package com.michaelhsieh.placetracker;

import androidx.recyclerview.widget.RecyclerView;

/** Implemented in MainActivity and called in PlaceAdapter to
 * allow user to start dragging a place when its drag handle is clicked.
 */
public interface StartDragListener {
    void requestDrag(RecyclerView.ViewHolder viewHolder);
}
