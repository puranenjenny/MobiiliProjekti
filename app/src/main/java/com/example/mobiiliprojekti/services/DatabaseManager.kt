package com.example.mobiiliprojekti.services

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.time.format.DateTimeFormatter


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
        db.execSQL("CREATE TABLE treat (treat_id INTEGER PRIMARY KEY AUTOINCREMENT, treat_name TEXT NOT NULL, value INTEGER DEFAULT 0, user INTEGER NOT NULL, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user) REFERENCES user(user_id))")
        db.execSQL("CREATE TABLE saved (saving_id INTEGER PRIMARY KEY AUTOINCREMENT, saving_value INTEGER DEFAULT 0, user INTEGER NOT NULL, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user) REFERENCES user(user_id))")


        val categories = arrayOf("Food", "Transportation", "Housing", "Clothes", "Well-being", "Entertainment", "Other")
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

    fun updateUser(userId: Long, username: String, email: String, password: String, isAdmin: Int): Long {
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
            put("is_admin", isAdmin)
        }
        return try {
            val result = db.update("user", contentValues, "user_id = ?", arrayOf(userId.toString()))
            db.close()
            println("user: $result")
            result.toLong()
        } catch (e: SQLiteConstraintException) {
            // for error handling
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
    fun fetchAdminUser(): Pair<Long?, String?> {
        val db = readableDatabase
        var userId: Long? = null
        var adminUsername: String? = null

        try {
            val cursor = db.rawQuery("SELECT user_id, username FROM user WHERE is_admin = 1", null)
            if (cursor.moveToFirst()) {
                val userIdIndex = cursor.getColumnIndex("user_id")
                val usernameIndex = cursor.getColumnIndex("username")
                if (usernameIndex >= 0) {
                    userId = cursor.getString(userIdIndex).toLong()
                    adminUsername = cursor.getString(usernameIndex)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return Pair(userId, adminUsername)
    }

    //function gets logged in users information
    fun fetchUser(userId: Long): Triple<String?, String?, Int?> {
        val db = readableDatabase
        var username: String? = null
        var email: String? = null
        var admin: Int? = null

        try {
            val cursor = db.rawQuery("SELECT username, email, is_admin FROM user WHERE user_id = $userId", null)
            if (cursor.moveToFirst()) {
                val usernameIndex = cursor.getColumnIndex("username")
                val emailIndex = cursor.getColumnIndex("email")
                val adminIndex = cursor.getColumnIndex("is_admin")
                if (usernameIndex >= 0 && emailIndex >= 0 && adminIndex >= 0) {
                    username = cursor.getString(usernameIndex)
                    email = cursor.getString(emailIndex)
                    admin = cursor.getInt(adminIndex)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return  Triple(username, email, admin)
    }

    //function gets logged in users monthly budget
    fun fetchMonthlyBudget(userId: Long): Pair<Int?, String?> {
        val db = readableDatabase
        var monthlyBudgetValue: Int? = null
        var date: String? = null

        try {
            val cursor = db.rawQuery("SELECT month_budget, date FROM monthly_budget WHERE user = $userId ORDER BY date DESC, mb_id DESC", null)
            if (cursor.moveToFirst()) {
                val monthlyBudgetIndex = cursor.getColumnIndex("month_budget")
                val dateIndex = cursor.getColumnIndex("date")
                if (monthlyBudgetIndex >= 0 && dateIndex >= 0) {
                    monthlyBudgetValue = cursor.getInt(monthlyBudgetIndex)
                    date = cursor.getString(dateIndex)
                }
            }
            cursor.close()
            printMonthBudget()
            println("monthly budget selected : $monthlyBudgetValue")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return Pair(monthlyBudgetValue, date)
    }

    //function gets logged in users category budget by category name
    fun fetchCategoryBudget(userId: Long, categoryName: String): Int? {
        val db = readableDatabase
        var categoryBudget: Int? = null

        try {
            val cursor = db.rawQuery("SELECT cb.cat_budget FROM category_budget AS cb JOIN category AS c ON cb.category = c.category_id WHERE cb.user = $userId AND c.category_name = '$categoryName' ORDER BY cb.date DESC LIMIT 1", null)
            if (cursor.moveToFirst()) {
                val categoryBudgetIndex = cursor.getColumnIndex("cat_budget")
                if (categoryBudgetIndex >= 0) {
                    categoryBudget = cursor.getInt(categoryBudgetIndex)

                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return categoryBudget
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



    // this prints all users to terminal for debugging -->
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

    // this function allows to change monthly budget. It makes new budget instead of updating old
    // TODO: Is new budget way togo or should old one be updated?
    fun changeMonthlyBudget(monthlyBudget: Int) {
        try {
            val userId = SessionManager.getLoggedInUserId() // get user id that is registered

            val db = writableDatabase
            val contentValues = ContentValues().apply {
                put("month_budget", monthlyBudget)
                put("user", userId)
            }
            db.insert("monthly_budget", null, contentValues)
            db.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    // this function allows to change category budget by category name. It makes new budget instead of updating old
    // TODO: Is new budget way togo or should old one be updated
    fun changeCategoryBudget(categoryBudget: Int, categoryName: String) {
        try {
            val userId = SessionManager.getLoggedInUserId() // get user id that is registered
            val db = writableDatabase

            // Use other function to get category id by category name
            val categoryId = fetchCategoryIdByName(categoryName)

            if (categoryId.toInt() != -1) {
                val contentValues = ContentValues().apply {
                    put("cat_budget", categoryBudget)
                    put("user", userId)
                    put("category", categoryId)
                }

                val result = db.insert("category_budget", null, contentValues)
                db.close()

                println("Category budget changed: $result") // for debugging
            } else {
                println("Category ID not found for category: $categoryName")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // this function gets category id by category name
    fun fetchCategoryIdByName(categoryName: String): Int {
        val db = readableDatabase
        var categoryId: Int = -1

        try {
            val cursor = db.rawQuery("SELECT category_id FROM category WHERE category_name = '$categoryName'", null)


            if (cursor.moveToFirst()) {
                val categoryIdIndex = cursor.getColumnIndex("category_id")
                if (categoryIdIndex >= 0) {
                    categoryId = cursor.getLong(categoryIdIndex).toInt()
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return categoryId
    }

    fun getCategoryNameById(categoryId: Int): String {
        val db = readableDatabase
        val cursor = db.query("category", arrayOf("category_name"), "category_id = ?", arrayOf(categoryId.toString()), null, null, null)
        var categoryName = ""
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndex("category_name")
            if (index != -1) {
                categoryName = cursor.getString(index)
            }
            cursor.close()
        }
        return categoryName
    }



    // this function gets the budget category id by category name for purchase
    fun fetchCategoryBudgetIdByNameforPurchase(categoryName: String): Int {
        val db = readableDatabase
        var categoryBudgetId: Int = -1

        try {
            // join category_budget and category tables to find the budget entry for a given category name
            val query = """
            SELECT cb.cb_id FROM category_budget cb
            JOIN category c ON cb.category = c.category_id
            WHERE c.category_name = ?
        """
            val cursor = db.rawQuery(query, arrayOf(categoryName))

            if (cursor.moveToFirst()) {
                val categoryIdIndex = cursor.getColumnIndex("cb_id")
                if (categoryIdIndex >= 0) {
                    categoryBudgetId = cursor.getInt(categoryIdIndex)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }

        return categoryBudgetId
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
            "Clothes" -> 100
            "Well-being" -> 150
            "Entertainment" -> 100
            "Other" -> 50
            else -> 0 // default value if category is unknown
        }
    }

    // this function deletes all user related data from database
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Home
// add a new purchase to the database
    fun addPurchase(name: String, value: Double, category: String, date: String, userId: Long): Long {
        val db = writableDatabase
        var result: Long = -1
        try {
            val categoryId = getCategoryID(category)
            val contentValues = ContentValues().apply {
                put("name", name)
                put("value", value)
                put("category", categoryId)
                put("date", date)
                put("user", userId)
            }
            result = db.insertOrThrow("purchase", null, contentValues)
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        } finally {
            //
        }
        return result
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
        return categoryId
    }

    // get lats purchases from the user
    fun getLastPurchases(userId: Long, year: Int, month: Int): List<Purchase> {
        val db = readableDatabase
        val purchases = mutableListOf<Purchase>()
        // Ensuring month is formatted to two digits
        val monthStr = String.format("%02d", month)
        val query = """
        SELECT * FROM purchase 
        WHERE user = ? AND strftime('%Y', date) = ? AND strftime('%m', date) = ? 
        ORDER BY date DESC LIMIT 20
    """
        val cursor = db.rawQuery(query, arrayOf(userId.toString(), year.toString(), monthStr))

        try {
            if (cursor.moveToFirst()) {
                do {
                    val purchaseId = cursor.getLong(cursor.getColumnIndexOrThrow("purchase_id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val value = cursor.getDouble(cursor.getColumnIndexOrThrow("value"))
                    val category = cursor.getInt(cursor.getColumnIndexOrThrow("category"))
                    val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))

                    purchases.add(Purchase(purchaseId, name, value, category, date, userId))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
        }
        return purchases
    }

    //function gets logged in users monthly budget by mont/year
    fun getSelectedMonthsBudget(userId: Long, selectedMonth: Int, selectedYear: Int): Int? {
        val db = readableDatabase
        var monthlyBudget: Int? = null
        val month = "%02d".format(selectedMonth)
        val year = selectedYear.toString()

        try {
            val query = "SELECT month_budget FROM monthly_budget WHERE user = ? AND strftime('%m', date) = ? AND strftime('%Y', date) = ? ORDER BY mb_id DESC, date DESC LIMIT 1"
            Log.d("DatabaseQuery", "Query: $query, User: ${userId}, Month: $month, Year: $year")
            val cursor = db.rawQuery(query, arrayOf(userId.toString(), month, year))
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
        println("Monthly budget is: $monthlyBudget")
        return monthlyBudget
    }

    fun changeMonthlyBudgetByMonth(monthlyBudget: Int, date: String) {
        try {
            println("new budget inserted $monthlyBudget")
            val userId = SessionManager.getLoggedInUserId() // get user id that is registered

            val db = writableDatabase
            val contentValues = ContentValues().apply {
                put("month_budget", monthlyBudget)
                put("user", userId)
                put("date", date)
            }
            db.insert("monthly_budget", null, contentValues)


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //db.execSQL("CREATE TABLE category_budget (cb_id INTEGER PRIMARY KEY AUTOINCREMENT, category INTEGER, cat_budget INTEGER, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, user INTEGER, FOREIGN KEY(category) REFERENCES category(category_id), FOREIGN KEY(user) REFERENCES user(user_id))")

    fun changeCategoryBudgetByMonth(categoryBudget: Int, date: String, category: String) {
        try {
            println("new budget inserted $categoryBudget")
            val userId = SessionManager.getLoggedInUserId() // get user id that is registered

            val db = writableDatabase
            println("Searched category: $category")

            // Hae category_id annetulle category-nimelle
            val categoryIdQuery = "SELECT category_id FROM category WHERE category_name = ?"
            val cursor = db.rawQuery(categoryIdQuery, arrayOf(category))
            var categoryId: Int? = null
            if (cursor.moveToFirst()) {
                val categoryIdIndex = cursor.getColumnIndex("category_id")
                categoryId = cursor.getInt(categoryIdIndex)
            }
            cursor.close()

            // Jos category_id löytyi, jatka tallentamista
            if (categoryId != null) {
                val contentValues = ContentValues().apply {
                    put("cat_budget", categoryBudget)
                    put("user", userId)
                    put("date", date)
                    put("category", categoryId)
                }
                db.insert("category_budget", null, contentValues)
                printCategoryBudgets()
            } else {
                println("Category not found: $category")
            }

            db.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    //function gets logged in users category budget by category name and mont/year
    fun getSelectedMonthsCategoryBudget(userId: Long, categoryName: String, selectedMonth: Int, selectedYear: Int): Int? {
        val db = readableDatabase
        var categoryBudget: Int? = null
        val month = "%02d".format(selectedMonth)
        val year = selectedYear.toString()


        try {
            val query = "SELECT cb.cat_budget FROM category_budget AS cb JOIN category AS c ON cb.category = c.category_id WHERE cb.user = ? AND c.category_name = ? AND strftime('%m', cb.date) = ? AND strftime('%Y', cb.date) = ? ORDER BY cb_id DESC, date DESC LIMIT 1"
            Log.d("DatabaseQuery", "Query: $query, User: ${userId}, Category: ${categoryName}, Month: $month, Year: $year")
            val cursor = db.rawQuery(query, arrayOf(userId.toString(),categoryName, month, year))
            if (cursor.moveToFirst()) {
                val categoryBudgetIndex = cursor.getColumnIndex("cat_budget")
                if (categoryBudgetIndex >= 0) {
                    categoryBudget = cursor.getInt(categoryBudgetIndex)

                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        println("Categorys $categoryName budget is: $categoryBudget")
        return categoryBudget
    }

    //get selected months purchases
    fun getSelectedMonthsPurchases(userId: Long, selectedMonth: Int, selectedYear: Int): List<Double> {
        val db = readableDatabase
        val values = mutableListOf<Double>()
        val month = "%02d".format(selectedMonth)
        val year = selectedYear.toString()
        val query = "SELECT value FROM purchase WHERE user = ? AND strftime('%m', date) = ? AND strftime('%Y', date) = ? ORDER BY date DESC"
        Log.d("DatabaseQuery", "Query: $query, User: ${userId}, Month: $month, Year: $year")

        val cursor = db.rawQuery(query, arrayOf(userId.toString(), month, year))

        try {
            if (cursor.moveToFirst()) {
                do {
                    val value = cursor.getDouble(cursor.getColumnIndexOrThrow("value"))
                    values.add(value)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
        }
        return values
    }

    fun addCategoryBudgetsForNewMonthlyBudget(userId: Long, date: String) {
        writableDatabase.use { db ->
            val insertCategoryBudgetQuery = "INSERT INTO category_budget (category, cat_budget, date, user) " +
                    "SELECT category, cat_budget, '$date', user " +
                    "FROM category_budget " +
                    "WHERE (category, user, date) IN ( " +
                    "   SELECT category, user, MAX(date) " +
                    "   FROM category_budget " +
                    "   WHERE user = $userId " +
                    "   GROUP BY category, user " +
                    ");"


            db.execSQL(insertCategoryBudgetQuery)
            println("category budgets added")
        }
    }

    //This function is used to add new budgets with previous values when month changes
    fun updateBudgetsForNewMonth(userId: Long) {
        writableDatabase.use { db ->
            val insertCategoryBudgetQuery = "INSERT INTO category_budget (category, cat_budget, date, user) " +
                    "SELECT category, cat_budget, CURRENT_TIMESTAMP, user " +
                    "FROM category_budget " +
                    "WHERE (category, user, date) IN ( " +
                    "   SELECT category, user, MAX(date) " +
                    "   FROM category_budget " +
                    "   WHERE user = $userId " +
                    "   GROUP BY category, user " +
                    ");"

            val insertMonthlyBudgetQuery = "INSERT INTO monthly_budget (month_budget, date, user) " +
                    "SELECT month_budget, CURRENT_TIMESTAMP, user " +
                    "FROM monthly_budget " +
                    "WHERE (user, date) IN ( " +
                    "   SELECT user, MAX(date) " +
                    "   FROM monthly_budget " +
                    "   WHERE user = $userId " +
                    "   GROUP BY user " +
                    ");"

            db.execSQL(insertCategoryBudgetQuery)
            db.execSQL(insertMonthlyBudgetQuery)
        }
    }

    // updates selected purchase
    fun updatePurchase(purchase: Purchase): Int {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put("name", purchase.name)
            put("value", purchase.price.toInt())
            put("category", purchase.category)
            put("date", purchase.date)
            put("user", purchase.userId)
        }
        val success = db.update("purchase", contentValues, "purchase_id = ?", arrayOf(purchase.purchaseId.toString()))
        db.close()
        return success
    }

    //deletes the purchase
  fun deletePurchase(purchase: Purchase): Int {
        val db = writableDatabase
        val success = db.delete("purchase", "purchase_id = ?", arrayOf(purchase.purchaseId.toString()))
        db.close()
        return success
    }


    //dashboard

    // get total money spent in a category for a given month and year and user
    fun getTotalExpenses(userId: Long, categoryName: String, month: Int, year: Int): Double {
        val db = readableDatabase
        val monthYear = String.format("%04d-%02d", year, month)
        // joins purchase with category on category_id and filters by user, category name and date
        val query = """
        SELECT SUM(p.value) AS total_expense FROM purchase p
        JOIN category c ON p.category = c.category_id
        WHERE p.user = ? AND c.category_name = ? AND strftime('%Y-%m', p.date) = ?
    """
        val cursor = db.rawQuery(query, arrayOf(userId.toString(), categoryName, monthYear))
        var totalExpense = 0.0
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex("total_expense")
            if (columnIndex != -1) {
                totalExpense = cursor.getDouble(columnIndex)
            }
        }
        cursor.close()
        return totalExpense
    }



    fun getSelectedMonthsAndCategoriesPurchases(userId: Long, categoryName: String, selectedMonth: Int, selectedYear: Int): List<Purchase> {
        val db = readableDatabase
        val monthFormatted = String.format("%02d", selectedMonth)
        val yearFormatted = selectedYear.toString()
        val query = """
        SELECT p.purchase_id, p.name, p.value, p.category, p.date
        FROM purchase p
        JOIN category c ON p.category = c.category_id
        WHERE p.user = ? AND c.category_name = ? AND strftime('%m', p.date) = ? AND strftime('%Y', p.date) = ?
        ORDER BY p.date DESC
    """

        Log.d("SQLQuery", "Executing query: $query")
        Log.d("SQLParams", "Params: userId=$userId, categoryName=$categoryName, month=$monthFormatted, year=$yearFormatted")

        val cursor = db.rawQuery(query, arrayOf(userId.toString(), categoryName, monthFormatted, yearFormatted))
        val purchases = mutableListOf<Purchase>()

        if (cursor.moveToFirst()) {
            do {
                val purchaseId = cursor.getLong(cursor.getColumnIndexOrThrow("purchase_id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val price = cursor.getDouble(cursor.getColumnIndexOrThrow("value"))
                val category = cursor.getInt(cursor.getColumnIndexOrThrow("category"))
                val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))

                purchases.add(Purchase(purchaseId, name, price, category, date, userId))
            } while (cursor.moveToNext())
        } else {
            Log.d("SQLQuery", "No data returned.")
        }
        cursor.close()
        return purchases
    }

    //db.execSQL("CREATE TABLE treat (treat_id INTEGER PRIMARY KEY AUTOINCREMENT, treat_name TEXT NOT NULL, value INTEGER DEFAULT 0, user INTEGER NOT NULL, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user) REFERENCES user(user_id))")
    //db.execSQL("CREATE TABLE saved (saving_id INTEGER PRIMARY KEY AUTOINCREMENT, saving_value INTEGER DEFAULT 0, user INTEGER NOT NULL, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user) REFERENCES user(user_id))")
    //add treat to db
    fun addTreat(name: String, value: Int) {
        val db = writableDatabase
        val userId = SessionManager.getLoggedInUserId()

        try {
            val contentValues = ContentValues().apply {
                put("treat_name", name)
                put("value", value)
                put("user", userId)
            }
            db.insertOrThrow("treat", null, contentValues)
            addSavings(0.0)
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        } finally {
            db.close()
        }
    }

    //get treat from db
    fun getTreat(): Pair<String?, Int?> {
        val db = readableDatabase
        val userId = SessionManager.getLoggedInUserId()
        var treat: String? = null
        var treatValue: Int? = null

        try {
            val query = "SELECT treat_name, value FROM treat WHERE user = ? ORDER BY treat_id DESC, date DESC"
            Log.d("DatabaseQuery", "Query: $query, User: $userId")
            val cursor = db.rawQuery(query, arrayOf(userId.toString()))
            if (cursor.moveToFirst()) {
                val treatIndex = cursor.getColumnIndex("treat_name")
                val treatValueIndex = cursor.getColumnIndex("value")
                if (treatIndex >= 0 && treatValueIndex >= 0) {
                    treat = cursor.getString(treatIndex)
                    treatValue = cursor.getInt(treatValueIndex)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }

        return Pair(treat, treatValue)
    }

    fun getTreatDate(): String? {
        val db = readableDatabase
        val userId = SessionManager.getLoggedInUserId()
        var treatDate: String? = null


        try {
            val query = "SELECT date FROM treat WHERE user = ? ORDER BY treat_id DESC, date DESC"
            Log.d("DatabaseQuery", "Query: $query, User: $userId")
            val cursor = db.rawQuery(query, arrayOf(userId.toString()))
            if (cursor.moveToFirst()) {
                val treatDateIndex = cursor.getColumnIndex("date")

                if (treatDateIndex >= 0) {
                    treatDate = cursor.getString(treatDateIndex)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }

        return treatDate
    }

    //

    private fun addSavings(value: Double) {
        val db = writableDatabase
        val userId = SessionManager.getLoggedInUserId()

        try {
            val contentValues = ContentValues().apply {
                put("saving_value", value)
                put("user", userId)
            }
            db.insertOrThrow("saved", null, contentValues)
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        } finally {
            //
        }

    }

    fun getSavings(): Triple<Int?, Double?, String?> {
        val db = readableDatabase
        val userId = SessionManager.getLoggedInUserId()
        var savingsId: Int? = null
        var savingsValue: Double? = null
        var savingsDate: String? = null

        try {
            val query = "SELECT saving_id, saving_value, date FROM saved WHERE user = ? ORDER BY saving_id DESC, date DESC"
            Log.d("DatabaseQuery", "Query: $query, User: $userId")
            val cursor = db.rawQuery(query, arrayOf(userId.toString()))
            if (cursor.moveToFirst()) {
                val savingsIdIndex = cursor.getColumnIndex("saving_id")
                val savingsValueIndex = cursor.getColumnIndex("saving_value")
                val savingsDateIndex = cursor.getColumnIndex("date")
                if (savingsValueIndex >= 0 && savingsValueIndex >= 0) {
                    savingsId = cursor.getInt(savingsIdIndex)
                    savingsValue = cursor.getDouble(savingsValueIndex)
                    savingsDate = cursor.getString(savingsDateIndex)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }

        return Triple(savingsId, savingsValue, savingsDate)
    }

    fun updateSavings(savingsId: Int, savingsValue: Double) {
        val db = writableDatabase
        val currentDateTime = java.time.LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val savingsDate =  currentDateTime.format(formatter)


        val contentValues = ContentValues().apply {
            put("saving_value", savingsValue)
            put("date", savingsDate)
        }
        return try {
            val result =
                db.update("saved", contentValues, "saving_id = ?", arrayOf(savingsId.toString()))
            db.close()
            println("user: $result")
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        }
    }

}
