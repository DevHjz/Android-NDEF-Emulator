package com.devhjz.ndefemulator.model;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * NDEF 标签数据模型，支持多个记录
 */
public class NdefTag implements Serializable {
    private long id;
    private String name;              // 标签名称
    private long createdTime;         // 创建时间
    private long lastModifiedTime;    // 最后修改时间
    private boolean isDefault;        // 是否为默认模拟标签
    
    // 存储记录的列表
    private List<NdefRecordItem> records = new ArrayList<>();

    public static class NdefRecordItem implements Serializable {
        public enum Type { TEXT, URL, APP }
        public Type type;
        public String content;

        public NdefRecordItem(Type type, String content) {
            this.type = type;
            this.content = content;
        }
    }

    public NdefTag() {
        this.createdTime = System.currentTimeMillis();
        this.lastModifiedTime = System.currentTimeMillis();
    }

    public NdefTag(String name) {
        this();
        this.name = name;
    }

    public void addRecord(NdefRecordItem.Type type, String content) {
        records.add(new NdefRecordItem(type, content));
    }

    /**
     * 将所有记录转换为 NdefMessage 的字节数组
     */
    public byte[] toNdefBytes() {
        if (records.isEmpty()) return null;
        
        List<NdefRecord> ndefRecords = new ArrayList<>();
        for (NdefRecordItem item : records) {
            try {
                switch (item.type) {
                    case TEXT:
                        ndefRecords.add(NdefRecord.createTextRecord("zh", item.content));
                        break;
                    case URL:
                        ndefRecords.add(NdefRecord.createUri(item.content));
                        break;
                    case APP:
                        ndefRecords.add(NdefRecord.createApplicationRecord(item.content));
                        break;
                }
            } catch (Exception e) {
                Log.e("NdefTag", "Error creating NDEF record", e);
            }
        }
        
        if (ndefRecords.isEmpty()) return null;
        
        NdefMessage message = new NdefMessage(ndefRecords.toArray(new NdefRecord[0]));
        return message.toByteArray();
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getCreatedTime() { return createdTime; }
    public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }
    public long getLastModifiedTime() { return lastModifiedTime; }
    public void setLastModifiedTime(long lastModifiedTime) { this.lastModifiedTime = lastModifiedTime; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
    public List<NdefRecordItem> getRecords() { return records; }
    public void setRecords(List<NdefRecordItem> records) { this.records = records; }

    @Override
    public String toString() {
        return "NdefTag{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", records=" + records.size() +
                ", isDefault=" + isDefault +
                '}';
    }
}
