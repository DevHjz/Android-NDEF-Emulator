package com.example.hcendeftag.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.example.hcendeftag.R;
import com.example.hcendeftag.model.NdefTag;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * NDEF 标签列表适配器
 */
public class NdefTagAdapter extends BaseAdapter {
    private Context context;
    private List<NdefTag> tags;
    private OnTagActionListener onTagActionListener;

    public interface OnTagActionListener {
        void onSimulate(NdefTag tag);
        void onSetDefault(NdefTag tag);
        void onDelete(NdefTag tag);
        void onEdit(NdefTag tag);
    }

    public NdefTagAdapter(Context context, List<NdefTag> tags) {
        this.context = context;
        this.tags = tags;
    }

    @Override
    public int getCount() {
        return tags != null ? tags.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return tags != null ? tags.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return tags != null ? tags.get(position).getId() : -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_ndef_tag, parent, false);
            holder = new ViewHolder();
            holder.tagName = convertView.findViewById(R.id.tag_name);
            holder.tagType = convertView.findViewById(R.id.tag_type);
            holder.tagContent = convertView.findViewById(R.id.tag_content);
            holder.tagTime = convertView.findViewById(R.id.tag_time);
            holder.radioDefault = convertView.findViewById(R.id.radio_default);
            holder.btnSimulate = convertView.findViewById(R.id.btn_simulate);
            holder.btnDelete = convertView.findViewById(R.id.btn_delete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        NdefTag tag = tags.get(position);

        holder.tagName.setText(tag.getName());
        holder.tagType.setText(tag.getContentType());
        
        String content = tag.getNdefContent();
        if (content != null && content.length() > 60) {
            holder.tagContent.setText(content.substring(0, 60) + "...");
        } else {
            holder.tagContent.setText(content);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.tagTime.setText("保存时间: " + sdf.format(new Date(tag.getLastModifiedTime())));

        // 默认标签状态
        holder.radioDefault.setChecked(tag.isDefault());
        
        // 点击整个条目或单选框都触发设置默认
        View.OnClickListener setDefaultListener = v -> {
            if (onTagActionListener != null && !tag.isDefault()) {
                onTagActionListener.onSetDefault(tag);
            }
        };
        convertView.setOnClickListener(setDefaultListener);
        holder.radioDefault.setOnClickListener(setDefaultListener);

        // 模拟按钮
        holder.btnSimulate.setOnClickListener(v -> {
            if (onTagActionListener != null) {
                onTagActionListener.onSimulate(tag);
            }
        });

        // 删除按钮
        holder.btnDelete.setOnClickListener(v -> {
            if (onTagActionListener != null) {
                onTagActionListener.onDelete(tag);
            }
        });

        return convertView;
    }

    public void updateData(List<NdefTag> newTags) {
        this.tags = newTags;
        notifyDataSetChanged();
    }

    public void setOnTagActionListener(OnTagActionListener listener) {
        this.onTagActionListener = listener;
    }

    private static class ViewHolder {
        TextView tagName;
        TextView tagType;
        TextView tagContent;
        TextView tagTime;
        RadioButton radioDefault;
        ImageButton btnSimulate;
        ImageButton btnDelete;
    }
}
