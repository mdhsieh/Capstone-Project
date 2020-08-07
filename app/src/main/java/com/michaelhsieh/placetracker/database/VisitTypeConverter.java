package com.michaelhsieh.placetracker.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.michaelhsieh.placetracker.models.expandablegroup.Visit;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import androidx.room.TypeConverter;

/** Converts a list of Visits into a JSON String that can
 *  be saved into PlaceRoomDatabase.
 *
 *  The Room database can't save Lists.
 */
public class VisitTypeConverter {

    private static Gson gson =  new Gson();

    @TypeConverter
    public static List<Visit> storedStringToVisits(String data) {
        if (data == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<Visit>>() {}.getType();
        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String visitsToStoredString(List<Visit> myObjects) {
        return gson.toJson(myObjects);
    }
}
