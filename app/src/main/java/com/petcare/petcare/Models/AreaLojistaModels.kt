package com.petcare.petcare.Models

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
        items.add("Bravecto Cães até 4,5 kg")
        items.add("Bravecto Cães de 4,5 a 10 kg")
        items.add("Bravecto Cães de 10 a 20 kg")
        items.add("Bravecto Cães de 20Kg a 40Kg")
        items.add("Bravecto Cães de 40 a 56 kg")
        items.add("Golden Fórmula Cães Adultos Frango e Arroz - 15kg")
        items.add("Golden Fórmula Cães Adultos Frango e Carne - 15kg")
        items.add("Golden Cães Filhotes Raças Grandes Frango e Arroz - 15kg")
        //aqui é a parte nova adição de 25/06/2020
        items.add("Ração Golden Duo para Gatos Adultos Sabor Cordeiro e Salmão")
        items.add("Ração Golden para Gatos Adultos Castrados Sabor Salmão")
        items.add("Ração Golden Gatos Filhotes Sabor Frango")
        items.add("Ração Golden para Gatos Adultos Castrados Sabor Frango")
        items.add("Ração Golden para Gatos Adultos Sabor Carne")
        items.add("Ração Golden para Gatos Adultos Castrados Sabor Carne")
        items.add("Ração Golden Seleção Natural para Gatos Filhotes Sabor Frango e Arroz")
        items.add("Ração Golden para Gatos Adultos Castrados Seleção Natural Abóbora")
        items.add("Ração Golden para Gatos Adultos Sabor Frango")
        items.add("Ração Golden para Gatos Adultos Sabor Salmão")
        items.add("Ração Golden Seleção Natural para Gatos Adultos Sabor Frango")
        items.add("Ração Golden para Gatos Sênior Castrados sabor Frango")

        items.add("Gaviz V Omeprazol 10mg Strip")
        items.add("Gaviz 20mg Omeprazol Strip")
        items.add("Drontal para Gatos")
        items.add("Biosan Flora Probiótico B12 Pet")
        items.add("Herbalvet Desinfetante Ourofino T.A")
        items.add("Alergovet Coveli para Cães e Gatos Até 15kg")
        items.add("Alergovet Coveli para Cães e Gatos Acima de 15kg")

        return items

    }

    fun getAllImages(): MutableList<Int>{

        val images: MutableList<Int> = ArrayList()
        images.add(R.drawable.banco_bravectoate45)
        images.add(R.drawable.banco_bravecto45a10)
        images.add(R.drawable.banco_bravecto10a20)
        images.add(R.drawable.banco_bravecto20a40)
        images.add(R.drawable.banco_bravecto46a56)
        images.add(R.drawable.banco_goldenformulaespecialadultosfrangoearroz)
        images.add(R.drawable.banco_goldenformulaespecialadultosfrangoecarne)
        images.add(R.drawable.banco_goldencaesfilhotesracasgrandesfrangoearroz15kg)
        //novas de 25/06
        images.add(R.drawable.banco_goldenduo)
        images.add(R.drawable.banco_goldengatossalmaocastrado)
        images.add(R.drawable.banco_goldengatosfrangofilhotes)
        images.add(R.drawable.banco_goldengatosfrangocastrados)
        images.add(R.drawable.banco_goldengatoscarneadultos)
        images.add(R.drawable.banco_goldengatoscarnecastrados)
        images.add(R.drawable.banco_goldenselecaonaturalfilhotesfrango)
        images.add(R.drawable.banco_goldenselecaonaturalcastradosabobora)
        images.add(R.drawable.banco_goldengatosfrangoadultos)
        images.add(R.drawable.banco_goldengatosadultossalmao)
        images.add(R.drawable.banco_goldengatosselecaonaturalfrangoearrozadultos)

        images.add(R.drawable.banco_gavizomeprazol10mg)
        images.add(R.drawable.banco_gavizomeprazol20mg)
        images.add(R.drawable.banco_drontalgatos)
        images.add(R.drawable.banco_biosan)
        images.add(R.drawable.banco_herbalvet)
        images.add(R.drawable.banco_alergovetate15)
        images.add(R.drawable.banco_alergovetacima15)


        return images
    }

    fun getAllDesc(): MutableList<String>{

        val items: MutableList<String> = ArrayList()
        items.add("Antipulgas e Carrapatos Bravecto MSD para Cães até 4,5 kg")
        items.add("Antipulgas e Carrapatos Bravecto MSD para Cães de 4,5 a 10 kg")
        items.add("Antipulgas e Carrapatos Bravecto MSD para Cães de 10 a 20 kg")
        items.add("Antipulgas e Carrapatos Bravecto MSD para Cães de 20Kg a 40Kg")
        items.add("Antipulgas e Carrapatos Bravecto MSD para Cães de 40 a 56 kg")
        items.add("Ração Golden  Fórmula para Cães Adultos Sabor Frango e Arroz - 15kg")
        items.add("Ração Golden  Fórmula para Cães Adultos Sabor Frango e Carne - 15kg")
        items.add("Ração Golden Cães Filhotes Raças Grandes Sabor Frango e Arroz - 15kg")

        //novos 25/06
        items.add("Ração Golden Duo para Gatos Adultos Sabor Cordeiro e Salmão")
        items.add("Ração Golden para Gatos Adultos Castrados Sabor Salmão")
        items.add("Ração Golden Gatos Filhotes Sabor Frango")
        items.add("Ração Golden para Gatos Adultos Castrados Sabor Frango")
        items.add("Ração Golden para Gatos Adultos Sabor Carne")
        items.add("Ração Golden para Gatos Adultos Castrados Sabor Carne")
        items.add("Ração Golden Seleção Natural para Gatos Filhotes Sabor Frango e Arroz")
        items.add("Ração Golden para Gatos Adultos Castrados Seleção Natural Abóbora")
        items.add("Ração Golden para Gatos Adultos Sabor Frango")
        items.add("Ração Golden para Gatos Adultos Sabor Salmão")
        items.add("Ração Golden Seleção Natural para Gatos Adultos Sabor Frango")

        items.add("Gaviz V Omeprazol 10mg Strip com 10 Comprimidos")
        items.add("Gaviz 20mg Omeprazol Strip - 10 comprimidos")
        items.add("Drontal para Gatos - 4 comprimidos")
        items.add("Biosan Flora Probiótico B12 Pet 14 g")
        items.add("Herbalvet Desinfetante Ourofino T.A - 1L")
        items.add("Alergovet Coveli para Cães e Gatos Até 15kg - 10 Comprimidos")
        items.add("Alergovet Coveli para Cães e Gatos Acima de 15kg - 10 Comprimidos")

        return items

    }


    fun getAllTipos(): MutableList<String>{

        /*
        tipo="racao"
        tipo="servicos"
        tipo="acessorios"
        tipo="estetica"
        tipo="remedios"
         */
        val items: MutableList<String> = ArrayList()
        items.add("remedios")
        items.add("remedios")
        items.add("remedios")
        items.add("remedios")
        items.add("remedios")
        items.add("racao")
        items.add("racao")
        items.add("racao")

        //25/06
        items.add("racao")
        items.add("racao")
        items.add("racao")
        items.add("racao")
        items.add("racao")
        items.add("racao")
        items.add("racao")
        items.add("racao")
        items.add("racao")
        items.add("racao")
        items.add("racao")
        items.add("racao")

        items.add("remedios")
        items.add("remedios")
        items.add("remedios")
        items.add("remedios")
        items.add("remedios")
        items.add("remedios")
        items.add("remedios")

        return items

    }


}