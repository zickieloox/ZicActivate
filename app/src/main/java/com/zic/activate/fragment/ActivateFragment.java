package com.zic.activate.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.zic.activate.R;
import com.zic.activate.listener.OnActivateCompletedListener;
import com.zic.activate.model.Key;
import com.zic.activate.model.MyApplication;
import com.zic.activate.util.NetUtils;
import com.zic.activate.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

import okhttp3.FormBody;

import static android.content.Context.CLIPBOARD_SERVICE;
import static com.zic.activate.model.MyApplication.FORM_KEY_CONTENT;
import static com.zic.activate.model.MyApplication.GITLAB_TOKEN;
import static com.zic.activate.model.MyApplication.KEY_LENGTH;
import static com.zic.activate.model.MyApplication.MSG_SPAM;
import static com.zic.activate.model.MyApplication.NOTE_LENGTH;
import static com.zic.activate.model.MyApplication.SNIPPET_URL;

public class ActivateFragment extends Fragment implements View.OnClickListener {

    private MyApplication my;

    private TextInputLayout tilKeyValue, tilNote;
    private Button btnActivate;
    private MaterialDialog progressDialog;
    private OnActivateCompletedListener mListener;

    private List<Key> newKeyList = new ArrayList<>();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnActivateCompletedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnActivateCompletedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activate, container, false);

        my = ((MyApplication) getActivity().getApplication());

        Button btnGetKey = (Button) view.findViewById(R.id.btn_get_key);
        tilKeyValue = (TextInputLayout) view.findViewById(R.id.til_key_value);
        tilNote = (TextInputLayout) view.findViewById(R.id.til_note);
        btnActivate = (Button) view.findViewById(R.id.btn_activate);

        setupInput();

        btnGetKey.setOnClickListener(this);
        btnActivate.setOnClickListener(this);

        btnActivate.setEnabled(false);

        return view;
    }

    private void setupInput() {
        if (tilNote.getEditText() != null) {
            tilNote.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(NOTE_LENGTH)});
        }
        tilNote.setCounterEnabled(true);
        tilNote.setCounterMaxLength(NOTE_LENGTH);

        if (tilKeyValue.getEditText() != null) {
            tilKeyValue.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(KEY_LENGTH)});
        }
        tilKeyValue.setCounterEnabled(true);
        tilKeyValue.setCounterMaxLength(KEY_LENGTH);

        tilKeyValue.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 0) {
                    tilKeyValue.setErrorEnabled(true);
                    tilKeyValue.setError(getString(R.string.til_err_empty_key));
                    tilNote.setEnabled(false);
                    btnActivate.setEnabled(false);
                } else {
                    if (editable.toString().contains("|")) {
                        tilKeyValue.setErrorEnabled(true);
                        tilKeyValue.setError(getString(R.string.til_err_pipe));
                        tilNote.setEnabled(false);
                        btnActivate.setEnabled(false);
                    } else {
                        tilKeyValue.setErrorEnabled(false);
                        tilNote.setEnabled(true);
                        btnActivate.setEnabled(true);
                    }
                }
            }
        });
        tilNote.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence chars, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().contains("|")) {
                    tilNote.setErrorEnabled(true);
                    tilNote.setError(getString(R.string.til_err_pipe));
                    btnActivate.setEnabled(false);
                } else {
                    tilNote.setErrorEnabled(false);
                    tilNote.setError(null);
                    btnActivate.setEnabled(true);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch (id) {
            case R.id.btn_get_key:
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

                String clipText = item.getText().toString();

                if (clipText.length() == KEY_LENGTH) {
                    assert tilKeyValue.getEditText() != null;
                    tilKeyValue.getEditText().setText(clipText);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.toast_invalid), Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.btn_activate:
                btnActivate.setEnabled(false);
                activateKey();
                break;
        }
    }

    private void activateKey() {

        assert tilKeyValue.getEditText() != null;
        assert tilNote.getEditText() != null;
        String keyValue = tilKeyValue.getEditText().getText().toString().trim().toUpperCase();
        String note = tilNote.getEditText().getText().toString().trim();
        Long lastModified = System.currentTimeMillis();

        newKeyList.clear();
        newKeyList.addAll(my.getKeyList());
        if (newKeyList.isEmpty()) {
            Toast.makeText(getActivity(), getString(R.string.toast_error_data), Toast.LENGTH_SHORT).show();
            return;
        }
        for (Key key : newKeyList) {
            if (key.getValue().equalsIgnoreCase(keyValue)) {
                Toast.makeText(getActivity(), getString(R.string.toast_existed), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        newKeyList.add(0, new Key(keyValue, note, lastModified));
        String newKeyListString = TextUtils.getKeyListString(newKeyList);

        new UpdateKeyListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, newKeyListString);
        progressDialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.dialog_content_working)
                .progress(true, 0)
                .progressIndeterminateStyle(false)
                .build();
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
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
                Toast.makeText(getContext(), getString(R.string.toast_error), Toast.LENGTH_SHORT).show();
                return;
            } else if (result.equals(MSG_SPAM)) {
                Toast.makeText(getActivity(), getString(R.string.toast_spam), Toast.LENGTH_SHORT).show();
                return;
            }

            my.setKeyList(newKeyList);
            Toast.makeText(getContext(), getString(R.string.toast_activated), Toast.LENGTH_SHORT).show();
            mListener.onActivateCompleted();
        }
    }

}
