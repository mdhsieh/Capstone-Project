package com.michaelhsieh.placetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import java.util.List;

public class VisitGroupAdapter extends ExpandableRecyclerViewAdapter<VisitGroupViewHolder, VisitViewHolder> {

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
}
