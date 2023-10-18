package com.example.featherfind.home

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.featherfind.R
import com.example.featherfind.databinding.FragmentAddSightingBinding
import com.example.featherfind.databinding.FragmentExploreBinding
import com.example.featherfind.databinding.FragmentHomeBinding
import com.example.featherfind.databinding.FragmentSettingsBinding
import com.example.featherfind.databinding.FragmentSightingsBinding

class Home : Fragment() {

    companion object {
        fun newInstance() = Home()
    }

    private lateinit var viewModel: HomeViewModel
    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)

        val btnExplore = binding.btnExploreHotspots
        val btnAddSighting = binding.btnAddYourSightings
        val btnViewObservations = binding.btnViewYourObservations
        val navController = findNavController()

        //When the user clicks the explore button
        btnExplore.setOnClickListener(){
            navController.navigate(R.id.navigation_explore)
        }

        //When the user clicks the add sighting button
        btnAddSighting.setOnClickListener(){
            navController.navigate(R.id.navigation_add_sightings)
        }

        //When the user clicks the view sightings button
        btnViewObservations.setOnClickListener(){
            navController.navigate(R.id.navigation_sightings)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        // TODO: Use the ViewModel
    }

}