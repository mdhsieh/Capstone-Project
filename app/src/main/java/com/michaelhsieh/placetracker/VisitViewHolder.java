package com.michaelhsieh.placetracker;

import android.view.View;
import android.widget.TextView;

import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;

public class VisitViewHolder extends ChildViewHolder {

    private TextView visitDate;
    private TextView visitTime;

    public VisitViewHolder(View itemView) {
        super(itemView);
        visitDate = itemView.findViewById(R.id.list_item_visit_date);
        visitTime = itemView.findViewById(R.id.list_item_visit_time);
    }

    public void setVisitDate(String date) {
        visitDate.setText(date);
    }

    public void setVisitTime(String time) {
        visitTime.setText(time);
    }
}
