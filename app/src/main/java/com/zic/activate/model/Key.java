package com.zic.activate.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Key {

    private String value;
    private String note = "";
    private long lastModified = 0;

    public Key(String value, String note, Long lastModified) {
        this.value = value;
        this.note = note;
        this.lastModified = lastModified;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getDate() {
        if (lastModified == 0) {
            return null;
        } else {
            String dateFormat = "HH:mm:ss dd/MM/yyyy";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.getDefault());

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(lastModified);
            return simpleDateFormat.format(calendar.getTime());
        }
    }

    @Override
    public String toString() {
        return value + "|" + String.valueOf(lastModified) + "|" + note;
    }
}
