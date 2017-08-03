package com.zic.activate.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.zic.activate.R;
import com.zic.activate.listener.OnKeyAdapterListener;
import com.zic.activate.model.Key;
import com.zic.activate.model.MyApplication;

import java.util.ArrayList;
import java.util.List;

public class KeyAdapter extends RecyclerView.Adapter<KeyAdapter.ViewHolder> implements View.OnClickListener {

    private List<Key> mKeys = new ArrayList<>();
    private List<Key> keysCopy = new ArrayList<>();

    private Activity mContext;
    private OnKeyAdapterListener mListener;

    public void setOnKeyAdapterListener(OnKeyAdapterListener listener) {
        this.mListener = listener;
    }

    public KeyAdapter(Activity context, List<Key> keys) {
        this.mContext = context;
        this.mKeys.addAll(keys);
        if (keysCopy.isEmpty()) {
            keysCopy.addAll(keys);
        }
    }

    public void filter(String text) {
        mKeys.clear();
        if (text.isEmpty()) {
            mKeys.addAll(keysCopy);
        } else {
            text = text.toLowerCase();
            for (Key key : keysCopy) {
                if (key.getValue().toLowerCase().contains(text) || key.getNote().toLowerCase().contains(text)) {
                    mKeys.add(key);
                }
            }
        }

        // !important
        ((MyApplication) mContext.getApplication()).setKeyList(keysCopy);
        notifyDataSetChanged();
    }

    public void filterNotes() {
        mKeys.clear();

        for (Key key : keysCopy) {
            if (!key.getNote().isEmpty()) {
                mKeys.add(key);
            }
        }

        // !important
        ((MyApplication) mContext.getApplication()).setKeyList(keysCopy);
        notifyDataSetChanged();
    }

    @Override
    public KeyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_key, parent, false);

        return new KeyAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(KeyAdapter.ViewHolder holder, int position) {
        Key curKey = mKeys.get(position);
        String note = curKey.getNote();

        holder.tvValue.setText(curKey.getValue());
        if (note.equals("")) {
            holder.tvNote.setText(mContext.getString(R.string.tv_empty));
        } else {
            holder.tvNote.setText(note);
        }
        holder.tvDate.setText(curKey.getDate());

        holder.itemView.setTag(curKey);
        holder.imgBtnDelete.setTag(curKey);
        holder.imgBtnEdit.setTag(curKey);

        holder.itemView.setOnClickListener(this);
        holder.imgBtnDelete.setOnClickListener(this);
        holder.imgBtnEdit.setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        return mKeys.size();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        Key curKey = (Key) view.getTag();
        int position = keysCopy.indexOf(curKey);

        switch (id) {
            case R.id.imgbtn_delete:
                mListener.OnDeleteIconClick(position);
                break;
            case R.id.imgbtn_edit:
                mListener.onEditIconClick(position);
                break;
            default:
                mListener.onItemClick(position);
        }
    }

    /*public void removeItem(int position) {
        mKeys.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mKeys.size());
    }

    public void updateItem(int position, String value, String note, long lastModified) {
        mCurKey = mKeys.get(position);
        mCurKey.setValue(value);
        mCurKey.setNote(note);
        mCurKey.setLastModified(lastModified);
        notifyItemChanged(position);
    }*/

    class ViewHolder extends RecyclerView.ViewHolder {
        private View itemView;
        private TextView tvValue;
        private TextView tvNote;
        private TextView tvDate;
        private ImageButton imgBtnDelete;
        private ImageButton imgBtnEdit;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.tvValue = (TextView) itemView.findViewById(R.id.tv_value);
            this.tvNote = (TextView) itemView.findViewById(R.id.tv_note);
            this.tvDate = (TextView) itemView.findViewById(R.id.tv_date);
            this.imgBtnDelete = (ImageButton) itemView.findViewById(R.id.imgbtn_delete);
            this.imgBtnEdit = (ImageButton) itemView.findViewById(R.id.imgbtn_edit);
        }
    }
}
