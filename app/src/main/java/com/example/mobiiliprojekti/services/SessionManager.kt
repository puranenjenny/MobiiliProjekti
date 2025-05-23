package com.example.mobiiliprojekti.services
object SessionManager {

    private var loggedInUserId: Long = -1

    fun setLoggedInUserId(userId: Long) {
        loggedInUserId = userId
    }

    fun getLoggedInUserId(): Long {
        return loggedInUserId
    }

    fun clearLoggedInUserId() {
        loggedInUserId = -1
    }
}