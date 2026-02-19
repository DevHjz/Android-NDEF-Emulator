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
    private int selectedPosition = -1;

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

        // 设置标签名称
        holder.tagName.setText(tag.getName());

        // 设置内容类型
        holder.tagType.setText("类型: " + tag.getContentType());

        // 设置内容预览（截断长内容）
        String content = tag.getNdefContent();
        if (content != null && content.length() > 50) {
            holder.tagContent.setText(content.substring(0, 50) + "...");
        } else {
            holder.tagContent.setText(content);
        }

        // 设置时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.tagTime.setText(sdf.format(new Date(tag.getLastModifiedTime())));

        // 设置默认单选按钮
        holder.radioDefault.setChecked(tag.isDefault());
        holder.radioDefault.setOnClickListener(v -> {
            if (onTagActionListener != null) {
                onTagActionListener.onSetDefault(tag);
            }
        });

        // 设置模拟按钮
        holder.btnSimulate.setOnClickListener(v -> {
            if (onTagActionListener != null) {
                onTagActionListener.onSimulate(tag);
            }
        });

        // 设置删除按钮
        holder.btnDelete.setOnClickListener(v -> {
            if (onTagActionListener != null) {
                onTagActionListener.onDelete(tag);
            }
        });

        return convertView;
    }

    /**
     * 更新数据
     */
    public void updateData(List<NdefTag> newTags) {
        this.tags = newTags;
        notifyDataSetChanged();
    }

    /**
     * 设置标签操作监听器
     */
    public void setOnTagActionListener(OnTagActionListener listener) {
        this.onTagActionListener = listener;
    }

    /**
     * ViewHolder 类
     */
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
