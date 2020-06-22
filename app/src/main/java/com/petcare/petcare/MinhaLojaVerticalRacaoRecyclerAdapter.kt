package com.petcare.petcare

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class MinhaLojaVertcialRacaoRecyclerAdapter(private var context: Context, private var arrayNome:MutableList<String>): RecyclerView.Adapter<MinhaLojaVertcialRacaoRecyclerAdapter.ViewHolder>() {

    var cont=0
    var max = arrayNome.size/5


    //do click do zoom
    var zoom = false

    override fun getItemCount(): Int {
        return arrayNome.size/5
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.minhaloja_recyclerview_item_row_vertical, parent, false))

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //aqui você define os valores em cada elemento

        var calc = position*5

        if (position>max){
            //nothing
        } else {
            holder.textViewNome.text = arrayNome.get(calc);
            Glide.with(context).load(arrayNome.get(calc+1)).apply(RequestOptions.circleCropTransform()).into(holder.img)
            if (arrayNome.get(calc+3).equals("nao")){
                holder.tvDesc.visibility = View.INVISIBLE
            } else{
                holder.tvDesc.setText(arrayNome.get(calc+3))
            }
            holder.tvPreco.text = arrayNome.get(calc+2)
        }

        holder.img.setOnClickListener {

            if (zoom){
                //esta com zoom, tirar zoom
                val animZoomOut = AnimationUtils.loadAnimation(context, R.anim.anim_zoom_out_image)
                holder.img.startAnimation(animZoomOut)
            } else {
                //sem zoom, dar zoom
                val animZoomIn = AnimationUtils.loadAnimation(context, R.anim.anim_zoom_image)
                holder.img.startAnimation(animZoomIn)
            }


        }


        /*
        var calc=0
        while (cont<max){
            calc = position*5

            if (calc<arrayNome.size){
                holder.textViewNome.text = arrayNome.get(calc);
                Glide.with(context).load(arrayNome.get(calc+1)).apply(RequestOptions.circleCropTransform()).into(holder.img)
                if (arrayNome.get(position)==null){
                    holder.tvDesc.visibility = View.INVISIBLE
                } else{
                    holder.tvDesc.setText(arrayNome.get(calc+3))
                }
                holder.tvPreco.text = arrayNome.get(calc+2)
            }

            cont++
        }

         */

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
