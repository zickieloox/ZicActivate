package com.zic.activate.model;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

    public static final int KEY_LENGTH = 32;
    public static final int NOTE_LENGTH = 60;

    //    public static final String GITLAB_USER_URL = "https://gitlab.com/api/v4/users/1496450";
    public static final String GITLAB_TOKEN = "EjKrDrvsTMykPnAhZpLw";
    public static final String SNIPPET_RAW_URL = "https://gitlab.com/tienkiemteam/activepro/snippets/1669619/raw";
    public static final String SNIPPET_URL = "https://gitlab.com/api/v4/projects/3802182/snippets/1669619";
    public static final String FORM_KEY_CONTENT = "code";

    public static final String MSG_SPAM = "Spam detected";

    private List<Key> keyList = new ArrayList<>();

    public List<Key> getKeyList() {
        return keyList;
    }

    public void setKeyList(List<Key> keyList) {
        this.keyList = keyList;
    }
}
