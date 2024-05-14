package com.example.mobiiliprojekti

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.mobiiliprojekti.services.SessionManager

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val sessionManager = SessionManager()
        sessionManager.clearLoggedInUserId()

        // set register button to variable
        val btnRegister: Button = findViewById(R.id.btn_register)

        // add onClickListener to that button
        btnRegister.setOnClickListener {
            // set register fragment to variable
            val fragmentRegister = RegisterFragment()

            // show fragment as dialog
            supportFragmentManager.beginTransaction()
                .add(fragmentRegister, "register_dialog")
                .commit()
        }

        // set login button to variable
        val btnLogin: Button = findViewById(R.id.btn_login)

        // add onClickListener to that button
        btnLogin.setOnClickListener {
                // set login fragment to variable
                val fragmentLogin = LoginFragment()

                // show fragment as dialog
                supportFragmentManager.beginTransaction()
                    .add(fragmentLogin, "login_dialog")
                    .commit()
        }
    }
}