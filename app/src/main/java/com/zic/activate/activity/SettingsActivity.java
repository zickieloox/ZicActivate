package com.zic.activate.activity;

import android.Manifest;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.google.gson.Gson;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.zic.activate.R;
import com.zic.activate.model.Key;
import com.zic.activate.model.MyApplication;
import com.zic.activate.util.FileUtils;
import com.zic.activate.util.NetUtils;
import com.zic.activate.util.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.FormBody;

import static com.zic.activate.model.MyApplication.FORM_KEY_CONTENT;
import static com.zic.activate.model.MyApplication.GITLAB_TOKEN;
import static com.zic.activate.model.MyApplication.MSG_SPAM;
import static com.zic.activate.model.MyApplication.SNIPPET_URL;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, PermissionListener, FileChooserDialog.FileCallback {

    private MyApplication my;
    private String sdcard;
    private List<Key> mKeys;
    private MaterialDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        my = (MyApplication) getApplication();
        sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        Button btnBackup = (Button) findViewById(R.id.btn_backup);
        Button btnRestore = (Button) findViewById(R.id.btn_restore);
        setSupportActionBar(toolbar);
        tabLayout.setVisibility(View.GONE);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.action_settings);

        btnBackup.setOnClickListener(this);
        btnRestore.setOnClickListener(this);
    }

    private void requestPerms() {
        new TedPermission(this)
                .setPermissionListener(this)
                .setDeniedMessage(getString(R.string.denied_message))
                .setGotoSettingButtonText(getString(R.string.btn_go_settings))
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.btn_backup:
                requestPerms();
                break;
            case R.id.btn_restore:
                restoreKeys();
                break;
        }
    }

    private void backupKeys() {
        List<Key> keys = new ArrayList<>();
        keys.addAll(my.getKeyList());

        if (keys.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_error_data), Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        String keysJson = gson.toJson(keys).trim();
        keysJson = TextUtils.encode(keysJson);

        String path = sdcard + File.separator + "ZicBackup";
        String fileName = FileUtils.millisToDate(System.currentTimeMillis()) + ".txt";
        if (!FileUtils.isExternalStorageWritable()) {
            Toast.makeText(this, R.string.toast_error_writable, Toast.LENGTH_SHORT).show();
            return;
        }
        FileUtils.writeToFile(path, fileName, keysJson);

        Toast.makeText(this, R.string.toast_backedup, Toast.LENGTH_SHORT).show();
    }

    private void restoreKeys() {
        if (!FileUtils.isExternalStorageWritable()) {
            Toast.makeText(this, R.string.toast_error_writable, Toast.LENGTH_SHORT).show();
            return;
        }

        new FileChooserDialog.Builder(this)
                .initialPath(sdcard + File.separator + "ZicBackup")
                .mimeType("text/plain")
                .extensionsFilter(".txt")
                .tag("restore")
                .show();
    }

    @Override
    public void onPermissionGranted() {
        backupKeys();
    }

    @Override
    public void onPermissionDenied(ArrayList<String> deniedPermissions) {
        Toast.makeText(getApplicationContext(), getString(R.string.toast_perm_denied) + "\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
        String keysJson = FileUtils.readTextFromFile(file.getAbsolutePath());
        keysJson = TextUtils.decode(keysJson);

        Gson gson = new Gson();
        Key[] keyArr = gson.fromJson(keysJson, Key[].class);
        mKeys = Arrays.asList(keyArr);

        final String keyListString = TextUtils.getKeyListString(mKeys);

        final MaterialDialog confirmDialog = new MaterialDialog.Builder(this)
                .content(R.string.dialog_content_sure)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        new UpdateKeyListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, keyListString);
                        progressDialog = new MaterialDialog.Builder(SettingsActivity.this)
                                .content(R.string.dialog_content_working)
                                .progress(true, 0)
                                .progressIndeterminateStyle(false)
                                .build();
                        progressDialog.setCancelable(false);
                        progressDialog.setCanceledOnTouchOutside(false);
                        dialog.dismiss();
                        progressDialog.show();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .build();
        confirmDialog.setCanceledOnTouchOutside(false);
        confirmDialog.show();
        dialog.dismiss();
    }

    @Override
    public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private class UpdateKeyListTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String newKeyListString = strings[0];

            FormBody formBody = new FormBody.Builder()
                    .add(FORM_KEY_CONTENT, newKeyListString)
                    .build();

            return NetUtils.putFormBody(SNIPPET_URL, GITLAB_TOKEN, formBody);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();

            if (result == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_error), Toast.LENGTH_SHORT).show();
                return;
            } else if (result.equals(MSG_SPAM)) {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_spam), Toast.LENGTH_SHORT).show();
                return;
            }

            my.setKeyList(mKeys);
            Toast.makeText(getApplicationContext(), getString(R.string.toast_restored), Toast.LENGTH_SHORT).show();
            finish();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));

        }
    }
}
