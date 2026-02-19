package com.example.hcendeftag.hce;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.CardEmulation;
import android.util.Log;

import com.example.hcendeftag.NdefHceService;
import com.example.hcendeftag.model.NdefTag;

/**
 * HCE 模拟管理类
 * 用于控制 NFC HCE 服务的启动、停止和模拟内容的更新
 */
public class HceSimulationManager {
    private static final String TAG = "HceSimulationManager";
    private Context context;
    private PackageManager packageManager;

    public HceSimulationManager(Context context) {
        this.context = context;
        this.packageManager = context.getPackageManager();
    }

    /**
     * 启用 HCE 服务
     */
    public void enableHceService() {
        enableComponent(NdefHceService.COMPONENT);
        Log.d(TAG, "HCE 服务已启用");
    }

    /**
     * 禁用 HCE 服务
     */
    public void disableHceService() {
        disableComponent(NdefHceService.COMPONENT);
        Log.d(TAG, "HCE 服务已禁用");
    }

    /**
     * 启用组件
     */
    private void enableComponent(ComponentName component) {
        packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * 禁用组件
     */
    private void disableComponent(ComponentName component) {
        packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * 使用 NDEF 标签内容启动 HCE 模拟
     */
    public void startHceSimulation(NdefTag tag) {
        if (tag == null) {
            Log.e(TAG, "NDEF 标签为空");
            return;
        }

        // 优先使用原始十六进制数据，以确保多记录和特殊格式能被完整模拟
        if (tag.getNdefHexData() != null && !tag.getNdefHexData().isEmpty()) {
            byte[] ndefBytes = hexStringToByteArray(tag.getNdefHexData());
            if (ndefBytes != null) {
                startHceSimulationWithBytes(ndefBytes);
                return;
            }
        }

        // 如果没有原始数据，则根据内容类型创建
        NdefMessage ndefMessage = createNdefMessage(tag);
        if (ndefMessage != null) {
            startHceSimulationWithMessage(ndefMessage);
        }
    }

    /**
     * 使用 NDEF 消息启动 HCE 模拟
     */
    public void startHceSimulationWithMessage(NdefMessage ndefMessage) {
        if (ndefMessage == null) {
            Log.e(TAG, "NDEF 消息为空");
            return;
        }
        startHceSimulationWithBytes(ndefMessage.toByteArray());
    }

    /**
     * 使用字节数组启动 HCE 模拟
     */
    public void startHceSimulationWithBytes(byte[] ndefBytes) {
        if (ndefBytes == null) {
            Log.e(TAG, "NDEF 字节数组为空");
            return;
        }

        // 确保 HCE 服务已启用
        enableHceService();

        // 发送 Intent 到 NdefHceService，更新模拟内容
        Intent intent = new Intent(context, NdefHceService.class);
        intent.putExtra("ndefMessage", ndefBytes);
        context.startService(intent);

        Log.d(TAG, "HCE 模拟已启动，数据长度: " + ndefBytes.length);
    }

    /**
     * 停止 HCE 模拟
     */
    public void stopHceSimulation() {
        disableHceService();
        Log.d(TAG, "HCE 模拟已停止");
    }

    /**
     * 根据 NDEF 标签创建 NDEF 消息（备用方案）
     */
    private NdefMessage createNdefMessage(NdefTag tag) {
        try {
            String contentType = tag.getContentType();
            String content = tag.getNdefContent();

            NdefRecord ndefRecord = null;

            if ("URL".equals(contentType)) {
                ndefRecord = NdefRecord.createUri(content);
            } else {
                // 默认作为文本处理
                ndefRecord = NdefRecord.createTextRecord("en", content);
            }

            return new NdefMessage(ndefRecord);
        } catch (Exception e) {
            Log.e(TAG, "创建 NDEF 消息失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查 HCE 服务是否启用
     */
    public boolean isHceServiceEnabled() {
        int state = packageManager.getComponentEnabledSetting(NdefHceService.COMPONENT);
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    /**
     * 注册为默认 NFC 付款应用
     */
    public void registerAsDefaultPaymentApp() {
        Intent intent = new Intent(CardEmulation.ACTION_CHANGE_DEFAULT);
        intent.putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT);
        intent.putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, NdefHceService.COMPONENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        Log.d(TAG, "已请求更改默认 NFC 付款应用");
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
