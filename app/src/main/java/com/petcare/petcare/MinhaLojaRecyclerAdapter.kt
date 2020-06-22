package com.petcare.petcare

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MinhaLojaRecyclerAdapter(private var context: Context, private var arrayNome:MutableList<String>, private var arrayImg:MutableList<String>, private var arrayDesc:MutableList<String>, private var arrayPreco:MutableList<String>, private var arrayBD:MutableList<String>): RecyclerView.Adapter<MinhaLojaRecyclerAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return arrayNome.size;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.minhaloja_recyclerview_item_row, parent, false))

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //aqui você define os valores em cada elemento

        holder.textViewNome.text = arrayNome.get(position);
        Glide.with(context).load(arrayImg.get(position)).into(holder.img)

        if (arrayDesc.get(position)==null){
            holder.tvDesc.visibility = View.INVISIBLE
        } else{
            holder.tvDesc.setText(arrayDesc.get(position))
        }

        holder.tvPreco.text = arrayPreco.get(position)

    }


    class ViewHolder(itemView: View?) :
        RecyclerView.ViewHolder(itemView!!) {

        //aqui você associa cada elemento a um nome para invocar nesta classe
        var textViewNome: TextView = itemView!!.findViewById(R.id.minhaloja_row_tvNome)
        var img: ImageView = itemView!!.findViewById(R.id.minhaloja_row_img)
        var tvDesc: TextView = itemView!!.findViewById(R.id.minhaloja_row_tvDesc)
        var tvPreco: TextView = itemView!!.findViewById(R.id.minhaloja_row_tvPreco)

    }

}
