package com.devhjz.ndefemulator;

import android.content.ComponentName;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import com.devhjz.ndefemulator.database.NdefTagDatabase;
import com.devhjz.ndefemulator.model.NdefTag;

import java.util.Arrays;

/**
 * 核心 HCE 服务，模拟 NFC Forum Type 4 标签
 */
public class NdefHceService extends HostApduService {

    public static final ComponentName COMPONENT = new ComponentName("com.devhjz.ndefemulator", NdefHceService.class.getName());
    private final static String TAG = "NdefHceService";

    // ISO-DEP APDU 指令集
    private static final byte[] SELECT_APPLICATION = {
            (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x07,
            (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x85, (byte) 0x01, (byte) 0x01,
            (byte) 0x00
    };

    private static final byte[] SELECT_CAPABILITY_CONTAINER = {
            (byte) 0x00, (byte) 0xa4, (byte) 0x00, (byte) 0x0c, (byte) 0x02, (byte) 0xe1, (byte) 0x03
    };

    private static final byte[] SELECT_NDEF_FILE = {
            (byte) 0x00, (byte) 0xa4, (byte) 0x00, (byte) 0x0c, (byte) 0x02, (byte) 0xE1, (byte) 0x04
    };

    // NDEF 能力容器文件 (CC File)
    private final static byte[] CAPABILITY_CONTAINER_FILE = new byte[]{
            0x00, 0x0f, // CCLEN
            0x20, // Mapping Version
            0x00, 0x3b, // Maximum R-APDU data size
            0x00, 0x34, // Maximum C-APDU data size
            0x04, 0x06, // Tag & Length
            (byte) 0xe1, 0x04, // NDEF File Identifier
            (byte) 0x04, (byte) 0x00, // Maximum NDEF size (1024 bytes)
            0x00, // NDEF file read access granted
            (byte) 0xff, // NDEF File write access denied
    };

    private final static byte[] SUCCESS_SW = new byte[]{(byte) 0x90, (byte) 0x00};
    private final static byte[] FAILURE_SW = new byte[]{(byte) 0x6a, (byte) 0x82};
    
    private byte[] mNdefRecordFile;
    private boolean mAppSelected;
    private boolean mCcSelected;
    private boolean mNdefSelected;
    private NdefTagDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "HCE Service Created");
        database = new NdefTagDatabase(this);
        loadDefaultNdefTag();
    }

    /**
     * 加载默认标签或初始欢迎消息
     */
    private void loadDefaultNdefTag() {
        NdefTag defaultTag = database.getDefaultNdefTag();
        if (defaultTag != null) {
            byte[] ndefBytes = defaultTag.toNdefBytes();
            if (ndefBytes != null) {
                initializeNdefRecordFile(ndefBytes);
                Log.d(TAG, "Loaded default tag: " + defaultTag.getName());
            } else {
                updateNdefFromText("标签内容为空");
            }
        } else {
            updateNdefFromText("NDEF 模拟器已就绪");
        }
    }

    private void updateNdefFromText(String text) {
        NdefRecord record = NdefRecord.createTextRecord("zh", text);
        NdefMessage msg = new NdefMessage(record);
        initializeNdefRecordFile(msg.toByteArray());
    }

    private void initializeNdefRecordFile(byte[] ndefBytes) {
        if (ndefBytes == null) return;
        int nlen = ndefBytes.length;
        // NDEF 文件前两个字节是长度
        mNdefRecordFile = new byte[nlen + 2];
        mNdefRecordFile[0] = (byte) ((nlen & 0xff00) / 256);
        mNdefRecordFile[1] = (byte) (nlen & 0xff);
        System.arraycopy(ndefBytes, 0, mNdefRecordFile, 2, nlen);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("ndef_tag_id")) {
            long tagId = intent.getLongExtra("ndef_tag_id", -1);
            if (tagId != -1) {
                NdefTag tag = database.getNdefTagById(tagId);
                if (tag != null) {
                    byte[] ndefBytes = tag.toNdefBytes();
                    if (ndefBytes != null) {
                        initializeNdefRecordFile(ndefBytes);
                        Log.d(TAG, "NDEF content updated to: " + tag.getName());
                    }
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (Arrays.equals(SELECT_APPLICATION, commandApdu)) {
            mAppSelected = true;
            mCcSelected = false;
            mNdefSelected = false;
            return SUCCESS_SW;
        } else if (mAppSelected && Arrays.equals(SELECT_CAPABILITY_CONTAINER, commandApdu)) {
            mCcSelected = true;
            mNdefSelected = false;
            return SUCCESS_SW;
        } else if (mAppSelected && Arrays.equals(SELECT_NDEF_FILE, commandApdu)) {
            mCcSelected = false;
            mNdefSelected = true;
            return SUCCESS_SW;
        } else if (commandApdu.length >= 4 && commandApdu[0] == (byte) 0x00 && commandApdu[1] == (byte) 0xb0) {
            // READ_BINARY 指令
            int offset = (0x00ff & commandApdu[2]) * 256 + (0x00ff & commandApdu[3]);
            int le = (commandApdu.length > 4) ? (0x00ff & commandApdu[4]) : 0;

            if (mCcSelected && offset < CAPABILITY_CONTAINER_FILE.length) {
                int length = Math.min(le, CAPABILITY_CONTAINER_FILE.length - offset);
                byte[] response = new byte[length + 2];
                System.arraycopy(CAPABILITY_CONTAINER_FILE, offset, response, 0, length);
                System.arraycopy(SUCCESS_SW, 0, response, length, 2);
                return response;
            } else if (mNdefSelected && mNdefRecordFile != null && offset < mNdefRecordFile.length) {
                int length = Math.min(le, mNdefRecordFile.length - offset);
                byte[] response = new byte[length + 2];
                System.arraycopy(mNdefRecordFile, offset, response, 0, length);
                System.arraycopy(SUCCESS_SW, 0, response, length, 2);
                return response;
            }
        }
        return FAILURE_SW;
    }

    @Override
    public void onDeactivated(int reason) {
        mAppSelected = false;
        mCcSelected = false;
        mNdefSelected = false;
    }
}
