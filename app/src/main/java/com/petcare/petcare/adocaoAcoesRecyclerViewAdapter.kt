package com.petcare.petcare

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*


class adocaoAcoesRecyclerViewAdapter(private var context: Context, private var array:MutableList<String>): RecyclerView.Adapter<adocaoAcoesRecyclerViewAdapter.ViewHolder>() {

    val delim="!?!%*"

    override fun getItemCount(): Int {
        return array.size;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.adocao_acoes_itemrow, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //aqui você define os valores em cada elemento

        /*pos 0 - nomepet
            pos 1 - anuncioPet
             poa 2 - adotante ou anunciante
                                 */

        val tokens = StringTokenizer(array.get(position), delim)
        val nomePet = tokens.nextToken() // this will contain "img link or nao"
        val anuncioBd = tokens.nextToken()
        val adotanteOuAnunciante = tokens.nextToken()

        holder.textViewNome.text = nomePet

        if (adotanteOuAnunciante.equals("anunciante")){
            holder.textViewSituacao.setText("O adotante confirmou que já adotou o pet. Você confirma para encerrar este processo?")
        } else {
            holder.textViewSituacao.setText("O anunciante confirmou que você adotou o pet. Você confirma para encerrar este processo?")
        }


    }


    class ViewHolder(itemView: View?) :
        RecyclerView.ViewHolder(itemView!!) {
        //aqui você associa cada elemento a um nome para invocar nesta classe
        val textViewNome: TextView = itemView!!.findViewById(R.id.adocoes_acoes_tvNome)
        val textViewSituacao: TextView = itemView!!.findViewById(R.id.adocoes_acoes_tvSituacao)

    }

}