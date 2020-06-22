package com.petcare.petcare


import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class MinhasFotosNoPerfilPublicoAutonomoRecyclerViewAdapter (private var context: Context, private var arrayFotos:MutableList<String>): RecyclerView.Adapter<MinhasFotosNoPerfilPublicoAutonomoRecyclerViewAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return arrayFotos.size;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.minhasfotos_autonomoperfil_itemrow, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //aqui você define os valores em cada elemento
        //holder.tvNome.text = arrayNome.get(position)
        Glide.with(context).load(arrayFotos.get(position)).centerCrop().transform(RoundedCorners(15))
            .into(holder.imageView)

    }


    class ViewHolder(itemView: View?) :
        RecyclerView.ViewHolder(itemView!!) {
        //aqui você associa cada elemento a um nome para invocar nesta classe
        var imageView: ImageView = itemView!!.findViewById(R.id.autonomoPublicPerfil_imageview)

    }



}