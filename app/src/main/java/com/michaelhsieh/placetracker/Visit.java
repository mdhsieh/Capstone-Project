package com.michaelhsieh.placetracker;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

// each Visit contains a Calendar, date, and time
// ExpandableRecyclerView library requires implementing Parcelable or error occurs
public class Visit implements Parcelable {

    // used to get date and time Strings which are displayed to user,
    // and also to update DatePicker and TimePicker to clicked Visit's date and time
    private Calendar calendar;
    private String date;
    private String time;

    // public Visit(String date, String time) {
    public Visit(Calendar calendar) {
        this.calendar = calendar;
        // Date object representing this Calendar's time value
        Date date = calendar.getTime();
        // format date to show only hours, minutes, and AM/PM
        this.time = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
        // format date to show day of week, month, day, year
        this.date = DateFormat.getDateInstance(DateFormat.FULL).format(date);
        // this.date = date;
        // this.time = time;
    }

    /** Get the Calendar used to display this Visit's date and time to the user.
     *
     * @return The Calendar used by this Visit
     */
    public Calendar getCalendar() {
        return calendar;
    }


    /** Get the date from Calendar's Date object, ex. Saturday. July 4, 2020.
     *
     * @return A String containing the day of week, month, day, and year
     */
    public String getDate() {
        return date;
    }

    /** Get the time from Calendar's Date object, ex. 6:35 PM.
     *
     * @return A String containing hours, minutes, and AM/PM
     */
    public String getTime() {
        return time;
    }

    private Visit(Parcel in) {
        // Calendar implements Serializable
        calendar = (Calendar) in.readSerializable();
        date = in.readString();
        time = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(calendar);
        dest.writeString(date);
        dest.writeString(time);
    }

    public static final Creator<Visit> CREATOR = new Creator<Visit>() {
        @Override
        public Visit createFromParcel(Parcel in) {
            return new Visit(in);
        }

        @Override
        public Visit[] newArray(int size) {
            return new Visit[size];
        }
    };
}
