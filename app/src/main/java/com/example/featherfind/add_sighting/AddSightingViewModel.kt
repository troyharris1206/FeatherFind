package com.example.featherfind.add_sighting

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class AddSightingViewModel : ViewModel() {

    private var photoReference: String = ""

    private fun addSightingInfo(birdName: String, birdSpecies: String, dateOfSighting: String, timeOfSighting: String, sightingDescription: String){
        val db = FirebaseFirestore.getInstance()

        val userData = hashMapOf(
            "birdName" to birdName,
            "birdSpecies" to birdSpecies,
            "dateOfSighting" to dateOfSighting,
            "timeOfSighting" to timeOfSighting,
            "sightingDescription" to sightingDescription
        )

        val userDocRef = db.collection("Sightings").document(System.currentTimeMillis().toString())

        userDocRef.set(userData)
            .addOnSuccessListener {
                Log.d("Sighting Data", "Sighting data added successfully")
            }
            .addOnFailureListener { e ->
                Log.w("Sighting Data", "Error adding sighting data", e)
            }
    }


}