package com.example.featherfind.add_sighting

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.example.featherfind.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
class AddSightingViewModel : ViewModel() {

    private var photoReference: String = ""
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //Adds the sighting info added by the user into the db
    fun initLocationService(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun addSightingWithLocation(
        context: Context,
        birdName: String,
        birdSpecies: String,
        dateOfSighting: String,
        timeOfSighting: String,
        sightingDescription: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationTask: Task<Location> = fusedLocationClient.lastLocation
            locationTask.addOnSuccessListener { location: Location? ->
                location?.let {
                    addSightingInfo(birdName, birdSpecies, dateOfSighting, timeOfSighting, sightingDescription, location.latitude, location.longitude, onSuccess, onFailure)
                }
            }.addOnFailureListener { exception ->
                onFailure(exception)
            }
        } else {
            onFailure(SecurityException("Location permission not granted"))
        }
    }

    internal fun addSightingInfo(
        birdName: String,
        birdSpecies: String,
        dateOfSighting: String,
        timeOfSighting: String,
        sightingDescription: String,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        val sightingData = hashMapOf(
            "userUID" to uid,
            "birdName" to birdName,
            "birdSpecies" to birdSpecies,
            "dateOfSighting" to dateOfSighting,
            "timeOfSighting" to timeOfSighting,
            "sightingDescription" to sightingDescription,
            "photoReference" to photoReference,
            "latitude" to latitude,
            "longitude" to longitude
        )

        val userDocRef = db.collection("Sightings").document(System.currentTimeMillis().toString())
        userDocRef.set(sightingData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Used to upload the selected photo to Firebase Storage
    fun uploadPhoto(photo: Any, mainActivity: MainActivity) {
        // Firebase storage reference
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        // Assigning the photo name
        val photoName = "photos/${System.currentTimeMillis()}.jpg"
        val photoRef = storageRef.child(photoName)

        when (photo) {
            is Uri -> {
                // Uploads photo to Firebase using Uri
                val uploadTask = photoRef.putFile(photo)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Photo uploaded successfully, assigning the photo reference
                    photoReference = photoName
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        mainActivity,
                        "Error occurred: $exception",
                        Toast.LENGTH_SHORT
                    ).show()
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
                    photoReference = photoName
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        mainActivity,
                        "Error occurred: $exception",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> {
                Toast.makeText(
                    mainActivity,
                    "Unsupported photo type",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }



}