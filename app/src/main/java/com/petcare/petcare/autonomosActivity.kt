package com.petcare.petcare

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import org.w3c.dom.Text
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class autonomosActivity : AppCompatActivity() {

    //envio de imagem
    private lateinit var filePath: Uri
    private var urifinal: String = "nao"
    private lateinit var mphotoStorageReference: StorageReference
    private lateinit var mFireBaseStorage: FirebaseStorage
    private lateinit var databaseReference: DatabaseReference

    var userBd: String = "nao"


    var nome: String = "nao"
    var apelido: String = "nao"
    var ddd: String = "nao"
    var nWhats: String = "nao"
    var servico: String = "nao"
    var fotoDoc:String = "nao"
    var txtDesc: String = "nao"
    var cartao: String = "n"  //pq pode ser sim ou nao depois

    var foto: String = "nao"

    var tela = 1  //para controlar o back

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_autonomos)

        databaseReference = FirebaseDatabase.getInstance().reference

        userBd = intent.getStringExtra("userBD")

        var tipo: String = intent.getStringExtra("tipo")
        if (tipo.equals("autonomo")){
            //exibe edicação
            paginaEdit()
        } else {
            //abre o cadastro padrão
            val textviewInfo: TextView = findViewById(R.id.autonomos_edit_tvSituacao)
            textviewInfo.visibility = View.GONE
            metodosIniciais()
        }

    }

    //define o servico
    fun metodosIniciais(){

        setupPermissions()

        //findViewById<ConstraintLayout>(R.id.autonomos_layEdit).visibility= View.VISIBLE
        val layInicial: ConstraintLayout = findViewById(R.id.autonomos_layInicial)
        layInicial.visibility = View.VISIBLE

        var leu=0
        //coloca uma umagem dentro do imageView da pagina 3 para quando aparecer já estar com a logo
        val ivPerfilUser: ImageView = findViewById(R.id.autonomos_layTerceira_ivPreviewPerfil)
        Glide.with(this@autonomosActivity).asBitmap().load(R.drawable.ic_logo).fitCenter().apply(RequestOptions.circleCropTransform()).into(ivPerfilUser)

        val btnProximo : Button = findViewById(R.id.autonomos_layInicial_btnProximo)
        val radioPasseador: RadioButton = findViewById(R.id.autonomos_layRadioPasseador)
        val radioComidaNat: RadioButton = findViewById(R.id.autonomos_layRadioComidaNat)
        val radioPetTaxi: RadioButton = findViewById(R.id.autonomos_layRadioTaxi)
        val radioSitter: RadioButton = findViewById(R.id.autonomos_layRadioPetSitter)

        //este edittext recebe o texto de servico. O que ocorria é que dava erro quando voltava da foto. Então eprdia a informação. Agora fica armazenada noe dittext e recupero ela depois.
        val txprovisorio: EditText = findViewById(R.id.tvmaintain)

        radioPasseador.setOnClickListener {
            radioComidaNat.isChecked = false
            radioPetTaxi.isChecked = false
            radioSitter.isChecked = false
            servico="passeador"

            txprovisorio.setText(servico)
        }
        radioComidaNat.setOnClickListener {
            radioPasseador.isChecked=false
            radioPetTaxi.isChecked = false
            radioSitter.isChecked = false
            servico="comida"
            txprovisorio.setText(servico)
        }
        radioSitter.setOnClickListener {
            radioPasseador.isChecked=false
            radioPetTaxi.isChecked = false
            radioComidaNat.isChecked = false
            servico="petsitter"
            txprovisorio.setText(servico)
        }
        radioPetTaxi.setOnClickListener {
            radioPasseador.isChecked=false
            radioSitter.isChecked = false
            radioComidaNat.isChecked = false
            servico="pettaxi"
            txprovisorio.setText(servico)
        }

        btnProximo.setOnClickListener {

            if (radioComidaNat.isChecked==false && radioPasseador.isChecked==false && radioPetTaxi.isChecked==false && radioSitter.isChecked==false){
                makeToast("Informe o serviço")
            } else if (leu==0){
                makeToast("Leia os termos e condições antes de prosseguir.")
            } else {

                paginaDois()
                val lay1:ConstraintLayout = findViewById(R.id.autonomos_layInicial)
                lay1.visibility = View.GONE
                val lay2:ConstraintLayout = findViewById(R.id.autonomos_laySegunda)
                lay2.visibility = View.VISIBLE
                btnProximo.setOnClickListener { null }
                radioComidaNat.setOnClickListener { null }
                radioPasseador.setOnClickListener { null }
                radioPetTaxi.setOnClickListener { null }
                radioSitter.setOnClickListener { null }
            }
        }

        val btnTermos: Button = findViewById(R.id.autonomos_layInicial_btnTermos)
        btnTermos.setOnClickListener {
            leu=1
            openPopUp("Termos e Condições", "1 - Ao se cadastrar você se compromete que é capaz de fornecer o serviço oferecido com qualidade não estando sujeito a qualquer treinamento pela plataforma. \n2 - Caberá ao prestador do serviço viablizar formas de pagamentos combinadas com os usuários. \n3 - A plataforma não cobra comissão de seus ganhos na plataforma mas também não se  responsabiliza por possíveis prejuízos e danos. \n4 - Você estará sujeito a avaliações dos usuários no app. \n5 - Farejador apenas disponibiliza um meio digital para que você ofereça seus serviços não ficando estabelecido qualquer tipo de vínculo empregatício. \n6 - Você se compromete a prestar nesta plataforma única e exclusivamente o serviço descrito aqui.\n7 - Ao finalizar seu cadastro você concorda com todas estas normas. \n8 - Antes de aparecer no mapa seu cadastro será sujeito a ánalise.", false, "n", "n")
        }

    }

    fun paginaDois(){

        //val lay1:ConstraintLayout = findViewById(R.id.autonomos_layInicial)
        //val lay2: ConstraintLayout = findViewById(R.id.autonomos_laySegunda)
        //lay1.visibility = View.GONE
        //lay2.visibility = View.VISIBLE

        Log.d("teste", "servicos em pagina dois é "+servico)
        tela = 2

        mFireBaseStorage = FirebaseStorage.getInstance()
        mphotoStorageReference = mFireBaseStorage.reference

        val btnVolta: Button = findViewById(R.id.autonomos_laySegundaBtnVoltar)
        val btnHelp: Button = findViewById(R.id.autonomos_laySegunda_btnHelp)
        val btnEnviaFoto: Button = findViewById(R.id.autonomos_laySegunda_btnTirarFoto)
        val btnProximaPag2: Button = findViewById(R.id.autonomos_laySegunda_btnProxima)

        btnVolta.setOnClickListener {
            findViewById<ConstraintLayout>(R.id.autonomos_layInicial).visibility = View.VISIBLE
            findViewById<ConstraintLayout>(R.id.autonomos_laySegunda).visibility = View.GONE
            btnHelp.setOnClickListener { null }
            btnEnviaFoto.setOnClickListener { null }
            btnProximaPag2.setOnClickListener { null }
        }

        btnHelp.setOnClickListener {
            openPopUp("Ajuda", "Para nós a segurança da comunidade é muito importante. Por isso precisamos da foto do seu documento. Iremos analisar os dados antes de liberarmos as funcioonalidades do aplicativo para você. Por que isso é importânte? Pois assim garantimos que apenas pessoas reais participem de nossa comunidade. É segurança para todos nós.", false, "n", "n")
        }

        btnEnviaFoto.setOnClickListener {
            takePictureFromCamera()
        }

        btnProximaPag2.setOnClickListener {

                makeToast("Primeiro envie a foto do documento")

        }


    }

    fun terceiraPagina () {

        Log.d("teste", "servicos em pagina tres é "+servico)

        tela = 3

        val laypag2: ConstraintLayout = findViewById(R.id.autonomos_laySegunda)
        val laypag3: ConstraintLayout = findViewById(R.id.autonomos_layTerceira)
        laypag2.visibility = View.GONE
        laypag3.visibility = View.VISIBLE

        fotoDoc = urifinal  //urifinal vai ser substituido pela foto do perfil

        val btnEnviarFoto: Button = findViewById(R.id.autonomos_layTerceira_btnEnviarFotoPerfil)
        btnEnviarFoto.setOnClickListener {
            openPopUp("Origem da foto", "Escolha a origem da foto", true, "Tirar foto", "Galeria") //nao precisa do call pois na primeira vez que a popup é usada so precisa do OK. Entao btn s e nao so vao ser usadas agora.
        }

        val btnRadioCartao: RadioButton = findViewById(R.id.autonomos_layTerceira_radioCartao)
        val btnRadioNaoCartao: RadioButton = findViewById(R.id.autonomos_layTerceira_radioNaoCartao)

        btnRadioCartao.setOnClickListener {
            btnRadioNaoCartao.isChecked=false
            cartao="sim"
        }
        btnRadioNaoCartao.setOnClickListener {
            btnRadioCartao.isChecked=false
            cartao="nao"
        }


        val etDesc: EditText = findViewById(R.id.autonomos_layTerceira_etDesc)
        val btnFinalizar: Button = findViewById(R.id.autonomos_layTerceira_btnFinalizar)
        btnFinalizar.setOnClickListener {
            val imageViewPerfil: ImageView = findViewById(R.id.autonomos_layTerceira_ivPreviewPerfil)

            if (imageViewPerfil.isVisible==false){
                makeToast("Primeiro envie a sua foto de perfill")
            } else if (etDesc.text.isEmpty()){
                etDesc.requestFocus()
                etDesc.setError("Descreva seus serviços")
            } else if (btnRadioCartao.isChecked==false && btnRadioNaoCartao.isChecked==false){
                btnRadioCartao.requestFocus()
                makeToast("Informe se aceita cartão ou não")
            } else  {
                //cadastrar
                txtDesc = etDesc.text.toString()
                uploadImagePerfil()
            }
        }

    }

    fun paginaFinalCad (){

        tela = 4
        val btnFinal: Button = findViewById(R.id.autonomos_btnFecharTudo)
        btnFinal.setOnClickListener {
            ChamaDialog()
            finish()
        }

    }

    fun paginaEdit () {
        tela = 5

        //exibe o codigo unico
        val tvCu: TextView = findViewById(R.id.autonomos_edit_btnPlanos)
        tvCu.setText("Código único: "+userBd)

        queryInfosParaEditar()

    }

    //esta query vai preencher os campos para o usuario editar
    fun queryInfosParaEditar(){

        ChamaDialog()
        val rootRef = databaseReference.child("autonomos").child(userBd)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()){

                    var values: String

                    //ler infos de autonomo
                    val radioPasseador: RadioButton = findViewById(R.id.autonomos_edit_radioPasseador)
                    val radioComidaNat: RadioButton = findViewById(R.id.autonomos_edit_radioComida)

                    values = p0.child("servico").getValue().toString()
                    servico = values
                    if (values.equals("passeador")){
                        radioPasseador.isChecked=true
                    } else if (values.equals("comida")){
                        radioComidaNat.isChecked=true
                    }

                    radioPasseador.setOnClickListener {
                        radioComidaNat.isChecked = false
                        servico="passeador"
                    }
                    radioComidaNat.setOnClickListener {
                        radioPasseador.isChecked=false
                        servico="comida"
                    }

                    values = p0.child("nome").getValue().toString()
                    nome = values
                    var editText:EditText = findViewById(R.id.autonomos_edit_etNome)
                    editText.setText(values)

                    values = p0.child("apelido").getValue().toString()
                    apelido = values
                    editText = findViewById(R.id.autonomos_edit_etApelido)
                    editText.setText(values)

                    values = p0.child("ddd").getValue().toString()
                    ddd =  values
                    editText = findViewById(R.id.autonomos_edit_etDdd)
                    editText.setText(values)

                    values = p0.child("whats").getValue().toString()
                    nWhats  = values
                    editText = findViewById(R.id.autonomos_edit_etCel)
                    editText.setText(values)

                    values = p0.child("desc").getValue().toString()
                    txtDesc  = values
                    editText = findViewById(R.id.autonomos_edit_etDesc)
                    editText.setText(values)

                    values = p0.child("situacao").getValue().toString()
                    findViewById<TextView>(R.id.autonomos_edit_tvSituacao).setText("Situação: "+values)

                    values = p0.child("imgPerfil").getValue().toString()
                    foto =  values
                    val imageView:ImageView = findViewById(R.id.autonomos_edit_ivPerfil)
                    if (values.equals("nao")){
                        Glide.with(this@autonomosActivity).asBitmap().load(R.drawable.ic_logo).fitCenter().apply(RequestOptions.circleCropTransform()).into(imageView)
                    } else {
                        Glide.with(this@autonomosActivity).load(values).fitCenter().apply(RequestOptions.circleCropTransform()).into(imageView)
                    }
                    val btnEnviarFoto: Button = findViewById(R.id.autonomos_edit_btnFotoPerfil)
                    btnEnviarFoto.setOnClickListener {
                        //vou mexer em fotoDoc para controlar se o user enviou foto nova.
                        openPopUpEdit("Origem da foto", "Escolha a origem da foto", true, "Tirar foto", "Galeria") //nao precisa do call pois na primeira vez que a popup é usada so precisa do OK. Entao btn s e nao so vao ser usadas agora.
                    }


                    val btnRadioCartao: RadioButton = findViewById(R.id.autonomos_edit_radioCartao)
                    val btnRadioNaoCartao: RadioButton = findViewById(R.id.autonomos_edit_radioSemCartao)

                    values = p0.child("cartao").getValue().toString()
                    cartao  = values
                    if (values.equals("sim")){
                        btnRadioCartao.isChecked=true
                    } else {
                        btnRadioNaoCartao.isChecked=true
                    }
                    btnRadioCartao.setOnClickListener {
                        btnRadioNaoCartao.isChecked=false
                        cartao="sim"
                    }
                    btnRadioNaoCartao.setOnClickListener {
                        btnRadioCartao.isChecked=false
                        cartao="nao"
                    }

                    val btnSalvar: Button = findViewById(R.id.autonomos_edit_btnSalvar)
                    btnSalvar.setOnClickListener {

                        ChamaDialog()
                        if (!servico.equals("nao")){ //se é diferente de nao é pq o user mexeu
                            databaseReference.child("autonomos").child(userBd).child("servico").setValue(servico)
                        }

                        var editText:EditText = findViewById(R.id.autonomos_edit_etNome)
                        if (nome.equals(editText.text.toString())){
                            databaseReference.child("autonomos").child(userBd).child("nome").setValue(nome)
                        }
                        editText = findViewById(R.id.autonomos_edit_etApelido)
                        if (apelido.equals(editText.text.toString())){
                            databaseReference.child("autonomos").child(userBd).child("apelido").setValue(apelido)
                        }
                        editText = findViewById(R.id.autonomos_edit_etDdd)
                        if (ddd.equals(editText.text.toString())){
                            databaseReference.child("autonomos").child(userBd).child("ddd").setValue(ddd)
                        }
                        editText = findViewById(R.id.autonomos_edit_etCel)
                        if (nWhats.equals(editText.text.toString())){
                            databaseReference.child("autonomos").child(userBd).child("whats").setValue(nWhats)
                        }
                        editText = findViewById(R.id.autonomos_edit_etDesc)
                        if (txtDesc.equals(editText.text.toString())){
                            databaseReference.child("autonomos").child(userBd).child("desc").setValue(txtDesc)
                        }
                        if (!cartao.equals("n")){ //se é diferente de nao é pq o user mexeu
                            databaseReference.child("autonomos").child(userBd).child("servico").setValue(cartao)
                        }

                        if (fotoDoc.equals("sim")){  //se for sim mudou a foto
                            uploadImagePerfilEdit()
                        }


                    }

                    val layEdit: ConstraintLayout = findViewById(R.id.autonomos_layEdit)
                    layEdit.visibility = View.VISIBLE


                    //edicao do planos e beneficios
                    values = p0.child("plano").getValue().toString()
                    val plano = values

                    if (plano.equals("premium")){

                        setupSpinnerAndbtnDeEndereco()

                        if (p0.child("endereco_custom").exists()){
                            values = p0.child("endereco_custom").getValue().toString()
                            val etEndereco: EditText = findViewById(R.id.autonomos_edit_etLogradouro)
                            etEndereco.setText(values)
                        }

                    }


                    val btnPlanos: Button = findViewById(R.id.autonomos_edit_btnPlanos)
                    btnPlanos.setOnClickListener {
                        abrePlanos(plano)
                    }

                    EncerraDialog()



                } else {

                    EncerraDialog()
                    makeToast("Ocorreu um erro.")
                    finish()
                }


            }
        })

    }

    fun setupSpinnerAndbtnDeEndereco(){

        //abre layout do endereço
        val layEndereco: ConstraintLayout = findViewById(R.id.autonomos_edit_layEndereco)
        layEndereco.visibility = View.VISIBLE

        var list_of_items = arrayOf(
            "Selecione Estado",
            "RJ",
            "AC",
            "AL",
            "AP",
            "AM",
            "BA",
            "CE",
            "DF",
            "ES",
            "GO",
            "MA",
            "MT",
            "MS",
            "MG",
            "PA",
            "PB",
            "PR",
            "PE",
            "PI",
            "RJ",
            "RN",
            "RS",
            "RO",
            "RR",
            "SC",
            "SP",
            "SE",
            "TO"
        )

        var estadoSelecionado = "Selecione Estado"
        val spinnerEstado: Spinner = findViewById(R.id.spinnerEndereco)
        //Adapter for spinner
        spinnerEstado.adapter = ArrayAdapter(this@autonomosActivity, android.R.layout.simple_spinner_dropdown_item, list_of_items)

        //item selected listener for spinner
        spinnerEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                hideKeyboard()
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                hideKeyboard()
                estadoSelecionado = list_of_items[position]

            }
        }

        val btnSalvarEnd: Button = findViewById(R.id.autonomos_edit_btnSalvarNovoEnd)
        btnSalvarEnd.setOnClickListener {
            val etLogradouro: EditText = findViewById(R.id.autonomos_edit_etLogradouro)
            val etNumero: EditText = findViewById(R.id.autonomos_edit_etNumero)
            val etCidade: EditText = findViewById(R.id.autonomos_edit_etCidade)
            val etBairro: EditText = findViewById(R.id.autonomos_edit_etBairro)

            if (etLogradouro.text.isEmpty()){
                etLogradouro.setError("Informe o logradouro")
                etLogradouro.requestFocus()
            } else if (etNumero.text.isEmpty()){
                etNumero.setError("Informe o número")
                etNumero.requestFocus()
            } else if (etCidade.text.isEmpty()) {
                etCidade.setError("Informe a cidade")
                etCidade.requestFocus()

            } else if (etBairro.text.isEmpty()){
                etBairro.setError("Informe o bairro")
                etBairro.requestFocus()

            } else if (estadoSelecionado.equals("Selecione Estado")){
                Toast.makeText(this, "Informe o Estado", Toast.LENGTH_SHORT).show()
            } else {
                //databaseReference.child("")
                getLatLong(etLogradouro.text.toString()+" "+etNumero.text.toString()+" "+etBairro.text.toString()+", "+etCidade.text.toString()+" "+estadoSelecionado.toString())
            }
        }

    }

    //pega um endereço e transforma em lat e long. Este método é usado uma única vez, quando o usuário cadastrou um petshop mas ainda não tem latitude e longitude ainda.
    //OBS: Este método poderia ser transferido para o cadastro do pet se for preciso no futuro.
    private fun getLatLong (endereco: String){

        val geocoder = Geocoder(this)
        //val addresses: List<Address>?
        //val address: Address?
        //var addressText = ""

        //Geocoder coder = new Geocoder(this);
        val address : List<Address>?
        //GeoPoint p1 = null;

        try {
            address = geocoder.getFromLocationName(endereco,1)

            if (address==null) {
                Toast.makeText(this, "Não foi possível encontrar sua localização ainda. Aguarde", Toast.LENGTH_SHORT).show()

            } else {
                var location: Address = address.get(0)
                //location.getLatitude();
                //location.getLongitude();

                //assim garantimos que este usuario fique online sempre. é obd dele acompanhado de !?!%
                val delim = "!!??!!lukako"
                databaseReference.child("onlineAutonomos").child(userBd+delim).child("lat")
                    .setValue(location.latitude)
                databaseReference.child("onlineAutonomos").child(userBd+delim).child("long")
                    .setValue(location.longitude)
                databaseReference.child("onlineAutonomos").child(userBd+delim).child("latlong")
                    .setValue(location.latitude + location.longitude)
                databaseReference.child("onlineAutonomos").child(userBd+delim).child("servico")
                    .setValue(servico)
                databaseReference.child("onlineAutonomos").child(userBd+delim).child("state")
                    .setValue("online")
                databaseReference.child("onlineAutonomos").child(userBd+delim).child("state")
                    .setValue("online")


                //ajusta a situacao. Ela vai ser usada na busca em MapsActivity. Se a situação não for regular nao vai exibir no mapa.
                val situacao = findViewById<TextView>(R.id.autonomos_edit_tvSituacao).text.toString()
                databaseReference.child("onlineAutonomos").child(userBd+delim).child("situacao")
                    .setValue(situacao)

                //salva o endereco no bd do autonomo para depois exibir pra ele editar
                databaseReference.child("autonomos").child(userBd).child("endereco_custom")
                    .setValue(endereco)


                Toast.makeText(
                    this,
                    "Pronto. Agora você aparece sempre online nesta localização.",
                    Toast.LENGTH_LONG
                ).show()

            }
        }catch (e: IOException) {
            Toast.makeText(this, "Erro no endereço", Toast.LENGTH_SHORT).show()
        }

    }

    fun abrePlanos(plano: String){

        val layAnterior: ConstraintLayout = findViewById(R.id.autonomos_layEdit)
        val layPlanosEbeneficios: ConstraintLayout = findViewById(R.id.layPlanos)

        layAnterior.visibility = View.GONE
        layPlanosEbeneficios.visibility = View.VISIBLE

        val btnVoltar: Button = findViewById(R.id.autonomos_layPlanos_btnVoltar)
        btnVoltar.setOnClickListener {
            layAnterior.visibility = View.VISIBLE
            layPlanosEbeneficios.visibility = View.GONE
            btnVoltar.setOnClickListener { null }
        }

        val radioBasico: RadioButton = findViewById(R.id.autonomos_planos_radioBasico)
        val radioPremium: RadioButton = findViewById(R.id.autonomos_planos_radioPremium)

        radioBasico.isEnabled=false
        radioPremium.isEnabled=false

        //marca o radio que o user já é assinante
        if (plano.equals("basico")){
            radioBasico.isChecked=true
        } else {
            //else é premium
            radioPremium.isChecked=true
        }

        val btnQueroBasico: Button = findViewById(R.id.autonomos_planos_btnBasico)
        val btnQueroPremium: Button = findViewById(R.id.autonomos_planos_btnPremium)

        btnQueroBasico.setOnClickListener {
            if (plano.equals("basico")){
                Toast.makeText(this, "Você já é assinante deste plano", Toast.LENGTH_SHORT).show()
            } else {
                mudaPlanoFinal("premium", "Você está prestes a mudar de plano e adquirir novos benefícios. Este plano custa R$5,00 por mês. Ao clicar no botão abaixo você confirma.")
            }
        }

        btnQueroPremium.setOnClickListener {
            if (plano.equals("premium")){
                Toast.makeText(this, "Você já é assinante deste plano", Toast.LENGTH_SHORT).show()
            } else {
                mudaPlanoFinal("basico", "Você está prestes a mudar de plano e perder seus benefícios. Ao clicar no botão abaixo você confirma.")
            }
        }
    }

    fun mudaPlanoFinal(novoPlano: String, mensagem: String){
        val btnFinalizaTroca: Button = findViewById(R.id.autonomos_planos_btnFinal)
        btnFinalizaTroca.setText("Mudar para plano "+novoPlano)
        val txtMensagem: TextView = findViewById(R.id.mudaPlanos_txtMensagem)
        txtMensagem.setText(mensagem)

        val layPlanosEbeneficios: ConstraintLayout = findViewById(R.id.layPlanos)
        val layEsta: ConstraintLayout = findViewById(R.id.layMudaPlanos)

        layEsta.visibility = View.VISIBLE
        layPlanosEbeneficios.visibility = View.GONE

        val btnvoltar: Button = findViewById(R.id.mudaPlanos_btnVoltar)
        btnvoltar.setOnClickListener {
            val layPlanosEbeneficios: ConstraintLayout = findViewById(R.id.layPlanos)
            val layEsta: ConstraintLayout = findViewById(R.id.layMudaPlanos)

            layEsta.visibility = View.GONE
            layPlanosEbeneficios.visibility = View.VISIBLE
        }

        btnFinalizaTroca.setOnClickListener {

            if (novoPlano.equals("premium")) {
                databaseReference.child("autonomos").child(userBd).child("plano")
                    .setValue("em analise")
                val newCad: DatabaseReference =
                    databaseReference.child("pedidoDeUpGradeAutonomo").push()
                    newCad.child("bdCliente").setValue(userBd)
                    newCad.child("contato").setValue(nWhats)
                    newCad.child("data").setValue(GetDate())
                    Toast.makeText(this, "Pronto! Seu pedido foi registrado e entraremos em contato logo", Toast.LENGTH_SHORT).show()
            } else {
                //salvar em downgrade
                val delim = "!!??!!lukako"
                databaseReference.child("autonomos").child(userBd).child("plano")
                    .setValue("basico")
                databaseReference.child("onlineAutonomos").child(userBd+delim).removeValue()
                    Toast.makeText(this, "Pronto! Seu plano foi reduzido para o básico.", Toast.LENGTH_SHORT).show()
            }

            val layEdit: ConstraintLayout = findViewById(R.id.autonomos_layEdit)
            val layPlanosEbeneficios: ConstraintLayout = findViewById(R.id.layPlanos)
            val layEste: ConstraintLayout = findViewById(R.id.layMudaPlanos)

            layEdit.visibility = View.VISIBLE
            layPlanosEbeneficios.visibility = View.GONE
            layEste.visibility = View.GONE

        }


    }

    private fun GetDate () : String {

        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.format(Date())

        return currentDate
    }

    fun takePictureFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, 100)
        }
    }

    fun takePictureFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/jpeg"
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(Intent.createChooser(intent, "Selecione a foto"), 101)

    }

    fun takePictureFromGalleryEdit() {
        mFireBaseStorage = FirebaseStorage.getInstance()
        mphotoStorageReference = mFireBaseStorage.reference
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/jpeg"
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(Intent.createChooser(intent, "Selecione a foto"), 301)

    }

    fun takePictureFromCameraEdit() {
        mFireBaseStorage = FirebaseStorage.getInstance()
        mphotoStorageReference = mFireBaseStorage.reference
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, 300)
        }
    }

    //retorno da imagem
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //retorno da camera
        //primeiro if resultado da foto tirada pela camera
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {

                Log.d("teste", "chegou em activityResult")
                val photo: Bitmap = data?.extras?.get("data") as Bitmap
                compressImage(photo)

            }

        } else if (requestCode==101) {
            //resultado da foto pega na galeria
            if (resultCode == RESULT_OK
                && data != null && data.getData() != null
            ) {

                filePath = data.getData()!!
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                compressImagePerfil(bitmap)

            }
        } else if (requestCode==102) {
            if (resultCode == RESULT_OK) {

                val photo: Bitmap = data?.extras?.get("data") as Bitmap
                compressImagePerfil(photo)

            }
        } else if (requestCode==300){  //foto da camera na edição de infos
            if (resultCode == RESULT_OK) {

                Log.d("teste", "chegou em activityResult")
                val photo: Bitmap = data?.extras?.get("data") as Bitmap
                compressImageEdit(photo)

            }

        } else if (requestCode==301){ //foto da galeria na edicao de infos
            if (resultCode == RESULT_OK
                && data != null && data.getData() != null
            ) {

                filePath = data.getData()!!
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                compressImageEdit(bitmap)

            }
        }
    }

    //aqui vamos reduzir o tamanho antes de enviar pro bd
    private fun compressImage(image: Bitmap) {

        //agora sabemos as dimensões da imagem.
        //neste exemplo queremos que caiba em um banner de 100x400
        //é alterando o tamanho aqui que o tamanho total da imagem cresce ao final**************************************
//pode ser 100x100, depende do formato que você quer exibir
//400x100 fica com 2,5 kb, 800x200 fica com 5 kb
        val imageProvisoria: Bitmap = calculateInSizeSampleToFitImageView(image, 200, 100)

        //image provisoria pode ser colocada no imageview pois já é pequena suficiente.
        val imageviewBanne: ImageView = findViewById(R.id.autonomos_laySegunda_ivPreview)
        imageviewBanne.setImageBitmap(imageProvisoria)
        imageviewBanne.visibility = View.VISIBLE

//esta parte é do método antigo. Imagino que ele nao tenha função mais
        val baos = ByteArrayOutputStream()
        var optionsCompress = 40  //taxa de compressao. 100 significa nenhuma compressao
        try {
            //Code here
            while (baos.toByteArray().size / 1024 > 50) {  //Loop if compressed picture is greater than 50kb, than to compression
                baos.reset() //Reset baos is empty baos
                imageProvisoria.compress(
                    Bitmap.CompressFormat.JPEG,
                    optionsCompress,
                    baos
                ) //The compression options%, storing the compressed data to the baos
                optionsCompress -= 25 //Every time reduced by 10
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }


        //aqui faz upload pro storage database
        val tempUri: Uri = getImageUri(this, imageProvisoria)
        filePath = tempUri
        Log.d("teste", "chegou ao fim de compressImage")
        uploadImage()

    }

    fun calculateInSizeSampleToFitImageView (image: Bitmap, imageViewWidth:Int, imageViewHeight:Int) : Bitmap{

        //ESTE BLOCO É PARA PEGAR AS DIMENSOES DA IMAGEM
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        //converte a imagem que o usuario escolheu para um Uri e depois para um File
        val file = bitmapToFile(image)
        val fpath = file.path
        BitmapFactory.decodeFile(fpath, options)
        //resultados pegos do método acima
        val imageHeight: Int = options.outHeight
        val imageWidth: Int = options.outWidth
        //FIM DAS DIMENSOES DA IMAGEM

        var adaptedHeight: Int =0
        var adaptedWidh: Int =0
        //vamos primeiro acerta a altura. Poderiamos fazer tudo ao mesmo tempo, mas como estamos trabalhando com possibilidade do height ser diferente do width poderia dar erro
        if (imageHeight > imageViewHeight){

            adaptedHeight = imageHeight / 2
            while (adaptedHeight > imageViewHeight){
                adaptedHeight = adaptedHeight/2
            }

        } else {
            adaptedHeight = imageViewHeight
        }

        if (imageWidth > imageViewWidth){

            adaptedWidh = imageWidth / 2
            while (adaptedWidh > imageViewHeight){
                adaptedWidh = adaptedWidh/2
            }
        } else {
            adaptedWidh = imageViewWidth
        }

        val newBitmap = Bitmap.createScaledBitmap(image, adaptedWidh, adaptedHeight, false)
        return newBitmap

    }


    // Method to save an bitmap to a file
    private fun bitmapToFile(bitmap:Bitmap): Uri {
        // Get the context wrapper
        val wrapper = ContextWrapper(applicationContext)

        // Initialize a new file instance to save bitmap object
        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")

        try{
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e: IOException){
            e.printStackTrace()
        }

        // Return the saved bitmap uri
        return Uri.parse(file.absolutePath)
    }

    //pega o uri
    fun  getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.PNG, 35, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null)
        return Uri.parse(path)
    }

    //envio da foto
    //existe uma opção especial aqui para o caso de ser alvará
    fun uploadImage(){

        ChamaDialog()

        mFireBaseStorage = FirebaseStorage.getInstance()

        mphotoStorageReference =mFireBaseStorage.getReference().child("autonomos").child(userBd).child("documentoFoto")

        Log.d("teste", "chegou em uploadImage")

        val bmp: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath)
        val baos: ByteArrayOutputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos)

        //get the uri from the bitmap
        val tempUri: Uri = getImageUri(this, bmp)
        //transform the new compressed bmp in filepath uri
        filePath = tempUri

        //var file = Uri.fromFile(bitmap)
        var uploadTask = mphotoStorageReference.putFile(filePath)

        val urlTask = uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                    EncerraDialog()
                    makeToast("Um erro ocorreu")
                }
            }
            return@Continuation mphotoStorageReference.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                urifinal = downloadUri.toString()

                Log.d("teste", "enviou arquivo")

                EncerraDialog()
                //estava dando erro. Aqui forçamos que apareça só a que queremos
                findViewById<ConstraintLayout>(R.id.autonomos_layTerceira).visibility=View.GONE
                findViewById<ConstraintLayout>(R.id.autonomos_laySegunda).visibility=View.VISIBLE
                findViewById<ConstraintLayout>(R.id.autonomos_layInicial).visibility=View.GONE

                val btnProximaPaginaDaPagina2: Button = findViewById(R.id.autonomos_laySegunda_btnProxima)
                //recoloca o listener do botão apos o envio da foto
                btnProximaPaginaDaPagina2.setOnClickListener {

                    if (!isNetworkAvailable(this)) {
                        makeToast("Você está sem internet")
                    } else {

                        val etNome: EditText = findViewById(R.id.autonomos_laySegunda_etNome)
                        val etApelido: EditText = findViewById(R.id.autonomos_laySegunda_Etapelido)
                        val etDdd: EditText = findViewById(R.id.autonomos_laySegunda_etDdd)
                        val etWhats: EditText = findViewById(R.id.autonomos_laySegunda_etWhatzapp)
                        val imageViewPreview: ImageView =
                            findViewById(R.id.autonomos_laySegunda_ivPreview)

                        if (etNome.text.isEmpty()) {
                            etNome.requestFocus()
                            etNome.setError("Informe o nome")
                        } else if (etApelido.text.isEmpty()) {
                            etApelido.requestFocus()
                            etApelido.setError("Informe como deseja ser chamado no app")
                        } else if (etDdd.text.isEmpty()) {
                            etDdd.requestFocus()
                            etDdd.setError("Ddd")
                        } else if (etWhats.text.isEmpty()) {
                            etWhats.requestFocus()
                            etWhats.setError("Informe o seu whatsApp")
                        } else {
                            nome = etNome.text.toString()
                            apelido = etApelido.text.toString()
                            ddd = etDdd.text.toString()
                            nWhats = etWhats.text.toString()

                            //o proximo layout é chamado quando termina o upload.
                            //uploadImage(true, servico, nome, apelido, ddd, nWhats)
                            terceiraPagina()
                        }


                    }
                }

            } else {
                // Handle failures
                Toast.makeText(this, "um erro ocorreu.", Toast.LENGTH_SHORT).show()
                EncerraDialog()
                // ...
            }
        }

    }


    fun takePictureFromCameraPerfil() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, 102)
        }
    }

    //aqui é onde salva no BD
    fun uploadImagePerfil () {

        ChamaDialog()

        mFireBaseStorage = FirebaseStorage.getInstance()

        mphotoStorageReference =mFireBaseStorage.getReference().child("autonomos").child(userBd).child("img")


        val bmp: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath)
        val baos: ByteArrayOutputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 25, baos)

        //get the uri from the bitmap
        val tempUri: Uri = getImageUri(this, bmp)
        //transform the new compressed bmp in filepath uri
        filePath = tempUri

        //var file = Uri.fromFile(bitmap)
        var uploadTask = mphotoStorageReference.putFile(filePath)

        val urlTask = uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                    EncerraDialog()
                    makeToast("Um erro ocorreu")
                }
            }
            return@Continuation mphotoStorageReference.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                urifinal = downloadUri.toString()

                if (servico.equals("nao")){
                    val txprovisorio: EditText = findViewById(R.id.tvmaintain)
                    servico = txprovisorio.text.toString()
                }

                Log.d("teste", "o valor de servico é "+servico)
                //salvar aqui no bd
                databaseReference.child("autonomos").child(userBd).child("imgPerfil").setValue(urifinal)
                databaseReference.child("autonomos").child(userBd).child("imgDoc").setValue(fotoDoc)
                databaseReference.child("autonomos").child(userBd).child("servico").setValue(servico)
                databaseReference.child("autonomos").child(userBd).child("imgDoc").setValue(fotoDoc)
                databaseReference.child("autonomos").child(userBd).child("nome").setValue(nome)
                databaseReference.child("autonomos").child(userBd).child("apelido").setValue(apelido)
                databaseReference.child("autonomos").child(userBd).child("ddd").setValue(ddd)
                databaseReference.child("autonomos").child(userBd).child("whats").setValue(nWhats)
                databaseReference.child("autonomos").child(userBd).child("desc").setValue(txtDesc)
                databaseReference.child("autonomos").child(userBd).child("situacao").setValue("analise")
                databaseReference.child("autonomos").child(userBd).child("cartao").setValue(cartao)
                databaseReference.child("autonomos").child(userBd).child("avaliacoes").setValue(0)
                databaseReference.child("autonomos").child(userBd).child("nota").setValue(0)
                databaseReference.child("autonomos").child(userBd).child("plano").setValue("basico")
                databaseReference.child("autonomos").child(userBd).child("qntFotos").setValue(0)

                //atualiza o tipo de usuário
                databaseReference.child("usuarios").child(userBd).child("tipo").setValue("autonomo")
                //ajusta foto do usuario
                databaseReference.child("usuarios").child(userBd).child("img").setValue(urifinal)
                //informa o servico ja aqui no bd do user
                databaseReference.child("usuarios").child(userBd).child("servico").setValue(servico)
                databaseReference.child("usuarios").child(userBd).child("apelido").setValue(apelido)

                //apaga o status de usuario comum online. Se nao apagar agora, nunca mais vai.
                databaseReference.child("onlineUsers").child(userBd).removeValue()


                //makeToast("Todas informações foram salvas. Você deve esperar a conferência do seu documento para liberação. Você ainda não aparece no mapa para osuários")

                val layPag3: ConstraintLayout = findViewById(R.id.autonomos_layTerceira)
                val layMsgFinal: ConstraintLayout = findViewById(R.id.autonomos_layFinal)

                layPag3.visibility = View.GONE
                layMsgFinal.visibility = View.VISIBLE
                paginaFinalCad()
                EncerraDialog()

                //finish()


            } else {
                // Handle failures
                Toast.makeText(this, "um erro ocorreu.", Toast.LENGTH_SHORT).show()
                EncerraDialog()
                // ...
            }
        }

    }

    fun uploadImagePerfilEdit () {

        ChamaDialog()

        mFireBaseStorage = FirebaseStorage.getInstance()

        mphotoStorageReference =mFireBaseStorage.getReference().child("autonomos").child(userBd).child("img")


        val bmp: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath)
        val baos: ByteArrayOutputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 25, baos)

        //get the uri from the bitmap
        val tempUri: Uri = getImageUri(this, bmp)
        //transform the new compressed bmp in filepath uri
        filePath = tempUri

        //var file = Uri.fromFile(bitmap)
        var uploadTask = mphotoStorageReference.putFile(filePath)

        val urlTask = uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                    EncerraDialog()
                    makeToast("Um erro ocorreu")
                }
            }
            return@Continuation mphotoStorageReference.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                urifinal = downloadUri.toString()

                databaseReference.child("autonomos").child(userBd).child("imgPerfil").setValue(urifinal)
                databaseReference.child("usuarios").child(userBd).child("img").setValue(urifinal)

                EncerraDialog()
                makeToast("As informações foram salvas")
                finish()

                //finish()

            } else {
                // Handle failures
                Toast.makeText(this, "um erro ocorreu.", Toast.LENGTH_SHORT).show()
                EncerraDialog()
                // ...
            }
        }

    }

    //aqui vamos reduzir o tamanho antes de enviar pro bd
    private fun compressImagePerfil(image: Bitmap) {

        //agora sabemos as dimensões da imagem.
        //neste exemplo queremos que caiba em um banner de 100x400
        //é alterando o tamanho aqui que o tamanho total da imagem cresce ao final**************************************
//pode ser 100x100, depende do formato que você quer exibir
//400x100 fica com 2,5 kb, 800x200 fica com 5 kb
        val imageProvisoria: Bitmap = calculateInSizeSampleToFitImageView(image, 130, 130)
        val ivProvisorio: ImageView = findViewById(R.id.layFotoPreview_ivPreview)
        ivProvisorio.setImageBitmap(imageProvisoria)



//esta parte é do método antigo. Imagino que ele nao tenha função mais
        val baos = ByteArrayOutputStream()
        var optionsCompress = 20  //taxa de compressao. 100 significa nenhuma compressao
        try {
            //Code here
            while (baos.toByteArray().size / 1024 > 50) {  //Loop if compressed picture is greater than 50kb, than to compression
                baos.reset() //Reset baos is empty baos
                imageProvisoria.compress(
                    Bitmap.CompressFormat.JPEG,
                    optionsCompress,
                    baos
                ) //The compression options%, storing the compressed data to the baos
                optionsCompress -= 25 //Every time reduced by 10
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }


        //aqui faz upload pro storage database
        val tempUri: Uri = getImageUri(this, imageProvisoria)
        filePath = tempUri
        //uploadImage()

        EncerraDialog()

        //vamos introduzir aqui o método de editar este bitmap
        val layPreviewFoto: ConstraintLayout = findViewById(R.id.layFotoPreview)
        layPreviewFoto.visibility = View.VISIBLE
        val btnRotate: Button = findViewById(R.id.layFotoPreview_btnRotate)
        btnRotate.setOnClickListener {

            val newProvisoria: Bitmap? = RotateBitmap(imageProvisoria, 90F)
            //ivProvisorio.setImageBitmap(newProvisoria)
            val bit: Bitmap = newProvisoria!!
            compressImagePerfil(bit)
        }

        val btnFinalizarEdicao: Button = findViewById(R.id.layFotoPreview_btnSalvarFoto)
        btnFinalizarEdicao.setOnClickListener {

            val imageviewBanne: ImageView = findViewById(R.id.autonomos_layTerceira_ivPreviewPerfil)
            //vamos colocar no lugar certo
            Glide.with(this@autonomosActivity).load(imageProvisoria).apply(RequestOptions.circleCropTransform()).apply(RequestOptions().override(130, 130)).into(imageviewBanne)
            //imageviewBanne.setImageBitmap(imageProvisoria)
            imageviewBanne.visibility = View.VISIBLE

            //ChamaDialog()
            //uploadImagePerfil()
            layPreviewFoto.visibility = View.GONE

        }

    }

    fun RotateBitmap(source: Bitmap?, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return source?.let {
            Bitmap.createBitmap(
                it,
                0,
                0,
                source.width,
                source.height,
                matrix,
                true
            )
        }
    }

    //aqui vamos reduzir o tamanho antes de enviar pro bd
    private fun compressImageEdit(image: Bitmap) {

        val imageProvisoria: Bitmap = calculateInSizeSampleToFitImageView(image, 130, 130)
        val ivProvisorio: ImageView = findViewById(R.id.layFotoPreview_ivPreview)
        Glide.with(this@autonomosActivity).load(imageProvisoria).apply(RequestOptions.circleCropTransform()).apply(RequestOptions().override(130, 130)).into(ivProvisorio)
        //ivProvisorio.setImageBitmap(imageProvisoria)


//esta parte é do método antigo. Imagino que ele nao tenha função mais
        val baos = ByteArrayOutputStream()
        var optionsCompress = 20  //taxa de compressao. 100 significa nenhuma compressao
        try {
            //Code here
            while (baos.toByteArray().size / 1024 > 50) {  //Loop if compressed picture is greater than 50kb, than to compression
                baos.reset() //Reset baos is empty baos
                imageProvisoria.compress(
                    Bitmap.CompressFormat.JPEG,
                    optionsCompress,
                    baos
                ) //The compression options%, storing the compressed data to the baos
                optionsCompress -= 25 //Every time reduced by 10
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }


        //aqui faz upload pro storage database
        val tempUri: Uri = getImageUri(this, imageProvisoria)
        filePath = tempUri
        //uploadImage()

        //vamos introduzir aqui o método de editar este bitmap
        val layPreviewFoto: ConstraintLayout = findViewById(R.id.layFotoPreview)
        layPreviewFoto.visibility = View.VISIBLE
        val btnRotate: Button = findViewById(R.id.layFotoPreview_btnRotate)
        btnRotate.setOnClickListener {

            val newProvisoria: Bitmap? = RotateBitmap(imageProvisoria, 90F)
            //ivProvisorio.setImageBitmap(newProvisoria)
            val bit: Bitmap = newProvisoria!!
            compressImagePerfil(bit)
        }

        val btnFinalizarEdicao: Button = findViewById(R.id.layFotoPreview_btnSalvarFoto)
        btnFinalizarEdicao.setOnClickListener {

            //image provisoria pode ser colocada no imageview pois já é pequena suficiente.
            val imageviewBanne: ImageView = findViewById(R.id.autonomos_edit_ivPerfil)
            Glide.with(this@autonomosActivity).load(imageProvisoria).apply(RequestOptions.circleCropTransform()).apply(RequestOptions().override(130, 130)).into(imageviewBanne)
            //imageviewBanne.setImageBitmap(imageProvisoria)
            imageviewBanne.visibility = View.VISIBLE

            fotoDoc="sim"

            //ChamaDialog()
            //uploadImagePerfil()
            layPreviewFoto.visibility = View.GONE

        }

    }


    override fun onBackPressed() {

        if (tela==1){
            finish()
        } else if (tela==2){
            val btnVolta: Button = findViewById(R.id.autonomos_laySegundaBtnVoltar)
            btnVolta.performClick()
            metodosIniciais()
        } else if (tela==3){
            makeToast("Você ainda não concluiu o cadastro")
        } else if (tela==4){
            finish()
        } else if (tela==5){
            finish()
        }

    }

    fun openPopUp (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao

        //EXIBIR POPUP
        // Initialize a new layout inflater instance
        val inflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate a custom view using layout inflater
        val view = inflater.inflate(R.layout.popup_model,null)

        // Initialize a new instance of popup window
        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.MATCH_PARENT, // Width of popup window
            LinearLayout.LayoutParams.WRAP_CONTENT // Window height
        )



        // Set an elevation for the popup window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0F
        }


        // If API level 23 or higher then execute the code
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Create a new slide animation for popup window enter transition
            val slideIn = Slide()
            slideIn.slideEdge = Gravity.TOP
            popupWindow.enterTransition = slideIn

            // Slide animation for popup window exit transition
            val slideOut = Slide()
            slideOut.slideEdge = Gravity.RIGHT
            popupWindow.exitTransition = slideOut

        }


        // Get the widgets reference from custom view
        val buttonPopupN = view.findViewById<Button>(R.id.btnReclamar)
        val buttonPopupS = view.findViewById<Button>(R.id.BtnRecebimento)
        val buttonPopupOk = view.findViewById<Button>(R.id.popupBtnOk)
        val txtTitulo = view.findViewById<TextView>(R.id.popupTitulo)
        val txtTexto = view.findViewById<TextView>(R.id.popupTexto)


        if (exibeBtnOpcoes){
            //vai exibir os botões com textos e esconder o btn ok
            buttonPopupOk.visibility = View.GONE
            //exibe e ajusta os textos dos botões
            buttonPopupN.text = btnNao
            buttonPopupS.text = btnSim

            // Set a click listener for popup's button widget
            buttonPopupN.setOnClickListener{
                // Dismiss the popup window
                takePictureFromGallery()
                popupWindow.dismiss()
            }

            buttonPopupS.setOnClickListener {
                takePictureFromCameraPerfil()
                popupWindow.dismiss()
            }

        } else {

            //vai esconder os botões com textos e exibir o btn ok
            buttonPopupOk.visibility = View.VISIBLE
            //exibe e ajusta os textos dos botões
            buttonPopupN.visibility = View.GONE
            buttonPopupS.visibility = View.GONE


            buttonPopupOk.setOnClickListener{
                // Dismiss the popup window
                popupWindow.dismiss()
            }

        }

        txtTitulo.text = titulo
        txtTexto.text = texto


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            popupWindow.dismiss()
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.autonomos_layPai)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }

    fun openPopUpEdit (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao

        //EXIBIR POPUP
        // Initialize a new layout inflater instance
        val inflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate a custom view using layout inflater
        val view = inflater.inflate(R.layout.popup_model,null)

        // Initialize a new instance of popup window
        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.MATCH_PARENT, // Width of popup window
            LinearLayout.LayoutParams.WRAP_CONTENT // Window height
        )



        // Set an elevation for the popup window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0F
        }


        // If API level 23 or higher then execute the code
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Create a new slide animation for popup window enter transition
            val slideIn = Slide()
            slideIn.slideEdge = Gravity.TOP
            popupWindow.enterTransition = slideIn

            // Slide animation for popup window exit transition
            val slideOut = Slide()
            slideOut.slideEdge = Gravity.RIGHT
            popupWindow.exitTransition = slideOut

        }


        // Get the widgets reference from custom view
        val buttonPopupN = view.findViewById<Button>(R.id.btnReclamar)
        val buttonPopupS = view.findViewById<Button>(R.id.BtnRecebimento)
        val buttonPopupOk = view.findViewById<Button>(R.id.popupBtnOk)
        val txtTitulo = view.findViewById<TextView>(R.id.popupTitulo)
        val txtTexto = view.findViewById<TextView>(R.id.popupTexto)


        if (exibeBtnOpcoes){
            //vai exibir os botões com textos e esconder o btn ok
            buttonPopupOk.visibility = View.GONE
            //exibe e ajusta os textos dos botões
            buttonPopupN.text = btnNao
            buttonPopupS.text = btnSim

            // Set a click listener for popup's button widget
            buttonPopupN.setOnClickListener{
                // Dismiss the popup window
                takePictureFromGalleryEdit()
                popupWindow.dismiss()
            }

            buttonPopupS.setOnClickListener {
                takePictureFromCameraEdit()
                popupWindow.dismiss()
            }

        } else {

            //vai esconder os botões com textos e exibir o btn ok
            buttonPopupOk.visibility = View.VISIBLE
            //exibe e ajusta os textos dos botões
            buttonPopupN.visibility = View.GONE
            buttonPopupS.visibility = View.GONE


            buttonPopupOk.setOnClickListener{
                // Dismiss the popup window
                popupWindow.dismiss()
            }

        }

        txtTitulo.text = titulo
        txtTexto.text = texto


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            popupWindow.dismiss()
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.autonomos_layPai)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }














    //permissões
    private fun setupPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
            //permissão concedida
        } else {
            RequestWriteStoragePermission()
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
        } else {
            RequestReadStoragePermission()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
        } else {
            RequestCameraPermission()
        }
    }

    //aqui sao tres métodos. Cada um para uma permissão
    fun RequestCameraPermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)){
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("Precisamos de sua permissão para acessar a Camera. Vamos usar para você poder tirar fotos para enviar ao App")
                .setTitle("Permissões necessárias")
                .setCancelable(false)
                .setPositiveButton("Sim, autorizar", DialogInterface.OnClickListener { dialog, which ->

                    //mude a permissão aqui
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.CAMERA),
                        1003)

                })
            // Display a negative button on alert dialog
            builder.setNegativeButton("Não"){dialog,which ->
                Toast.makeText(applicationContext,"Você negou a permissão e não poderá acessar as funcionalidades.",Toast.LENGTH_SHORT).show()
            }
            val alert : AlertDialog = builder.create()
            alert.show()
        } else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("Precisamos de sua permissão para acessar a Camera. Vamos usar para você poder tirar fotos para enviar ao App")
                .setTitle("Permissões necessárias")
                .setCancelable(false)
                .setPositiveButton("Sim, autorizar", DialogInterface.OnClickListener { dialog, which ->

                    //mude a permissão aqui
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.CAMERA),
                        1003)

                })
            // Display a negative button on alert dialog
            builder.setNegativeButton("Não"){dialog,which ->
                Toast.makeText(applicationContext,"Você negou a permissão e não poderá acessar as funcionalidades.",Toast.LENGTH_SHORT).show()
            }
            val alert : AlertDialog = builder.create()
            alert.show()
        }
    }

    fun RequestReadStoragePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("Precisamos de sua permissão para ler arquivos do seu celular. Vamos usar para você poder enviar as fotos para o App")
                .setTitle("Permissões necessárias")
                .setCancelable(false)
                .setPositiveButton("Sim, autorizar", DialogInterface.OnClickListener { dialog, which ->

                    //mude a permissão aqui
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1001)

                })
            // Display a negative button on alert dialog
            builder.setNegativeButton("Não"){dialog,which ->
                Toast.makeText(applicationContext,"Você negou a permissão e não poderá acessar as funcionalidades.",Toast.LENGTH_SHORT).show()
            }
            val alert : AlertDialog = builder.create()
            alert.show()
        } else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("Precisamos de sua permissão para ler arquivos do seu celular. Vamos usar para você poder enviar as fotos para o App")
                .setTitle("Permissões necessárias")
                .setCancelable(false)
                .setPositiveButton("Sim, autorizar", DialogInterface.OnClickListener { dialog, which ->

                    //mude a permissão aqui
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1002)

                })
            // Display a negative button on alert dialog
            builder.setNegativeButton("Não"){dialog,which ->
                Toast.makeText(applicationContext,"Você negou a permissão e não poderá acessar as funcionalidades.",Toast.LENGTH_SHORT).show()
            }
            val alert : AlertDialog = builder.create()
            alert.show()
        }
    }

    fun RequestWriteStoragePermission (){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("Precisamos de sua permissão para salvar arquivos no seu celular")
                .setTitle("Permissões necessárias")
                .setCancelable(false)
                .setPositiveButton("Sim, autorizar", DialogInterface.OnClickListener { dialog, which ->

                    //mude a permissão aqui
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1002)

                })
            // Display a negative button on alert dialog
            builder.setNegativeButton("Não"){dialog,which ->
                Toast.makeText(applicationContext,"Você negou a permissão e não poderá acessar as funcionalidades.",Toast.LENGTH_SHORT).show()
            }
            val alert : AlertDialog = builder.create()
            alert.show()
        } else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("Precisamos de sua permissão para salvar arquivos no seu celular")
                .setTitle("Permissões necessárias")
                .setCancelable(false)
                .setPositiveButton("Sim, autorizar", DialogInterface.OnClickListener { dialog, which ->

                    //mude a permissão aqui
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1002)

                })
            // Display a negative button on alert dialog
            builder.setNegativeButton("Não"){dialog,which ->
                Toast.makeText(applicationContext,"Você negou a permissão e não poderá acessar as funcionalidades.",Toast.LENGTH_SHORT).show()
            }
            val alert : AlertDialog = builder.create()
            alert.show()
        }
    }

    //por fim, pegue o retorno dos métodos aqui
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1002){
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                //permissao garantida
            } else {
                //permissao negada
            }
        }
        if (requestCode == 1001){
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                //permissao garantida
            } else {

            }
        }
        if (requestCode == 1003){
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                //permissão garantida
            } else {
                //permissao negada
            }
        }
    }






    fun makeToast(mensagem: String){
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var activeNetworkInfo: NetworkInfo? = null
        activeNetworkInfo = cm.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

    /* To hide Keyboard */
    fun hideKeyboard() {
        try {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
