package com.michaelhsieh.placetracker.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.michaelhsieh.placetracker.R;
import com.michaelhsieh.placetracker.StartDragListener;
import com.michaelhsieh.placetracker.models.PlaceModel;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Adapter to populate Views in each row of RecyclerView with data.
 * <p></p>
 * Source:
 * Suragch
 * https://stackoverflow.com/questions/40584424/simple-android-recyclerview-example
 *
 */
public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    // if not initialized, place is null before first LiveData update
    private List<PlaceModel> places;
    private LayoutInflater inflater;
    private ItemClickListener clickListener;
    // listener used when user touches drag handle
    private StartDragListener startDragListener;

    // data is passed into the constructor
    public PlaceAdapter(Context context, List<PlaceModel> places, StartDragListener startDragListener) {
        this.inflater = LayoutInflater.from(context);
        this.places = places;
        this.startDragListener = startDragListener;
    }

    // inflates the row layout from XML when needed
    @NonNull
    @Override
    public PlaceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.place_list_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(@NonNull PlaceAdapter.ViewHolder holder, int position) {
        PlaceModel place = places.get(position);
        holder.nameDisplay.setText(place.getName());
        holder.addressDisplay.setText(place.getAddress());

        // drag and drop when handle clicked
        holder.dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    startDragListener.requestDrag(holder);
                }
                return false;
            }
        });
    }

    // total number of rows
    @Override
    public int getItemCount() {
        // when getItemCount() is first called, places has not been updated.
        // Initially, places is null, and we can't return null.
        if (places == null) {
            return 0;
        } else {
            return places.size();
        }
    }

    /** Set the adapter data to a list of PlaceModels
     *
     * @param newPlaces the list of places observed
     * by LiveData
     */
    public void setPlaces(List<PlaceModel> newPlaces){
        places = newPlaces;
        notifyDataSetChanged();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView nameDisplay;
        TextView addressDisplay;
        ImageView dragHandle;

        public ViewHolder(View itemView) {
            super(itemView);
            nameDisplay = itemView.findViewById(R.id.tv_name);
            addressDisplay = itemView.findViewById(R.id.tv_address);
            dragHandle = itemView.findViewById(R.id.iv_drag_handle);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (clickListener != null) {
                clickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }

    // convenience method for getting data at click position
    PlaceModel getItem(int id) {
        return places.get(id);
    }

    // allows click events to be caught
    public void setClickListener(ItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
