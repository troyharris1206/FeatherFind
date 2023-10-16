package com.example.featherfind.profile

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.featherfind.GetStarted
import com.example.featherfind.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class Profile : Fragment() {

    companion object {
        fun newInstance() = Profile()
    }

    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        val deleteButton = requireView().findViewById<Button>(R.id.btnDeleteAccount)
        deleteButton.setOnClickListener {
            showPopup(requireContext()) // 'this' is the context of your activity
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

    fun deleteUser(){
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        user?.delete()
            ?.addOnCompleteListener{task ->
                if(task.isSuccessful){
                    Toast.makeText(context, "User deleted successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, GetStarted::class.java)
                    startActivity(intent)
                }else{
                    val errorMessage = task.exception?.message
                    Toast.makeText(context, "User deletion failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }
}