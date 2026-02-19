package com.example.hcendeftag;

import android.app.AlertDialog;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.hcendeftag.database.NdefTagDatabase;
import com.example.hcendeftag.model.NdefTag;
import com.example.hcendeftag.nfc.NfcReaderManager;
import com.example.hcendeftag.databinding.FragmentNfcReaderBinding;

/**
 * NFC 读卡 Fragment
 */
public class NfcReaderFragment extends Fragment {
    private FragmentNfcReaderBinding binding;
    private NfcReaderManager nfcReader;
    private NdefTagDatabase database;
    private NdefMessage currentNdefMessage;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentNfcReaderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nfcReader = new NfcReaderManager(requireActivity());
        database = new NdefTagDatabase(requireContext());

        // 检查 NFC 支持
        if (!nfcReader.isNfcSupported()) {
            binding.statusText.setText("设备不支持 NFC");
            binding.statusText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            return;
        }

        if (!nfcReader.isNfcEnabled()) {
            binding.statusText.setText("NFC 未启用，请在设置中启用 NFC");
            binding.statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        } else {
            binding.statusText.setText("请将 NFC 标签靠近设备");
            binding.statusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        }

        // 保存按钮
        binding.btnSave.setOnClickListener(v -> saveCurrentTag());

        // 模拟按钮
        binding.btnSimulate.setOnClickListener(v -> simulateCurrentTag());

        // 清除按钮
        binding.btnClear.setOnClickListener(v -> clearCurrentTag());
    }

    @Override
    public void onResume() {
        super.onResume();
        nfcReader.enableForegroundDispatch();
    }

    @Override
    public void onPause() {
        super.onPause();
        nfcReader.disableForegroundDispatch();
    }

    // 修复：移除 @Override 注解（Fragment无此父类方法），删除 super.onNewIntent 调用
    public void onNewIntent(Intent intent) {
        handleNfcIntent(intent);
    }

    /**
     * 处理 NFC Intent
     */
    private void handleNfcIntent(Intent intent) {
        NdefMessage ndefMessage = nfcReader.readNdefFromIntent(intent);
        if (ndefMessage != null) {
            currentNdefMessage = ndefMessage;
            String content = nfcReader.parseNdefContent(ndefMessage);
            String contentType = nfcReader.getNdefContentType(ndefMessage);

            binding.contentText.setText(content);
            binding.typeText.setText("类型: " + contentType);
            binding.statusText.setText("成功读取 NDEF 标签");
            binding.statusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            binding.statusText.setText("读取失败，请重试");
            binding.statusText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }
    }

    /**
     * 保存当前标签
     */
    private void saveCurrentTag() {
        if (currentNdefMessage == null) {
            Toast.makeText(requireContext(), "请先读取 NFC 标签", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示对话框让用户输入标签名称
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("保存标签");
        builder.setMessage("请输入标签名称:");

        EditText input = new EditText(requireContext());
        input.setHint("标签名称");
        builder.setView(input);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String tagName = input.getText().toString().trim();
            if (tagName.isEmpty()) {
                Toast.makeText(requireContext(), "标签名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            String content = nfcReader.parseNdefContent(currentNdefMessage);
            String contentType = nfcReader.getNdefContentType(currentNdefMessage);
            String hexData = nfcReader.ndefToHexString(currentNdefMessage);

            NdefTag tag = new NdefTag(tagName, content, contentType);
            tag.setNdefHexData(hexData);

            long id = database.insertNdefTag(tag);
            if (id > 0) {
                Toast.makeText(requireContext(), "标签已保存", Toast.LENGTH_SHORT).show();
                clearCurrentTag();
            } else {
                Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 模拟当前标签
     */
    private void simulateCurrentTag() {
        if (currentNdefMessage == null) {
            Toast.makeText(requireContext(), "请先读取 NFC 标签", Toast.LENGTH_SHORT).show();
            return;
        }

        // 这里可以启动 HCE 模拟
        Toast.makeText(requireContext(), "已启动 HCE 模拟", Toast.LENGTH_SHORT).show();
    }

    /**
     * 清除当前标签
     */
    private void clearCurrentTag() {
        currentNdefMessage = null;
        binding.contentText.setText("");
        binding.typeText.setText("");
        binding.statusText.setText("请将 NFC 标签靠近设备");
        binding.statusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}