package com.michaelhsieh.placetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;

import java.util.List;

import static android.view.animation.Animation.RELATIVE_TO_SELF;

public class VisitGroupAdapter extends ExpandableRecyclerViewAdapter<VisitGroupAdapter.VisitGroupViewHolder, VisitGroupAdapter.VisitViewHolder> {

    /* There's only one parent, which is at position 0 of the adapter position.
    * Its first child will be at position 1. */
    private static final int NUM_VISIT_GROUPS = 1;

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
                /* Subtract by 1 to get the correct adapter position of the Visit clicked.
                * Since position 0 is already occupied by the VisitGroup parent, the first Visit
                * is really at adapter position 1.
                * Using getAdapterPosition() by itself will cause an IndexOutOfBoundsException. */
                clickListener.onItemClick(view, getAdapterPosition() - NUM_VISIT_GROUPS);
            }
        }
    }


    // moved VisitGroupViewHolder into same file as VisitGroupAdapter so
    // both ViewHolders are inside the Adapter
    public class VisitGroupViewHolder extends GroupViewHolder {

        private TextView visitGroupName;
        private ImageView arrow;

        public VisitGroupViewHolder(View itemView) {
            super(itemView);
            visitGroupName = itemView.findViewById(R.id.list_item_visit_group_name);
            arrow = itemView.findViewById(R.id.list_item_visit_group_arrow);
        }

        public void setVisitGroupTitle(ExpandableGroup visitGroup) {
            if (visitGroup instanceof VisitGroup) {
                visitGroupName.setText(visitGroup.getTitle());
            }
        }

        @Override
        public void expand() {
            animateExpand();
        }

        @Override
        public void collapse() {
            animateCollapse();
        }

        private void animateExpand() {
            RotateAnimation rotate =
                    new RotateAnimation(360, 180, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(300);
            rotate.setFillAfter(true);
            arrow.setAnimation(rotate);
        }

        private void animateCollapse() {
            RotateAnimation rotate =
                    new RotateAnimation(180, 360, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(300);
            rotate.setFillAfter(true);
            arrow.setAnimation(rotate);
        }
    }

}
