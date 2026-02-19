package com.example.hcendeftag.model;

import java.io.Serializable;

/**
 * NDEF 标签数据模型
 */
public class NdefTag implements Serializable {
    private long id;
    private String name;              // 标签名称
    private String ndefContent;       // NDEF 内容
    private String contentType;       // 内容类型：TEXT、URL、VCARD 等
    private long createdTime;         // 创建时间
    private long lastModifiedTime;    // 最后修改时间
    private boolean isDefault;        // 是否为默认模拟标签
    private String ndefHexData;       // NDEF 十六进制数据（用于保存原始数据）

    public NdefTag() {
    }

    public NdefTag(String name, String ndefContent, String contentType) {
        this.name = name;
        this.ndefContent = ndefContent;
        this.contentType = contentType;
        this.createdTime = System.currentTimeMillis();
        this.lastModifiedTime = System.currentTimeMillis();
        this.isDefault = false;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNdefContent() {
        return ndefContent;
    }

    public void setNdefContent(String ndefContent) {
        this.ndefContent = ndefContent;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public String getNdefHexData() {
        return ndefHexData;
    }

    public void setNdefHexData(String ndefHexData) {
        this.ndefHexData = ndefHexData;
    }

    @Override
    public String toString() {
        return "NdefTag{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", contentType='" + contentType + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
}
