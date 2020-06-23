package com.michaelhsieh.placetracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Adapter to populate Views in each row of RecyclerView with data.
 *
 * Source:
 * Suragch
 * https://stackoverflow.com/questions/40584424/simple-android-recyclerview-example
 *
 */
public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    // TODO: change String to Place model class
    private List<String> places;
    private LayoutInflater inflater;
    private ItemClickListener clickListener;

    // data is passed into the constructor
    public PlaceAdapter(Context context, List<String> places) {
        this.inflater = LayoutInflater.from(context);
        this.places = places;
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
        String place = places.get(position);
        holder.nameDisplay.setText(place);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return places.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView nameDisplay;

        public ViewHolder(View itemView) {
            super(itemView);
            nameDisplay = itemView.findViewById(R.id.tv_name);
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
    String getItem(int id) {
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
