package com.petcare.petcare.Utils

import android.content.SharedPreferences
import android.provider.Settings.Global.getString
import com.facebook.internal.Mutable
import com.petcare.petcare.R

class userClass {

    // property (data member)
    var userMail = "nao"
    var userBD = "nao"
    var alvara = "nao"
    var tipo = "usuario"
    var petBDseForEmpresario = "nao"
    var bdDoPet = "nao"
    var plano = "nao"
    var imgDoUser = "nao"

    fun simpleUser(email: String, tipo: String, userBd: String, imgdoUser: String){
        this.userMail = email;
        this.tipo = tipo;
        this.userBD = userBd;
        this.imgDoUser = imgdoUser;

    }

    fun empresarioUser(email: String, tipo: String, userBd: String, petBd: String, bdDoPet: String, plano: String, imgdoUser: String){
        this.userMail = email;
        this.tipo = tipo;
        this.userBD = userBd;
        this.imgDoUser = imgdoUser;
        this.petBDseForEmpresario = petBd;
        this.bdDoPet = bdDoPet;
        this.plano = plano;
        this.imgDoUser = imgdoUser;

    }

    // member function
    fun getUserInfo() : MutableList<String> {

        val infos: MutableList<String> = ArrayList()


        return infos
    }


    // member function
    fun getProprietarioInfo() : MutableList<String> {

        val infos: MutableList<String> = ArrayList()


        return infos
    }


    // member function
    fun getAutonomoInfo() : MutableList<String> {

        val infos: MutableList<String> = ArrayList()


        return infos;
    }

}