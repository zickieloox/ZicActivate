package com.zic.activate.util;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.zic.activate.model.MyApplication.MSG_SPAM;

public class NetUtils {

    private static final String TAG = "NetUtils";

    public static String getHtml(String url) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30000, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response;
        try {
            response = client.newCall(request).execute();

            String body = response.body().string();
            if (response.isSuccessful()) {
                return body;
            } else {
                Log.e(TAG, "getHtml: " + "Unexpected code " + response);
                Log.e(TAG, "getHtml: " + "Response Body " + body);
            }
        } catch (IOException e) {
            Log.e(TAG, "getHtml: " + e.toString());
        }

        return null;
    }

    public static String putFormBody(String url, String token, FormBody formBody) {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(40000, TimeUnit.MILLISECONDS)
                .readTimeout(40000, TimeUnit.MILLISECONDS)
                .writeTimeout(40000, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(false)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", token)
                .put(formBody)
                .build();

        Response response;
        try {
            response = client.newCall(request).execute();

            String body = response.body().string();
            if (response.isSuccessful()) {
                return body;
            } else {
                Log.e(TAG, "putFormBody: " + "Unexpected code " + response);
                Log.e(TAG, "putFormBody: " + "Response Body " + body);
                if (body.contains(MSG_SPAM)) {
                    return MSG_SPAM;
                } else {
                    return null;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "put: " + e.toString());
            return null;
        }
    }
}
