package com.example.featherfind

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class CreateAccount : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        // Get references to the EditText fields and the Create Account button
        val txtFirstName = findViewById<EditText>(R.id.txtFirstName)
        val txtSurname = findViewById<EditText>(R.id.txtSurname)
        val txtEmail = findViewById<EditText>(R.id.txtEmail)
        val txtPassword = findViewById<EditText>(R.id.txtPassword)
        val txtConfirmPassword = findViewById<EditText>(R.id.txtConfirmPassword)
        val btnCreateAccount = findViewById<Button>(R.id.btnCreateAccount)
        val txtLogin = findViewById<TextView>(R.id.txtHaveAnAccount)

        btnCreateAccount.setOnClickListener{
            // Validate input fields
            if (txtFirstName.text.isEmpty() || txtSurname.text.isEmpty() || txtEmail.text.isEmpty() || txtPassword.text.isEmpty() || txtConfirmPassword.text.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (txtPassword.text.toString() != txtConfirmPassword.text.toString()) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(txtEmail.text.toString(), txtPassword.text.toString(), txtFirstName.text.toString(), txtSurname.text.toString())
        }

        txtLogin.setOnClickListener(){
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

    }

    private fun registerUser(email: String, password: String, firstName: String, surname: String) {
        val auth = FirebaseAuth.getInstance()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success
                    val user = auth.currentUser
                    // Update user's profile with first name and surname
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName("$firstName $surname")
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // User profile updated
                                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                            }
                        }

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val db = FirebaseFirestore.getInstance()

                            val userData = hashMapOf(
                                "email" to email,
                                "firstName" to firstName,
                                "lastName" to surname,
                            )

                            val userDocRef = db.collection("Users").document(user?.uid ?: "")

                            userDocRef.set(userData)
                                .addOnSuccessListener {
                                    Log.d("User Data", "User data added successfully")
                                }
                                .addOnFailureListener { e ->
                                    Log.w("User Data", "Error adding user data", e)
                                }
                        } else {
                            val errorMessage = task.exception?.message
                            Log.e("User Data", "Registration failed: $errorMessage")
                        }

                    }

                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                } else {
                    // If sign up fails, display a message to the user.
                    val errorMessage = task.exception?.message
                    Toast.makeText(this, "Registration failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }

}
