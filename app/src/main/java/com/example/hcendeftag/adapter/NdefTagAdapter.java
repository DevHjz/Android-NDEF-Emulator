package com.example.hcendeftag.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hcendeftag.R;
import com.example.hcendeftag.model.NdefTag;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NdefTagAdapter extends RecyclerView.Adapter<NdefTagAdapter.ViewHolder> {

    private List<NdefTag> tags = new ArrayList<>();
    private final OnTagActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public interface OnTagActionListener {
        void onTagSelected(NdefTag tag);
        void onSetDefault(NdefTag tag);
        void onDeleteTag(NdefTag tag);
    }

    public NdefTagAdapter(OnTagActionListener listener) {
        this.listener = listener;
    }

    public void setTags(List<NdefTag> tags) {
        this.tags = tags;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ndef_tag, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NdefTag tag = tags.get(position);
        holder.tvName.setText(tag.getName());
        
        StringBuilder details = new StringBuilder();
        details.append("记录数: ").append(tag.getRecords().size());
        details.append(" | ").append(dateFormat.format(new Date(tag.getCreatedTime())));
        holder.tvDetails.setText(details.toString());

        holder.rbDefault.setChecked(tag.isDefault());
        
        holder.itemView.setOnClickListener(v -> listener.onTagSelected(tag));
        holder.rbDefault.setOnClickListener(v -> listener.onSetDefault(tag));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteTag(tag));
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails;
        RadioButton rbDefault;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_tag_name);
            tvDetails = itemView.findViewById(R.id.tv_tag_details);
            rbDefault = itemView.findViewById(R.id.rb_default);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
