package com.example.mobiiliprojekti.services

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseManager(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // set up security manager
    private val securityManager = SecurityManager()
    companion object {
        private const val DATABASE_NAME = "db_penny_paladin.db"
        private const val DATABASE_VERSION = 1
    }

    // creates new database at first use.
    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("CREATE TABLE category (category_id INTEGER PRIMARY KEY AUTOINCREMENT, category_name TEXT NOT NULL)")
        db.execSQL("CREATE TABLE category_budget (cb_id INTEGER PRIMARY KEY AUTOINCREMENT, category INTEGER, cat_budget INTEGER, date TEXT, user INTEGER, FOREIGN KEY(category) REFERENCES category(category_id), FOREIGN KEY(user) REFERENCES user(user_id))")
        db.execSQL("CREATE TABLE monthly_budget (mb_id INTEGER PRIMARY KEY AUTOINCREMENT, mont_budget INTEGER DEFAULT 0, user INTEGER NOT NULL, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user) REFERENCES user(user_id))")
        db.execSQL("CREATE TABLE purchase (purchase_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, value INTEGER DEFAULT 0, category INTEGER NOT NULL, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, user INTEGER NOT NULL, FOREIGN KEY(category) REFERENCES category_budget(category), FOREIGN KEY(user) REFERENCES user(user_id))")
        db.execSQL("CREATE TABLE user (user_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, email TEXT NOT NULL UNIQUE, password TEXT NOT NULL, is_admin INTEGER DEFAULT 0, salt TEXT NOT NULL)")

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

    // Adding/registering new user to db
    fun addUser(username: String, email: String, password: String): Long {
        // Generate salt
        val salt = securityManager.generateSalt()

        // Salt and hash password
        val hashedPassword = securityManager.hashPassword(password, salt)

        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put("username", username)
            put("email", email)
            put("password", hashedPassword)
            put("salt", salt)
        }
        return try {
            val result = db.insertOrThrow("user", null, contentValues)
            db.close()
            result
        } catch (e: SQLiteConstraintException) {
            // returns -1 to handle exception in ui fragment
            -1
        }
    }

    // Changing user as primary user and sets other users as non primary.
    fun updateUserAsAdmin(userId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Sets all users is_admin-value as 0
            val updateAllUsersQuery = "UPDATE user SET is_admin = 0 WHERE user_id != ?"
            val updateAllUsersStatement = db.compileStatement(updateAllUsersQuery)
            updateAllUsersStatement.bindLong(1, userId)
            updateAllUsersStatement.executeUpdateDelete()

            // Sets selected users is_admin-value as 1
            val updateAdminUserQuery = "UPDATE user SET is_admin = 1 WHERE user_id = ?"
            val updateAdminUserStatement = db.compileStatement(updateAdminUserQuery)
            updateAdminUserStatement.bindLong(1, userId)
            updateAdminUserStatement.executeUpdateDelete()

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
            printAllUsers() // for testing/debugging
        }
    }

    // get primary users username from db
    fun fetchAdminUser(): String? {
        val db = readableDatabase
        var adminUsername: String? = null
        try {
            val cursor = db.rawQuery("SELECT username FROM user WHERE is_admin = 1", null)
            if (cursor.moveToFirst()) {
                val usernameIndex = cursor.getColumnIndex("username")
                if (usernameIndex >= 0) {
                    adminUsername = cursor.getString(usernameIndex)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return adminUsername
    }

    //Check if user and password matches to user in db for login
    fun loginUser(username: String, password: String): Boolean {
        var result = false
        val db = readableDatabase

        // get inputted usernames hashed password and salt
        val selection = "username = ?"
        val selectionArgs = arrayOf(username)
        val cursor = db.query("user", arrayOf("salt", "password"), selection, selectionArgs, null, null, null)
        val passwordIndex = cursor.getColumnIndex("password")
        val saltIndex = cursor.getColumnIndex("salt")

        try {
                if (passwordIndex >= 0 && saltIndex >= 0) {
                    if (cursor.moveToFirst()) {
                        val salt = cursor.getBlob(saltIndex)
                        val storedPassword = cursor.getString(passwordIndex)


                        // hash inputted password with users unique salt
                        val hashedPassword = SecurityManager().hashPassword(password, salt)

                        // compare if inputted and stored password match
                        if (hashedPassword == storedPassword) {
                            result = true
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
            db.close()
        }
        return result
    }


    // set new password if user forgets it.
    fun resetPassword(username: String, email: String, newPassword: String): Boolean {
        // Generate salt
        val salt = securityManager.generateSalt()

        // Salt and hash password
        val hashedPassword = securityManager.hashPassword(newPassword, salt)

        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM user WHERE username = ? AND email = ?", arrayOf(username, email))
        try {
            if (cursor.moveToFirst()) {
                val userIdIndex = cursor.getColumnIndex("user_id")
                val userId = cursor.getInt(userIdIndex)
                if (userId >= 0) {
                    val contentValues = ContentValues().apply {
                        put("password", hashedPassword)
                        put("salt", salt)
                    }
                    val rowsAffected = db.update("user", contentValues, "user_id = ?", arrayOf(userId.toString()))
                    if (rowsAffected > 0) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
            db.close()
        }
        return false
    }



    // this prints all user to terminal for debugging -->
    private fun printAllUsers() {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM user", null)
        val userIdIndex = cursor.getColumnIndex("user_id")
        val usernameIndex = cursor.getColumnIndex("username")
        val emailIndex = cursor.getColumnIndex("email")
        val passwordIndex = cursor.getColumnIndex("password")
        val isAdminIndex = cursor.getColumnIndex("is_admin")
        val saltIndex = cursor.getColumnIndex("salt")

        try {
        if (userIdIndex >= 0 && usernameIndex >= 0 && emailIndex >= 0 && passwordIndex >= 0 && isAdminIndex >= 0) {
            if (cursor.moveToFirst()) {
                do {
                    val userId = cursor.getInt(userIdIndex)
                    val username = cursor.getString(usernameIndex)
                    val email = cursor.getString(emailIndex)
                    val password = cursor.getString(passwordIndex)
                    val isAdmin = cursor.getInt(isAdminIndex) == 1
                    val salt = cursor.getBlob(saltIndex)

                    println("User ID: $userId, Username: $username, Password: $password, Email: $email, isAdmin: $isAdmin, salt: $salt")
                } while (cursor.moveToNext())
            } else {
                println("User table is empty")
            }
        } else {
            println("Column indexes not found")
        }} finally {
            cursor.close()
            db.close()
        }
    }

    // this prints all user to dashboard fragment for testing purposes -->
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

    fun allCategoryNames(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT category_name FROM category", null)
        val categories = mutableListOf<String>()
        try {
            while (cursor.moveToNext()) {
                val categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"))
                categories.add(categoryName)
            }
        } catch (e: Exception) {
            e.printStackTrace()  // exception
        } finally {
            cursor.close()
            db.close()
        }
        return categories //return category names only
    }

    // add a new purchase to the database
    fun addPurchase(name: String, value: Double, category: String, date: String, userId: Int): Long {
        val db = writableDatabase
        val categoryId = getCategoryID(category)
        return try {
            val sql = "INSERT INTO purchase (name, value, category, date, user) VALUES (?, ?, ?, ?, ?)"
            val stmt = db.compileStatement(sql)
            stmt.bindString(1, name)
            stmt.bindDouble(2, value)
            stmt.bindLong(3, categoryId.toLong())
            stmt.bindString(4, date)
            stmt.bindLong(5, userId.toLong())
            stmt.executeInsert()
        } catch (e: SQLiteConstraintException) {
            -1
        } finally {
            db.close()
        }
    }


    // get category ID from category name
    private fun getCategoryID(categoryName: String): Int {
        var categoryId = -1
        val db = readableDatabase
        db.rawQuery("SELECT category_id FROM category WHERE category_name = ?", arrayOf(categoryName)).use { cursor ->
            if (cursor.moveToFirst()) {
                categoryId = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"))
            }
        }
        db.close()
        return categoryId
    }

    // get user ID from username
    fun getUserId(username: String): Int {
        var userId = -1
        val db = readableDatabase
        db.rawQuery("SELECT user_id FROM user WHERE username = ?", arrayOf(username)).use { cursor ->
            if (cursor.moveToFirst()) {
                userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"))
                println("userId on $userId")
            } else {
                Log.e("DatabaseError", "User not found: $username")
            }
        }
        db.close()
        return userId
    }
}