package com.example.mobiiliprojekti.services

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _userId = MutableLiveData<Int>()
    val userId: LiveData<Int> get() = _userId

    fun setUserId(userId: Int) {
        _userId.value = userId
        Log.d("SharedViewModel", "setUserId set with: $userId")
        Log.d("SharedViewModel","SharedViewModel userid: $userId")
    }
}
