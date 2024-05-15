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
        db.execSQL("CREATE TABLE category_budget (cb_id INTEGER PRIMARY KEY AUTOINCREMENT, category INTEGER, cat_budget INTEGER, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, user INTEGER, FOREIGN KEY(category) REFERENCES category(category_id), FOREIGN KEY(user) REFERENCES user(user_id))")
        db.execSQL("CREATE TABLE monthly_budget (mb_id INTEGER PRIMARY KEY AUTOINCREMENT, month_budget INTEGER DEFAULT 0, user INTEGER NOT NULL, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user) REFERENCES user(user_id))")
        db.execSQL("CREATE TABLE purchase (purchase_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, value INTEGER DEFAULT 0, category INTEGER NOT NULL, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, user INTEGER NOT NULL, FOREIGN KEY(category) REFERENCES category_budget(category), FOREIGN KEY(user) REFERENCES user(user_id))")
        db.execSQL("CREATE TABLE user (user_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, email TEXT NOT NULL UNIQUE, password TEXT NOT NULL, is_admin INTEGER DEFAULT 0, salt TEXT NOT NULL, is_logged_in INTEGER DEFAULT 0)")

        val categories = arrayOf("Food", "Transportation", "Housing", "Clothing", "Well-being", "Entertainment", "Other")
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
            println("user: $result")

            SessionManager.setLoggedInUserId(result) //sets user id to session manager for tracking current user

            insertDefaultMonthlyBudget() // sets default monthly budget for registered user
            insertDefaultCategoryBudgets() // sets default category budgets for registered user

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
            val cursor = db.rawQuery("SELECT user_id, username FROM user WHERE is_admin = 1", null)
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

    fun fetchUser(userId: Long): Pair<String?, String?> {
        val db = readableDatabase
        var username: String? = null
        var email: String? = null

        try {
            val cursor = db.rawQuery("SELECT username, email FROM user WHERE user_id = $userId", null)
            if (cursor.moveToFirst()) {
                val usernameIndex = cursor.getColumnIndex("username")
                val emailIndex = cursor.getColumnIndex("email")
                if (usernameIndex >= 0 && emailIndex >= 0) {
                    username = cursor.getString(usernameIndex)
                    email = cursor.getString(emailIndex)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return Pair(username, email)
    }

    fun fetchMonthlyBudget(userId: Long): Int? {
        val db = readableDatabase
        var monthlyBudget: Int? = null

        try {
            val cursor = db.rawQuery("    SELECT month_budget FROM monthly_budget WHERE user = $userId ORDER BY date DESC LIMIT 1", null)
            if (cursor.moveToFirst()) {
                val monthlyBudgetIndex = cursor.getColumnIndex("month_budget")
                if (monthlyBudgetIndex >= 0) {
                    monthlyBudget = cursor.getInt(monthlyBudgetIndex)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return monthlyBudget
    }

    //db.execSQL("CREATE TABLE category_budget (cb_id INTEGER PRIMARY KEY AUTOINCREMENT, category INTEGER, cat_budget INTEGER, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, user INTEGER, FOREIGN KEY(category) REFERENCES category(category_id), FOREIGN KEY(user) REFERENCES user(user_id))")

    fun fetchHousingBudget(userId: Long): Int? {
        val db = readableDatabase
        var housingBudget: Int? = null

        try {
            val cursor = db.rawQuery("SELECT cb.cat_budget FROM category_budget AS cb JOIN category AS c ON cb.category = c.category_id WHERE cb.user = $userId AND c.category_name = 'food' ORDER BY cb.date DESC LIMIT 1", null)
            Log.d("DeleteUserData", "Query executed successfully: $cursor")
            if (cursor.moveToFirst()) {
                val housingBudgetIndex = cursor.getColumnIndex("cat_budget")
                if (housingBudgetIndex >= 0) {
                    housingBudget = cursor.getInt(housingBudgetIndex)

                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return housingBudget
    }

    //Check if user and password matches to user in db for login
    fun loginUser(username: String, password: String): Boolean {
        var result = false
        val db = readableDatabase

        // get inputted usernames hashed password and salt
        val selection = "username = ?"
        val selectionArgs = arrayOf(username)
        val cursor = db.query("user", arrayOf("user_id", "salt", "password"), selection, selectionArgs, null, null, null)
        val passwordIndex = cursor.getColumnIndex("password")
        val saltIndex = cursor.getColumnIndex("salt")
        val userIdIndex = cursor.getColumnIndex("user_id")

        try {
                if (passwordIndex >= 0 && saltIndex >= 0 && userIdIndex >= 0) {
                    if (cursor.moveToFirst()) {
                        val salt = cursor.getBlob(saltIndex)
                        val storedPassword = cursor.getString(passwordIndex)
                        val userId = cursor.getLong(userIdIndex)


                        // hash inputted password with users unique salt
                        val hashedPassword = SecurityManager().hashPassword(password, salt)

                        // compare if inputted and stored password match
                        if (hashedPassword == storedPassword) {
                            result = true

                            SessionManager.setLoggedInUserId(userId)  //sets user id to session manager for tracking current user
                            println("Login UserId: ${SessionManager.getLoggedInUserId()}")
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
    fun printAllUsers() {
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

    // this prints all categories to dashboard fragment for testing purposes -->
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

    // this function is used to add default monthly budget for new user
    private fun insertDefaultMonthlyBudget() {
        try {
            val userId = SessionManager.getLoggedInUserId() // get user id that is registered
            val defaultBudget = 1500 // default monthly budget

            val db = writableDatabase
            val contentValues = ContentValues().apply {
                put("month_budget", defaultBudget)
                put("user", userId)
            }

            val result = db.insert("monthly_budget", null, contentValues)
            db.close()

            println("default monthly budget added: $result") // for debugging

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // this function is used to add default category budgets for new user
    private fun insertDefaultCategoryBudgets() {
        try {
            val userId = SessionManager.getLoggedInUserId() // get user id that is registered

            val db = writableDatabase

            // Get categories from table "category"
            val cursor = db.rawQuery("SELECT category_id, category_name FROM category", null)
            val categoryIdIndex = cursor.getColumnIndex("category_id")
            val categoryNameIndex = cursor.getColumnIndex("category_name")


            if (categoryIdIndex >= 0 && categoryNameIndex >= 0) {
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            val categoryId = cursor.getLong(categoryIdIndex)
                            val categoryName = cursor.getString(categoryNameIndex)

                            // Get default values for category by using category's name
                            val defaultBudget = getDefaultBudgetForCategory(categoryName)

                            // write default value to table "category_budget" for all categories in category table
                            val contentValues = ContentValues().apply {
                                put("category", categoryId)
                                put("cat_budget", defaultBudget)
                                put("user", userId)
                            }
                            db.insert("category_budget", null, contentValues)
                        } while (cursor.moveToNext())
                    }
                    cursor.close()
                }
            }
            db.close()
            println("oletus kategoria budjetti lisätty.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // This function returns category's budgets default value by category's name
    private fun getDefaultBudgetForCategory(categoryName: String): Int {
        // Set default value here
        return when (categoryName) {
            "Food" -> 250
            "Transportation" -> 200
            "Housing" -> 500
            "Clothing" -> 100
            "Well-being" -> 150
            "Entertainment" -> 100
            "Other" -> 50
            else -> 0 // default value if category is unknown
        }
    }

    // this function deleter all user related data from database
    fun deleteUserData(userId: Long): Boolean {
        val db = writableDatabase
        val deleteQueries = arrayOf(
            "DELETE FROM category_budget WHERE user = $userId",
            "DELETE FROM monthly_budget WHERE user = $userId",
            "DELETE FROM purchase WHERE user = $userId",
            "DELETE FROM user WHERE user_id = $userId"
        )

        return try {
            db.beginTransaction()
            for (query in deleteQueries) {
                db.execSQL(query)
            }
            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            false
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    //Print category budget table to logcat for debugging
    fun printCategoryBudgets() {
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT * FROM category_budget", null)
            if (cursor != null) {
                val categoryBudgetIndex = cursor.getColumnIndex("cb_id")
                val categoryIndex = cursor.getColumnIndex("category")
                val budgetIndex = cursor.getColumnIndex("cat_budget")
                val dateIndex = cursor.getColumnIndex("date")
                val userIndex = cursor.getColumnIndex("user")

                if (categoryBudgetIndex != -1 && categoryIndex != -1 && budgetIndex != -1 && dateIndex != -1 && userIndex != -1) {
                    if (cursor.moveToFirst()) {
                        do {
                            val categoryBudgetId = cursor.getLong(categoryBudgetIndex)
                            val categoryId = cursor.getLong(categoryIndex)
                            val budget = cursor.getInt(budgetIndex)
                            val date = cursor.getString(dateIndex)
                            val user = cursor.getLong(userIndex)

                            println("Category budget ID: $categoryBudgetId, Category: $categoryId, Budget: $budget, Date: $date, User ID: $user")
                        } while (cursor.moveToNext())
                    }
                } else {
                    println("Error: One or more columns not found")
                }
                cursor.close()
            }
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //Print monthly budget table to logcat for debugging
    fun printMonthBudget() {
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT * FROM monthly_budget", null)
            if (cursor != null) {
                val idIndex = cursor.getColumnIndex("mb_id")
                val budgetIndex = cursor.getColumnIndex("month_budget")
                val dateIndex = cursor.getColumnIndex("date")
                val userIndex = cursor.getColumnIndex("user")

                if (idIndex != -1 && budgetIndex != -1 && dateIndex != -1 && userIndex != -1) {
                    if (cursor.moveToFirst()) {
                        do {
                            val mbId = cursor.getLong(idIndex)
                            val monthBudget = cursor.getInt(budgetIndex)
                            val date = cursor.getString(dateIndex)
                            val user = cursor.getLong(userIndex)

                            println("Monthly budget ID: $mbId, Budget: $monthBudget, Date: $date, User ID: $user")
                        } while (cursor.moveToNext())
                    }
                } else {
                    println("Error: One or more columns not found")
                }
                cursor.close()
            }
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}