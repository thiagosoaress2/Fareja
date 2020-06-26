package com.petcare.petcare

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

//obs: O nome apos class tem que ser rigorosamente igual ao que vc deu
//ao adapter, cuidado com letra maiuscula
class produtosDoBancoRecyclerAdapter(private var context: Context, private var arrayNome:MutableList<String>, private var arrayImg:MutableList<Int>): RecyclerView.Adapter<produtosDoBancoRecyclerAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return arrayNome.size;

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.produtos_do_banco_itemrow, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //aqui você define os valores em cada elemento
        holder.textViewNome.text = arrayNome.get(position);

        Glide.with(context).load("").placeholder(arrayImg.get(position)).into(holder.img)
        Glide.with(context)

    }


    class ViewHolder(itemView: View?) :
        RecyclerView.ViewHolder(itemView!!) {
        //aqui você associa cada elemento a um nome para invocar nesta classe
        var textViewNome: TextView = itemView!!.findViewById(R.id.produtos_do_banco_tvNome)
        var img: ImageView = itemView!!.findViewById(R.id.produtos_do_banco_img)

    }

}
