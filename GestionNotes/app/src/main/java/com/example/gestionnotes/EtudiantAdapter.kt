package com.example.gestionnotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EtudiantAdapter(private val etudiants: List<Etudiant>) :
    RecyclerView.Adapter<EtudiantAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text1: TextView = itemView.findViewById(android.R.id.text1)
        val text2: TextView = itemView.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val etudiant = etudiants[position]

        holder.text1.text = etudiant.nom
        holder.text1.setTextColor(android.graphics.Color.BLACK)
        holder.text1.textSize = 16f

        holder.text2.text = String.format("Note: %.2f - Grade: %s",
            etudiant.note, etudiant.grade)
        holder.text2.setTextColor(android.graphics.Color.DKGRAY)
        holder.text2.textSize = 14f
    }

    override fun getItemCount() = etudiants.size
}