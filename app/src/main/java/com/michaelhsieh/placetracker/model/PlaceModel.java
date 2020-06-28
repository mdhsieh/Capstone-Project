package com.michaelhsieh.placetracker.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.michaelhsieh.placetracker.Visit;

import java.util.ArrayList;
import java.util.List;

/** Model class to hold place information.
 * This is named PlaceModel to avoid confusion with the existing Place class in the
 * Google Places SDK.
 */

// implement Parcelable to pass PlaceModel to detail screen using Intent
/* Source: Jeremy Logan
https://stackoverflow.com/questions/2139134/how-to-send-an-object-from-one-android-activity-to-another-using-intents?page=1&tab=votes#tab-top
 */
public class PlaceModel implements Parcelable {
    // a place's unique Place ID
    private String id;
    private String name;
    private String address;
    private int numVisits;
    // notes the user writes about the place
    // notes default empty text
    private String notes = "";
    // each visit contains a day and a time
    // initialize visits
    private List<Visit> visits = new ArrayList<>();

    public PlaceModel(String id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getNumVisits() {
        return numVisits;
    }

    public String getNotes() {
        return notes;
    }

    public List<Visit> getVisits() {
        return visits;
    }

    public void addVisit() {
        numVisits += 1;
    }

    /* everything below here is for implementing Parcelable */
    @Override
    public int describeContents() {
        return 0;
    }

    // write the object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(id);
        out.writeString(name);
        out.writeString(address);
        out.writeList(visits);
    }

    // This is used to regenerate the object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<PlaceModel> CREATOR = new Parcelable.Creator<PlaceModel>() {
        public PlaceModel createFromParcel(Parcel in) {
            return new PlaceModel(in);
        }

        public PlaceModel[] newArray(int size) {
            return new PlaceModel[size];
        }
    };

    /* In the case you have more than one field to retrieve from a given Parcel,
    you must do this in the same order you put them in (that is, in a FIFO approach). */

    // constructor that takes a Parcel and gives you an object populated with its values
    private PlaceModel(Parcel in) {
        id = in.readString();
        name = in.readString();
        address = in.readString();
        //in.readParcelableList(visits, Visit.class.getClassLoader());
        in.readList(visits, Visit.class.getClassLoader());
    }


}
