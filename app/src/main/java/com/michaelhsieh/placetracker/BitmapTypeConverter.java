/*
package com.michaelhsieh.placetracker;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import androidx.room.TypeConverter;

public class BitmapTypeConverter {
    private static final String TAG = BitmapTypeConverter.class.getSimpleName();

    private static Gson gson =  new Gson();

//    private static final int WIDTH_PX = 100;
//    private static final int HEIGHT_PX = 100;

    @TypeConverter
    public static List<Bitmap> storedStringToBitmaps(String data) {
        if (data == null) {
            Log.d(TAG, "storedStringToBitmap: creating empty list");
            return Collections.emptyList();

//            Log.d(TAG, "storedStringToBitmap: data null, creating empty bitmap");
//            // create an empty bitmap
//            Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
//            return Bitmap.createBitmap(WIDTH_PX, HEIGHT_PX, conf);
        }
        Type listType = new TypeToken<List<Bitmap>>() {}.getType();
        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String bitmapsToStoredString(List<Bitmap> myObjects) {
        return gson.toJson(myObjects);
    }
}
*/
