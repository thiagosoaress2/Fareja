package com.petcare.petcare.Models

import android.media.Image
import android.net.Uri
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.petcare.petcare.R


object AreaLojistaModels {

    lateinit var databaseReference: DatabaseReference
    lateinit var mFireBaseStorage: FirebaseStorage
    lateinit var mphotoStorageReference: StorageReference

    //envio de imagem
    lateinit var filePath: Uri
    var urifinal: String = "nao"

    //controle do alvara
    var alvaraOk = 0 //alvaraOK = 1 siginfica que o ja enviou o alvara
    var processo = "nao"  //esta variavel vai regular se é um envio de foto normal ou de alvara
    var petBD = "nao"
    var userMail = "nao"
    var userBD = "nao"
    var alvara = "nao"
    var tipo = "usuario"
    var itens_venda = "nao"  //quantidade de itens da loja que está a venda
    var inventario_size = 15  //quantidade de itens que esta loja pode vender. 15 é o padrão
    var endereco = "nao"


    fun init(){
        databaseReference = FirebaseDatabase.getInstance().reference
    }

    fun getAllItems() : MutableList<String> {

        val items: MutableList<String> = ArrayList()
        items.add("Bravecto")
        items.add("Braveco2")
        return items

    }

    fun getAllImages(): MutableList<Int>{

        val images: MutableList<Int> = ArrayList()
        images.add(R.drawable.banco_bravecto)
        images.add(R.drawable.banco_bravecto)

        return images
    }

    fun getAllDesc(): MutableList<String>{

        val items: MutableList<String> = ArrayList()
        items.add("Descrição bravecto 1")
        items.add("Desc brav 2")
        return items

    }

    fun getAllPrecos(): MutableList<String>{

        val items: MutableList<String> = ArrayList()
        items.add("R$15,00")
        items.add("R$25,00")
        return items

    }


}