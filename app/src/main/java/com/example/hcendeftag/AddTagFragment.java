package com.example.hcendeftag;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hcendeftag.database.NdefTagDatabase;
import com.example.hcendeftag.databinding.FragmentAddTagBinding;
import com.example.hcendeftag.model.NdefTag;

import java.util.ArrayList;
import java.util.List;

public class AddTagFragment extends Fragment {

    private FragmentAddTagBinding binding;
    private List<NdefTag.NdefRecordItem> records = new ArrayList<>();
    private RecordAdapter recordAdapter;
    private NdefTagDatabase database;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddTagBinding.inflate(inflater, container, false);
        database = new NdefTagDatabase(requireContext());

        setupRecyclerView();

        binding.btnAddRecord.setOnClickListener(v -> showAddRecordDialog());
        binding.btnSave.setOnClickListener(v -> saveTag());

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        recordAdapter = new RecordAdapter();
        binding.rvRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecords.setAdapter(recordAdapter);
    }

    private void showAddRecordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_record, null);
        Spinner spinner = dialogView.findViewById(R.id.spinner_type);
        EditText editText = dialogView.findViewById(R.id.et_content);

        String[] types = {"TEXT", "URL", "APP (Package Name)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("添加记录")
                .setView(dialogView)
                .setPositiveButton("添加", (dialog, which) -> {
                    String content = editText.getText().toString().trim();
                    if (content.isEmpty()) return;

                    NdefTag.NdefRecordItem.Type type;
                    switch (spinner.getSelectedItemPosition()) {
                        case 1: type = NdefTag.NdefRecordItem.Type.URL; break;
                        case 2: type = NdefTag.NdefRecordItem.Type.APP; break;
                        default: type = NdefTag.NdefRecordItem.Type.TEXT; break;
                    }

                    records.add(new NdefTag.NdefRecordItem(type, content));
                    recordAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveTag() {
        String name = binding.etTagName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "请输入标签名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (records.isEmpty()) {
            Toast.makeText(requireContext(), "请至少添加一条记录", Toast.LENGTH_SHORT).show();
            return;
        }

        NdefTag tag = new NdefTag(name);
        tag.setRecords(new ArrayList<>(records));
        database.insertNdefTag(tag);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onTagSaved();
        }
    }

    private class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ndef_record, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NdefTag.NdefRecordItem item = records.get(position);
            holder.tvType.setText(item.type.name());
            holder.tvContent.setText(item.content);
            holder.btnRemove.setOnClickListener(v -> {
                records.remove(position);
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvType, tvContent;
            View btnRemove;
            ViewHolder(View itemView) {
                super(itemView);
                tvType = itemView.findViewById(R.id.tv_record_type);
                tvContent = itemView.findViewById(R.id.tv_record_content);
                btnRemove = itemView.findViewById(R.id.btn_remove_record);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
