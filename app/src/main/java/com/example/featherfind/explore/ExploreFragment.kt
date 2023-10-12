package com.example.featherfind.explore

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.Manifest
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.Intent
import com.example.featherfind.R


class ExploreFragment : Fragment() {
    private lateinit var viewModel: ExploreViewModel
    private val PERMISSION_REQUEST_CODE = 1001  // Any unique number

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        val adapter = BirdAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        val searchEditText: EditText = view.findViewById(R.id.searchBird)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val btnHotspot: Button = view.findViewById(R.id.btnHotspots)

        viewModel = ViewModelProvider(this).get(ExploreViewModel::class.java)

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
        })
        // Check for permissions using coroutine
        lifecycleScope.launch {
            if (hasLocationPermission()) {
                // Fetch both birds and histograms
                viewModel.initiateFetchBirdsAndHistograms()
            } else {
                // Request permissions
                requestLocationPermissions()
            }
        }

        viewModel.birdList.observe(viewLifecycleOwner, Observer { birds ->
            // Update RecyclerView
            if (birds != null && birds.isNotEmpty()) {
                adapter.updateData(birds)
            } else {
                // Handle empty list scenario
            }
        })
        // Observe changes to the bird list and update the RecyclerView
        viewModel.filteredBirdList.observe(viewLifecycleOwner, Observer { birds ->
            // Update RecyclerView
            if (!birds.isNullOrEmpty()) {
                adapter.updateData(birds)
            } else {
                // Handle empty list scenario
            }
        })
        // Set up the text watcher for the search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.filterBirds(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        // Set an OnClickListener to navigate to HotspotFragment
        // Set an OnClickListener to navigate to MapsActivity
        btnHotspot.setOnClickListener {
            val intent = Intent(activity, MapsActivity::class.java)
            startActivity(intent)
        }

    }

    private fun hasLocationPermission(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch birds using coroutine
                lifecycleScope.launch {
                    viewModel.initiateFetchBirdsAndHistograms()
                }
            } else {
                // Permission denied, show a message to the user or disable functionality
                // You can provide feedback to the user here
            }
        }
    }
}
