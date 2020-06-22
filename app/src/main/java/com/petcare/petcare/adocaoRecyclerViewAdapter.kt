package com.petcare.petcare

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class adocaoRecyclerViewAdapter(private var context: Context, private var arrayNome:MutableList<String>, private var arrayImg:MutableList<String>, private var arrayBdUser:MutableList<String>, private var arrayBdPet:MutableList<String>, private var arrayDesc:MutableList<String>, private var arrayTipo:MutableList<String>, private var arrayWhats:MutableList<String>): RecyclerView.Adapter<adocaoRecyclerViewAdapter.ViewHolder>() {
    override fun getItemCount(): Int {
        return arrayNome.size;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.adocao_linerow, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //aqui você define os valores em cada elemento



        holder.textViewNome.text = arrayNome.get(position);

            //nao tem imagem, exibir uma imagem vazia

        Glide.with(context).load(arrayImg.get(position)).transform(RoundedCorners(15)).into(holder.img)

    }


    class ViewHolder(itemView: View?) :
        RecyclerView.ViewHolder(itemView!!) {
        //aqui você associa cada elemento a um nome para invocar nesta classe
        val textViewNome: TextView = itemView!!.findViewById(R.id.adocao_itemrow_tvNome)
        val img: ImageView = itemView!!.findViewById(R.id.adocao_itemrow_iv)

    }

}
