package com.example.hcendeftag.nfc;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.util.Log;

/**
 * NFC 读卡器管理类
 * 用于读取 NFC 标签中的 NDEF 信息
 */
public class NfcReaderManager {
    private static final String TAG = "NfcReaderManager";
    private NfcAdapter nfcAdapter;
    private Activity activity;
    private OnNdefReadListener onNdefReadListener;

    public interface OnNdefReadListener {
        void onNdefRead(NdefMessage ndefMessage);
        void onNdefReadError(String error);
    }

    public NfcReaderManager(Activity activity) {
        this.activity = activity;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
    }

    /**
     * 检查设备是否支持 NFC
     */
    public boolean isNfcSupported() {
        return nfcAdapter != null;
    }

    /**
     * 检查 NFC 是否启用
     */
    public boolean isNfcEnabled() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }

    /**
     * 启用前台分发系统
     */
    public void enableForegroundDispatch() {
        if (!isNfcSupported()) {
            Log.w(TAG, "设备不支持 NFC");
            return;
        }

        IntentFilter[] intentFilters = new IntentFilter[]{
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        };

        String[][] techLists = new String[][]{
                {Ndef.class.getName()}
        };

        nfcAdapter.enableForegroundDispatch(activity, getPendingIntent(), intentFilters, techLists);
        Log.d(TAG, "前台分发已启用");
    }

    /**
     * 禁用前台分发系统
     */
    public void disableForegroundDispatch() {
        if (isNfcSupported()) {
            nfcAdapter.disableForegroundDispatch(activity);
            Log.d(TAG, "前台分发已禁用");
        }
    }

    /**
     * 从 Intent 中读取 NDEF 信息
     */
    public NdefMessage readNdefFromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }

        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                return readNdefFromTag(tag);
            }
        }

        return null;
    }

    /**
     * 从 Tag 对象中读取 NDEF 信息
     */
    public NdefMessage readNdefFromTag(Tag tag) {
        if (tag == null) {
            Log.e(TAG, "Tag 对象为空");
            return null;
        }

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                ndef.close();
                Log.d(TAG, "成功读取 NDEF 信息");
                return ndefMessage;
            } else {
                Log.w(TAG, "Tag 不支持 NDEF");
            }
        } catch (Exception e) {
            Log.e(TAG, "读取 NDEF 失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 解析 NDEF 记录内容
     */
    public String parseNdefContent(NdefMessage ndefMessage) {
        if (ndefMessage == null || ndefMessage.getRecords().length == 0) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        for (NdefRecord record : ndefMessage.getRecords()) {
            String recordContent = parseNdefRecord(record);
            if (recordContent != null && !recordContent.isEmpty()) {
                content.append(recordContent).append("\n");
            }
        }

        return content.toString().trim();
    }

    /**
     * 解析单个 NDEF 记录
     */
    private String parseNdefRecord(NdefRecord record) {
        try {
            byte[] payload = record.getPayload();
            
            // 处理文本类型
            if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && 
                    record.getType()[0] == NdefRecord.RTD_TEXT[0]) {
                int langCodeLength = payload[0] & 0x3F;
                String text = new String(payload, langCodeLength + 1, payload.length - langCodeLength - 1, "UTF-8");
                return text;
            }
            
            // 处理 URI 类型
            if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && 
                    record.getType()[0] == NdefRecord.RTD_URI[0]) {
                String uri = new String(payload, 1, payload.length - 1, "UTF-8");
                return uri;
            }
            
            // 处理其他类型，尝试转换为字符串
            return new String(payload, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "解析 NDEF 记录失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取 NDEF 内容类型
     */
    public String getNdefContentType(NdefMessage ndefMessage) {
        if (ndefMessage == null || ndefMessage.getRecords().length == 0) {
            return "UNKNOWN";
        }

        NdefRecord record = ndefMessage.getRecords()[0];
        
        if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
            if (record.getType()[0] == NdefRecord.RTD_TEXT[0]) {
                return "TEXT";
            } else if (record.getType()[0] == NdefRecord.RTD_URI[0]) {
                return "URL";
            }
        }
        
        return "OTHER";
    }

    /**
     * 将 NDEF 消息转换为十六进制字符串
     */
    public String ndefToHexString(NdefMessage ndefMessage) {
        if (ndefMessage == null) {
            return "";
        }

        byte[] ndefBytes = ndefMessage.toByteArray();
        StringBuilder hexString = new StringBuilder();
        for (byte b : ndefBytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }

    /**
     * 从十六进制字符串恢复 NDEF 消息
     */
    public NdefMessage hexStringToNdef(String hexString) {
        try {
            byte[] ndefBytes = new byte[hexString.length() / 2];
            for (int i = 0; i < ndefBytes.length; i++) {
                ndefBytes[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
            }
            return new NdefMessage(ndefBytes);
        } catch (Exception e) {
            Log.e(TAG, "从十六进制字符串恢复 NDEF 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 设置 NDEF 读取监听器
     */
    public void setOnNdefReadListener(OnNdefReadListener listener) {
        this.onNdefReadListener = listener;
    }

    /**
     * 获取待定 Intent
     */
    private android.app.PendingIntent getPendingIntent() {
        Intent intent = new Intent(activity, activity.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE :
                android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        return android.app.PendingIntent.getActivity(activity, 0, intent, flags);
    }
}
