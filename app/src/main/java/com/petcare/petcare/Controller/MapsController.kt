package com.petcare.petcare.Controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.petcare.petcare.MapsActivity
import com.petcare.petcare.Models.MapsModels
import com.petcare.petcare.Utils.mySharedPrefs
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.acos

object MapsController {

    fun requestToOpenGpsLikeWaze (activity: Activity){
        val locationRequest : LocationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(30*1000)
        locationRequest.setFastestInterval(5*1000)
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        builder.setAlwaysShow(true)

        val result: Task<LocationSettingsResponse> =
            LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build())
        result.addOnCompleteListener(object : OnCompleteListener<LocationSettingsResponse?> {
            override fun onComplete(@NonNull task: Task<LocationSettingsResponse?>) {

                try {
                    task.getResult(ApiException::class.java)
                } catch (exception: ApiException) {
                    when (exception.getStatusCode()) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val resolvable: ResolvableApiException =
                                exception as ResolvableApiException
                            resolvable.startResolutionForResult(activity, 100)
                        } catch (e: IntentSender.SendIntentException) {

                        } catch (e: ClassCastException) {
                            Log.d("testeGps", e.message)
                        }
                    }
                }
            }
        })
    }

    fun getLatLong (endereco: String, petBD: String, activity: Activity){

        val geocoder = Geocoder(activity)
        val address : List<Address>?

        try {
            address = geocoder.getFromLocationName(endereco,1)

            if (address==null) {

                //nao achou nada e fica pra proxima

            } else {
                var location: Address = address.get(0)

                MapsModels.saveLatLongInPetshopWithouLocationYet(petBD, location)
            }
        }catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

    }

    //descobre o lat e long a partir do endereço
    fun findLocationFromAdress(endereco: String, activity: Activity) : Address? {

        val geocoder = Geocoder(activity)
        val address : List<Address>?
        try {
            address = geocoder.getFromLocationName(endereco,1)
            if (address==null || address.size==0) {
                //makeToast("Nenhum endereço encontrado.")
                return null
            } else {

                val location : Address = address.get(0)

                return location
            }


        }catch (e: IOException) {
            return null
        }


    }

    fun getAddressOnlyPaisParaAnuncioProprio(latLng: LatLng, activity: Activity): String {
        // 1
        val geocoder = Geocoder(activity)
        val addresses: List<Address>?
        var pais: String = "nao"

        try {
            // 2
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3
            if (null != addresses && !addresses.isEmpty()) {


                if (addresses[0].countryName == null){
                    return "nao"
                } else {
                    pais = addresses[0].countryName.toString()
                }

            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return pais
    }

    fun getAddress(latLng: LatLng, activity: Activity): String {
        // 1
        val geocoder = Geocoder(activity)
        val addresses: List<Address>?
        //val address: Address?
        var addressText = ""

        val enderecoUser: MutableList<String> = ArrayList()

        try {
            // 2
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3
            if (null != addresses && !addresses.isEmpty()) {


                if (addresses[0].countryName == null){

                } else {
                    enderecoUser.add(addresses[0].countryName)
                }

                if (addresses[0].postalCode == null){

                } else {
                    enderecoUser.add(addresses[0].postalCode)
                }

                if (addresses[0].adminArea == null){ //estado

                } else {
                    enderecoUser.add(addresses[0].adminArea)
                }

                //este é diferente pq as vezes o estado vem em subadminarea e as vezes em locality. Entao ele testa
                if (addresses[0].locality == null) {
                    //mUserCidade = addresses[0].subAdminArea
                    enderecoUser.add(addresses[0].subAdminArea)
                } else {
                    //mUserCidade = addresses[0].locality
                    enderecoUser.add(addresses[0].locality)
                }

                if (addresses[0].subLocality == null){

                } else{
                    enderecoUser.add(addresses[0].subLocality)
                }

                if (addresses[0].subThoroughfare == null){

                } else {
                    enderecoUser.add(addresses[0].subThoroughfare)
                }

                if (addresses[0].thoroughfare == null){

                } else {
                    enderecoUser.add(addresses[0].thoroughfare)
                }



                var cont=0
                val size = enderecoUser.size-1  //pq o tamanho conta o 0. Entãodigamos, um array de tamanho 6 vai só até 5. Ai dava erro.
                while (cont<enderecoUser.size){
                    addressText = addressText+" "+enderecoUser.get(size-cont).toString()
                    cont++
                }
                /*
                /*
                array   pos 0 - cidade
                        pos 1 - estado
                        pos 2 - bairro
                        pos 3 - numero Casa
                        pos 4 - rua
                        pos 5 - cep
                 */
                addressText =
                    enderecoUser.get(4) + " nº " + enderecoUser.get(3) + ", " + enderecoUser.get(2) + ", " + enderecoUser.get(0) + " - " + enderecoUser.get(1)

                 */
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return addressText
    }

    fun getAddressOnlyEstadoParaAnuncioProprio(latLng: LatLng, activity: Activity): String {
        // 1
        val geocoder = Geocoder(activity)
        val addresses: List<Address>?
        var estado: String = "nao"

        try {
            // 2
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3
            if (null != addresses && !addresses.isEmpty()) {


                if (addresses[0].adminArea == null){ //estado

                } else {
                    estado = addresses[0].adminArea
                }

            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return estado
    }

    fun getAddressOnlyCidadeParaAnuncioProprio(latLng: LatLng, activity: Activity): String {
        // 1
        val geocoder = Geocoder(activity)
        val addresses: List<Address>?
        var cidade: String = "nao"

        try {
            // 2
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3
            if (null != addresses && !addresses.isEmpty()) {


                if (addresses[0].locality == null){
                    cidade = addresses[0].subAdminArea
                } else{
                    //enderecoUser.add(addresses[0].subLocality)
                    cidade = addresses[0].locality

                }

            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return cidade
    }

    //ajusta o zoom para aparecer o petshop mais distante
    fun calculateZoomToFit(distanceInMeter: Float) : Float {

        var zoom = 0.0f
        //teste
        if (distanceInMeter < 400.000){
            zoom = 18.0f
        } else if (distanceInMeter < 550.000){
            zoom = 16.0f
        } else if (distanceInMeter < 1010.000 ){
            zoom = 15.0f
        } else if (distanceInMeter < 2020.000){
            zoom = 13.0f
        } else if (distanceInMeter < 3050.000){
            zoom = 12.7f
        } else if (distanceInMeter <= 4050.000){
            zoom = 12.5f
        } else if (distanceInMeter >4050.000){
            zoom = 11.8f
        }
        return zoom

    }


    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var activeNetworkInfo: NetworkInfo? = null
        activeNetworkInfo = cm.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

    fun makeToast(mensagem: String, activity: Activity){
        Toast.makeText(activity, mensagem, Toast.LENGTH_SHORT).show()
    }

    fun CalculeEstesValoresEmDinheiro(val1:String, val2:String, quantidade:Int) :String{

        var precoMedio = val1.replace("R$", "")
        precoMedio = precoMedio.replace("R$", "")
        var precoaqui = val2.replace("R$", "")
        precoMedio = precoMedio.replace(",", ".")
        var precoMedioBigDec = precoMedio.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
        precoaqui = precoaqui.replace(",", ".")
        var precoAquiBigDec = precoaqui.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
        precoMedioBigDec = precoMedioBigDec+precoAquiBigDec
        precoMedio = precoMedioBigDec.toString()
        precoMedio = precoMedio.replace(".", ",")
        precoMedio = "R$"+precoMedio

        return precoMedio

    }

    fun CalculeSeEsteEoMenorPreco(valorDoBd: String, valorDaNovaVenda: String) :String {

        var precoArmazenado = valorDoBd.replace("R$", "")
        precoArmazenado = precoArmazenado.replace("R$", "")
        var precoNovo = valorDaNovaVenda.replace("R$", "")
        precoArmazenado = precoArmazenado.replace(",", ".")
        var precoArmazenadoBigDec = precoArmazenado.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
        precoNovo = precoNovo.replace(",", ".")
        var precoNovoBigDec = precoNovo.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)

        val menor: BigDecimal
        //menor ou igual pra ja contemplar essa possibilidade e não precisar fazer alteração
        if (precoArmazenadoBigDec<=precoNovoBigDec){
            menor = precoArmazenadoBigDec
        } else {
            menor = precoNovoBigDec
        }

        //aqui vamos usar a variavel de preco armazenado apenas pra nao precisar criar outra. Mas na verdade ela está passando o valor de val menor
        precoArmazenado = menor.toString()
        precoArmazenado = precoArmazenado.replace(".", ",")
        precoArmazenado = "R$"+precoArmazenado

        return precoArmazenado

    }

    fun GetMonthWithYear(): String{
        val c: Calendar = GregorianCalendar()
        c.time = Date()
        val sdf = SimpleDateFormat("MMMM YYYY")
        var datafinal: String = sdf.format(c.time)
        datafinal = datafinal.trim()
        return  datafinal
        //fazer a query para ver se ja existe um node com o nome do produto
    }

    fun GetDate () : String {

        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.format(Date())

        return currentDate
    }

    fun GetfutureDate (daysToAdd: Int) : String {

        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.format(Date())

        val c = Calendar.getInstance()
        c.time = sdf.parse(currentDate)
        c.add(Calendar.DATE, daysToAdd) // number of days to add

        var tomorrow: String = sdf.format(Date())
        tomorrow = sdf.format(c.time) // dt is now the new date

        return tomorrow

    }

    //pega a hora
    fun GetHour () : String {

        val sdf = SimpleDateFormat("hh:mm")
        val currentDate = sdf.format(Date())

        return currentDate
    }

    fun getDifferenceInTwoDates (dateStart: String, dateStop: String, hourToStart: String, hourToStop: String, activity: Activity) : String {

        //format to imput "01/14/2012 09:29:58";
        //Este exemplo estou assumindo que usei GetHours e GetDate. Então hora é dd/MM/yyyy  e hora é 00:00 sem segundos
        val dataStartComplete = dateStart+" "+hourToStart
        val dataFinalComplete = dateStop+" "+hourToStop

        var d1: Date? = null
        var d2: Date? = null

        //HH converts hour in 24 hours format (0-23), day calculation

        //HH converts hour in 24 hours format (0-23), day calculation
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm")
        d1 = format.parse(dataStartComplete)
        d2 = format.parse(dataFinalComplete)
        var dateFinal : String = "nao"

        try {

            //in milliseconds
            val diff = d2.getTime() - d1.getTime()
            val diffSeconds = diff / 1000 % 60
            val diffMinutes = diff / (60 * 1000) % 60
            val diffHours = diff / (60 * 60 * 1000) % 24
            val diffDays = diff / (24 * 60 * 60 * 1000)
            //print("$diffDays days, ")
            //print("$diffHours hours, ")
            //print("$diffMinutes minutes, ")
            //print("$diffSeconds seconds.")
            //output  1 days, 1 hours, 1 minutes, 50 seconds.

            dateFinal = diffDays.toString()+" dias, "+diffHours+" horas e "+diffMinutes+" min"

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }


        return dateFinal

    }

    fun ConvertDateToMillis (dataArmazenada: String) :Long {

        //primeiro converte o string para Calendar
        var sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.parse(dataArmazenada)
        val calendarDate: Calendar = sdf.calendar

        return calendarDate.timeInMillis
    }

    fun removeSpecialCharsAndToLowerCase(word: String): String {

        var x = word.replace("á", "a")
        x = x.replace("à", "a")
        x = x.replace("é", "e")
        x = x.replace("ê", "e")
        x = x.replace("â", "")
        x = x.replace("ó", "o")
        x = x.replace("ô", "o")
        x = x.replace("ú","u")
        x = x.replace("ü", "u")
        x = x.toLowerCase()

        return x
    }

    fun scaleDown(realImage: Bitmap, maxImageSize: Float, filter: Boolean): Bitmap? {
        val ratio = Math.min(maxImageSize / realImage.width,maxImageSize / realImage.height)
        val width = Math.round(ratio * realImage.width)
        val height = Math.round(ratio * realImage.height)
        return Bitmap.createScaledBitmap(realImage, width, height, filter)
    }

    fun temLembreteHoje(activity: Activity) : Boolean {

        val mySharedPrefs: mySharedPrefs = mySharedPrefs(activity)

        val dateToRemember = mySharedPrefs.getValue("rememberDate")

        if (!dateToRemember.equals("nao")){ //se nao tiver é pq nao tem lembrete. O user nao comprou ração.
            //tem lembrete. Mas vamos ver se é a data certa pra mostrar.
            val dataHoje = MapsController.GetDate()
            //ja temos a data de hoje e a data armazenada para lembrar.
            //agora vamos transformar ela em um objeto Date para podermos comparar
            val format = SimpleDateFormat("dd/MM/yyyy")
            val date1 = format.parse(dateToRemember)
            val date2 = format.parse(dataHoje)

            if (date1.compareTo(date2) >=0){  //se for hoje ou no futuro

                //val bdDoPet = mySharedPrefs.getValue("rememberBdDoPEt")
                //val nomeProduto = mySharedPrefs.getValue("remeberNomeProduto")

                return true
            } else {
                return false
            }


        } else {
            return false
        }

    }

    fun currencyTranslation(valorOriginal: String): String{

        //passar o valor para string para poder ver o tamanho
        var valorString = valorOriginal.toString()
        valorString = valorString.trim()
        valorString.replace("R$", "")
        valorString.replace(".", "")
        valorString.replace(",", "")

        //na casa de menos de 100 mil
        //90.000 - 5 casas
        //entre 100 mil e 1 mi
        //100.000
        //entre 1 milhão pra cima
        //1.000,000
        if (valorString.length ==3){ //exemplo 002 222 012  fica 0,02 2,22 0,12

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            valorString = sb.toString()

        } else if (valorString.length == 4){ // 1234  fica 12,34

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            valorString = sb.toString()
        } else if (valorString.length==5){ //12345  fica 123,45

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            valorString = sb.toString()

        } else if (valorString.length==6){ //123456  fica 1.234,56

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(1, ".")
            valorString = sb.toString()

        } else if (valorString.length==7){ //1234567  fica 12.345,67

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(2, ".")
            valorString = sb.toString()

        } else if (valorString.length==8){ //12345678  fica 123.456,78

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(3, ".")
            valorString = sb.toString()

        }  else if (valorString.length==9){ //123456789  fica 1.234.567,89

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(4, ".")
            sb.insert(1, ".")
            valorString = sb.toString()

        }  else if (valorString.length==10){ //1234567890  fica 12.345.678,90

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(5, ".")
            sb.insert(2, ".")
            valorString = sb.toString()

        }  else if (valorString.length==11){ //12345678901  fica 123.456.789,01

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(6, ".")
            sb.insert(3, ".")
            valorString = sb.toString()

        }

        valorString = "R$"+valorString
        return valorString

    }


}