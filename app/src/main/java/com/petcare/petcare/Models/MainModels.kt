package com.petcare.petcare.Models

import android.app.Activity
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

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
        if (currentUser!!.isEmailVerified){
            return true
        } else {
            return false
        }
    }

    fun sendEmailVerification(activity: Activity) {
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener(activity) { task ->
                // [START_EXCLUDE]
                // Re-enable button

                if (task.isSuccessful) {
                    Toast.makeText(activity,
                        "E-mail enviado para ${user.email} ",
                        Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity,
                        "Falha no envio do e-mail de verificação.",
                        Toast.LENGTH_SHORT).show()
                }
                // [END_EXCLUDE]
            }
        // [END send_email_verification]
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

    fun createNewUser (){

        databaseReference = FirebaseDatabase.getInstance().reference
        val user: FirebaseUser? = auth.currentUser
        val emailAddress = user?.email

        val newCad: DatabaseReference = databaseReference.child("usuarios").push()
        val userBD = newCad.key.toString()
        newCad.child("email").setValue(emailAddress)
        newCad.child("tipo").setValue("usuario")
        newCad.child("userBD").setValue(userBD)
        newCad.child("nota").setValue(0)
        newCad.child("avaliacoes").setValue(0)
        newCad.child("img").setValue("nao")
    }

}