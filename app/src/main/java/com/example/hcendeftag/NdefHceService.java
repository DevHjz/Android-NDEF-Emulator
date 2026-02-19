package com.example.hcendeftag;

import android.content.ComponentName;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import com.example.hcendeftag.database.NdefTagDatabase;
import com.example.hcendeftag.model.NdefTag;

import java.util.Arrays;

public class NdefHceService extends HostApduService {

    public static final ComponentName COMPONENT = new ComponentName("com.example.hcendeftag", NdefHceService.class.getName());

    private final static String TAG = "NdefHceService";

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
        Log.d(TAG, "onCreate");
        mAppSelected = false;
        mCcSelected = false;
        mNdefSelected = false;
        
        database = new NdefTagDatabase(this);
        loadDefaultNdefTag();
    }

    private void loadDefaultNdefTag() {
        NdefTag defaultTag = database.getDefaultNdefTag();
        if (defaultTag != null) {
            if (defaultTag.getNdefHexData() != null && !defaultTag.getNdefHexData().isEmpty()) {
                byte[] ndefBytes = hexStringToByteArray(defaultTag.getNdefHexData());
                initializeNdefRecordFile(ndefBytes);
                Log.d(TAG, "已加载默认 NDEF 标签 (Hex): " + defaultTag.getName());
            } else {
                updateNdefRecordFile(defaultTag.getNdefContent(), defaultTag.getContentType());
                Log.d(TAG, "已加载默认 NDEF 标签 (Text): " + defaultTag.getName());
            }
        } else {
            initializeNdefRecordFile(getNdefMessage("欢迎使用 NDEF 模拟器").toByteArray());
        }
    }

    private void initializeNdefRecordFile(byte[] ndefBytes) {
        if (ndefBytes == null) return;
        int nlen = ndefBytes.length;
        mNdefRecordFile = new byte[nlen + 2];
        mNdefRecordFile[0] = (byte) ((nlen & 0xff00) / 256);
        mNdefRecordFile[1] = (byte) (nlen & 0xff);
        System.arraycopy(ndefBytes, 0, mNdefRecordFile, 2, nlen);
    }

    private void updateNdefRecordFile(String content, String contentType) {
        NdefMessage ndefMessage;
        if ("URL".equals(contentType)) {
            ndefMessage = getNdefUrlMessage(content);
        } else {
            ndefMessage = getNdefMessage(content);
        }
        if (ndefMessage != null) {
            initializeNdefRecordFile(ndefMessage.toByteArray());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("ndefMessage")) {
            byte[] ndefBytes = intent.getByteArrayExtra("ndefMessage");
            if (ndefBytes != null) {
                initializeNdefRecordFile(ndefBytes);
                Log.d(TAG, "NDEF 模拟内容已更新");
            }
        }
        return START_STICKY;
    }

    private NdefMessage getNdefMessage(String ndefData) {
        NdefRecord ndefRecord = NdefRecord.createTextRecord("en", ndefData);
        return new NdefMessage(ndefRecord);
    }

    private NdefMessage getNdefUrlMessage(String ndefData) {
        if (ndefData == null || ndefData.isEmpty()) return null;
        return new NdefMessage(NdefRecord.createUri(ndefData));
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
