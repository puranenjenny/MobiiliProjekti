package com.example.mobiiliprojekti

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.viewModels
import com.example.mobiiliprojekti.services.SharedViewModel

class LoginActivity : AppCompatActivity() {

    private val sharedViewModel: SharedViewModel by viewModels() //SVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedViewModel.setUserId(-1) // initialize the viewmodel with invalid ID
        Log.d("LoginActivity", "onCreate - User ID initialized to -1")

        val btnRegister: Button = findViewById(R.id.btn_register)
        btnRegister.setOnClickListener {
            val fragmentRegister = RegisterFragment()
            supportFragmentManager.beginTransaction()
                .add(fragmentRegister, "register_dialog")
                .commit()
        }

        val btnLogin: Button = findViewById(R.id.btn_login)
        btnLogin.setOnClickListener {
                val fragmentLogin = LoginFragment()
                supportFragmentManager.beginTransaction()
                    .add(fragmentLogin, "login_dialog")
                    .commit()


        }
    }
}