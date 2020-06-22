package com.petcare.petcare.Models

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference

//controla ida e volta de dados do bd
object MainModels {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference


    fun setupInicial (){
        auth = FirebaseAuth.getInstance()
    }

    fun checkAuth () : Boolean {
        val currentUser = auth.currentUser
        if (currentUser == null){
            auth.signOut()
            return false
        } else {
            return true
        }
    }

    fun returnUser (): String {
        val currentUser = auth.currentUser
        if (currentUser==null){
            return "nao"
        } else {
            return "unknown"
        }
    }

    fun isEmailVerified () : Boolean {
        val currentUser = auth.currentUser
        return isEmailVerified()
    }

    fun getUserMail () : String {
        val user: FirebaseUser? = auth.currentUser
        return user?.email.toString()
    }

    fun getLoginType (): String {

        val user: FirebaseUser? = auth.currentUser
        var valor:Int =0
        var provedor: String;

        if (user != null) {
            for (userInfo in user.getProviderData()) {
                if (userInfo.getProviderId().equals("facebook.com")) {
                    valor=1
                }
            }
        } else {
            valor=2
        }

        if (valor==1){ //se entrar neste if é pq é facebook e ai não precisa verificar e-mail
            provedor="facebook"
        } else {
            provedor="mail"
        }

        return provedor
    }
}