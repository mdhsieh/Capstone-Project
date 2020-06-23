package com.michaelhsieh.placetracker.model;

/** Model class to hold place information.
 * This is named PlaceModel to avoid confusion with the existing Place class in the
 * Google Places SDK.
 */

public class PlaceModel {
    // a place's unique Place ID
    private String id;
    private String name;
    private String address;

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
}
