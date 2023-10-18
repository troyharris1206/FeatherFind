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
import android.widget.Toast
import com.example.featherfind.R

/**
 * Fragment class for the Explore feature.
 *
 * This fragment handles the UI and interactions for exploring bird data.
 */
class ExploreFragment : Fragment() {
    private lateinit var viewModel: ExploreViewModel
    private val PERMISSION_REQUEST_CODE = 1001

    /**
     * Inflates the layout for the Explore Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    /**
     * Initializes UI components and sets up data bindings.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        val adapter = BirdAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        val searchEditText: EditText = view.findViewById(R.id.searchBird)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val btnHotspot: Button = view.findViewById(R.id.btnHotspots)

        viewModel = ViewModelProvider(this)[ExploreViewModel::class.java]

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
        })
        lifecycleScope.launch {
            if (hasLocationPermission()) {
                viewModel.initiateFetchBirdsAndHistograms()
            } else {
                requestLocationPermissions()
            }
        }

        viewModel.birdList.observe(viewLifecycleOwner, Observer { birds ->
            if (!birds.isNullOrEmpty()) {
                adapter.updateData(birds)
            }
        })
        viewModel.filteredBirdList.observe(viewLifecycleOwner, Observer { birds ->
            if (!birds.isNullOrEmpty()) {
                adapter.updateData(birds)
            }
        })
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.filterBirds(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        btnHotspot.setOnClickListener {
            val intent = Intent(activity, MapsActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Called when the fragment becomes visible to the user.
     */
    override fun onResume() {
        // Call the superclass implementation.
        super.onResume()

        // Check if the app has location permissions.
        if (hasLocationPermission()) {
            // If location permissions are granted, initiate the asynchronous operation to fetch birds and their histograms.
            lifecycleScope.launch {
                // Asynchronously fetch bird and histogram data.
                viewModel.initiateFetchBirdsAndHistograms()
            }
        } else {
            // If location permissions are not granted, inform the user via a Toast message.
            activity?.runOnUiThread {
                Toast.makeText(
                    context,
                    "Please allow location request to load graphs",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Checks if the app has location permissions.
     *
     * @return True if permissions are granted, false otherwise.
     */
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

    /**
     * Requests location permissions.
     */
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

    /**
     * Handles the result of the permission request.
     *
     * @param requestCode The request code passed in requestPermissions().
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lifecycleScope.launch {
                    viewModel.initiateFetchBirdsAndHistograms()
                }
            } else {
                activity?.runOnUiThread {
                    Toast.makeText(
                        context,
                        "Please allow location request to load graphs",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
