package com.michaelhsieh.placetracker.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.michaelhsieh.placetracker.Visit;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Model class to hold place information. This is also the Entity for the Room database.
 * This is named PlaceModel to avoid confusion with the existing Place class in the
 * Google Places SDK.
 */

// implement Parcelable to pass PlaceModel to detail screen using Intent
/* Source: Jeremy Logan
https://stackoverflow.com/questions/2139134/how-to-send-an-object-from-one-android-activity-to-another-using-intents?page=1&tab=votes#tab-top
 */
@Entity(tableName = "place_table")
public class PlaceModel implements Parcelable {
    // a place's unique Place ID, which is a String
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "place_id")
    private String placeId;
    private String name;
    private String address;
    // notes the user writes about the place
    // default empty text
    private String notes = "";

    // each visit contains a day and a time
    // initialize visits
    private List<Visit> visits = new ArrayList<>();

    /* The first photo available from the place.
       Photos of the place are Bitmaps, but store the Bitmap as a Base64 String
       in Room database to avoid transferring too much data
       between Activities via Parcelable,
       ex. large Bitmaps, and causing TransactionTooLarge exception,
       which will crash the app. */
    private String base64String;

    public PlaceModel(String placeId, String name, String address) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
    }

    // Room requires all fields to have getters and setters
    public String getPlaceId() {
        return placeId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getNumVisits() {
        return visits.size();
    }

    public String getNotes() {
        return notes;
    }

    public List<Visit> getVisits() {
        return visits;
    }

    public String getBase64String() {
        return base64String;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setVisits(List<Visit> visits) {
        this.visits = visits;
    }

    public void setBase64String(String base64String) {
        this.base64String = base64String;
    }

    /* everything below here is for implementing Parcelable */
    @Override
    public int describeContents() {
        return 0;
    }

    // write the object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(placeId);
        out.writeString(name);
        out.writeString(address);
        out.writeList(visits);
        out.writeString(notes);
        out.writeString(base64String);
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
        placeId = in.readString();
        name = in.readString();
        address = in.readString();
        in.readList(visits, Visit.class.getClassLoader());
        notes = in.readString();
        base64String = in.readString();
    }
}
