package com.zic.activate.util;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static String millisToDate(Long lastModified) {
        String dateFormat = "ddMMyy_HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.getDefault());

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(lastModified);
        return simpleDateFormat.format(calendar.getTime()).trim();
    }

    public static String readTextFromFile(String filePath) {
        File file = new File(filePath);
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append(System.getProperty("line.separator"));
            }
            br.close();
        } catch (IOException e) {
            Log.e(TAG, "readTextFromFile: " + e.toString());
        }

        return text.toString().trim();
    }

    public static void writeToFile(String path, String fileName, String text) {

        File filePath = new File(path);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }

        final File file = new File(filePath, fileName);
        try {
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            writer.append(text);

            writer.close();
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "writeToFile: " + e.toString());
        }
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
