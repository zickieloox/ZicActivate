package com.zic.activate.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.zic.activate.R;
import com.zic.activate.adapter.KeyAdapter;
import com.zic.activate.dialog.EditKeyDialog;
import com.zic.activate.listener.OnKeyAdapterListener;
import com.zic.activate.model.Key;
import com.zic.activate.model.MyApplication;
import com.zic.activate.util.NetUtils;
import com.zic.activate.util.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.FormBody;

import static com.zic.activate.model.MyApplication.FORM_KEY_CONTENT;
import static com.zic.activate.model.MyApplication.GITLAB_TOKEN;
import static com.zic.activate.model.MyApplication.MSG_SPAM;
import static com.zic.activate.model.MyApplication.SNIPPET_RAW_URL;
import static com.zic.activate.model.MyApplication.SNIPPET_URL;
import static com.zic.activate.util.NetUtils.putFormBody;

public class KeyListFragment extends Fragment implements OnKeyAdapterListener {

    //private static final String TAG = "KeyListFragment";

    private MyApplication my;

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView keyRecycler;
    private KeyAdapter keyAdapter;

    private MaterialDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_key_list, container, false);

        my = (MyApplication) getActivity().getApplication();

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        keyRecycler = (RecyclerView) view.findViewById(R.id.recycler_key);

        keyRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        List<Key> keys = new ArrayList<>();
        keyAdapter = new KeyAdapter(getActivity(), keys);
        keyRecycler.setAdapter(keyAdapter);
        setHasOptionsMenu(true);

        loadData();
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadData();
            }
        });

        return view;
    }

    private void loadData() {
        refreshLayout.setRefreshing(true);
        new LoadingDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setupAdapter(List<Key> keys) {
        keyAdapter = new KeyAdapter(getActivity(), keys);
        keyAdapter.setOnKeyAdapterListener(this);
        keyRecycler.setAdapter(keyAdapter);
    }

    @Override
    public void onItemClick(int position) {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("key", my.getKeyList().get(position).getValue());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), String.valueOf(position) + " - copied!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void OnDeleteIconClick(final int position) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.dialog_content_sure)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        new DeleteKeyTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, position);
                        progressDialog = new MaterialDialog.Builder(getActivity())
                                .content(R.string.dialog_content_working)
                                .progress(true, 0)
                                .progressIndeterminateStyle(true)
                                .build();
                        progressDialog.setCancelable(false);
                        progressDialog.setCanceledOnTouchOutside(false);
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
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public void onEditIconClick(int position) {
        String pos = String.valueOf(position);
        List<Key> keyList = new ArrayList<>();
        keyList.addAll(my.getKeyList());
        Key key = keyList.get(position);
        String keyValue = key.getValue();
        String note = key.getNote();
        String lastModified = String.valueOf(System.currentTimeMillis());

        EditKeyDialog.create(pos, keyValue, lastModified, note).show(getActivity().getSupportFragmentManager(), "edit_key_dialog");
    }

    private class LoadingDataTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            String result = null;
            Activity activity = getActivity();
            if (isAdded() && activity != null) {
                result = NetUtils.getHtml(SNIPPET_RAW_URL);
            }

            return result;
        }

        @Override
        protected void onPostExecute(String keyList) {
            refreshLayout.setRefreshing(false);

            if (keyList == null) {
                Toast.makeText(getActivity(), getString(R.string.toast_err_internet), Toast.LENGTH_SHORT).show();
                return;
            }

            getKeysFromString(keyList.trim());
        }
    }

    private void getKeysFromString(String keyList) {
        List<Key> keys = new ArrayList<>();

        // !important
        keys.clear();

        String[] lines = keyList.split(System.getProperty("line.separator"));
        int length = lines.length;
        //Toast.makeText(getActivity(), String.valueOf(length), Toast.LENGTH_SHORT).show();
        if (length == 0) {
            Toast.makeText(getActivity(), getString(R.string.toast_empty_list), Toast.LENGTH_SHORT).show();
            return;
        }

        for (String line : lines) {
            String[] data = line.split(Pattern.quote("|"));

            int len = data.length;
            if (len >= 3) {
                keys.add(new Key(data[0], data[2], Long.parseLong(data[1])));
            } else {
                keys.add(new Key(data[0], "", Long.parseLong(data[1])));
            }
        }

        List<Key> sortedKeys = TextUtils.sortKeys(keys);
        my.setKeyList(sortedKeys);

        setupAdapter(sortedKeys);
    }

    private class DeleteKeyTask extends AsyncTask<Integer, Void, Integer> {
        private List<Key> newKeyList = new ArrayList<>();

        @Override
        protected Integer doInBackground(Integer... integers) {
            int pos = integers[0];

            newKeyList.addAll(my.getKeyList());
            newKeyList.remove(pos);
            String newKeyListString = TextUtils.getKeyListString(newKeyList);

            FormBody formBody = new FormBody.Builder()
                    .add(FORM_KEY_CONTENT, newKeyListString)
                    .build();

            String result = putFormBody(SNIPPET_URL, GITLAB_TOKEN, formBody);
            if (result == null) {
                return -1;
            } else if (result.equals(MSG_SPAM)) {
                publishProgress();
                cancel(true);
            }

            return integers[0];
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            progressDialog.dismiss();
            Toast.makeText(getActivity(), getString(R.string.toast_spam), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(Integer pos) {
            progressDialog.dismiss();

            if (pos == -1) {
                Toast.makeText(getContext(), getString(R.string.toast_error), Toast.LENGTH_SHORT).show();
                return;
            }

//            keyAdapter.removeItem(pos);

            my.setKeyList(newKeyList);
            Toast.makeText(getActivity(), getString(R.string.toast_deleted), Toast.LENGTH_SHORT).show();
            getActivity().recreate();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (menu != null) {
            MenuItem searchItem = menu.findItem(R.id.action_search);
            menu.findItem(R.id.action_notes).setVisible(true);

            searchItem.setVisible(true);
            final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    keyAdapter.filter(query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    keyAdapter.filter(newText);
                    return false;
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_notes:
                keyAdapter.filterNotes();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
