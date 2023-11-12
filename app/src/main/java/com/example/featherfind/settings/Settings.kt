package com.example.featherfind.settings

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.featherfind.databinding.FragmentSettingsBinding
import com.example.featherfind.R
import android.widget.RadioButton
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.featherfind.Users
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class Settings : Fragment() {

    companion object {
        fun newInstance() = Settings()
    }

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(layoutInflater)

        val radioGroup = binding.radioGroup
        val txtTravelDistance = binding.txtTravelDistance
        val txtUserDistance = binding.txtUserDistance
        val txtMinAndMax = binding.txtMinAndMax
        val btnApplySettings = binding.btnApplySettings
        val rbMetric = binding.rbMetric
        val rbImperial = binding.rbImperial

        //Gets the current user logged in
        val user = FirebaseAuth.getInstance().currentUser

        //If there is a logged in user
        if (user != null) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val firestore = FirebaseFirestore.getInstance()
            val userDocument = firestore.collection("Users").document(userId.toString())

            //Gets the user's details from the db
            userDocument.get()
                .addOnSuccessListener { documentSnapshot: DocumentSnapshot? ->
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val userData = documentSnapshot.toObject(Users::class.java)

                        Log.d("SettingsFragment", "UID: $userId")
                        Log.d("SettingsFragment", "UserData: $userData")

                        //If the user setting for measurementType is set to metric
                        if (userData?.measurementSystem == "Metric") {
                            rbMetric.isChecked = true
                            txtUserDistance.setText(userData?.maxDistance)
                        }
                        //If the user setting for measurementType is set to imperial
                        else {
                            rbImperial.isChecked = true
                            txtUserDistance.setText(userData?.maxDistance)
                        }
                    }
                }
                .addOnFailureListener { exception: Exception ->
                    Log.e("ProfileFragment", "Firestore error: ${exception.message}")
                    Toast.makeText(context, "Unable to fetch profile.", Toast.LENGTH_SHORT).show()
                }

        }

        //When the user chooses a different measurementType from the radio buttons
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            // Check which radio button is selected
            val radioButton = binding.root.findViewById<RadioButton>(checkedId)
            when (radioButton.id) {
                //If the metric option is chosen
                R.id.rbMetric -> {
                    txtTravelDistance.text = "Maximum Travel Distance (Kilometers)"
                    txtMinAndMax.text = "Min Value: 0.10\nMax Value: 5000.00"
                    txtUserDistance.setText(convertToKilometers(txtUserDistance.text.toString()))
                }
                //If the imperial option is chosen
                R.id.rbImperial -> {
                    txtTravelDistance.text = "Maximum Travel Distance (Miles)"
                    txtMinAndMax.text = "Min Value: 0.0062\nMax Value: 3106.86"
                    txtUserDistance.setText(convertToMiles(txtUserDistance.text.toString()))
                }
            }
        }

        btnApplySettings.setOnClickListener() {
            val navController = findNavController()
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val firestore = FirebaseFirestore.getInstance()
            val userDocument = firestore.collection("Users").document(userId.toString())

            if (txtUserDistance.text.isNotEmpty()) {
                try {
                    val distance = txtUserDistance.text.toString().replace(',', '.').toDouble()

                    val updates = HashMap<String, Any>()

                    if (rbMetric.isChecked && distance <= 5000 && distance >= 0.1) {
                        updates["measurementSystem"] = "Metric"
                        updates["maxDistance"] = distance.toString()
                    } else if (rbMetric.isChecked && distance > 5000) {
                        Toast.makeText(
                            context,
                            "Max distance is out of range specified.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    } else if (rbMetric.isChecked && distance < 0.1) {
                        Toast.makeText(
                            context,
                            "Max distance is out of range specified.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    } else if (rbImperial.isChecked && distance <= 3106.86 && distance >= 0.0062) {
                        updates["measurementSystem"] = "Imperial"
                        updates["maxDistance"] = distance.toString()
                    } else if (rbImperial.isChecked && distance > 3106.86) {
                        Toast.makeText(
                            context,
                            "Max distance is out of range specified.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }else if (rbImperial.isChecked && distance < 0.0062) {
                        Toast.makeText(
                            context,
                            "Max distance is out of range specified.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    userDocument.update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Successfully updated user settings",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.navigate(R.id.navigation_profile)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "Unable to update settings: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } catch (e: NumberFormatException) {
                    Toast.makeText(
                        context,
                        "Please enter a valid number for max distance",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    "Please enter a max distance value.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return binding.root
    }

        //Converts miles into kilometers
        private fun convertToKilometers(miles: String): String {
            return try {
                val milesValue = miles.replace(',', '.').toDouble()
                val kilometersValue = milesValue * 1.60934 // 1 mile = 1.60934 kilometers
                String.format("%.2f", kilometersValue)
            } catch (e: NumberFormatException) {
                // Handle the exception, maybe return a default value or alert the user
                "Invalid number"
            }
        }

    //Converts kilometers into miles
    private fun convertToMiles(kilometers: String): String {
        return try {
            val kilometersValue = kilometers.replace(',', '.').toDouble()
            val milesValue = kilometersValue / 1.60934 // 1 kilometer = 1/1.60934 miles
            String.format("%.2f", milesValue)
        } catch (e: NumberFormatException) {
            // Handle the exception, maybe return a default value or alert the user
            "Invalid number"
        }
    }
}
