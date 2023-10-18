package com.example.featherfind

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class ResetPassword : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val btnResetPassword = findViewById<Button>(R.id.btnExploreHotspots)
        val email = findViewById<EditText>(R.id.txtEmailReset)
        val txtLogin = findViewById<TextView>(R.id.txtBackToLogin)

        //When the user clicks the reset password button
        btnResetPassword.setOnClickListener(){
            val auth = FirebaseAuth.getInstance()

            //Sends an email to the registered email account with instructions to reset their password
            //Only occurs if the email entered is registered and valid
            auth.sendPasswordResetEmail(email.text.toString())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, Login::class.java)
                        startActivity(intent)
                    } else {
                        val errorMessage = task.exception?.message
                        Toast.makeText(this, "Error sending password reset email: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        txtLogin.setOnClickListener(){
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }
}