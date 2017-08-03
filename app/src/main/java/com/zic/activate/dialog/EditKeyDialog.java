package com.zic.activate.dialog;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.zic.activate.R;
import com.zic.activate.model.Key;
import com.zic.activate.model.MyApplication;
import com.zic.activate.util.NetUtils;
import com.zic.activate.util.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.FormBody;

import static android.content.Context.CLIPBOARD_SERVICE;
import static com.zic.activate.model.MyApplication.FORM_KEY_CONTENT;
import static com.zic.activate.model.MyApplication.GITLAB_TOKEN;
import static com.zic.activate.model.MyApplication.KEY_LENGTH;
import static com.zic.activate.model.MyApplication.MSG_SPAM;
import static com.zic.activate.model.MyApplication.NOTE_LENGTH;
import static com.zic.activate.model.MyApplication.SNIPPET_URL;

public class EditKeyDialog extends DialogFragment implements View.OnClickListener {

    private static final String KEY_POS = "position";
    private static final String KEY_VALUE = "value";
    private static final String KEY_LASTMODIFIED = "last_modified";
    private static final String KEY_NOTE = "note";

    private MaterialDialog dialog;
    private TextInputLayout tilKeyValue, tilNote;
    private Button btnEdit;
    private MaterialDialog progressDialog;

    private static MyApplication my;
    private String pos, keyValue, lastModified, note;

    public static EditKeyDialog create(String pos, String keyValue, String lastModified, String note) {
        EditKeyDialog dialog = new EditKeyDialog();
        Bundle args = new Bundle();
        args.putString(KEY_POS, pos);
        args.putString(KEY_VALUE, keyValue);
        args.putString(KEY_LASTMODIFIED, lastModified);
        args.putString(KEY_NOTE, note);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        my = ((MyApplication) getActivity().getApplication());

        dialog = new MaterialDialog.Builder(getActivity())
                .customView(R.layout.dialog_edit_key, true)
                .build();

        View view = dialog.getCustomView();
        assert view != null;
        Button btnGetKey = (Button) view.findViewById(R.id.btn_get_key);
        tilKeyValue = (TextInputLayout) view.findViewById(R.id.til_key_value);
        tilNote = (TextInputLayout) view.findViewById(R.id.til_note);
        btnEdit = (Button) view.findViewById(R.id.btn_edit);

        Bundle bundle = getArguments();
        pos = bundle.getString(KEY_POS);
        keyValue = bundle.getString(KEY_VALUE);
        lastModified = bundle.getString(KEY_LASTMODIFIED);
        note = bundle.getString(KEY_NOTE);

        setupInput();

        btnGetKey.setOnClickListener(this);
        btnEdit.setOnClickListener(this);

        btnEdit.setEnabled(false);

        return dialog;
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
                    btnEdit.setEnabled(false);
                } else {
                    if (editable.toString().contains("|")) {
                        tilKeyValue.setErrorEnabled(true);
                        tilKeyValue.setError(getString(R.string.til_err_pipe));
                        tilNote.setEnabled(false);
                        btnEdit.setEnabled(false);
                    } else {
                        tilKeyValue.setErrorEnabled(false);
                        tilNote.setEnabled(true);
                        btnEdit.setEnabled(true);
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
                    btnEdit.setEnabled(false);
                } else {
                    tilNote.setErrorEnabled(false);
                    tilNote.setError(null);
                    btnEdit.setEnabled(true);
                }
            }
        });

        tilKeyValue.getEditText().setText(keyValue);
        tilNote.getEditText().setText(note);
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
            case R.id.btn_edit:
                btnEdit.setEnabled(false);
                editKey();
                break;
        }
    }

    private void editKey() {
        assert tilKeyValue.getEditText() != null;
        assert tilNote.getEditText() != null;
        String newKeyValue = tilKeyValue.getEditText().getText().toString().trim().toUpperCase();
        String newNote = tilNote.getEditText().getText().toString().trim();
        new EditKeyTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pos, newKeyValue, lastModified, newNote);
        dialog.hide();
        progressDialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.dialog_content_working)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .build();
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    private class EditKeyTask extends AsyncTask<String, Void, List<String>> {
        private List<Key> newKeyList = new ArrayList<>();

        @Override
        protected List<String> doInBackground(String... strings) {
            int pos = Integer.valueOf(strings[0]);
            String keyValue = strings[1];
            String lastModified = strings[2];
            String note = strings[3];

            newKeyList.addAll(my.getKeyList());
            newKeyList.get(pos).setValue(keyValue);
            newKeyList.get(pos).setLastModified(Long.parseLong(lastModified));
            newKeyList.get(pos).setNote(note);

            String newKeyListString = TextUtils.getKeyListString(newKeyList);

            FormBody formBody = new FormBody.Builder()
                    .add(FORM_KEY_CONTENT, newKeyListString)
                    .build();

            String result = NetUtils.putFormBody(SNIPPET_URL, GITLAB_TOKEN, formBody);

            if (result == null) {
                return null;
            } else if (result.equals(MSG_SPAM)) {
                publishProgress();
                cancel(true);
            }

            return Arrays.asList(strings);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            progressDialog.dismiss();
            Toast.makeText(getActivity(), getString(R.string.toast_spam), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(List<String> resultArr) {
            progressDialog.dismiss();

            if (resultArr == null) {
                Toast.makeText(getActivity(), getString(R.string.toast_error), Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }

//            int pos = Integer.parseInt(resultList.get(0));
//            String keyValue = resultList.get(1);
//            Long lastModified = Long.parseLong(resultList.get(2));
//            String note = resultList.get(3);
//            keyAdapter.updateItem(pos, keyValue, note, lastModified);

            my.setKeyList(newKeyList);

            getActivity().recreate();
            Toast.makeText(getActivity(), getString(R.string.toast_edited), Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

}
