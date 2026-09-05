package com.example.diaryapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SQLiteHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "diary.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "diaries"

        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_CONTENT = "content"
        private const val KEY_DATE_TIME = "date_time"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = ("CREATE TABLE " + TABLE_NAME + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TITLE + " TEXT,"
                + KEY_CONTENT + " TEXT,"
                + KEY_DATE_TIME + " TEXT" + ")")
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // 1. Thêm nhật ký
    fun insertDiary(title: String, content: String, dateTime: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, title)
            put(KEY_CONTENT, content)
            put(KEY_DATE_TIME, dateTime)
        }
        val result = db.insert(TABLE_NAME, null, values)
        db.close()
        return result
    }

    // 2. Cập nhật nhật ký
    fun updateDiary(id: Int, title: String, content: String, dateTime: String): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, title)
            put(KEY_CONTENT, content)
            put(KEY_DATE_TIME, dateTime)
        }
        val result = db.update(TABLE_NAME, values, "$KEY_ID=?", arrayOf(id.toString()))
        db.close()
        return result
    }

    // 3. Xóa một hoặc nhiều nhật ký
    fun deleteDiaries(ids: List<Int>): Int {
        val db = this.writableDatabase
        val idString = ids.joinToString(",")
        val result = db.delete(TABLE_NAME, "$KEY_ID IN ($idString)", null)
        db.close()
        return result
    }

    fun searchDiaries(query: String): List<DiaryItem> {
        val list = mutableListOf<DiaryItem>()
        val db = readableDatabase
        // Tìm kiếm các bài có tiêu đề HOẶC nội dung chứa từ khóa query
        val cursor = db.rawQuery(
            "SELECT * FROM diaries WHERE title LIKE ? OR content LIKE ? ORDER BY id DESC",
            arrayOf("%$query%", "%$query%")
        )
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
                val dateTime = cursor.getString(cursor.getColumnIndexOrThrow("date_time"))
                list.add(DiaryItem(id, title, content, dateTime))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
}