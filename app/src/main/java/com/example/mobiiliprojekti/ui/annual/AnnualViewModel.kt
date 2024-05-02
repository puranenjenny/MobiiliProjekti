package com.example.mobiiliprojekti.ui.annual

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AnnualViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Annual Fragment"
    }
    val text: LiveData<String> = _text
}