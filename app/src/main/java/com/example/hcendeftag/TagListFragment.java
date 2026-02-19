package com.example.hcendeftag;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.hcendeftag.adapter.NdefTagAdapter;
import com.example.hcendeftag.database.NdefTagDatabase;
import com.example.hcendeftag.hce.HceSimulationManager;
import com.example.hcendeftag.model.NdefTag;
import com.example.hcendeftag.databinding.FragmentTagListBinding;

import java.util.List;

/**
 * NDEF 标签列表 Fragment
 */
public class TagListFragment extends Fragment {
    private FragmentTagListBinding binding;
    private NdefTagDatabase database;
    private HceSimulationManager hceManager;
    private NdefTagAdapter adapter;
    private ListView listView;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentTagListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = new NdefTagDatabase(requireContext());
        hceManager = new HceSimulationManager(requireContext());
        listView = binding.tagListView;

        // 初始化适配器
        refreshTagList();

        // 刷新按钮
        binding.btnRefresh.setOnClickListener(v -> refreshTagList());

        // 启用 HCE 按钮
        binding.btnEnableHce.setOnClickListener(v -> {
            hceManager.enableHceService();
            Toast.makeText(requireContext(), "HCE 服务已启用", Toast.LENGTH_SHORT).show();
        });

        // 禁用 HCE 按钮
        binding.btnDisableHce.setOnClickListener(v -> {
            hceManager.disableHceService();
            Toast.makeText(requireContext(), "HCE 服务已禁用", Toast.LENGTH_SHORT).show();
        });

        // 注册为默认付款应用按钮
        binding.btnRegisterPayment.setOnClickListener(v -> registerAsPaymentApp());
    }

    /**
     * 模拟标签
     */
    private void simulateTag(NdefTag tag) {
        hceManager.startHceSimulation(tag);
        Toast.makeText(requireContext(), "正在模拟: " + tag.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置默认标签
     */
    private void setDefaultTag(NdefTag tag) {
        database.setDefaultNdefTag(tag.getId());
        refreshTagList();
        // 设置默认后立即启动该标签的模拟
        hceManager.startHceSimulation(tag);
        Toast.makeText(requireContext(), "已设为默认并启动模拟: " + tag.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * 删除标签
     */
    private void deleteTag(NdefTag tag) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除标签")
                .setMessage("确定要删除标签 \"" + tag.getName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    database.deleteNdefTag(tag.getId());
                    refreshTagList();
                    Toast.makeText(requireContext(), "标签已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 刷新标签列表
     */
    private void refreshTagList() {
        List<NdefTag> tags = database.getAllNdefTags();
        if (adapter == null) {
            adapter = new NdefTagAdapter(requireContext(), tags);
            adapter.setOnTagActionListener(new NdefTagAdapter.OnTagActionListener() {
                @Override
                public void onSimulate(NdefTag tag) {
                    simulateTag(tag);
                }

                @Override
                public void onSetDefault(NdefTag tag) {
                    setDefaultTag(tag);
                }

                @Override
                public void onDelete(NdefTag tag) {
                    deleteTag(tag);
                }

                @Override
                public void onEdit(NdefTag tag) {
                    // 暂不实现复杂的编辑，仅提示
                    Toast.makeText(requireContext(), "编辑功能暂未开放", Toast.LENGTH_SHORT).show();
                }
            });
            listView.setAdapter(adapter);
        } else {
            adapter.updateData(tags);
        }
    }

    /**
     * 注册为默认付款应用
     */
    private void registerAsPaymentApp() {
        hceManager.registerAsDefaultPaymentApp();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshTagList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
