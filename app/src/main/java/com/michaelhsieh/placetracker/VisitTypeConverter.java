package com.michaelhsieh.placetracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import androidx.room.TypeConverter;

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
