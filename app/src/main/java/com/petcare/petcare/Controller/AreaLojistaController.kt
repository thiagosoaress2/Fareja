package com.petcare.petcare.Controller

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.*

object AreaLojistaController {

    fun PrimeiraLetraMaiuscula (text: String) : String {
        val sb: java.lang.StringBuilder = java.lang.StringBuilder(text)
        sb.setCharAt(0, Character.toUpperCase(sb[0]))
        return sb.toString()
    }

    //corrige o valor informado pelo usuário ou pelo BD em dinheiro
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

    fun GetDate () : String {

        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.format(Date())

        return currentDate
    }

    fun CurrencyWatcherNew( editText: EditText) {

        editText.addTextChangedListener(object : TextWatcher {
            var changed: Boolean = false

            override fun afterTextChanged(p0: Editable?) {
                changed = false

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                //changed=false
                editText.setSelection(p0.toString().length)
            }

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                if (!changed) {
                    changed = true

                    var str: String = p0.toString().replace("R$0,00", "")
                    //str = str.replace("00,", "")
                    str = str.replace("R$0", "")
                    //str.replace("0,", "")
                    str = str.replace(",", "").trim()
                    str = str.replace("R$", "").trim()
                    str = str.replace(".", "")

                    if (str.isEmpty()) {

                        //put mascara
                        //ao implementar retirar R$ e virgula no app para não gravar com estes simbolos
                        editText.setText("R$0,00")

                    } else {

                        editText.removeTextChangedListener(this)

                        if (str.length == 1){ //se entrar aqui, significa que era R$00,x na máscara, mas como tiramos R$00, nas verificações iniciais só sobrou o x

                            str = "R$00,"+str  //entra 1 saí 00,1
                        } else if (str.length==2){

                            str = "R$0,"+str  //entre 11 saí 0,11
                        } else if (str.length==3){

                            val sb: StringBuilder = StringBuilder(str)
                            //coloca a virgula no lugar certo
                            sb.insert(str.length - 2, ",")
                            str = sb.toString()
                            str = "R$"+str  //entra 111 saí 1,11
                        } else if (str.length==4){

                            val sb: StringBuilder = StringBuilder(str)
                            //coloca a virgula no lugar certo
                            sb.insert(str.length - 2, ",")
                            str = sb.toString()
                            str = "R$"+str          //entra 1111 saí 11,11
                        } else if (str.length==5){

                            val sb: StringBuilder = StringBuilder(str)
                            //coloca a virgula no lugar certo
                            sb.insert(str.length - 2, ",")
                            str = sb.toString()
                            str = "R$"+str  //entra 11111 saí 111,11
                        } else if (str.length==6){

                            val sb: StringBuilder = StringBuilder(str)
                            //coloca a virgula no lugar certo
                            sb.insert(str.length - 2, ",")
                            sb.insert(1, ".") //adiciona o ponto na segunda casa
                            str = sb.toString()
                            str = "R$"+str  //entra 111111 saí 1.111,11
                        } else if (str.length==7){

                            val sb: StringBuilder = StringBuilder(str)
                            //coloca a virgula no lugar certo
                            sb.insert(str.length - 2, ",")
                            sb.insert(2, ".") //adiciona o ponto na segunda casa
                            str = sb.toString()
                            str = "R$"+str  //entra 1111111 saí 11.111,11
                        } else if (str.length==8){

                            val sb: StringBuilder = StringBuilder(str)
                            //coloca a virgula no lugar certo
                            sb.insert(str.length - 2, ",")
                            sb.insert(3, ".") //adiciona o ponto na segunda casa
                            str = sb.toString()
                            str = "R$"+str  //entra 11111111 saí 111.111,11   ou R$111.111,11
                        } else if (str.length==9){

                            val sb: StringBuilder = StringBuilder(str)
                            //coloca a virgula no lugar certo
                            sb.insert(str.length - 2, ",")
                            sb.insert(4, ".") //adiciona o ponto na segunda casa
                            sb.insert(1, ".") //adiciona o ponto na segunda casa
                            str = sb.toString()
                            str = "R$"+str  //entra 111111111 saí 1.111.111,11   ou R$1.111.111,11
                        } else if (str.length==10){

                            val sb: StringBuilder = StringBuilder(str)
                            //coloca a virgula no lugar certo
                            sb.insert(str.length - 2, ",")
                            sb.insert(5, ".") //adiciona o ponto na segunda casa
                            sb.insert(2, ".") //adiciona o ponto na segunda casa
                            str = sb.toString()
                            str = "R$"+str  //entra 1111111111 saí 11.111.111,11   ou R$11.111.111,11
                        } else if (str.length==11){

                            val sb: StringBuilder = StringBuilder(str)
                            //coloca a virgula no lugar certo
                            sb.insert(str.length - 2, ",")
                            sb.insert(6, ".") //adiciona o ponto na segunda casa
                            sb.insert(3, ".") //adiciona o ponto na segunda casa
                            str = sb.toString()
                            str = "R$"+str  //entra 11111111111 saí 111.111.111,11   ou R$111.111.111,11  (999 milhões)
                        } else if (str.length > 11){
                            //do nothing
                            var sb: StringBuilder = StringBuilder(str)
                            //coloca a virgula no lugar certo
                            sb.deleteCharAt(str.length-1)  //retira o novo número adicionado
                            str = sb.toString()

                            sb = StringBuilder(str)
                            sb.insert(str.length - 2, ",")
                            sb.insert(6, ".") //adiciona o ponto na segunda casa
                            sb.insert(3, ".") //adiciona o ponto na segunda casa
                            str = sb.toString()
                            str = "R$"+str  //recoloca o valor antigo

                        }

                        /*
                        R$00,9 //1 digito
                        R$0,90 //2 digitos
                        R$9,00 //3 digitos
                        R$90,00 //4 digitos
                        R$900,00 //5 digitos
                        R$9.000,00 //6 digitos
                        R$90.000,00 //7 digitos
                        R$900.000,00 //8 digitos
                        R$9.000.000,00 //9 digitos
                        R$90.000.000,00 //10 digitos
                        R$900.000.000,00 ///11 digitos
                        //para em 1 bilhão

                        9     - 1 digito, mas aí ganha ,00 ficando 9,00 (verificar se tem virgula para contar)
                        90    - 2 digitos, mas ai ganha ,00 ficando 90,00 (verificar se tem virgula para contar)
                        900   - 3 digitos, mas ai ganha ,00 ficando 900,00 (verificar se tem virgula para contar)
                        9.000 - 4 digitos
                        99.000 - 5 digitos
                        999.000 - 6 digitos
                        1.000.000 - 7 digitos
                        11.000.000 - 8 digitos
                        121.000.000 - 9 digitos
                        */

                        editText.setText(str)
                        editText.setSelection(str.length)  //coloca o proximo texto no final
                        editText.addTextChangedListener(this)   //recoloca o textWatcher
                    }

                }

            }
        })
    }


}