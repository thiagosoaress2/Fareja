package com.petcare.petcare.Controller

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.view.View
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseUser
import com.petcare.petcare.MapsActivity
import com.petcare.petcare.Models.MainModels

//controla método que precisam de inteligencia
object MainController {

    fun setInicial(){

    }

    fun updateUI(user: String, tipoLogin:String) : String {

        var retorno = "nao"

        if (tipoLogin.equals("mail")) {

            //hideProgressDialog()
            if (user.equals("nao")) {

                if (MainModels.isEmailVerified()==false){
                    retorno = "email_nao_verificado"
                } else {

                    retorno = "email_verificado"

                }
                //verifyEmailButton.isEnabled = !user.isEmailVerified  //tem que mexer aqui ainda

            } else {

                retorno = "email_logado"

            }
        } else if (tipoLogin.equals("unknown")) {  //este if é para o caso do usuario entrar depois, então nao sei qual métood de login mas ainda nao verificou email.

            val tipoLoginMeth = MainModels.getLoginType()

            if (tipoLoginMeth.equals("mail")) { //se for email verifica se a pessoa ja verificou o email. Se nao tiver feito abre a lay com verificacao. Senao vai abrir a proxima activity
                if (!user.equals("nao")){
                    if (MainModels.isEmailVerified()==false){
                        retorno = "email_nao_verificado"
                    } else {
                        retorno = "email_verificado"
                    }
                }

            } else { //aqui é para o caso de nao ser via email. E neste caso, nao precisa verificar nada. Abre direto a segunda activity.

                retorno = "logado"

            }

        } else if (tipoLogin.equals("facebook")) {

            retorno = "facebook"

            //intent.putExtra("key", value)

        } else {

            retorno = "naoLogado"

        }

        return retorno

    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var activeNetworkInfo: NetworkInfo? = null
        activeNetworkInfo = cm.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

}