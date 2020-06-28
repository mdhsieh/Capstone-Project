/*package com.michaelhsieh.placetracker;

import android.view.View;
import android.widget.TextView;

import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;

public class VisitViewHolder extends ChildViewHolder implements View.OnClickListener {

    private TextView visitDate;
    private TextView visitTime;

//    Since the group and child ViewHolders of the ExpandingRecyclerView are
//    in classes separate from VisitGroupAdapter, the click listener which
//    handles clicks to visits will be set up in VisitViewHolder only.
//
//    Source:
//    MLProgrammer-CiM
//    https://stackoverflow.com/questions/24885223/why-doesnt-recyclerview-have-onitemclicklistener
    private IVisitViewHolderClicks clickListener;

    public VisitViewHolder(View itemView, IVisitViewHolderClicks clickListener) {
        super(itemView);
        visitDate = itemView.findViewById(R.id.list_item_visit_date);
        visitTime = itemView.findViewById(R.id.list_item_visit_time);
        this.clickListener = clickListener;
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

    // parent activity will implement this method to respond to click events
    public interface IVisitViewHolderClicks {
        void onItemClick(View view, int position);
    }
}*/