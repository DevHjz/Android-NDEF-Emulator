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

    // source: https://github.com/TechBooster/C85-Android-4.4-Sample/blob/master/chapter08/NdefCard/src/com/example/ndefcard/NdefHostApduService.java
    public static final ComponentName COMPONENT = new ComponentName("com.example.hcendeftag", NdefHceService.class.getName());

    private final static String TAG = "NfcTest_NdefHostApduService";

    private static final byte[] SELECT_APPLICATION = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xA4, // INS	- Instruction - Instruction code
            (byte) 0x04, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x07, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x85, (byte) 0x01,
            (byte) 0x01, // NDEF Tag Application name D2 76 00 00 85 01 01
            (byte) 0x00  // Le field	- Maximum number of bytes expected in the data field of
            // the response to the command
    };

    private static final byte[] SELECT_CAPABILITY_CONTAINER = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // INS	- Instruction - Instruction code
            (byte) 0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x0c, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xe1, (byte) 0x03 // file identifier of the CC file
    };

    private static final byte[] SELECT_NDEF_FILE = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // Instruction byte (INS) for Select command
            (byte) 0x00, // Parameter byte (P1), select by identifier
            (byte) 0x0c, // Parameter byte (P1), select by identifier
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xE1, (byte) 0x04 // file identifier of the NDEF file retrieved from the CC file
    };

    private final static byte[] CAPABILITY_CONTAINER_FILE = new byte[]{
            0x00, 0x0f, // CCLEN
            0x20, // Mapping Version
            0x00, 0x3b, // Maximum R-APDU data size
            0x00, 0x34, // Maximum C-APDU data size
            0x04, 0x06, // Tag & Length
            (byte) 0xe1, 0x04, // NDEF File Identifier
            (byte) 0x00, (byte) 0xff, // Maximum NDEF size, do NOT extend this value
            0x00, // NDEF file read access granted
            (byte) 0xff, // NDEF File write access denied
    };

    // Status Word success
    private final static byte[] SUCCESS_SW = new byte[]{
            (byte) 0x90,
            (byte) 0x00,
    };
    // Status Word failure
    private final static byte[] FAILURE_SW = new byte[]{
            (byte) 0x6a,
            (byte) 0x82,
    };
    
    public static String mNdefMessage = "default NDEF-message----test123456";
    private byte[] mNdefRecordFile;
    private boolean mAppSelected; // true when SELECT_APPLICATION detected
    private boolean mCcSelected; // true when SELECT_CAPABILITY_CONTAINER detected
    private boolean mNdefSelected; // true when SELECT_NDEF_FILE detected
    private NdefTagDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mAppSelected = false;
        mCcSelected = false;
        mNdefSelected = false;
        
        database = new NdefTagDatabase(this);

        // 尝试加载默认的 NDEF 标签，如果没有则使用默认消息
        loadDefaultNdefTag();
    }

    /**
     * 加载默认的 NDEF 标签
     */
    private void loadDefaultNdefTag() {
        NdefTag defaultTag = database.getDefaultNdefTag();
        if (defaultTag != null) {
            updateNdefRecordFile(defaultTag.getNdefContent(), defaultTag.getContentType());
            Log.d(TAG, "已加载默认 NDEF 标签: " + defaultTag.getName());
        } else {
            // 使用内置默认消息
            NdefMessage ndefDefaultMessage = getNdefMessage(mNdefMessage);
            initializeNdefRecordFile(ndefDefaultMessage);
        }
    }

    /**
     * 初始化 NDEF 记录文件
     */
    private void initializeNdefRecordFile(NdefMessage ndefMessage) {
        if (ndefMessage == null) {
            return;
        }
        int nlen = ndefMessage.getByteArrayLength();
        mNdefRecordFile = new byte[nlen + 2];
        mNdefRecordFile[0] = (byte) ((nlen & 0xff00) / 256);
        mNdefRecordFile[1] = (byte) (nlen & 0xff);
        System.arraycopy(ndefMessage.toByteArray(), 0, mNdefRecordFile, 2,
                ndefMessage.getByteArrayLength());
    }

    /**
     * 更新 NDEF 记录文件
     */
    private void updateNdefRecordFile(String content, String contentType) {
        NdefMessage ndefMessage = null;
        
        if ("URL".equals(contentType)) {
            ndefMessage = getNdefUrlMessage(content);
        } else {
            ndefMessage = getNdefMessage(content);
        }
        
        if (ndefMessage != null) {
            initializeNdefRecordFile(ndefMessage);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // 从 intent 中获取 NDEF 消息字节数组
            if (intent.hasExtra("ndefMessage")) {
                byte[] ndefBytes = intent.getByteArrayExtra("ndefMessage");
                if (ndefBytes != null) {
                    try {
                        NdefMessage ndefMessage = new NdefMessage(ndefBytes);
                        initializeNdefRecordFile(ndefMessage);
                        Log.d(TAG, "onStartCommand: 从 Intent 更新 NDEF 消息");
                    } catch (Exception e) {
                        Log.e(TAG, "onStartCommand: 解析 NDEF 消息失败 - " + e.getMessage());
                    }
                }
            }
            
            // intent contains a text message
            if (intent.hasExtra("ndefMessageText")) {
                String msg = intent.getStringExtra("ndefMessageText");
                NdefMessage ndefMessage = getNdefMessage(msg);
                Log.d(TAG, "onStartCommand msg:" + msg);
                if (ndefMessage != null) {
                    initializeNdefRecordFile(ndefMessage);
                }
            }
            
            // intent contains an URL
            if (intent.hasExtra("ndefUrl")) {
                NdefMessage ndefMessage = getNdefUrlMessage(intent.getStringExtra("ndefUrl"));
                if (ndefMessage != null) {
                    initializeNdefRecordFile(ndefMessage);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private NdefMessage getNdefMessage(String ndefData) {
        NdefRecord ndefRecord = NdefRecord.createTextRecord("en", ndefData);
        return new NdefMessage(ndefRecord);
    }

    private NdefMessage getNdefUrlMessage(String ndefData) {
        if (ndefData.length() == 0) {
            return null;
        }
        NdefRecord ndefRecord;
        ndefRecord = NdefRecord.createUri(ndefData);
        return new NdefMessage(ndefRecord);
    }

    /**
     * emulates an NFC Forum Tag Type 4
     */
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        //if (Arrays.equals(SELECT_APP, commandApdu)) {
        // check if commandApdu qualifies for SELECT_APPLICATION
        if (Arrays.equals(SELECT_APPLICATION, commandApdu)) {
            mAppSelected = true;
            mCcSelected = false;
            mNdefSelected = false;
            return SUCCESS_SW;
            // check if commandApdu qualifies for SELECT_CAPABILITY_CONTAINER
        } else if (mAppSelected && Arrays.equals(SELECT_CAPABILITY_CONTAINER, commandApdu)) {
            mCcSelected = true;
            mNdefSelected = false;
            return SUCCESS_SW;
            // check if commandApdu qualifies for SELECT_NDEF_FILE
        } else if (mAppSelected && Arrays.equals(SELECT_NDEF_FILE, commandApdu)) {
            // NDEF
            mCcSelected = false;
            mNdefSelected = true;
            return SUCCESS_SW;
            // check if commandApdu qualifies for // READ_BINARY
        } else if (commandApdu[0] == (byte) 0x00 && commandApdu[1] == (byte) 0xb0) {
            // READ_BINARY
            // get the offset an le (length) data
            //System.out.println("** " + Utils.bytesToHex(commandApdu) + " in else if
            // (commandApdu[0] == (byte)0x00 && commandApdu[1] == (byte)0xb0) {");
            int offset = (0x00ff & commandApdu[2]) * 256 + (0x00ff & commandApdu[3]);
            int le = 0x00ff & commandApdu[4];

            byte[] responseApdu = new byte[le + SUCCESS_SW.length];

            if (mCcSelected && offset == 0 && le == CAPABILITY_CONTAINER_FILE.length) {
                System.arraycopy(CAPABILITY_CONTAINER_FILE, offset, responseApdu, 0, le);
                System.arraycopy(SUCCESS_SW, 0, responseApdu, le, SUCCESS_SW.length);
                return responseApdu;
            } else if (mNdefSelected) {
                if (offset + le <= mNdefRecordFile.length) {
                    System.arraycopy(mNdefRecordFile, offset, responseApdu, 0, le);
                    System.arraycopy(SUCCESS_SW, 0, responseApdu, le, SUCCESS_SW.length);
                    return responseApdu;
                }
            }
        }

        // The tag should return different errors for different reasons
        // this emulation just returns the general error message
        return FAILURE_SW;
    }

    /**
     * onDeactivated is called when reading ends
     * reset the status boolean values
     */
    @Override
    public void onDeactivated(int reason) {
        mAppSelected = false;
        mCcSelected = false;
        mNdefSelected = false;
    }
}
