package com.petcare.petcare.Models

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

object MapsModels {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference


    fun setupInicial (){
        auth = FirebaseAuth.getInstance()
    }

}