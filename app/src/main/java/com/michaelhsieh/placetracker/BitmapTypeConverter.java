package com.michaelhsieh.placetracker;

import android.graphics.Bitmap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import androidx.room.TypeConverter;

/** Converts a list of Bitmaps into a JSON String that can
 *  be saved into PlaceRoomDatabase.
 *
 *  The Room database can't save Lists.
 */
public class BitmapTypeConverter {
    private static Gson gson =  new Gson();

    @TypeConverter
    public static List<Bitmap> storedStringToBitmap(String data) {
        if (data == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<Bitmap>>() {}.getType();
        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String bitmapToStoredString(List<Bitmap> myObjects) {
        return gson.toJson(myObjects);
    }
}
