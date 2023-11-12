package com.example.featherfind.add_sighting

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.featherfind.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class AddSightingViewModel : ViewModel() {

    private var photoReference: String = ""

    //Adds the sighting info added by the user into the db
    fun addSightingInfo(birdName: String, birdSpecies: String, dateOfSighting: String, timeOfSighting: String, sightingDescription: String){

        val db = FirebaseFirestore.getInstance()

        // Get the current user's UID
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        //Creating a hashMap for input into the db
        val sightingData = hashMapOf(
            "userUID" to uid,
            "birdName" to birdName,
            "birdSpecies" to birdSpecies,
            "dateOfSighting" to dateOfSighting,
            "timeOfSighting" to timeOfSighting,
            "sightingDescription" to sightingDescription,
            "photoReference" to photoReference
        )

        //Adds data to the db
        val userDocRef = db.collection("Sightings").document(System.currentTimeMillis().toString())

        userDocRef.set(sightingData)
            .addOnSuccessListener {
                Log.d("Sighting Data", "Sighting data added successfully")
            }
            .addOnFailureListener { e ->
                Log.w("Sighting Data", "Error adding sighting data", e)
            }

    }

    // Used to upload the selected photo to Firebase Storage
    fun uploadPhoto(photo: Any, mainActivity: MainActivity) {
        // Firebase storage reference
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        // Assigning the photo name
        this.photoReference = "photos/${System.currentTimeMillis()}.jpg"
        val photoRef = storageRef.child(this.photoReference)

        when (photo) {
            is Uri -> {
                // Uploads photo to Firebase using Uri
                val uploadTask = photoRef.putFile(photo)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Photo uploaded successfully, assigning the photo reference
                    Toast.makeText(
                        mainActivity,
                        "Photo successfully added",
                        Toast.LENGTH_SHORT
                    ).show()
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        mainActivity,
                        "Error occurred: $exception",
                        Toast.LENGTH_SHORT
                    ).show()

                    this.photoReference = ""
                }
            }
            is Bitmap -> {
                // Convert Bitmap to ByteArray
                val baos = ByteArrayOutputStream()
                photo.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()

                // Uploads photo to Firebase using ByteArray
                val uploadTask = photoRef.putBytes(data)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Photo uploaded successfully, assigning the photo reference
                    Toast.makeText(
                        mainActivity,
                        "Photo successfully added",
                        Toast.LENGTH_SHORT
                    ).show()
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        mainActivity,
                        "Error occurred: $exception",
                        Toast.LENGTH_SHORT
                    ).show()

                    this.photoReference = ""
                }
            }
            else -> {
                Toast.makeText(
                    mainActivity,
                    "Unsupported photo type",
                    Toast.LENGTH_SHORT
                ).show()

                this.photoReference = ""
            }
        }
    }
}