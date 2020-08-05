package com.michaelhsieh.placetracker.expandablegroup;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.michaelhsieh.placetracker.R;
import com.michaelhsieh.placetracker.StartDragListener;
import com.michaelhsieh.placetracker.models.Visit;
import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;

import java.util.List;

import static android.view.animation.Animation.RELATIVE_TO_SELF;

public class VisitGroupAdapter extends ExpandableRecyclerViewAdapter<VisitGroupAdapter.VisitGroupViewHolder, VisitGroupAdapter.VisitViewHolder> {

    private VisitItemClickListener clickListener;

    // listener used when user touches drag handle
    private StartDragListener startDragListener;
    // track whether drag handles should be visible or not
    private boolean isHandleVisible = false;

    public VisitGroupAdapter(List<? extends ExpandableGroup> groups, StartDragListener startDragListener) {
        super(groups);
        this.startDragListener = startDragListener;
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

        // set drag handle visibility
        if (isHandleVisible) {
            holder.dragHandle.setVisibility(View.VISIBLE);
        } else {
            holder.dragHandle.setVisibility(View.GONE);
        }

        // start drag when handle clicked
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

    // moved VisitViewHolder inside VisitGroupAdapter to allow
    // click listener to be set up
    public class VisitViewHolder extends ChildViewHolder implements View.OnClickListener {
        private TextView visitDate;
        private TextView visitTime;
        ImageView dragHandle;

        public VisitViewHolder(View itemView) {
            super(itemView);
            visitDate = itemView.findViewById(R.id.list_item_visit_date);
            visitTime = itemView.findViewById(R.id.list_item_visit_time);
            dragHandle = itemView.findViewById(R.id.iv_drag_handle);
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

    public void setHandleVisible(boolean isHandleVisible) {
        this.isHandleVisible = isHandleVisible;
    }


    // moved VisitGroupViewHolder inside VisitGroupAdapter so
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
