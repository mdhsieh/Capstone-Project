package com.michaelhsieh.placetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;

import java.util.List;

public class VisitGroupAdapter extends ExpandableRecyclerViewAdapter<VisitGroupViewHolder, VisitGroupAdapter.VisitViewHolder> {

    private VisitItemClickListener clickListener;

    public VisitGroupAdapter(List<? extends ExpandableGroup> groups) {
        super(groups);
    }

    @Override
    public VisitGroupViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.visit_group_list_item, parent, false);
        return new VisitGroupViewHolder(view);
    }

    @Override
    public VisitViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.visit_list_item, parent, false);
        return new VisitViewHolder(view);
    }

    @Override
    public void onBindChildViewHolder(VisitViewHolder holder, int flatPosition, ExpandableGroup group, int childIndex) {
        final Visit visit = ((VisitGroup) group).getItems().get(childIndex);
        holder.setVisitDate(visit.getDate());
        holder.setVisitTime(visit.getTime());
    }

    @Override
    public void onBindGroupViewHolder(VisitGroupViewHolder holder, int flatPosition, ExpandableGroup group) {
        holder.setVisitGroupTitle(group);
    }


    // allows click events to be caught
    public void setClickListener(VisitItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface VisitItemClickListener {
        void onItemClick(View view, int position);
    }

    // moved VisitViewHolder into the same file as VisitGroupAdapter to allow
    // click listener to be set up
    public class VisitViewHolder extends ChildViewHolder implements View.OnClickListener {
        private TextView visitDate;
        private TextView visitTime;

        public VisitViewHolder(View itemView) {
            super(itemView);
            visitDate = itemView.findViewById(R.id.list_item_visit_date);
            visitTime = itemView.findViewById(R.id.list_item_visit_time);
            itemView.setOnClickListener(this);
        }

        public void setVisitDate(String date) {
            visitDate.setText(date);
        }

        public void setVisitTime(String time) {
            visitTime.setText(time);
        }

        @Override
        public void onClick(View view) {
            if (clickListener != null) {
                clickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }
}
