package com.petcare.petcare

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class listaDeAutonomosRecyclerViewAdapter(private var context: Context, private var array:MutableList<String>): RecyclerView.Adapter<listaDeAutonomosRecyclerViewAdapter.ViewHolder>() {

    var cont=0
    //var max = array.size/2  //mas vai controlar position

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
            1-bd

        */


        val tokens = StringTokenizer(array.get(position), "!?!00!")
        val nome = tokens.nextToken() // this will contain "Fruit"
        val bdAutonomo = tokens.nextToken() // this will contain " they
        val servico = tokens.nextToken()


            Log.d("teste", "O valor que vai em textviewnome é "+nome)
            holder.textViewNome.visibility = View.VISIBLE
            holder.textViewNome.text = nome
            holder.textViewServico.text = servico
        holder.textViewServico.visibility = View.VISIBLE





    }


    class ViewHolder(itemView: View?) :
        RecyclerView.ViewHolder(itemView!!) {
        //aqui você associa cada elemento a um nome para invocar nesta classe
        val textViewNome: TextView = itemView!!.findViewById(R.id.listaDePetsItemrow_nomePet)
        val background: ConstraintLayout =itemView!!.findViewById(R.id.listaDePetsItemrow_background)
        val textViewServico: TextView = itemView!!.findViewById(R.id.listaDePetsItemrow_servico)

    }

}