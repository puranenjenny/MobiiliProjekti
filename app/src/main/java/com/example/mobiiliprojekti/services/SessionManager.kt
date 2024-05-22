package com.example.mobiiliprojekti.services

//object to keep track and pass info between fragments who is logged in user
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