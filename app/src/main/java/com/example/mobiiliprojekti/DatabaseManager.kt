package com.example.mobiiliprojekti

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseManager(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "db_penny_paladin.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("CREATE TABLE category (category_id INTEGER PRIMARY KEY AUTOINCREMENT, category_name TEXT NOT NULL)")
        db.execSQL("CREATE TABLE category_budget (cb_id INTEGER PRIMARY KEY AUTOINCREMENT, category INTEGER, cat_budget INTEGER, date TEXT, user INTEGER, FOREIGN KEY(category) REFERENCES category(category_id), FOREIGN KEY(user) REFERENCES user(user_id))")
        db.execSQL("CREATE TABLE monthly_budget (mb_id INTEGER PRIMARY KEY AUTOINCREMENT, mont_budget INTEGER DEFAULT 0, user INTEGER NOT NULL, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user) REFERENCES user(user_id))")
        db.execSQL("CREATE TABLE purchase (purchase_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, value INTEGER DEFAULT 0, category INTEGER NOT NULL, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, user INTEGER NOT NULL, FOREIGN KEY(category) REFERENCES category_budget(category), FOREIGN KEY(user) REFERENCES user(user_id))")
        db.execSQL("CREATE TABLE user (user_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL, email TEXT NOT NULL UNIQUE)")

        val categories = arrayOf("Food", "Transportation", "Household", "Clothing", "Well-being", "Entertainment", "Other")
        val insertStatement = db.compileStatement("INSERT INTO category (category_name) VALUES (?)")
        categories.forEach { category ->
            insertStatement.bindString(1, category)
            insertStatement.executeInsert()
        }
        println("db created")

    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        //updates for database goes here
    }

    fun allCategories(): String {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM category", null)
        val categoryList = StringBuilder()
        try {
            val categoryIdIndex = cursor.getColumnIndex("category_id")
            val categoryNameIndex = cursor.getColumnIndex("category_name")
            while (cursor.moveToNext()) {
                val categoryId = cursor.getInt(categoryIdIndex)
                val categoryName = cursor.getString(categoryNameIndex)
                categoryList.append("Category ID: $categoryId, Name: $categoryName\n")
            }
        } finally {
            cursor.close()
            db.close()
        }
        return categoryList.toString()
    }



}