package com.example.hcendeftag.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.hcendeftag.model.NdefTag;

import java.util.ArrayList;
import java.util.List;

/**
 * NDEF 标签数据库管理类
 */
public class NdefTagDatabase extends SQLiteOpenHelper {
    private static final String TAG = "NdefTagDatabase";
    private static final String DATABASE_NAME = "ndef_tags.db";
    private static final int DATABASE_VERSION = 1;

    // 表名和列名
    private static final String TABLE_NAME = "ndef_tags";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_NDEF_CONTENT = "ndef_content";
    private static final String COLUMN_CONTENT_TYPE = "content_type";
    private static final String COLUMN_CREATED_TIME = "created_time";
    private static final String COLUMN_LAST_MODIFIED_TIME = "last_modified_time";
    private static final String COLUMN_IS_DEFAULT = "is_default";
    private static final String COLUMN_NDEF_HEX_DATA = "ndef_hex_data";

    // SQL 创建表语句
    private static final String CREATE_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_NAME + " TEXT NOT NULL, " +
            COLUMN_NDEF_CONTENT + " TEXT, " +
            COLUMN_CONTENT_TYPE + " TEXT, " +
            COLUMN_CREATED_TIME + " LONG, " +
            COLUMN_LAST_MODIFIED_TIME + " LONG, " +
            COLUMN_IS_DEFAULT + " INTEGER DEFAULT 0, " +
            COLUMN_NDEF_HEX_DATA + " TEXT" +
            ")";

    public NdefTagDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SQL);
        Log.d(TAG, "数据库表创建成功");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 升级数据库时的处理逻辑
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * 插入新的 NDEF 标签
     */
    public long insertNdefTag(NdefTag tag) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, tag.getName());
        values.put(COLUMN_NDEF_CONTENT, tag.getNdefContent());
        values.put(COLUMN_CONTENT_TYPE, tag.getContentType());
        values.put(COLUMN_CREATED_TIME, tag.getCreatedTime());
        values.put(COLUMN_LAST_MODIFIED_TIME, tag.getLastModifiedTime());
        values.put(COLUMN_IS_DEFAULT, tag.isDefault() ? 1 : 0);
        values.put(COLUMN_NDEF_HEX_DATA, tag.getNdefHexData());

        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        Log.d(TAG, "插入 NDEF 标签成功，ID: " + id);
        return id;
    }

    /**
     * 更新 NDEF 标签
     */
    public int updateNdefTag(NdefTag tag) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, tag.getName());
        values.put(COLUMN_NDEF_CONTENT, tag.getNdefContent());
        values.put(COLUMN_CONTENT_TYPE, tag.getContentType());
        values.put(COLUMN_LAST_MODIFIED_TIME, System.currentTimeMillis());
        values.put(COLUMN_IS_DEFAULT, tag.isDefault() ? 1 : 0);
        values.put(COLUMN_NDEF_HEX_DATA, tag.getNdefHexData());

        int rows = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(tag.getId())});
        db.close();
        Log.d(TAG, "更新 NDEF 标签成功，受影响行数: " + rows);
        return rows;
    }

    /**
     * 删除 NDEF 标签
     */
    public int deleteNdefTag(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        Log.d(TAG, "删除 NDEF 标签成功，受影响行数: " + rows);
        return rows;
    }

    /**
     * 获取所有 NDEF 标签
     */
    public List<NdefTag> getAllNdefTags() {
        List<NdefTag> tags = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_CREATED_TIME + " DESC");

        if (cursor.moveToFirst()) {
            do {
                NdefTag tag = new NdefTag();
                tag.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                tag.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                tag.setNdefContent(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NDEF_CONTENT)));
                tag.setContentType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT_TYPE)));
                tag.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME)));
                tag.setLastModifiedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_MODIFIED_TIME)));
                tag.setDefault(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DEFAULT)) == 1);
                tag.setNdefHexData(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NDEF_HEX_DATA)));
                tags.add(tag);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return tags;
    }

    /**
     * 根据 ID 获取 NDEF 标签
     */
    public NdefTag getNdefTagById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);

        NdefTag tag = null;
        if (cursor.moveToFirst()) {
            tag = new NdefTag();
            tag.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            tag.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            tag.setNdefContent(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NDEF_CONTENT)));
            tag.setContentType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT_TYPE)));
            tag.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME)));
            tag.setLastModifiedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_MODIFIED_TIME)));
            tag.setDefault(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DEFAULT)) == 1);
            tag.setNdefHexData(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NDEF_HEX_DATA)));
        }

        cursor.close();
        db.close();
        return tag;
    }

    /**
     * 获取默认的 NDEF 标签
     */
    public NdefTag getDefaultNdefTag() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_IS_DEFAULT + " = ?", new String[]{"1"}, null, null, null);

        NdefTag tag = null;
        if (cursor.moveToFirst()) {
            tag = new NdefTag();
            tag.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            tag.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            tag.setNdefContent(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NDEF_CONTENT)));
            tag.setContentType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT_TYPE)));
            tag.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME)));
            tag.setLastModifiedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_MODIFIED_TIME)));
            tag.setDefault(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DEFAULT)) == 1);
            tag.setNdefHexData(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NDEF_HEX_DATA)));
        }

        cursor.close();
        db.close();
        return tag;
    }

    /**
     * 设置默认 NDEF 标签
     */
    public void setDefaultNdefTag(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // 首先清除所有默认标记
        ContentValues clearValues = new ContentValues();
        clearValues.put(COLUMN_IS_DEFAULT, 0);
        db.update(TABLE_NAME, clearValues, null, null);

        // 设置指定 ID 的标签为默认
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DEFAULT, 1);
        db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        Log.d(TAG, "设置默认 NDEF 标签成功，ID: " + id);
    }

    /**
     * 获取标签总数
     */
    public int getTagCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }
}
