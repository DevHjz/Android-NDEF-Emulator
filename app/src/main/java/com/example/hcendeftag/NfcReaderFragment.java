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
import com.example.hcendeftag.hce.HceSimulationManager;

/**
 * NFC 读卡 Fragment
 */
public class NfcReaderFragment extends Fragment {
    private FragmentNfcReaderBinding binding;
    private NfcReaderManager nfcReader;
    private NdefTagDatabase database;
    private HceSimulationManager hceManager;
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
        hceManager = new HceSimulationManager(requireContext());

        // 检查 NFC 支持
        if (!nfcReader.isNfcSupported()) {
            binding.statusText.setText("设备不支持 NFC");
            binding.statusText.setBackgroundColor(0xFFFF4444);
            return;
        }

        updateNfcStatus();

        // 保存按钮
        binding.btnSave.setOnClickListener(v -> saveCurrentTag());

        // 模拟按钮
        binding.btnSimulate.setOnClickListener(v -> simulateCurrentTag());

        // 清除按钮
        binding.btnClear.setOnClickListener(v -> clearCurrentTag());
    }

    private void updateNfcStatus() {
        if (!nfcReader.isNfcEnabled()) {
            binding.statusText.setText("NFC 未启用，请在设置中开启");
            binding.statusText.setBackgroundColor(0xFFFFBB33);
        } else {
            binding.statusText.setText("请将 NFC 标签靠近设备");
            binding.statusText.setBackgroundColor(0xFF2196F3);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        nfcReader.enableForegroundDispatch();
        updateNfcStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        nfcReader.disableForegroundDispatch();
    }

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
            binding.typeText.setText("检测到标签: " + contentType);
            binding.statusText.setText("读取成功 (" + ndefMessage.getRecords().length + " 条记录)");
            binding.statusText.setBackgroundColor(0xFF4CAF50);
            
            binding.btnSave.setEnabled(true);
            binding.btnSimulate.setEnabled(true);
        } else {
            binding.statusText.setText("读取失败，请重试");
            binding.statusText.setBackgroundColor(0xFFFF4444);
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

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("保存并命名标签");
        
        final EditText input = new EditText(requireContext());
        input.setHint("输入标签名称，例如：公司门禁卡");
        input.setPadding(40, 40, 40, 40);
        builder.setView(input);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String tagName = input.getText().toString().trim();
            if (tagName.isEmpty()) {
                Toast.makeText(requireContext(), "名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            String content = nfcReader.parseNdefContent(currentNdefMessage);
            String contentType = nfcReader.getNdefContentType(currentNdefMessage);
            String hexData = nfcReader.ndefToHexString(currentNdefMessage);

            NdefTag tag = new NdefTag(tagName, content, contentType);
            tag.setNdefHexData(hexData);

            long id = database.insertNdefTag(tag);
            if (id > 0) {
                Toast.makeText(requireContext(), "标签 \"" + tagName + "\" 已保存", Toast.LENGTH_SHORT).show();
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

        hceManager.startHceSimulationWithMessage(currentNdefMessage);
        Toast.makeText(requireContext(), "已启动 HCE 模拟", Toast.LENGTH_SHORT).show();
    }

    /**
     * 清除当前标签
     */
    private void clearCurrentTag() {
        currentNdefMessage = null;
        binding.contentText.setText("读取到的 NDEF 详细内容将显示在这里...");
        binding.typeText.setText("等待读取...");
        updateNfcStatus();
        binding.btnSave.setEnabled(false);
        binding.btnSimulate.setEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
