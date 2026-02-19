package com.example.hcendeftag.hce;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
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

        // 根据内容类型创建相应的 NDEF 消息
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

        // 启动 HCE 服务
        enableHceService();

        // 发送 Intent 到 NdefHceService，更新模拟内容
        Intent intent = new Intent(context, NdefHceService.class);
        intent.putExtra("ndefMessage", ndefMessage.toByteArray());
        context.startService(intent);

        Log.d(TAG, "HCE 模拟已启动");
    }

    /**
     * 使用文本内容启动 HCE 模拟
     */
    public void startHceSimulationWithText(String text) {
        NdefRecord ndefRecord = NdefRecord.createTextRecord("en", text);
        NdefMessage ndefMessage = new NdefMessage(ndefRecord);
        startHceSimulationWithMessage(ndefMessage);
    }

    /**
     * 使用 URL 启动 HCE 模拟
     */
    public void startHceSimulationWithUrl(String url) {
        NdefRecord ndefRecord = NdefRecord.createUri(url);
        NdefMessage ndefMessage = new NdefMessage(ndefRecord);
        startHceSimulationWithMessage(ndefMessage);
    }

    /**
     * 停止 HCE 模拟
     */
    public void stopHceSimulation() {
        disableHceService();
        Log.d(TAG, "HCE 模拟已停止");
    }

    /**
     * 根据 NDEF 标签创建 NDEF 消息
     */
    private NdefMessage createNdefMessage(NdefTag tag) {
        try {
            String contentType = tag.getContentType();
            String content = tag.getNdefContent();

            NdefRecord ndefRecord = null;

            if ("TEXT".equals(contentType)) {
                ndefRecord = NdefRecord.createTextRecord("en", content);
            } else if ("URL".equals(contentType)) {
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
     * 注意：这需要系统权限，普通应用可能无法实现
     */
    public void registerAsDefaultPaymentApp() {
        // 这通常需要系统权限或通过设置应用进行
        // 这里仅作为示意，实际实现可能需要更多权限
        Log.d(TAG, "尝试注册为默认 NFC 付款应用");
        enableHceService();
    }

    /**
     * 更新 HCE 模拟内容
     */
    public void updateHceSimulationContent(NdefTag tag) {
        startHceSimulation(tag);
    }
}
