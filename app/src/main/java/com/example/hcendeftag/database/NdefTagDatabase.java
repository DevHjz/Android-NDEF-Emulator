package com.devhjz.ndefemulator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.devhjz.ndefemulator.model.NdefTag;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * NDEF 标签数据库管理类，采用 JSON 序列化存储多条记录
 */
public class NdefTagDatabase extends SQLiteOpenHelper {
    private static final String TAG = "NdefTagDatabase";
    private static final String DATABASE_NAME = "ndef_tags_v2.db";
    private static final int DATABASE_VERSION = 1;

    // 表名和列名
    private static final String TABLE_NAME = "ndef_tags";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_RECORDS_JSON = "records_json"; // 存储 JSON 序列化的记录列表
    private static final String COLUMN_CREATED_TIME = "created_time";
    private static final String COLUMN_LAST_MODIFIED_TIME = "last_modified_time";
    private static final String COLUMN_IS_DEFAULT = "is_default";

    // SQL 创建表语句
    private static final String CREATE_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_NAME + " TEXT NOT NULL, " +
            COLUMN_RECORDS_JSON + " TEXT, " +
            COLUMN_CREATED_TIME + " LONG, " +
            COLUMN_LAST_MODIFIED_TIME + " LONG, " +
            COLUMN_IS_DEFAULT + " INTEGER DEFAULT 0" +
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
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * 将 NdefTag 记录列表序列化为 JSON 字符串
     */
    private String serializeRecords(List<NdefTag.NdefRecordItem> records) {
        JSONArray array = new JSONArray();
        for (NdefTag.NdefRecordItem item : records) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", item.type.name());
                obj.put("content", item.content);
                array.put(obj);
            } catch (JSONException e) {
                Log.e(TAG, "Error serializing record", e);
            }
        }
        return array.toString();
    }

    /**
     * 从 JSON 字符串反序列化为 NdefTag 记录列表
     */
    private List<NdefTag.NdefRecordItem> deserializeRecords(String json) {
        List<NdefTag.NdefRecordItem> records = new ArrayList<>();
        if (json == null || json.isEmpty()) return records;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                NdefTag.NdefRecordItem.Type type = NdefTag.NdefRecordItem.Type.valueOf(obj.getString("type"));
                String content = obj.getString("content");
                records.add(new NdefTag.NdefRecordItem(type, content));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error deserializing record", e);
        }
        return records;
    }

    /**
     * 插入新的 NDEF 标签
     */
    public long insertNdefTag(NdefTag tag) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, tag.getName());
        values.put(COLUMN_RECORDS_JSON, serializeRecords(tag.getRecords()));
        values.put(COLUMN_CREATED_TIME, tag.getCreatedTime());
        values.put(COLUMN_LAST_MODIFIED_TIME, tag.getLastModifiedTime());
        values.put(COLUMN_IS_DEFAULT, tag.isDefault() ? 1 : 0);

        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id;
    }

    /**
     * 更新 NDEF 标签
     */
    public int updateNdefTag(NdefTag tag) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, tag.getName());
        values.put(COLUMN_RECORDS_JSON, serializeRecords(tag.getRecords()));
        values.put(COLUMN_LAST_MODIFIED_TIME, System.currentTimeMillis());
        values.put(COLUMN_IS_DEFAULT, tag.isDefault() ? 1 : 0);

        int rows = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(tag.getId())});
        db.close();
        return rows;
    }

    /**
     * 删除 NDEF 标签
     */
    public int deleteNdefTag(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
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
                tag.setRecords(deserializeRecords(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORDS_JSON))));
                tag.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME)));
                tag.setLastModifiedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_MODIFIED_TIME)));
                tag.setDefault(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DEFAULT)) == 1);
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
            tag.setRecords(deserializeRecords(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORDS_JSON))));
            tag.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME)));
            tag.setLastModifiedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_MODIFIED_TIME)));
            tag.setDefault(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DEFAULT)) == 1);
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
            tag.setRecords(deserializeRecords(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORDS_JSON))));
            tag.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME)));
            tag.setLastModifiedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_MODIFIED_TIME)));
            tag.setDefault(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DEFAULT)) == 1);
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
    }
}
