package com.example.featherfind.add_sighting

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
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
        hotspotLatitude: Double? = null,
        hotspotLongitude: Double? = null,
        photoReference: String? = null,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (hotspotLatitude != null && hotspotLongitude != null) {
            // Hotspot coordinates are provided, use them
            addSightingInfo(birdName, birdSpecies, dateOfSighting, timeOfSighting, sightingDescription, hotspotLatitude, hotspotLongitude,photoReference, onSuccess, onFailure)
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // No hotspot coordinates, use current location
            val locationTask: Task<Location> = fusedLocationClient.lastLocation
            locationTask.addOnSuccessListener { location: Location? ->
                location?.let {
                    addSightingInfo(birdName, birdSpecies, dateOfSighting, timeOfSighting, sightingDescription, location.latitude, location.longitude,photoReference, onSuccess, onFailure)
                }
            }.addOnFailureListener { exception ->
                onFailure(exception)
            }
        } else {
            onFailure(SecurityException("Location permission not granted"))
        }
    }

    fun addSightingWithCurrentLocation(
        context: Context,
        birdName: String,
        birdSpecies: String,
        dateOfSighting: String,
        timeOfSighting: String,
        sightingDescription: String,
        photoReference: String? = null,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationTask: Task<Location> = fusedLocationClient.lastLocation
            locationTask.addOnSuccessListener { location: Location? ->
                location?.let {
                    // Use current location for sighting
                    addSightingInfo(
                        birdName,
                        birdSpecies,
                        dateOfSighting,
                        timeOfSighting,
                        sightingDescription,
                        location.longitude,
                        location.latitude,
                        photoReference,
                        onSuccess,
                        onFailure
                    )
                } ?: onFailure(Exception("Location data not available"))
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
        photoReference: String? = null,
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
    fun uploadPhoto(photo: Any, context: Context, onPhotoUploaded: (String) -> Unit) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val photoName = "photos/${System.currentTimeMillis()}.jpg"
        val photoRef = storageRef.child(photoName)

        val uploadTask = when (photo) {
            is Uri -> photoRef.putFile(photo)
            is Bitmap -> {
                val baos = ByteArrayOutputStream()
                photo.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()
                photoRef.putBytes(data)
            }
            else -> {
                Toast.makeText(context, "Unsupported photo type", Toast.LENGTH_SHORT).show()
                return
            }
        }

        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Fetching the download URL of the uploaded photo
            photoRef.downloadUrl.addOnSuccessListener { uri ->
                // Notify user that the photo has been uploaded successfully
                Toast.makeText(context, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show()

                // Pass the download URL to the callback
                onPhotoUploaded(uri.toString())
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(context, "Error occurred: $exception", Toast.LENGTH_SHORT).show()
        }
    }
}