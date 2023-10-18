package com.example.featherfind

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val txtEmail = findViewById<EditText>(R.id.txtEmailLogin)
        val txtPassword = findViewById<EditText>(R.id.txtPasswordLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtCreateAccount = findViewById<TextView>(R.id.txtCreateAccount)
        val txtForgotPassword = findViewById<TextView>(R.id.txtForgotPassword)

        txtForgotPassword.setOnClickListener(){
            val intent = Intent(this, ResetPassword::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener(){
            // Validate input fields
            if (txtEmail.text.isEmpty() || txtPassword.text.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(txtEmail.text.toString(), txtPassword.text.toString())
        }

        txtCreateAccount.setOnClickListener(){
            val intent = Intent(this, CreateAccount::class.java)
            startActivity(intent)
        }
    }

    //Used to check whether the entered email and password is correct
    fun loginUser(email: String, password: String) {
        val auth = FirebaseAuth.getInstance()

        //Checks whether the details matches an entry in Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // If sign in fails
                    val errorMessage = task.exception?.message
                    Toast.makeText(this, "Authentication failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }


}