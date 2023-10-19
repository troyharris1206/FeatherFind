package com.example.featherfind

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.featherfind.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)

        navView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    // If the Home button is pressed, navigate to the Home fragment
                    navController.navigate(R.id.navigation_home)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_explore -> {
                    // If the Explore button is pressed, navigate to the Explore fragment
                    navController.navigate(R.id.navigation_explore)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_add_sightings -> {
                    // If the Add Sightings button is pressed, navigate to the Add Sightings fragment
                    navController.navigate(R.id.navigation_add_sightings)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_sightings -> {
                    // If the Sightings button is pressed, navigate to the Sightings fragment
                    navController.navigate(R.id.navigation_sightings)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_profile -> {
                    // If the Profile button is pressed, navigate to the Profile fragment
                    navController.navigate(R.id.navigation_profile)
                    return@setOnNavigationItemSelectedListener true
                }
                else -> false
            }
        }

    }
}