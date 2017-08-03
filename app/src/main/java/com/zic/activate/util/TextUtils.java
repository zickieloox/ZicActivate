package com.zic.activate.util;

import android.util.Base64;
import android.util.Log;

import com.zic.activate.model.Key;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.ContentValues.TAG;

public class TextUtils {

    public static List<Key> sortKeys(List<Key> keys) {
        // Sort the $keys by last modified date
        Collections.sort(keys, new Comparator<Key>() {
            @Override
            public int compare(Key o1, Key o2) {
                return String.valueOf(o2.getLastModified()).compareTo(String.valueOf(o1.getLastModified()));
            }
        });

        return keys;
    }

    public static String getKeyListString(List<Key> keys) {
        String keyListString = "";
        for (Key key : keys) {
            keyListString += key.toString() + System.getProperty("line.separator");
        }

        return keyListString.trim();
    }

    public static String encode(String text) {
        byte[] data = new byte[0];
        try {
            data = text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "encode: " + e.toString());
            return null;
        }

        return Base64.encodeToString(data, Base64.DEFAULT);

    }

    public static String decode(String base64) {
        byte[] data = Base64.decode(base64, Base64.DEFAULT);

        String text;
        try {
            text = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "decode: " + e.toString());
            return null;
        }

        return text;
    }
}
