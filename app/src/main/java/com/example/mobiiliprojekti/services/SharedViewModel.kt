package com.example.mobiiliprojekti.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _userId = MutableLiveData<Long>()
    val userId: LiveData<Long> get() = _userId

    fun setUserId(userId: Long) {
        _userId.value = userId
        println("SharedViewModelissa saatu userid: $userId")
    }
}
