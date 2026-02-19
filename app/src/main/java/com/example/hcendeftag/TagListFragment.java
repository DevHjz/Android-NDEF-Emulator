package com.example.hcendeftag;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hcendeftag.adapter.NdefTagAdapter;
import com.example.hcendeftag.database.NdefTagDatabase;
import com.example.hcendeftag.databinding.FragmentTagListBinding;
import com.example.hcendeftag.model.NdefTag;

import java.util.List;

public class TagListFragment extends Fragment implements NdefTagAdapter.OnTagActionListener {

    private FragmentTagListBinding binding;
    private NdefTagDatabase database;
    private NdefTagAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTagListBinding.inflate(inflater, container, false);
        database = new NdefTagDatabase(requireContext());
        
        setupRecyclerView();
        loadTags();

        binding.btnRegisterPayment.setOnClickListener(v -> {
            Intent intent = new Intent(CardEmulation.ACTION_CHANGE_DEFAULT);
            intent.putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT);
            intent.putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, NdefHceService.COMPONENT);
            startActivity(intent);
        });

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new NdefTagAdapter(this);
        binding.rvTags.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTags.setAdapter(adapter);
    }

    private void loadTags() {
        List<NdefTag> tags = database.getAllNdefTags();
        adapter.setTags(tags);
        binding.tvEmpty.setVisibility(tags.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTagSelected(NdefTag tag) {
        // 启动 HCE 服务并传递当前选中的标签 ID
        Intent intent = new Intent(requireContext(), NdefHceService.class);
        intent.putExtra("ndef_tag_id", tag.getId());
        requireContext().startService(intent);
        
        // 确保组件已启用
        requireContext().getPackageManager().setComponentEnabledSetting(
                NdefHceService.COMPONENT,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
                
        Toast.makeText(requireContext(), "正在模拟: " + tag.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSetDefault(NdefTag tag) {
        database.setDefaultNdefTag(tag.getId());
        loadTags();
        Toast.makeText(requireContext(), "已设为默认标签", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteTag(NdefTag tag) {
        database.deleteNdefTag(tag.getId());
        loadTags();
        Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
