package com.michaelhsieh.placetracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import androidx.room.TypeConverter;

/** Converts a list of Base64 Strings into a JSON String that can
 *  be saved into PlaceRoomDatabase.
 *
 *  The Room database can't save Lists.
 */
public class Base64StringTypeConverter {
    private static Gson gson =  new Gson();

    @TypeConverter
    public static List<String> storedStringToBase64Strings(String data) {
        if (data == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String base64StringsToStoredString(List<String> myObjects) {
        return gson.toJson(myObjects);
    }
}
