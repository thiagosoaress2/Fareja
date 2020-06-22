package com.petcare.petcare

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import java.util.*


class listaDePetsRecyclerViewAdapter(private var context: Context, private var array:MutableList<String>): RecyclerView.Adapter<listaDePetsRecyclerViewAdapter.ViewHolder>() {

    var cont=0
    var max = array.size/7  //mas vai controlar position

    override fun getItemCount(): Int {
        return array.size;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.lista_de_pets_itemrow, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //aqui você define os valores em cada elemento

        /*

            0-nome
            1-lat
            2-long
            3-bd
            4-raio
            5-plano
            6-impulsionamentos
        */

        var calc = position*7

        if (position>=max){
            holder.background.visibility = View.GONE
            //nothing
        } else {
            holder.textViewNome.visibility = View.VISIBLE
            holder.textViewNome.text = array.get(calc)

        }




    }


    class ViewHolder(itemView: View?) :
        RecyclerView.ViewHolder(itemView!!) {
        //aqui você associa cada elemento a um nome para invocar nesta classe
        val textViewNome: TextView = itemView!!.findViewById(R.id.listaDePetsItemrow_nomePet)
        val background: ConstraintLayout=itemView!!.findViewById(R.id.listaDePetsItemrow_background)

    }

}