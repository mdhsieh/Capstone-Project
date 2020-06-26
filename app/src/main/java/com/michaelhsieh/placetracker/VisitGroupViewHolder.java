package com.michaelhsieh.placetracker;

import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;

import static android.view.animation.Animation.RELATIVE_TO_SELF;

public class VisitGroupViewHolder extends GroupViewHolder {

    private TextView visitGroupName;
    private ImageView arrow;

    public VisitGroupViewHolder(View itemView) {
        super(itemView);
        visitGroupName = (TextView) itemView.findViewById(R.id.list_item_visit_group_name);
        arrow = (ImageView) itemView.findViewById(R.id.list_item_visit_group_arrow);
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
