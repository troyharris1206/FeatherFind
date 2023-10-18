package com.example.featherfind

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BirdsAdapter(private val birds: List<Birds>):
    RecyclerView.Adapter<BirdsAdapter.ViewHolder>(){

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.txtBirdName)
        val specieTextView: TextView = itemView.findViewById(R.id.txtBirdSpecies)
        val dateTextView: TextView = itemView.findViewById(R.id.txtBirdDate)
        val descriptionTextView: TextView = itemView.findViewById(R.id.txtBirdDescription)
        val timeTextView: TextView = itemView.findViewById(R.id.txtBirdTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.birds_recycler_layout, parent, false)
            return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bird = birds[position]
        holder.nameTextView.text = bird.birdName
        holder.specieTextView.text = bird.birdSpecies
        holder.dateTextView.text = bird.dateOfSighting
        holder.descriptionTextView.text = bird.sightingDescription
        holder.timeTextView.text = bird.timeOfSighting
    }

    override fun getItemCount(): Int {
            return birds.size
    }
}