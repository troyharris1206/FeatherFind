package com.example.featherfind.add_sighting

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.featherfind.R

class AddSighting : Fragment() {

    companion object {
        fun newInstance() = AddSighting()
    }

    private lateinit var viewModel: AddSightingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_sighting, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AddSightingViewModel::class.java)
        // TODO: Use the ViewModel
    }

}