package com.example.featherfind.profile

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.featherfind.GetStarted
import com.example.featherfind.R
import com.example.featherfind.Users
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class Profile : Fragment() {

    companion object {
        fun newInstance() = Profile()
    }

    private lateinit var viewModel: ProfileViewModel
    private lateinit var firstNameEditText: EditText
    private lateinit var surnameEditText: EditText
    private lateinit var usernameEditText: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        firstNameEditText = view.findViewById(R.id.etFirstname)
        surnameEditText = view.findViewById(R.id.etSurname)

        val user = FirebaseAuth.getInstance().currentUser


        if (user != null) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val firestore = FirebaseFirestore.getInstance()
            val userDocument = firestore.collection("Users").document(userId.toString())

            userDocument.get()
                .addOnSuccessListener { documentSnapshot: DocumentSnapshot? ->
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val userData = documentSnapshot.toObject(Users::class.java)

                        Log.d("ProfileFragment", "UID: $userId")
                        Log.d("ProfileFragment", "UserData: $userData")

                        // Set user attributes to EditText views
                        firstNameEditText.setText(userData?.firstName)
                        surnameEditText.setText(userData?.lastName)
                    }
                }
                .addOnFailureListener { exception: Exception ->
                    Log.e("ProfileFragment", "Firestore error: ${exception.message}")
                    Toast.makeText(context, "Unable to fetch profile.", Toast.LENGTH_SHORT).show()
                }

        }


        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        val deleteButton = requireView().findViewById<Button>(R.id.btnDeleteAccount)
        deleteButton.setOnClickListener {
            showPopup(requireContext()) // 'this' is the context of your activity
        }

        val changepasswordButton = requireView().findViewById<Button>(R.id.btnChangePassword)
        changepasswordButton.setOnClickListener {
            showPopupPassword(requireContext())
        }

        val logoutButton = requireView().findViewById<Button>(R.id.btnLogout)
        logoutButton.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            auth.signOut()
            Toast.makeText(context, "Logged out Successfully.", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, GetStarted::class.java)
            startActivity(intent)

        }

        val applyChangesButton = requireView().findViewById<Button>(R.id.btnApplyChanges)
        val etFirstName = requireView().findViewById<EditText>(R.id.etFirstname)
        val etSurname = requireView().findViewById<EditText>(R.id.etSurname)


        applyChangesButton.setOnClickListener {

            val newFirstName = etFirstName.text.toString()
            val newSurname = etSurname.text.toString()




            if (newFirstName.isNotEmpty() && newSurname.isNotEmpty()) {
                val user = FirebaseAuth.getInstance().currentUser
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                val firestore = FirebaseFirestore.getInstance()
                val userDocument = firestore.collection("Users").document(userId.toString())

                val updates = HashMap<String, Any>()
                updates["firstName"] = newFirstName
                updates["lastName"] = newSurname

                userDocument.update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(
                            context,
                            "Successfully updated user profile",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Unable to update profile in Firestore: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }else{
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showPopup(context: Context){
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.popup_delete_layout, null)

        builder.setView(view)
        val dialog = builder.create()

        val cancelButton = view.findViewById<Button>(R.id.btnCancel)
        val deleteButton = view.findViewById<Button>(R.id.btnDelete)
        val userInput = view.findViewById<EditText>(R.id.etDeleteProfile)

        userInput.setOnClickListener {
            userInput.text.clear()
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        deleteButton.setOnClickListener {
            val enteredText = userInput.text.toString()

            if(enteredText == "yes"){
                deleteUser()
            }else{
                Toast.makeText(context, "not 'yes', cancel or try again.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    fun showPopupPassword(context: Context){
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.popup_changepassword_layout, null)

        builder.setView(view)
        val dialog = builder.create()

        val cancelpasswordButton = view.findViewById<Button>(R.id.btnCancelPassword)
        val confirmButton = view.findViewById<Button>(R.id.btnConfirmPassword)
        val password = view.findViewById<EditText>(R.id.etpassword)
        val retypepassword = view.findViewById<EditText>(R.id.etretypepassword)

        cancelpasswordButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmButton.setOnClickListener {

            if(password.text.isEmpty() || retypepassword.text.isEmpty()){
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.text.toString() != retypepassword.text.toString()) {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(password.text.contains(" ") || retypepassword.text.contains(" ")){
                Toast.makeText(context, "No spaces allowed in password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Change the user's password
            val user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                val newPassword = password.text.toString()

                user.updatePassword(newPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(context, "Failed to update password: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "User is not signed in", Toast.LENGTH_SHORT).show()
            }

        }

        dialog.show()
    }

    fun deleteUser(){

        val user = FirebaseAuth.getInstance().currentUser

        if(user != null){
            val userID= user.uid
            val firestore = FirebaseFirestore.getInstance()
            val userDocument = firestore.collection("Users").document(userID)

            userDocument.delete()
                .addOnSuccessListener {
                    user.delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "User deleted successfully", Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, GetStarted::class.java)
                            startActivity(intent)
                        }
                }
                .addOnFailureListener{e ->
                    Toast.makeText(context, "Unable to delete profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}