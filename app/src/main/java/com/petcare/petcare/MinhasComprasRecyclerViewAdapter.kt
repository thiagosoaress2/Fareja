package com.petcare.petcare

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MinhasComprasRecyclerViewAdapter (private var context: Context, private var arrayNome:MutableList<String>, private var arrayData:MutableList<String>, private var arrayHora:MutableList<String>, private var arrayPreco:MutableList<String>, private var arrayBdPet:MutableList<String>, private var arrayStatus:MutableList<String>, private var arrayBdDaCompra:MutableList<String>): RecyclerView.Adapter<MinhasComprasRecyclerViewAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return arrayNome.size;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.minhascompras_itemrow, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //aqui você define os valores em cada elemento
        holder.tvNome.text = arrayNome.get(position)
        holder.tvData.text = arrayData.get(position)
        holder.tvHora.text = arrayHora.get(position)
        holder.tvPreco.text = arrayPreco.get(position)
        holder.tvStatus.text = arrayStatus.get(position)

        if (arrayStatus.get(position).equals("encerrado")){
            //holder.background.setBackgroundColor(Color.parseColor("#ffffff"))
            holder.iconAlert.setImageResource(R.drawable.ic_done)
        } else {
            //o icone de alerta já está setado default.
            //vamos mudar a cor das letras
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.vermelho))
            holder.tvPreco.setTextColor(ContextCompat.getColor(context, R.color.vermelho))
        }
    }


    class ViewHolder(itemView: View?) :
        RecyclerView.ViewHolder(itemView!!) {
        //aqui você associa cada elemento a um nome para invocar nesta classe
        var tvNome: TextView = itemView!!.findViewById(R.id.tvNome)
        var tvData: TextView = itemView!!.findViewById(R.id.tvdata)
        var tvHora: TextView = itemView!!.findViewById(R.id.tvhora)
        var tvPreco: TextView = itemView!!.findViewById(R.id.tvvalor)
        var tvStatus: TextView = itemView!!.findViewById(R.id.tvStatusPedido)
        var background: ConstraintLayout = itemView!!.findViewById(R.id.listaDePetsItemrow_background)
        var iconAlert: ImageView = itemView!!.findViewById(R.id.imageViewResumo)

    }



}
