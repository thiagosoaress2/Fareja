package com.petcare.petcare

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MeusServicosContratadosAdapter(private var context: Context, private var arrayServicosPrestados:MutableList<String>): RecyclerView.Adapter<MeusServicosContratadosAdapter.ViewHolder>() {

    val delim:String = "!?!%"

    override fun getItemCount(): Int {
        return arrayServicosPrestados.size;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.meusservicosprestados_recyclerview_itemrow, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        /*  arrayServicoPrestado
            pos 0 - data
            pos 1 - hora
            pos 2 - status
            pos 3 - apelido
            pos 4 - servico
            pos 6 - bd do servico
         */

        val tokens = StringTokenizer(arrayServicosPrestados.get(position), delim)
        val data = tokens.nextToken() // this will contain "img link or nao"
        val hora = tokens.nextToken()
        val status = tokens.nextToken()
        val apelido = tokens.nextToken()
        val servico = tokens.nextToken()
        //val bdCliente = tokens.nextToken()
        //val bdDoServico = tokens.nextToken()

        //aqui você define os valores em cada elemento
        holder.textHora.text = hora
        holder.textData.text = data
        holder.textStatus.text = status
        holder.textApelido.text = apelido
        holder.textServico.text = servico
        if (status.equals("encerrado")){
            holder.iconAlert.setImageResource(R.drawable.ic_done)
        }

    }


    class ViewHolder(itemView: View?) :
        RecyclerView.ViewHolder(itemView!!) {
        //aqui você associa cada elemento a um nome para invocar nesta classe
        var textHora: TextView = itemView!!.findViewById(R.id.itemrow_hora)
        var textData: TextView = itemView!!.findViewById(R.id.itemrow_data)
        var textStatus: TextView = itemView!!.findViewById(R.id.itemrow_status)
        var iconAlert: ImageView = itemView!!.findViewById(R.id.iv_iconAlert)
        var textApelido: TextView = itemView!!.findViewById(R.id.itemrow_nome)  //nome é apelido
        var textServico: TextView = itemView!!.findViewById(R.id.itemrow_servico)

    }

}
