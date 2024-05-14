package com.example.mobiiliprojekti

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import com.example.mobiiliprojekti.services.SharedViewModel

class LoginActivity : AppCompatActivity() {

    private val sharedViewModel: SharedViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedViewModel.setUserId(-1L) // initialize the viewmodel with invalid ID

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

    // sharedviewmodel
//    fun onLoginSuccess(userId: Long) {
  //      sharedViewModel.setUserId(userId)
    //    val intent = Intent(this, MainActivity::class.java)
      //  startActivity(intent)
        //finish() // close LoginActivity
    //}
}