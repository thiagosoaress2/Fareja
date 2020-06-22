package com.petcare.petcare

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class impulsionarPromoActivity : AppCompatActivity() {


        lateinit var img: String
        lateinit var nome: String
        lateinit var plano: String
        lateinit var bdPetDoEmpresario: String
        lateinit var preco: String
        lateinit var email: String
        lateinit var desc: String

        lateinit var tipo: String
        private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impulsionar_promo)

        databaseReference = FirebaseDatabase.getInstance().reference


        img = intent.getStringExtra("img")
        nome = intent.getStringExtra("nome")
        plano = intent.getStringExtra("plano")
        bdPetDoEmpresario = intent.getStringExtra("bdPet")
        preco = intent.getStringExtra("preco")
        email = intent.getStringExtra("email")
        desc = intent.getStringExtra("desc")
        tipo = intent.getStringExtra("tipo")

        fazOperacao()
    }


    fun fazOperacao (){

        val imgView: ImageView = findViewById(R.id.layPromo_img)
        val tv: TextView = findViewById(R.id.layPromo_tvNome)
        val etPreco: EditText = findViewById(R.id.layPromo_etPreco)
        val btnImpulsionar : Button = findViewById(R.id.layPromo_btnConfirma)
        val btnPreviw: Button = findViewById(R.id.layPromo_btnPrevia)
        val layPreview : ConstraintLayout = findViewById(R.id.layPromo_laypreview)

        Glide.with(this@impulsionarPromoActivity).load(img).apply(RequestOptions.circleCropTransform()).into(imgView)
        tv.setText(nome)

        CurrencyWatcherNew(etPreco)

        var btnFechar : Button = findViewById(R.id.layPromo_btnVoltar)
        btnFechar.setOnClickListener {

            finish()
        }


        btnPreviw.setOnClickListener {
            layPreview.visibility = View.VISIBLE
            layPreview.setOnClickListener {
                layPreview.visibility = View.GONE
            }
        }


        btnImpulsionar.setOnClickListener {
            var preconovo = "nao"
            if (!etPreco.text.toString().contains("00,0") || !etPreco.text.isEmpty()) {
                preconovo = etPreco.text.toString()
            }
            // gerenciaImpulsionamentos(preconovo)  aqui vai entrar o codigo
            var impulsionamento = "nao"
            if (plano.equals("basico") ) {

                //neste caso o usuário tem plano básico e vamos sugerir upgrade
                makeToast("Seu plano não oferece esta funcionalidade.")
                val intent = Intent(this, arealojista::class.java)
                intent.putExtra("petBD", bdPetDoEmpresario)
                intent.putExtra("tipo", "empresario")
                intent.putExtra("especial_plano", "especial")

                //colocar qq coisa nestes. Nao vao ser usados, so pra n dar erro.
                intent.putExtra("userBD", "na")
                intent.putExtra("alvara", "sim")
                intent.putExtra("email", email)

                startActivity(intent)
                finish()

            } else if (plano.equals("pendenteDeUpGrade")){
                makeToast("Seu processo de liberação deste serviço está ocorrendo. Por favor aguarde contato por e-mail")
            } else {

                // Get the checked radio button id from radio group
                val radioGroup: RadioGroup = findViewById(R.id.promoRadioGroup)
                val id: Int = radioGroup.checkedRadioButtonId
                val radio: RadioButton = findViewById(id)
                val valorRadio = radio.text.toString()


                ChamaDialog()
                //pegando dados do plano do usuario
                val rootRef = databaseReference.child("petshops").child(bdPetDoEmpresario)
                rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        //TODO("Not yet implemented")
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        //TODO("Not yet implemented")
                        var values: String
                        values = p0.child("impulsionamentos").getValue().toString()
                        impulsionamento = values

                        if (values.equals("sim")) {
                            makeToast("Você já possui um impulsionamento ativo. Espere terminar para fazer outro.")
                        } else {
                            //publicar impulsionamento

                            databaseReference.child("petshops").child(bdPetDoEmpresario).child("impulsionamentos").setValue("sim")
                            val newCad: DatabaseReference = databaseReference.child("impulsionamentos").push()
                            //petBD = newCad.key.toString()
                            newCad.child("pet").setValue(bdPetDoEmpresario)
                            newCad.child("duracao").setValue(valorRadio)
                            newCad.child("nome_prod").setValue(nome)
                            newCad.child("img_prod").setValue(img)
                            newCad.child("desc_prod").setValue(desc)
                            newCad.child("tipo").setValue(tipo)

                            //definir horarios de inicio
                            newCad.child("hora_inicio").setValue(GetHour())
                            newCad.child("data_inicio").setValue(GetDate())
                            newCad.child("data_final").setValue(GetfutureDate(valorRadio.toInt()))


                            //definir hora e data pra terminar

                            values = p0.child("latlong").getValue().toString()
                            val latlongTempo: Double = values.toDouble()
                            newCad.child("latlong").setValue(latlongTempo)
                            if (etPreco.text.isEmpty()){
                                newCad.child("preco").setValue(preco)
                            } else {
                                newCad.child("preco").setValue(etPreco.text.toString())
                            }

                            finish()
                            EncerraDialog()

                            //salvar no bd da loja que tem um impulsionamento ativo
                        }

                        makeToast("Pronto. Seu impulsionamento já pode ser visto pelos seus clientes.")

                    }

                })
                //aqui pra baix é do else

            }
        }
    }

    //pega  a data
    private fun GetDate () : String {

        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.format(Date())

        return currentDate
    }

    private fun GetfutureDate (daysToAdd: Int) : String {

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
    private fun GetHour () : String {

        val sdf = SimpleDateFormat("hh:mm")
        val currentDate = sdf.format(Date())

        return currentDate
    }

    fun makeToast(mensagem: String){
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
    }

    //Máscara de edição de texto
    fun CurrencyWatcherNew( editText:EditText) {

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

    fun ChamaDialog() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        ) //este serve para bloquear cliques que pdoeriam dar erros
        val layout = findViewById<RelativeLayout>(R.id.LayoutProgressBar)
        layout.visibility = View.VISIBLE
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.visibility = View.VISIBLE
    }

    //este método torna invisivel um layout e encerra o dialogbar spinner.
    fun EncerraDialog() {
        val layout = findViewById<RelativeLayout>(R.id.LayoutProgressBar)
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        layout.visibility = View.GONE
        spinner.visibility = View.GONE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) //libera os clicks
    }

}
