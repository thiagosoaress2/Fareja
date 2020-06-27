package com.petcare.petcare

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.petcare.petcare.Controller.AreaLojistaController
import com.petcare.petcare.Models.AreaLojistaModels
import com.petcare.petcare.Utils.cameraPermissions
import com.petcare.petcare.Utils.mySharedPrefs
import com.petcare.petcare.Utils.readFilesPermissions
import com.petcare.petcare.Utils.writeFilesPermissions
import kotlinx.android.synthetic.main.activity_arealojista.*
import java.io.*
import java.util.*
import kotlin.collections.ArrayList

class arealojista : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var mFireBaseStorage: FirebaseStorage
    private lateinit var mphotoStorageReference: StorageReference

    //envio de imagem
    private lateinit var filePath: Uri
    private var urifinal: String = "nao"

    var arrayNomes: MutableList<String> = ArrayList()
    var arrayImg: MutableList<String> = ArrayList()
    var arrayDesc: MutableList<String> = ArrayList()
    var arrayPreco: MutableList<String> = ArrayList()
    var arrayBD: MutableList<String> = ArrayList()

    private val CAMERA_PERMISSION_CODE = 100
    private val READ_PERMISSION_CODE = 101
    private val WRITE_PERMISSION_CODE = 102


    //to do
    //remover o alvará
    //dar um jeito de impedir de criar 2 pets em cad novo empreendimento

    //no Oncreate ele prepara os cliques dos primeiros botões
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arealojista)

        if (cameraPermissions.hasPermissions(this) && readFilesPermissions.hasPermissions(this) && writeFilesPermissions.hasPermissions(this)){
            clicksIniciais() //tudp que ocorria aqui foi movido para clicksIniciais () para aumentar velocidade da abertura desta activity
        } else {
            setupPermissions()
            val intent = intent
            finish()
            startActivity(intent)


        }



    }


    fun clicksIniciais () {  //sairam de onCreate para acelerar a abertura dessa Activity

        AreaLojistaModels.userBD = intent.getStringExtra("userBD")
        AreaLojistaModels.alvara = intent.getStringExtra("alvara")
        AreaLojistaModels.tipo = intent.getStringExtra("tipo")
        AreaLojistaModels.userMail = intent.getStringExtra("email")

        AreaLojistaModels.init()

        liberaBotoesDoEmpresario()

        databaseReference = FirebaseDatabase.getInstance().reference

        val situacaoEspecial = intent.getStringExtra("especial_plano")  //quando o user esta na loja e decide fazer upgrade de plano
        if (situacaoEspecial!=null){
            if (situacaoEspecial.equals("especial")){
                clickPlanos(situacaoEspecial)
            }
        }

        setupPermissions()

        //botões iniciais
        val btnAbreEditLayout : Button = findViewById(R.id.arealojistaIndexBtneditLayout)
        btnAbreEditLayout.setOnClickListener {
            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layEditLayout : ConstraintLayout = findViewById(R.id.lay_edit_layout_loja)

            queryPetLayoutEdit()

            layIndex.visibility = View.GONE
            layEditLayout.visibility = View.VISIBLE
            clicksLayoutEdit()

            //openPopUp("Dica", "Aqui você pode editar a aparência da sua loja dentro do aplicativo. Você pode trocar o banner e o logo padrão pelo personalizado da sua empresa. Será assim que o usuário verá sua loja", false, "n", "n", "n")


        }

        /*
        ATENÇÃO. VOU COLOCAR JUNTOS AQUI TODOS OS BACKUPS DE FUNÇÕES QUE DE UMA FORMA OU DE OUTRA PRECISEI ALTERAR PARA TIRAR A VERIFICAÇÃO DE ALVARÁ

        val btnCadastrarEmpreendimento : Button = findViewById(R.id.arealojistaIndexBtnCadastrarEmpreendimento)
        btnCadastrarEmpreendimento.setOnClickListener {

            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layCadEmp : ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)


            if (AreaLojistaModels.tipo.equals("usuario")) {
                layIndex.visibility = View.GONE
                layCadEmp.visibility = View.VISIBLE
                clicksCadastroEmpresa(true) //true significa que é um usuário cadastrando a empresa pela primeira vez.

            } else {

                if (AreaLojistaModels.alvara.equals("nao")){
                    layIndex.visibility = View.GONE
                    val layCadEmpAlvara: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                    layCadEmpAlvara.visibility = View.VISIBLE
                    Toast.makeText(this, "Você precisa finalizar seu cadastro enviando uma foto do alvará.", Toast.LENGTH_SHORT).show()
                    clicksCadastroEmpresa(false)
                } else {
                    layIndex.visibility = View.GONE
                    layCadEmp.visibility = View.VISIBLE
                    clicksCadastroEmpresa(false) //true significa que é um usuário cadastrando a empresa pela primeira vez.
                }
            }


        }


        fun clicksCadastroEmpresa (eNovo: Boolean) {

        var mLogradouro="nao"
        var mNumero = "nao"
        var mBairro = "nao"
        var mCidade = "nao"
        var mNome = "nao"
        var mDddTel = "nao"
        var mTel = "nao"
        var mDddCel = "nao"
        var mCel = "nao"

        val btnVoltar : Button = findViewById(R.id.layCadProd_btnVoltar)
        btnVoltar.setOnClickListener {
            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layCadEmp : ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)

            hideKeyboard()
            layIndex.visibility = View.VISIBLE
            layCadEmp.visibility = View.GONE

        }


        if (!eNovo){ //se nao for um usuário novo, carrega os dados já preenchidos nos campos

            ChamaDialog()

            var eT: EditText

            val rootRef = databaseReference.child("petshops").child(AreaLojistaModels.petBD)
            rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    //TODO("Not yet implemented")
                    EncerraDialog()
                }

                override fun onDataChange(p0: DataSnapshot) {
                    //TODO("Not yet implemented")
                    var values: String
                    values = p0.child("nome").value.toString()
                    eT = findViewById(R.id.cadEmpEtNome)
                    eT.setText(values)
                    mNome=values
                    values = p0.child("telefone").value.toString()
                    eT = findViewById(R.id.cadEmpEtTel)
                    eT.setText(values)
                    mTel=values
                    values = p0.child("logradouro").value.toString()
                    eT = findViewById(R.id.cadEmpEtLogradouro)
                    mLogradouro=values
                    eT.setText(values)
                    values = p0.child("numero").value.toString()
                    eT = findViewById(R.id.cadEmpEtNumero)
                    eT.setText(values)
                    mNumero=values
                    values = p0.child("bairro").value.toString()
                    eT = findViewById(R.id.cadEmpEtBairro)
                    eT.setText(values)
                    mBairro=values
                    values = p0.child("cidade").value.toString()
                    eT = findViewById(R.id.cadEmpEtCidade)
                    eT.setText(values)
                    mCidade=values
                    values = p0.child("dddTel").value.toString()
                    eT = findViewById(R.id.cadEmpEtTel_ddd)
                    eT.setText(values)
                    mDddTel=values
                    values = p0.child("dddCel").value.toString()
                    eT = findViewById(R.id.cadEmpEtCel_ddd)
                    eT.setText(values)
                    mDddCel=values
                    values = p0.child("cel").value.toString()
                    eT = findViewById(R.id.cadEmpEtCel)
                    eT.setText(values)
                    mCel=values


                    EncerraDialog()
                }

            })
        }

        //setUpPermissionCamera()

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
        val spinnerEstado: Spinner = findViewById(R.id.spinnerEstado)
        //Adapter for spinner
        spinnerEstado.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list_of_items)

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


        val btnAjudaSendAlvara:Button = findViewById(R.id.cadEmpBtnSendalvaraHelp)
        btnAjudaSendAlvara.setOnClickListener {
            openPopUp("Por que é importante para nós uma foto do alvará?", "A imagem do seu alvará não será pública. Nós exigimos uma foto deste documento para ajudar a garantir a segurança de todos na nossa comunidade. O envio de uma foto do alvará nos ajuda a previnir fraudes e golpes contra nossos usuários.", false, "sim", "nao", "alvaraHelp")
        }

        val btnSendAlvara:Button = findViewById(R.id.cadEmpBtnSendAlvara)
        btnSendAlvara.setOnClickListener {
            //takePicture(1)
            //verifica se o usuario deu as permissões. O resultado disto sai no método override onRequestPermissionResult logo abaixo do onCreate.
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissões não concedidas. Impossível prosseguir", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    0
                )
            } else {
                //ChamaDialog()
                AreaLojistaModels.processo="alvara"
                takePictureFromCamera()
            }

        }

        val btnProximo:Button = findViewById(R.id.cadEmpProximo)
        btnProximo.setOnClickListener {
            ChamaDialog()

            hideKeyboard()

            var mEtNome: EditText = findViewById(R.id.cadEmpEtNome)
            var mEtTelDdd : EditText = findViewById(R.id.cadEmpEtTel_ddd) //falta fazer
            var mEtTel: EditText = findViewById(R.id.cadEmpEtTel)
            var mEtLogradouro: EditText = findViewById(R.id.cadEmpEtLogradouro)
            var mEtNumero: EditText = findViewById(R.id.cadEmpEtNumero)
            var mEtBairro: EditText = findViewById(R.id.cadEmpEtBairro)
            var mEtCidade: EditText = findViewById(R.id.cadEmpEtCidade)
            var mEtCelDdd : EditText = findViewById(R.id.cadEmpEtCel_ddd) //falta fazer
            var mEtCel : EditText = findViewById(R.id.cadEmpEtCel) //falta fazer


            //verificações dos ettexts e se o alvara=1
            if (mEtNome.text.isEmpty()){
                mEtNome.requestFocus()
                mEtNome.setError("Informe o nome")
                EncerraDialog()
            } else if (mEtTel.text.isEmpty()){
                mEtTel.requestFocus()
                mEtTel.setError("Informe o telefone de contato")
                EncerraDialog()
            } else if (mEtLogradouro.text.isEmpty()){
                mEtLogradouro.requestFocus()
                mEtLogradouro.setError("Informe o logradouro")
                EncerraDialog()
            } else if (mEtNumero.text.isEmpty()){
                mEtNumero.requestFocus()
                mEtNumero.setError("Informe o número do endereço")
                EncerraDialog()
            } else if (mEtBairro.text.isEmpty()){
                mEtBairro.requestFocus()
                mEtBairro.setError("Informe o bairro")
                EncerraDialog()
            } else if (mEtCidade.text.isEmpty()) {
                mEtCidade.requestFocus()
                mEtCidade.setError("Informe a cidade")
                EncerraDialog()
                //} else if (alvaraOk==0){
                // Toast.makeText(this, "Você precisa enviar a foto do alvará", Toast.LENGTH_SHORT).show()
            } else if (mEtTelDdd.text.isEmpty() || mEtTelDdd.text.length != 2) {
                mEtTelDdd.requestFocus()
                mEtTelDdd.setError("!")
                EncerraDialog()
            } else if (mEtCelDdd.text.isEmpty() || mEtCelDdd.text.length != 2) {
                mEtCelDdd.requestFocus()
                mEtCelDdd.setError("!")
                EncerraDialog()
            } else if (mEtCel.text.isEmpty()){
                mEtCel.requestFocus()
                mEtCel.setError("!")
                EncerraDialog()
            } else if (estadoSelecionado.equals("Selecione Estado")){
                Toast.makeText(this, "Selecione o Estado antes de prosseguir.", Toast.LENGTH_SHORT).show()
                spinnerEstado.requestFocus()
                EncerraDialog()
            } else {

                btnProximo.isEnabled=false

                if (eNovo) { //se for novo, vai criar todos os campos no BD
                    //registrar criando o path do usuario. Vai ficar faltando o alvará que será feito na próxima página.
                    val newCad: DatabaseReference = databaseReference.child("petshops").push()
                    AreaLojistaModels.petBD = newCad.key.toString()
                    newCad.child("nome").setValue(mEtNome.text.toString())
                    newCad.child("telefone").setValue(mEtTel.text.toString())
                    newCad.child("logradouro").setValue(mEtLogradouro.text.toString())
                    newCad.child("numero").setValue(mEtNumero.text.toString())
                    newCad.child("bairro").setValue(mEtBairro.text.toString())
                    newCad.child("cidade").setValue(mEtCidade.text.toString())
                    newCad.child("estado").setValue(estadoSelecionado)
                    newCad.child("emailCriador").setValue(AreaLojistaModels.userMail.toString())

                    AreaLojistaModels.endereco = mEtLogradouro.text.toString()+" "+mEtNumero.text.toString()+", "+mEtBairro.text.toString()+", "+mEtCidade.text.toString()+" - "+estadoSelecionado
                    // neste formato address = logradouro+" "+numero+", "+bairro+", "+cidade+" - "+estado

                    //só salvou até aqui não sei porque
                    newCad.child("dddTel").setValue(mEtTelDdd.text.toString())
                    newCad.child("dddCel").setValue(mEtCelDdd.text.toString())
                    newCad.child("cel").setValue(mEtCel.text.toString())

                    newCad.child("visitas").setValue(0)

                    //especiais
                    newCad.child("plano").setValue("basico")
                    newCad.child("impulsionamentos").setValue("nao") //vai ser sim quando tiver um impulsionamento ativo e nao quando nao estiver.Vai controlar apenas um por vez

                    newCad.child("bd").setValue(AreaLojistaModels.petBD)
                    newCad.child("alvara").setValue("nao")
                    newCad.child("lat").setValue("nao")
                    newCad.child("long").setValue("nao")
                    newCad.child("latlong").setValue("nao")
                    newCad.child("BDdoDono").setValue(AreaLojistaModels.userBD)
                    newCad.child("entrega").setValue("nao")
                    newCad.child("raio_entrega").setValue("nao")

                    newCad.child("aceita_credito").setValue("nao")
                    newCad.child("aceita_debito").setValue("nao")
                    newCad.child("cartoes").child("master").setValue("nao")
                    newCad.child("cartoes").child("visa").setValue("nao")
                    newCad.child("cartoes").child("elo").setValue("nao")
                    newCad.child("cartoes").child("outros").setValue("nao")

                    //parte delayout
                    newCad.child("banner").setValue("nao")
                    newCad.child("logo").setValue("nao")

                    newCad.child("servicos").child("24hrs").setValue("nao")
                    newCad.child("servicos").child("banhoTosa").setValue("nao")
                    newCad.child("servicos").child("entrega").setValue("nao")
                    newCad.child("servicos").child("farmacia").setValue("nao")
                    newCad.child("servicos").child("hospedagem").setValue("nao")
                    newCad.child("servicos").child("vetAtendDom").setValue("nao")
                    newCad.child("servicos").child("veterinario").setValue("nao")


                    newCad.child("itens_venda").setValue(0) //quantidade de produtos para vender.
                    newCad.child("tamanho_inventario").setValue(15) //Limite de usuário padrão

                    databaseReference.child("usuarios").child(AreaLojistaModels.userBD).child("tipo").setValue("empresario")
                    databaseReference.child("usuarios").child(AreaLojistaModels.userBD).child("petBD").setValue(AreaLojistaModels.petBD)
                    //val sharedPref: SharedPreferences = getSharedPreferences(getString(R.string.sharedpreferences), 0) //0 é private mode
                    //val editor = sharedPref.edit()

                    val mySharedPrefs: mySharedPrefs = mySharedPrefs(this)
                    mySharedPrefs.setValue("tipo", "empresario")
                    AreaLojistaModels.tipo = "empresario"
                    //editor.putString("tipo", "empresario")
                    //editor.putString("petBdSeForEmpresarioInicial", AreaLojistaModels.petBD)
                    mySharedPrefs.setValue("petBdSeForEmpresarioInicial", AreaLojistaModels.petBD)
                    //editor.apply()

                    val layCad1: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)
                    val layCad2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)

                    layCad1.visibility = View.GONE
                    layCad2.visibility = View.VISIBLE

                    EncerraDialog()
                } else {
                    //se for usuário existente, vai apenas atualizar alguns datos. Primeiro pega os dados, preenche os etdText

                    var apagaLatLong = 0  //se o user tiver mudado alguma informação do endereço, vamos apagar seus dados de latitude, longitude e latlong pois sua localização do mapa mudou.
                    if(!mNome.equals(mEtNome.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("nome")
                            .setValue(mEtNome.text.toString())
                    }

                    if(!mTel.equals(mEtTel.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("telefone")
                            .setValue(mEtTel.text.toString())
                    }
                    if(!mLogradouro.equals(mEtLogradouro.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("logradouro")
                            .setValue(mEtLogradouro.text.toString())
                        apagaLatLong=1
                    }
                    if(!mNumero.equals(mEtNumero.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("numero")
                            .setValue(mEtNumero.text.toString())
                        apagaLatLong=1
                    }
                    if(!mBairro.equals(mEtBairro.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("bairro")
                            .setValue(mEtBairro.text.toString())
                        apagaLatLong=1
                    }
                    if(!mCidade.equals(mEtCidade.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("cidade")
                            .setValue(mEtCidade.text.toString())
                        apagaLatLong=1
                    }

                    //estado salvamos sempre
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("estado").setValue(estadoSelecionado)

                    if(!mDddTel.equals(mEtTelDdd.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("dddTel")
                            .setValue(mEtTelDdd.text.toString())
                    }

                    if(!mDddCel.equals(mEtCelDdd.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("dddCel")
                            .setValue(mEtCelDdd.text.toString())
                    }

                    if(!mCel.equals(mEtCel.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("cel")
                            .setValue(mEtCel.text.toString())
                    }

                    //se alterou alguma info do endereço, zerar estas informações. Assim, no MapsActivity, será identificaod por algum usuário automáticamente e pegará o novo latlong.
                    if (apagaLatLong==1){
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("lat").setValue("nao")
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("long").setValue("nao")
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("latlong").setValue("nao")
                    }


                    AreaLojistaModels.endereco = mEtLogradouro.text.toString()+" "+mEtNumero.text.toString()+", "+mEtBairro.text.toString()+", "+mEtCidade.text.toString()+" - "+estadoSelecionado

                    val layCad1: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)
                    val layCad2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                    layCad1.visibility = View.GONE
                    layCad2.visibility = View.VISIBLE

                    EncerraDialog()

                }
            }
        }

        btnProximo.isEnabled=true //Libera ele. Pois quando aperta ele na primeira vez trava pra evitar de criar dois pets. Mas se o user voltar aqui na mesma sessão estaria enable=false. Entao liberamos

        /*
        val btnFinalizaCad: Button = findViewById(R.id.cadEmpBtnFinalizaCad)
        btnFinalizaCad.setOnClickListener {

            if (!AreaLojistaModels.alvara.equals("nao")){
                val layCad2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                layCad2.visibility = View.GONE
                val layIndexAreaLoja: ConstraintLayout = findViewById(R.id.lay_arealojista_index)
                layIndexAreaLoja.visibility = View.VISIBLE

                val intent = Intent(this, MapsActivity::class.java)
                //intent.putExtra("userBD", userBD)
                intent.putExtra("email", AreaLojistaModels.userMail)
                //intent.putExtra("alvara", alvara)
                //intent.putExtra("tipo", tipo)
                if (!AreaLojistaModels.petBD.equals("nao")){
                    intent.putExtra("petBD", AreaLojistaModels.petBD)
                }
                intent.putExtra("endereco", AreaLojistaModels.endereco)
                intent.putExtra("chamaLatLong", "sim")
                startActivity(intent)
                finish()

                /* não faz mais isso. Agora encerra e manda direto pra mapsActivity para pegar latLong
                //habilita os botões
                var btn: Button
                btn = findViewById(R.id.arealojistaIndexBtneditLayout)
                btn.isEnabled = true

                btn = findViewById(R.id.arealojistaIndexBtnCadProd)
                btn.isEnabled = true

                btn = findViewById(R.id.arealojistaIndexBtnGerenciaProd)
                btn.isEnabled = true

                btn = findViewById(R.id.arealojistaIndexBtnEntrega)
                btn.isEnabled = true

                btn = findViewById(R.id.arealojistaIndexBtnFormaPagamento)
                btn.isEnabled = true

                btn = findViewById(R.id.arealojistaIndexBtnCadastrarEmpreendimento)
                btn.setText("Atualizar informações")

                 */
            } else {
                openPopUp("Atenção", "Seu alvará ainda não foi enviado. Sua loja ainda não aparece para os cliente. Isto ocorrerá somente quando o alvará for recebido.", false, "n", "m", "n")
                val layCad2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                layCad2.visibility = View.GONE
                val layIndexAreaLoja: ConstraintLayout = findViewById(R.id.lay_arealojista_index)
                layIndexAreaLoja.visibility = View.VISIBLE
            }
        }

         */

        val btnWhatAppHelp: Button = findViewById(R.id.cadEmpEtCel_help)
        btnWhatAppHelp.setOnClickListener {
            openPopUp("Ajuda", "O número do WhatsApp é importante pois será por ele que as compras serão avisadas e os contatos com o cliente também serão feitos. Após cada compra, um resumo é enviado ao número cadastrado. Isto facilita o contato entre cliente e lojista.", false, "n", "n", "n")
        }

    }


         */

        val btnCadastrarEmpreendimento : Button = findViewById(R.id.arealojistaIndexBtnCadastrarEmpreendimento)
        btnCadastrarEmpreendimento.setOnClickListener {

            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layCadEmp : ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)


            if (AreaLojistaModels.tipo.equals("usuario")) {
                layIndex.visibility = View.GONE
                layCadEmp.visibility = View.VISIBLE
                clicksCadastroEmpresa(true) //true significa que é um usuário cadastrando a empresa pela primeira vez.

            } else {

                clicksCadastroEmpresa(false) //true significa que é um usuário cadastrando a empresa pela primeira vez.
                layIndex.visibility = View.GONE
                layCadEmp.visibility = View.VISIBLE

                /*
                if (AreaLojistaModels.alvara.equals("nao")){
                    layIndex.visibility = View.GONE
                    val layCadEmpAlvara: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                    layCadEmpAlvara.visibility = View.VISIBLE
                    Toast.makeText(this, "Você precisa finalizar seu cadastro enviando uma foto do alvará.", Toast.LENGTH_SHORT).show()
                    clicksCadastroEmpresa(false)
                } else {
                    layIndex.visibility = View.GONE
                    layCadEmp.visibility = View.VISIBLE
                    clicksCadastroEmpresa(false) //true significa que é um usuário cadastrando a empresa pela primeira vez.
                }

                 */
            }

        }

        val btnCadProdutos : Button = findViewById(R.id.arealojistaIndexBtnCadProd)
        btnCadProdutos.setOnClickListener {

            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layCadProd : ConstraintLayout = findViewById(R.id.layCadProdutos)

            layIndex.visibility = View.GONE
            layCadProd.visibility = View.VISIBLE
            clicksCadNovosProdutos()
        }

        val btnGerenciaProdutos: Button = findViewById(R.id.arealojistaIndexBtnGerenciaProd)
        btnGerenciaProdutos.setOnClickListener {

            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layGerenciaProd : ConstraintLayout = findViewById(R.id.layGerenciaProd)

            layIndex.visibility = View.GONE
            layGerenciaProd.visibility = View.VISIBLE
            ClicksGerenciaProdutos()
        }

        val btnEntrega: Button = findViewById(R.id.arealojistaIndexBtnEntrega)
        btnEntrega.setOnClickListener {
            ClicksConfigEntrega()
        }

        val btnFormaPagamento : Button = findViewById(R.id.arealojistaIndexBtnFormaPagamento)
        btnFormaPagamento.setOnClickListener {
            ClicksFormasPagamento()
        }

        val btnFechaActivity: Button = findViewById(R.id.btnFechaActivity)
        btnFechaActivity.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            //intent.putExtra("userBD", userBD)
            intent.putExtra("email", AreaLojistaModels.userMail)
            //intent.putExtra("alvara", alvara)
            //intent.putExtra("tipo", tipo)
            if (!AreaLojistaModels.petBD.equals("nao")){
                intent.putExtra("petBD", AreaLojistaModels.petBD)
            }
            intent.putExtra("endereco", AreaLojistaModels.endereco)
            if (!AreaLojistaModels.endereco.equals("nao")){
                intent.putExtra("chamaLatLong", "sim")
            }
            startActivity(intent)
            finish()
        }

        val btnPlanos: Button = findViewById(R.id.btnPlanos)
        btnPlanos.setOnClickListener {
            clickPlanos("normal")  //a situacao nao vai ser normal quando vier direto da outra activit
        }

        val btnImpulso: Button = findViewById(R.id.btnMinhasPromos)
        btnImpulso.setOnClickListener {
            clickImpulsionamentos()
        }

        btnCancelamento.setOnClickListener {
            ClicksCancelamento()
        }

        val btnGerarRelatorios: Button = findViewById(R.id.arealojistaIndexBtnGerarRelatorio)
        btnGerarRelatorios.setOnClickListener {
            ChamaDialog()
            ChamaDialog()
            val rootRef = databaseReference.child("petshops").child(AreaLojistaModels.petBD)
            rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    //TODO("Not yet implemented")
                    EncerraDialog()
                }

                override fun onDataChange(p0: DataSnapshot) {
                    //TODO("Not yet implemented")
                    var values: String
                    values = p0.child("plano").getValue().toString()

                    if (values.equals("basico")) {
                        //se o plano do usuário for o básico, ele vai ser direcionado para a página de mudar de planos
                        openPopUp("Que pena", "Você não tem acesso a estes recursos. Para acessar estes recursos você precisa fazer upgrade para o plano pago. Gostaria de passar para o plano premium?", true, "Sim, mudar de plano", "Não", "vaiPraPlano")

                    } else {

                        //abrir nova activity se for plano premium
                        val intent = Intent(this@arealojista, relatorio::class.java)
                        intent.putExtra("userBD", AreaLojistaModels.userBD)
                        intent.putExtra("petBD", AreaLojistaModels.petBD)
                        startActivity(intent)

                    }
                    EncerraDialog()

                }
            })
        }


        val motivo:String? = intent.getStringExtra("motivo")  //quando vem para completar cadastro a partir da popup
        if (motivo!=null){
            if (motivo.equals("logo")){//colocar a logomarca da empresa na loja
                btnAbreEditLayout.performClick()
            } else if (motivo.equals("itens")){ //cadastrar produtos
                Log.d("teste", "entrou no click certo")
                btnCadProdutos.performClick()
            } else if (motivo.equals("raio")){ //definir o raio da entrega
                btnEntrega.performClick()
            }
        }

        //checkAlvara()


    } //tudo que havia no onCreate

    fun liberaBotoesDoEmpresario (){

        if (AreaLojistaModels.tipo.equals("empresario")){
            AreaLojistaModels.petBD = intent.getStringExtra("petBD")



        } else {
            //se o tipo nao for empresário, significa então que é o primeiro acesso dele então todos botoes ficam escondidos menos o de cadastro
            //pode estar pendente de alvará mas ja ir trabalhando nas outras coisas da loja
            var btn: Button
            btn = findViewById(R.id.arealojistaIndexBtneditLayout)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnCadProd)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnGerenciaProd)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnEntrega)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnFormaPagamento)
            btn.isEnabled = false

            btn = findViewById(R.id.btnPlanos)
            btn.isEnabled = false

            btn = findViewById(R.id.btnMinhasPromos)
            btn.isEnabled = false

            //retirar loja
            btn = findViewById(R.id.btnCancelamento)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnGerarRelatorio)
            btn.isEnabled = false

        }

    }

    fun checkAlvara (){

        val btnCadastrarEmpreendimento : Button = findViewById(R.id.arealojistaIndexBtnCadastrarEmpreendimento)

        if (AreaLojistaModels.alvara.equals("nao") && AreaLojistaModels.tipo.equals("empresario")){
            //btnAbreEditLayout.isEnabled = false reforma 2
            //significa que ele já começou o cadastro mas nao enviou o alvará
            btnCadastrarEmpreendimento.setText("Enviar alvará pendente")
            //este procedimento nos outros botões já estão sendo feitos acima. Verificar


            var btn: Button
            btn = findViewById(R.id.arealojistaIndexBtneditLayout)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnCadProd)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnGerenciaProd)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnEntrega)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnFormaPagamento)
            btn.isEnabled = false

            btn = findViewById(R.id.btnPlanos)
            btn.isEnabled = false

            btn = findViewById(R.id.btnMinhasPromos)
            btn.isEnabled = false

            //retirar loja
            btn = findViewById(R.id.btnCancelamento)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnGerarRelatorio)
            btn.isEnabled = false


        } else if (!AreaLojistaModels.alvara.equals("nao") && AreaLojistaModels.tipo.equals("empresario")) {
            //significa que que já enviou alvará e aí mudamos o texto do botão
            btnCadastrarEmpreendimento.setText("Atualizar informações")

            var btn: Button
            btn = findViewById(R.id.arealojistaIndexBtneditLayout)
            btn.isEnabled = true

            btn = findViewById(R.id.arealojistaIndexBtnCadProd)
            btn.isEnabled = true

            btn = findViewById(R.id.arealojistaIndexBtnGerenciaProd)
            btn.isEnabled = true

            btn = findViewById(R.id.arealojistaIndexBtnEntrega)
            btn.isEnabled = true

            btn = findViewById(R.id.arealojistaIndexBtnFormaPagamento)
            btn.isEnabled = true

            btn = findViewById(R.id.btnPlanos)
            btn.isEnabled = true

            btn = findViewById(R.id.btnMinhasPromos)
            btn.isEnabled = true

            //retirar loja
            btn = findViewById(R.id.btnCancelamento)
            btn.isEnabled = true

            btn = findViewById(R.id.arealojistaIndexBtnGerarRelatorio)
            btn.isEnabled = true


        } else {
            //significa que é a primeira vez que o usuário entra ou ainda nem começou seu cadastro
            btnCadastrarEmpreendimento.setText("Cadastrar empresa")

            var btn: Button
            btn = findViewById(R.id.arealojistaIndexBtneditLayout)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnCadProd)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnGerenciaProd)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnEntrega)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnFormaPagamento)
            btn.isEnabled = false

            btn = findViewById(R.id.btnPlanos)
            btn.isEnabled = false

            btn = findViewById(R.id.btnMinhasPromos)
            btn.isEnabled = false

            //retirar loja
            btn = findViewById(R.id.btnCancelamento)
            btn.isEnabled = false

            btn = findViewById(R.id.arealojistaIndexBtnGerarRelatorio)
            btn.isEnabled = false


        }

    }

    fun clickImpulsionamentos (){

        var lay: ConstraintLayout = findViewById(R.id.layGerenciaImpulsos)
        lay.visibility = View.VISIBLE
        lay = findViewById(R.id.lay_arealojista_index)
        lay.visibility = View.GONE

        val btnVoltar: Button = findViewById(R.id.LayImpulsos_btnVoltar)
        btnVoltar.setOnClickListener {
            lay.visibility = View.VISIBLE
            lay = findViewById(R.id.layGerenciaImpulsos)
            lay.visibility = View.GONE
        }


        //também vai estar o click do botão de cancelar, caso haja impulsionamento ativo
        queryimpulsionamentosAtivos()
        //queryImpulsionamentosAtivos2()

    }

    fun clickPlanos (situacao: String)  {
        var layBasico : ConstraintLayout = findViewById(R.id.layPlanos_layPlanoBasico)
        var layPremium : ConstraintLayout = findViewById(R.id.layPlanos_layPlanoCompleto)
        val btnProximo: Button = findViewById(R.id.layPlano_btnProximo)
        var lay: ConstraintLayout = findViewById(R.id.layPlanos)


        var nomePet: String = "nao"
        lay.visibility = View.VISIBLE
        lay = findViewById(R.id.lay_arealojista_index)
        lay.visibility = View.GONE

        layBasico.visibility = View.VISIBLE
        layPremium.visibility = View.VISIBLE
        btnProximo.visibility = View.VISIBLE

        var btnRadioCom: RadioButton = findViewById(R.id.radioCompleto)
        var btnRadioBas: RadioButton = findViewById(R.id.radioBasico)
        val scroll: ScrollView = findViewById(R.id.scrollPlanos)
        val btnVoltar: Button = findViewById(R.id.layPlanos_btnvoltar)
        var laypag2Bas : ConstraintLayout = findViewById(R.id.layPlanos_pag2bas)
        val laypag2Comp : ConstraintLayout = findViewById(R.id.layPlanos_pag2Comp)

        btnVoltar.setOnClickListener {

            findViewById<EditText>(R.id.layCompl_etEmail).setText("")
            findViewById<Button>(R.id.layPlanos_btnConfirmaCompl).setOnClickListener { null }
            findViewById<Button>(R.id.layPlanos_btnConfirmaBas).setOnClickListener { null }

            if (situacao.equals("normal")){
                lay = findViewById(R.id.layPlanos)
                lay.visibility = View.GONE
                lay = findViewById(R.id.lay_arealojista_index)
                lay.visibility = View.VISIBLE
                laypag2Bas.visibility = View.GONE
                laypag2Comp.visibility = View.GONE
            } else {
                finish()
            }

        }

        btnRadioBas.setOnClickListener {
            btnRadioCom.isChecked = false
            layBasico.setBackgroundResource(R.drawable.ic_btnazulclaro)
            layPremium.setBackgroundResource(R.drawable.ic_btnbranco)
            scroll.scrollY=0


        }
        btnRadioCom.setOnClickListener {
            btnRadioBas.isChecked = false
            layPremium.setBackgroundResource(R.drawable.ic_btnazulclaro)
            layBasico.setBackgroundResource(R.drawable.ic_btnbranco)
            scroll.scrollY=0
        }

        var planoAntigo = "nao"

        btnProximo.setOnClickListener {
            layBasico.visibility = View.GONE
            layPremium.visibility = View.GONE
            btnProximo.visibility = View.GONE

            if (planoAntigo.equals("basico") && radioBasico.isChecked){
                Toast.makeText(this, "Não existem mudanças para fazer.", Toast.LENGTH_SHORT).show()
                btnVoltar.performClick()
            } else if (planoAntigo.equals("basico") && radioCompleto.isChecked){
                //implementar texto de upgrade
                laypag2Comp.visibility = View.VISIBLE
                val txt: TextView = findViewById(R.id.layPlanos_tvCompl)
                val btnConfirma : Button = findViewById(R.id.layPlanos_btnConfirmaCompl)
                val etEmail: EditText = findViewById(R.id.layCompl_etEmail)
                txt.setText("Que bom que você optou por fazer um upgrade e poder aproveitar nossos recursos para ajudar você a vender mais. \n\nNós vamos enviar um e-mail com isntruções e um boleto de cobrança no valor de R$20,00 assim que possível. Dependendo da demanda, este processo pode demorar alguns dias. Enviaremos para o seu e-mail de cadastro mas se quiser receber uma cópia deste e-mail em outro endereço de e-mail, informe no campo abaixo. Senão, deixe em branco.")
                btnConfirma.setOnClickListener {

                    //estes dados tem que mudar somente depois do pagamento do boleto.
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("plano").setValue("pendenteDeUpGrade")
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("impulsixonamentos").setValue("nao")
                    //databaseReference.child("petshops").child(petBD).child("tamanho_inventario").setValue("50")

                    databaseReference.child("pedidoDeUpGrade").child(AreaLojistaModels.petBD).child("email").setValue(AreaLojistaModels.userMail)
                    if (!etEmail.text.isEmpty()){
                        databaseReference.child("pedidoDeUpGrade").child(AreaLojistaModels.petBD).child("emailAlternativo").setValue(AreaLojistaModels.userMail)
                    }
                    databaseReference.child("pedidoDeUpGrade").child(AreaLojistaModels.petBD).child("bd").setValue(AreaLojistaModels.petBD)
                    databaseReference.child("pedidoDeUpGrade").child(AreaLojistaModels.petBD).child("data").setValue(AreaLojistaController.GetDate())
                    databaseReference.child("pedidoDeUpGrade").child(AreaLojistaModels.petBD).child("situacao").setValue("pendente")
                    databaseReference.child("pedidoDeUpGrade").child(AreaLojistaModels.petBD).child("nome").setValue(nomePet)


                    Toast.makeText(
                        this,
                        "As mudanças foram salvas. Seu plano agora é o Premium, mas está pendente de pagamento para liberar as funcionalidades.",
                        Toast.LENGTH_LONG
                    ).show()
                    btnVoltar.performClick()
                }

            } else if (planoAntigo.equals("completo") && radioCompleto.isChecked){
                Toast.makeText(this, "Não existem mudanças para fazer.", Toast.LENGTH_SHORT).show()
                btnVoltar.performClick()
            } else {
                //implementar saindo do completo e voltando pro básico
                laypag2Bas.visibility = View.VISIBLE
                val txt: TextView = findViewById(R.id.layPlanos_tvBas)
                txt.setText("Olá, toda mudança é importante.\nGostariamos de lembrar que ao mudar de plano você perderá funcionalidades. A capacidade da loja será reduzida, você não poderá colocar produtos em destaque para promoção e sua loja não aparecerá em destaque no mapa. \n\nSe ainda assim você quiser voltar ao plano básico, clique em confirmar abaixo. ")
                val btnConfirma : Button = findViewById(R.id.layPlanos_btnConfirmaBas)
                btnConfirma.setOnClickListener {
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("plano").setValue("basico")
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("tamanho_inventario").setValue("10")
                    Toast.makeText(this, "As mudanças foram salvas. Seu plano agora é o básico gratuito", Toast.LENGTH_SHORT).show()

                    databaseReference.child("pedidoDeDowngrade").child(AreaLojistaModels.petBD).child("email").setValue(AreaLojistaModels.userMail)
                    databaseReference.child("pedidoDeDowngrade").child(AreaLojistaModels.petBD).child("bd").setValue(AreaLojistaModels.petBD)
                    databaseReference.child("pedidoDeDowngrade").child(AreaLojistaModels.petBD).child("data").setValue(AreaLojistaController.GetDate())
                    databaseReference.child("pedidoDeDowngrade").child(AreaLojistaModels.petBD).child("situacao").setValue("pendente")

                    btnVoltar.performClick()
                }
            }




        }

        ChamaDialog()
        val rootRef = databaseReference.child("petshops").child(AreaLojistaModels.petBD)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")
                var values: String
                values = p0.child("plano").getValue().toString()
                planoAntigo = values
                if (values.equals("basico")) {
                    btnRadioBas.performClick()
                    //layBasico.setBackgroundResource(R.drawable.ic_btnazulclaro)
                } else {
                    btnRadioCom.performClick()
                    //layPremium.setBackgroundResource(R.drawable.ic_btnazulclaro)

                    //btnRadioCom.isChecked = true
                    //btnRadioBas.isChecked = false
                }
                nomePet = p0.child("nome").getValue().toString()

                EncerraDialog()

            }
        })

    }

    //cliques do Cadastro da empresa
    //novo ou velho é a modalidade. Se for um usuário cadastrando uma nova empresa, eNovo e true. Se for um usuário já existente que está apenas atualizando informações, é falso.
    fun clicksCadastroEmpresa (eNovo: Boolean) {

        var mLogradouro="nao"
        var mNumero = "nao"
        var mBairro = "nao"
        var mCidade = "nao"
        var mNome = "nao"
        var mDddTel = "nao"
        var mTel = "nao"
        var mDddCel = "nao"
        var mCel = "nao"

        val btnVoltar : Button = findViewById(R.id.layCadProd_btnVoltar)
        btnVoltar.setOnClickListener {
            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layCadEmp : ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)

            hideKeyboard()
            layIndex.visibility = View.VISIBLE
            layCadEmp.visibility = View.GONE

        }


        if (!eNovo){ //se nao for um usuário novo, carrega os dados já preenchidos nos campos

            ChamaDialog()

            var eT: EditText

            val rootRef = databaseReference.child("petshops").child(AreaLojistaModels.petBD)
            rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    //TODO("Not yet implemented")
                    EncerraDialog()
                }

                override fun onDataChange(p0: DataSnapshot) {
                    //TODO("Not yet implemented")
                    var values: String
                    values = p0.child("nome").value.toString()
                    eT = findViewById(R.id.cadEmpEtNome)
                    eT.setText(values)
                    mNome=values
                    values = p0.child("telefone").value.toString()
                    eT = findViewById(R.id.cadEmpEtTel)
                    eT.setText(values)
                    mTel=values
                    values = p0.child("logradouro").value.toString()
                    eT = findViewById(R.id.cadEmpEtLogradouro)
                    mLogradouro=values
                    eT.setText(values)
                    values = p0.child("numero").value.toString()
                    eT = findViewById(R.id.cadEmpEtNumero)
                    eT.setText(values)
                    mNumero=values
                    values = p0.child("bairro").value.toString()
                    eT = findViewById(R.id.cadEmpEtBairro)
                    eT.setText(values)
                    mBairro=values
                    values = p0.child("cidade").value.toString()
                    eT = findViewById(R.id.cadEmpEtCidade)
                    eT.setText(values)
                    mCidade=values
                    values = p0.child("dddTel").value.toString()
                    eT = findViewById(R.id.cadEmpEtTel_ddd)
                    eT.setText(values)
                    mDddTel=values
                    values = p0.child("dddCel").value.toString()
                    eT = findViewById(R.id.cadEmpEtCel_ddd)
                    eT.setText(values)
                    mDddCel=values
                    values = p0.child("cel").value.toString()
                    eT = findViewById(R.id.cadEmpEtCel)
                    eT.setText(values)
                    mCel=values


                    EncerraDialog()
                }

            })
        }

        //setUpPermissionCamera()

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
        val spinnerEstado: Spinner = findViewById(R.id.spinnerEstado)
        //Adapter for spinner
        spinnerEstado.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list_of_items)

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

        val btnWhatAppHelp: Button = findViewById(R.id.cadEmpEtCel_help)
        btnWhatAppHelp.setOnClickListener {
            openPopUp("Ajuda", "O número do WhatsApp é importante pois será por ele que as compras serão avisadas e os contatos com o cliente também serão feitos. Após cada compra, um resumo é enviado ao número cadastrado. Isto facilita o contato entre cliente e lojista.", false, "n", "n", "n")
        }


        val btnProximo:Button = findViewById(R.id.cadEmpProximo)
        btnProximo.setOnClickListener {
            ChamaDialog()

            hideKeyboard()

            var mEtNome: EditText = findViewById(R.id.cadEmpEtNome)
            var mEtTelDdd : EditText = findViewById(R.id.cadEmpEtTel_ddd) //falta fazer
            var mEtTel: EditText = findViewById(R.id.cadEmpEtTel)
            var mEtLogradouro: EditText = findViewById(R.id.cadEmpEtLogradouro)
            var mEtNumero: EditText = findViewById(R.id.cadEmpEtNumero)
            var mEtBairro: EditText = findViewById(R.id.cadEmpEtBairro)
            var mEtCidade: EditText = findViewById(R.id.cadEmpEtCidade)
            var mEtCelDdd : EditText = findViewById(R.id.cadEmpEtCel_ddd) //falta fazer
            var mEtCel : EditText = findViewById(R.id.cadEmpEtCel) //falta fazer


            //verificações dos ettexts e se o alvara=1
            if (mEtNome.text.isEmpty()){
                mEtNome.requestFocus()
                mEtNome.setError("Informe o nome")
                EncerraDialog()
            } else if (mEtTel.text.isEmpty()){
                mEtTel.requestFocus()
                mEtTel.setError("Informe o telefone de contato")
                EncerraDialog()
            } else if (mEtLogradouro.text.isEmpty()){
                mEtLogradouro.requestFocus()
                mEtLogradouro.setError("Informe o logradouro")
                EncerraDialog()
            } else if (mEtNumero.text.isEmpty()){
                mEtNumero.requestFocus()
                mEtNumero.setError("Informe o número do endereço")
                EncerraDialog()
            } else if (mEtBairro.text.isEmpty()){
                mEtBairro.requestFocus()
                mEtBairro.setError("Informe o bairro")
                EncerraDialog()
            } else if (mEtCidade.text.isEmpty()) {
                mEtCidade.requestFocus()
                mEtCidade.setError("Informe a cidade")
                EncerraDialog()
                //} else if (alvaraOk==0){
                // Toast.makeText(this, "Você precisa enviar a foto do alvará", Toast.LENGTH_SHORT).show()
            } else if (mEtTelDdd.text.isEmpty() || mEtTelDdd.text.length != 2) {
                mEtTelDdd.requestFocus()
                mEtTelDdd.setError("!")
                EncerraDialog()
            } else if (mEtCelDdd.text.isEmpty() || mEtCelDdd.text.length != 2) {
                mEtCelDdd.requestFocus()
                mEtCelDdd.setError("!")
                EncerraDialog()
            } else if (mEtCel.text.isEmpty()){
                mEtCel.requestFocus()
                mEtCel.setError("!")
                EncerraDialog()
            } else if (estadoSelecionado.equals("Selecione Estado")){
                Toast.makeText(this, "Selecione o Estado antes de prosseguir.", Toast.LENGTH_SHORT).show()
                spinnerEstado.requestFocus()
                EncerraDialog()
            } else {

                btnProximo.isEnabled=false

                if (eNovo) { //se for novo, vai criar todos os campos no BD
                    //registrar criando o path do usuario. Vai ficar faltando o alvará que será feito na próxima página.
                    val newCad: DatabaseReference = databaseReference.child("petshops").push()
                    AreaLojistaModels.petBD = newCad.key.toString()
                    newCad.child("nome").setValue(mEtNome.text.toString())
                    newCad.child("telefone").setValue(mEtTel.text.toString())
                    newCad.child("logradouro").setValue(mEtLogradouro.text.toString())
                    newCad.child("numero").setValue(mEtNumero.text.toString())
                    newCad.child("bairro").setValue(mEtBairro.text.toString())
                    newCad.child("cidade").setValue(mEtCidade.text.toString())
                    newCad.child("estado").setValue(estadoSelecionado)
                    newCad.child("emailCriador").setValue(AreaLojistaModels.userMail.toString())

                    AreaLojistaModels.endereco = mEtLogradouro.text.toString()+" "+mEtNumero.text.toString()+", "+mEtBairro.text.toString()+", "+mEtCidade.text.toString()+" - "+estadoSelecionado
                    // neste formato address = logradouro+" "+numero+", "+bairro+", "+cidade+" - "+estado

                    //só salvou até aqui não sei porque
                    newCad.child("dddTel").setValue(mEtTelDdd.text.toString())
                    newCad.child("dddCel").setValue(mEtCelDdd.text.toString())
                    newCad.child("cel").setValue(mEtCel.text.toString())

                    newCad.child("visitas").setValue(0)

                    //especiais
                    newCad.child("plano").setValue("basico")
                    newCad.child("impulsionamentos").setValue("nao") //vai ser sim quando tiver um impulsionamento ativo e nao quando nao estiver.Vai controlar apenas um por vez

                    newCad.child("bd").setValue(AreaLojistaModels.petBD)
                    newCad.child("alvara").setValue("nao")
                    newCad.child("lat").setValue("nao")
                    newCad.child("long").setValue("nao")
                    newCad.child("latlong").setValue("nao")
                    newCad.child("BDdoDono").setValue(AreaLojistaModels.userBD)
                    newCad.child("entrega").setValue("nao")
                    newCad.child("raio_entrega").setValue("nao")

                    newCad.child("aceita_credito").setValue("nao")
                    newCad.child("aceita_debito").setValue("nao")
                    newCad.child("cartoes").child("master").setValue("nao")
                    newCad.child("cartoes").child("visa").setValue("nao")
                    newCad.child("cartoes").child("elo").setValue("nao")
                    newCad.child("cartoes").child("outros").setValue("nao")

                    //parte delayout
                    newCad.child("banner").setValue("nao")
                    newCad.child("logo").setValue("nao")

                    newCad.child("servicos").child("24hrs").setValue("nao")
                    newCad.child("servicos").child("banhoTosa").setValue("nao")
                    newCad.child("servicos").child("entrega").setValue("nao")
                    newCad.child("servicos").child("farmacia").setValue("nao")
                    newCad.child("servicos").child("hospedagem").setValue("nao")
                    newCad.child("servicos").child("vetAtendDom").setValue("nao")
                    newCad.child("servicos").child("veterinario").setValue("nao")


                    newCad.child("itens_venda").setValue(0) //quantidade de produtos para vender.
                    newCad.child("tamanho_inventario").setValue(15) //Limite de usuário padrão

                    databaseReference.child("usuarios").child(AreaLojistaModels.userBD).child("tipo").setValue("empresario")
                    databaseReference.child("usuarios").child(AreaLojistaModels.userBD).child("petBD").setValue(AreaLojistaModels.petBD)
                    //val sharedPref: SharedPreferences = getSharedPreferences(getString(R.string.sharedpreferences), 0) //0 é private mode
                    //val editor = sharedPref.edit()

                    val mySharedPrefs: mySharedPrefs = mySharedPrefs(this)
                    mySharedPrefs.setValue("tipo", "empresario")
                    AreaLojistaModels.tipo = "empresario"
                    //editor.putString("tipo", "empresario")
                    //editor.putString("petBdSeForEmpresarioInicial", AreaLojistaModels.petBD)
                    mySharedPrefs.setValue("petBdSeForEmpresarioInicial", AreaLojistaModels.petBD)
                    //editor.apply()


                    //esta parte entrou por ultimo quando retiramos a obrigatoriedade do alvará
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("alvara").setValue(urifinal)
                    AreaLojistaModels.alvaraOk=1
                    AreaLojistaModels.alvara="pendente"  //vamos declarar alvara pendente para todos pets que fizerem cadastro sem necessidade. Assim no futuro se quisermos voltar, basta que questionemos todos os "pendente"

                    mySharedPrefs.setValue("tipo", "empresario")
                    AreaLojistaModels.tipo = "empresario"

                    val layCad1: Button = findViewById(R.id.lay_cadastrarEmpreendimento)
                    val layCad2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)

                    layCad1.visibility = View.GONE
                    layCad2.visibility = View.VISIBLE

                    liberaBotoesDoEmpresario ()
                    //checkAlvara ()  nao fazemos mais esta checkagem

                    openPopUp("Muito bem!", "Vamos cadastrar seu primeiro produto? Demora menos de 30 segundos.", false, "n", "n", "n")
                    val btnCadProd: Button = findViewById(R.id.arealojistaIndexBtnCadProd)
                    btnCadProd.performClick()

                    EncerraDialog()
                } else {
                    //se for usuário existente, vai apenas atualizar alguns datos. Primeiro pega os dados, preenche os etdText

                    var apagaLatLong = 0  //se o user tiver mudado alguma informação do endereço, vamos apagar seus dados de latitude, longitude e latlong pois sua localização do mapa mudou.
                    if(!mNome.equals(mEtNome.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("nome")
                            .setValue(mEtNome.text.toString())
                    }

                    if(!mTel.equals(mEtTel.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("telefone")
                            .setValue(mEtTel.text.toString())
                    }
                    if(!mLogradouro.equals(mEtLogradouro.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("logradouro")
                            .setValue(mEtLogradouro.text.toString())
                        apagaLatLong=1
                    }
                    if(!mNumero.equals(mEtNumero.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("numero")
                            .setValue(mEtNumero.text.toString())
                        apagaLatLong=1
                    }
                    if(!mBairro.equals(mEtBairro.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("bairro")
                            .setValue(mEtBairro.text.toString())
                        apagaLatLong=1
                    }
                    if(!mCidade.equals(mEtCidade.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("cidade")
                            .setValue(mEtCidade.text.toString())
                        apagaLatLong=1
                    }

                    //estado salvamos sempre
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("estado").setValue(estadoSelecionado)

                    if(!mDddTel.equals(mEtTelDdd.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("dddTel")
                            .setValue(mEtTelDdd.text.toString())
                    }

                    if(!mDddCel.equals(mEtCelDdd.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("dddCel")
                            .setValue(mEtCelDdd.text.toString())
                    }

                    if(!mCel.equals(mEtCel.text.toString())) {
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("cel")
                            .setValue(mEtCel.text.toString())
                    }

                    //se alterou alguma info do endereço, zerar estas informações. Assim, no MapsActivity, será identificaod por algum usuário automáticamente e pegará o novo latlong.
                    if (apagaLatLong==1){
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("lat").setValue("nao")
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("long").setValue("nao")
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("latlong").setValue("nao")
                    }


                    AreaLojistaModels.endereco = mEtLogradouro.text.toString()+" "+mEtNumero.text.toString()+", "+mEtBairro.text.toString()+", "+mEtCidade.text.toString()+" - "+estadoSelecionado

                    val layCad1: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)
                    val layCad2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                    layCad1.visibility = View.GONE
                    layCad2.visibility = View.VISIBLE

                    EncerraDialog()

                }
            }
        }

        btnProximo.isEnabled=true //Libera ele. Pois quando aperta ele na primeira vez trava pra evitar de criar dois pets. Mas se o user voltar aqui na mesma sessão estaria enable=false. Entao liberamos

        /*
        val btnFinalizaCad: Button = findViewById(R.id.cadEmpBtnFinalizaCad)
        btnFinalizaCad.setOnClickListener {

            if (!AreaLojistaModels.alvara.equals("nao")){
                val layCad2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                layCad2.visibility = View.GONE
                val layIndexAreaLoja: ConstraintLayout = findViewById(R.id.lay_arealojista_index)
                layIndexAreaLoja.visibility = View.VISIBLE

                val intent = Intent(this, MapsActivity::class.java)
                //intent.putExtra("userBD", userBD)
                intent.putExtra("email", AreaLojistaModels.userMail)
                //intent.putExtra("alvara", alvara)
                //intent.putExtra("tipo", tipo)
                if (!AreaLojistaModels.petBD.equals("nao")){
                    intent.putExtra("petBD", AreaLojistaModels.petBD)
                }
                intent.putExtra("endereco", AreaLojistaModels.endereco)
                intent.putExtra("chamaLatLong", "sim")
                startActivity(intent)
                finish()

                /* não faz mais isso. Agora encerra e manda direto pra mapsActivity para pegar latLong
                //habilita os botões
                var btn: Button
                btn = findViewById(R.id.arealojistaIndexBtneditLayout)
                btn.isEnabled = true

                btn = findViewById(R.id.arealojistaIndexBtnCadProd)
                btn.isEnabled = true

                btn = findViewById(R.id.arealojistaIndexBtnGerenciaProd)
                btn.isEnabled = true

                btn = findViewById(R.id.arealojistaIndexBtnEntrega)
                btn.isEnabled = true

                btn = findViewById(R.id.arealojistaIndexBtnFormaPagamento)
                btn.isEnabled = true

                btn = findViewById(R.id.arealojistaIndexBtnCadastrarEmpreendimento)
                btn.setText("Atualizar informações")

                 */
            } else {
                openPopUp("Atenção", "Seu alvará ainda não foi enviado. Sua loja ainda não aparece para os cliente. Isto ocorrerá somente quando o alvará for recebido.", false, "n", "m", "n")
                val layCad2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                layCad2.visibility = View.GONE
                val layIndexAreaLoja: ConstraintLayout = findViewById(R.id.lay_arealojista_index)
                layIndexAreaLoja.visibility = View.VISIBLE
            }
        }

         */

        /*  Estes dois métodos perderam o sentido quando removemos o alvará do cadastro. Se for voltar depois basta ressucitar aqui. Aliás, todos os métodos originais deste procedimiento estão guardados aqui em um comentário

        val btnAjudaSendAlvara:Button = findViewById(R.id.cadEmpBtnSendalvaraHelp)
        btnAjudaSendAlvara.setOnClickListener {
            openPopUp("Por que é importante para nós uma foto do alvará?", "A imagem do seu alvará não será pública. Nós exigimos uma foto deste documento para ajudar a garantir a segurança de todos na nossa comunidade. O envio de uma foto do alvará nos ajuda a previnir fraudes e golpes contra nossos usuários.", false, "sim", "nao", "alvaraHelp")
        }

         */
        /*
        val btnSendAlvara:Button = findViewById(R.id.cadEmpBtnSendAlvara)
        btnSendAlvara.setOnClickListener {
            //takePicture(1)
            //verifica se o usuario deu as permissões. O resultado disto sai no método override onRequestPermissionResult logo abaixo do onCreate.
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissões não concedidas. Impossível prosseguir", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    0
                )
            } else {
                //ChamaDialog()
                AreaLojistaModels.processo="alvara"
                takePictureFromCamera()
            }

        }
         */

    }

    //Neste método salvamos tudo que iremos precisar depois
    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Save the user's current game state

        savedInstanceState.putString("alvaraOk", AreaLojistaModels.alvaraOk.toString())
        savedInstanceState.putString("processo", AreaLojistaModels.processo)
        savedInstanceState.putString("petBD", AreaLojistaModels.petBD)
        savedInstanceState.putString("userMail", AreaLojistaModels.userMail)
        savedInstanceState.putString("userBD", AreaLojistaModels.userBD)
        savedInstanceState.putString("alvara", AreaLojistaModels.alvara)
        savedInstanceState.putString("tipo", AreaLojistaModels.tipo)

        //savedInstanceState.putInt(PLAYER_LEVEL, mCurrentLevel)

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState)
    }

    //aqui recuperamos tudo.
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState)

        val nome = savedInstanceState.getString("nome")
        val desc = savedInstanceState.getString("desc")

        savedInstanceState.putString("alvaraOk", AreaLojistaModels.alvaraOk.toString())
        savedInstanceState.putString("processo", AreaLojistaModels.processo)
        savedInstanceState.putString("petBD", AreaLojistaModels.petBD)
        savedInstanceState.putString("userMail", AreaLojistaModels.userMail)
        savedInstanceState.putString("userBD", AreaLojistaModels.userBD)
        savedInstanceState.putString("alvara", AreaLojistaModels.alvara)
        savedInstanceState.putString("tipo", AreaLojistaModels.tipo)

        val x = savedInstanceState.getString("alvaraOk").toString()
        AreaLojistaModels.alvaraOk = x.toInt()
        AreaLojistaModels.processo = savedInstanceState.getString("processo").toString()
        AreaLojistaModels.petBD = savedInstanceState.getString("petBD").toString()
        AreaLojistaModels.userMail = savedInstanceState.getString("userMail").toString()
        AreaLojistaModels.userBD = savedInstanceState.getString("userBD").toString()
        AreaLojistaModels.alvara = savedInstanceState.getString("alvara").toString()
        AreaLojistaModels.tipo = savedInstanceState.getString("tipo").toString()

        val layIndex: ConstraintLayout = findViewById(R.id.lay_arealojista_index)
        val layCad1: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)
        val layCad2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)

        Log.d("teste", "entrou em onRestoreInstanceStete() e o valor de alvara é "+AreaLojistaModels.alvara)
        Toast.makeText(this, "Um erro ocorreu. Tentando recuperar os dados.", Toast.LENGTH_SHORT).show()
        clicksCadastroEmpresa(false)


        layIndex.visibility = View.GONE
        layCad1.visibility = View.GONE
        layCad2.visibility = View.VISIBLE


        //val btnAdicionarPet: Button = findViewById(R.id.adocao_btnAnunciar)
        //btnAdicionarPet.performClick()

    }

    override fun onPause() {
        super.onPause()

        // val sharedPref: SharedPreferences = this@arealojista.getSharedPreferences(
          //  getString(R.string.sharedpreferences), Context.MODE_PRIVATE
            //na linha abaixo: “nome” é como vc vai se referir a esta info para
            //recuperar dados depois, e nome é a variável que guardei.
        val sharedPref: SharedPreferences = getSharedPreferences(getString(R.string.sharedpreferences), 0) //0 é private mode
        val editor = sharedPref.edit()
        editor.putString("alvaraOk", AreaLojistaModels.alvaraOk.toString())
        editor.putString("processo", AreaLojistaModels.processo)
        editor.putString("petBD", AreaLojistaModels.petBD)
        editor.putString("userMail", AreaLojistaModels.userMail)
        editor.putString("userBD", AreaLojistaModels.userBD)
        editor.putString("alvara", AreaLojistaModels.alvara)
        editor.putString("tipo", AreaLojistaModels.tipo)
        editor.putString("alvaraProblem", "sim")
        editor.apply()


    }


    /*
    override fun onResume() {
        super.onResume()

        val sharedPref: SharedPreferences = getSharedPreferences(getString(R.string.sharedpreferences), 0) //0 é private mode
        val editor = sharedPref.edit()

        val x = sharedPref.getString("alvaraProblem", "nao")
        if (x!=null){
            //entrou
            processo = sharedPref.getString("processo", "nao")!!
            petBD = sharedPref.getString("petBD", "nao")!!
            userMail = sharedPref.getString("userMail", "nao")!!
            userBD = sharedPref.getString("userBD", "nao")!!
            alvara = sharedPref.getString("alvara", "nao")!!
            tipo = sharedPref.getString("tipo", "nao")!!

            if (x.equals("sim")){

                val layIndex: ConstraintLayout = findViewById(R.id.lay_arealojista_index)
                val layCad1: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)
                val layCad2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)

                Log.d("teste", "entrou em onResume() e o valor de alvara é "+alvara)
                Toast.makeText(this, "Um erro ocorreu. Tentando recuperar os dados.", Toast.LENGTH_SHORT).show()
                clicksCadastroEmpresa(false)


                layIndex.visibility = View.GONE
                layCad1.visibility = View.GONE
                layCad2.visibility = View.VISIBLE


            }
        }



    }
     */


    //prepara os cliques dos botões dentro de edição do layout. Inclusive para o envio das fotos
    fun clicksLayoutEdit (){

        val btnBack : Button = findViewById(R.id.lay_editLayout_btnVoltar)
        btnBack.setOnClickListener {
            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layEditLayout : ConstraintLayout = findViewById(R.id.lay_edit_layout_loja)

            layIndex.visibility = View.VISIBLE
            layEditLayout.visibility = View.GONE

        }

        /*
        val btnChangeBanner : Button = findViewById(R.id.lay_edit_layout_loja_bannerChangeBtn)
        btnChangeBanner.setOnClickListener {
            //aqui mudar imagem
            processo = "banner"
            takePictureFromGallery()
        }

         */

        val btnChangeLogo : Button = findViewById(R.id.lay_edit_layout_loja_logoChangeBtn)
        btnChangeLogo.setOnClickListener {
            AreaLojistaModels.processo="logo"
            takePictureFromGallery()
        }


    }


    fun uploadImafeFromBanco(bitmap: Bitmap, newCad: DatabaseReference, nomeProd:String) {

        mFireBaseStorage = FirebaseStorage.getInstance()
        mphotoStorageReference = mFireBaseStorage.reference
        //mphotoStorageReference = mFireBaseStorage.getReference().child(AreaLojistaModels.petBD).child("alvara").child("alvara")

        mphotoStorageReference = mFireBaseStorage.getReference().child(AreaLojistaModels.petBD).child("produtos").child("img_"+nomeProd)

        //get the uri from the bitmap
        val tempUri: Uri = getImageUri(this, bitmap)
        //transform the new compressed bmp in filepath uri
        filePath = tempUri

        val bmp: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath)
        val baos: ByteArrayOutputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 55, baos)

        //var file = Uri.fromFile(bitmap)
        var uploadTask = mphotoStorageReference.putFile(filePath)

        val urlTask = uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                    EncerraDialog()
                    Toast.makeText(this, "Ocorreu um erro", Toast.LENGTH_SHORT).show()
                }
            }
            return@Continuation mphotoStorageReference.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                urifinal = downloadUri.toString()

                newCad.child("img").setValue(urifinal)

                EncerraDialog()

            } else {
                // Handle failures
                EncerraDialog()
                Toast.makeText(this, "um erro ocorreu no envio da imagem.", Toast.LENGTH_SHORT).show()
                // ...
            }
        }.addOnFailureListener {
            // Uh-oh, an error occurred.
        }


    }

    fun clicksCadNovosProdutos (){

        if (AreaLojistaModels.itens_venda.equals("nao")){ //se for diferente de nao é pq já fez a query antes.
            queryProdutosDoPet()
        }
        atualizaProdutos()


        //todo procedimento abaixo é para cadastro de produtos pré-fabricados num banco de dados interno do app

        //metodos do cadastro proprio de produtos
        //primeiro carregar os dados no array
        //var list_of_nomes = AreaLojistaModels.getAllItems()

        var list_of_nomes = AreaLojistaModels.getAllItems()
        var list_of_images = AreaLojistaModels.getAllImages()
        var list_of_desc = AreaLojistaModels.getAllDesc()
        var list_of_tipo = AreaLojistaModels.getAllTipos()

        //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
        val adapter: produtosDoBancoRecyclerAdapter = produtosDoBancoRecyclerAdapter(this, list_of_nomes, list_of_images)
        //chame a recyclerview
        val recyclerView: RecyclerView = findViewById(R.id.cadProd_RecyclerView)
        //define o tipo de layout (linerr, grid)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)
        //coloca o adapter na recycleview
        recyclerView.adapter = adapter
        recyclerView.layoutManager = linearLayoutManager
        // Notify the adapter for data change.
        adapter.notifyDataSetChanged()



        val etPrecoBanco: EditText = findViewById(R.id.etSearchEngine)

        //s: CharSequence, start: Int,  count: Int, after: Int
        //parte de filtragem
        etPrecoBanco.addTextChangedListener(object : TextWatcher {
            var changed: Boolean = false

            override fun afterTextChanged(p0: Editable?) {
                changed = false


            }

            override fun beforeTextChanged(p0: CharSequence?, start: Int,count: Int, after: Int) {
                changed=false

                /*
                   list_of_nomes = AreaLojistaModels.getAllItems()
                   list_of_images = AreaLojistaModels.getAllImages()
                   list_of_desc = AreaLojistaModels.getAllDesc()
                   list_of_tipo = AreaLojistaModels.getAllTipos()
                    adapter.notifyDataSetChanged()


                 */

            }

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                if (!changed) {
                    changed = true

                    var cont=0
                    while (cont<list_of_nomes.size){
                        if (list_of_nomes.get(cont).contains(p0.toString())){
                            //arrayIndexes.add(cont)
                            //do nothing
                        } else {
                            list_of_nomes.removeAt(cont)
                            list_of_images.removeAt(cont)
                            list_of_desc.removeAt(cont)
                            list_of_tipo.removeAt(cont)
                        }
                        cont++
                    }
                    adapter.notifyDataSetChanged()

                    if (p0!!.length==0){

                            list_of_nomes = AreaLojistaModels.getAllItems()
                            list_of_images = AreaLojistaModels.getAllImages()
                            list_of_desc = AreaLojistaModels.getAllDesc()
                            list_of_tipo = AreaLojistaModels.getAllTipos()
                            ExibeEscondePreview("n", "n", "n", "n")
                            clicksCadNovosProdutos()
                    }


                }


            }
        })



        recyclerView.addOnItemTouchListener(RecyclerTouchListener(this, recyclerView!!, object: ClickListener{
            override fun onClick(view: View, position: Int) {

                val pop: ConstraintLayout= findViewById(R.id.cadProdDoBanco_PopUp)
                pop.visibility = View.VISIBLE

                //val etPreco: EditText = findViewById(R.id.cadProdDoBanco_Preco)
                AreaLojistaController.CurrencyWatcherNew(etPrecoBanco)

                val btnFechar: Button = findViewById(R.id.cadProdDoBanco_btnFechar)
                btnFechar.setOnClickListener { pop.visibility = View.GONE }

                val tvNome: TextView = findViewById(R.id.cadProdDoBanco_etNome)
                val img: ImageView = findViewById(R.id.cadProdDoBanco_img)

                tvNome.setText(list_of_nomes.get(position))
                Glide.with(this@arealojista).load("").placeholder(list_of_images.get(position)).into(img)

                val btnCad: Button = findViewById(R.id.cadProdDoBanco_btnCad)
                btnCad.setOnClickListener {
                    btnCad.isEnabled=false
                    if (etPrecoBanco.text.isEmpty()){
                        etPrecoBanco.setError("Informe o preço")
                        btnCad.isEnabled = true
                    } else if (etPrecoBanco.text.equals("R$0,00")){
                        etPrecoBanco.requestFocus()
                        //etPreco.setError("informe um valor ao produto")
                        Toast.makeText(this@arealojista, "informe um valor ao produto", Toast.LENGTH_SHORT).show()
                    } else if (etPrecoBanco.text.equals("R$00,0")){
                        etPrecoBanco.requestFocus()
                        //etPreco.setError("informe um valor ao produto")
                        Toast.makeText(this@arealojista, "informe um valor ao produto", Toast.LENGTH_SHORT).show()
                    } else if (AreaLojistaModels.itens_venda.toInt()>=AreaLojistaModels.inventario_size){ //caso já tenha atingido o limite.
                        hideKeyboard()
                        openPopUp2("Limite atingido", "Você atingiu o limite de itens. Você pode expandir a quantidade pagando uma pequena taxa ou apagar itens antigos.", false, "aa", "bb", "aa")

                    } else {

                        var str:String = etPrecoBanco.text.toString().replace("R$", "")
                        str = str.replace(",", "").trim()
                        str = str.replace(".", "").trim()

                        val newCad: DatabaseReference = databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("produtos").push()

                        val bm = BitmapFactory.decodeResource(this@arealojista.getResources(), list_of_images.get(position)) //conver o drawable em um bitmap
                        uploadImafeFromBanco(bm, newCad, list_of_nomes.get(position))

                        newCad.child("nome").setValue(list_of_nomes.get(position))
                        newCad.child("desc").setValue(list_of_desc.get(position))
                        newCad.child("preco").setValue(str)
                        ///newCad.child("img").setValue(link) tem q fazer upload
                        newCad.child("controle").setValue("item")
                        newCad.child("tipo").setValue(list_of_tipo.get(position))

                        AreaLojistaModels.itens_venda = (AreaLojistaModels.itens_venda.toInt()+1).toString()
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("itens_venda").setValue(AreaLojistaModels.itens_venda)
                        atualizaProdutos()
                        btnFechar.performClick()
                        Toast.makeText(this@arealojista, "Produto cadastrado!", Toast.LENGTH_SHORT).show()

                        btnCad.isEnabled = true
                    }


                }

            }
            override fun onLongClick(view: View?, position: Int) {

            }
        }))


        ExibeEscondePreview("n", "n", "n", "n")

        val btnNovaImagem : Button = findViewById(R.id.cadNovoProd_btnEnviarImagem)
        val etPreco : EditText = findViewById(R.id.cadNovoProd_preco)
        val btnFechar: Button = findViewById(R.id.cadNovoProd_Fechar)
        val etNome: EditText = findViewById(R.id.cadNovoProd_nome)
        //val etDesc: EditText = findViewById(R.id.cadNovoProd_descric)

        etPreco.setInputType(InputType.TYPE_CLASS_NUMBER)
        //so colocar o currencyTextWatcher se o campo estiver vazio. Se for R$0,00 é pq o user saiu e voltou. Evitando travar

        if (etPreco.text.toString().isEmpty()){
            AreaLojistaController.CurrencyWatcherNew(etPreco)
        } else {

        }

        btnFechar.setOnClickListener {
            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layCadProd : ConstraintLayout = findViewById(R.id.layCadProdutos)

            layIndex.visibility = View.VISIBLE
            layCadProd.visibility = View.GONE

            //limpando tudo
            findViewById<EditText>(R.id.cadNovoProd_nome).setText("")
            findViewById<EditText>(R.id.cadNovoProd_descric).setText("")
            //etPreco.removeTextChangedListener(CurrencyWatcherNew(etPreco))
            findViewById<ImageView>(R.id.cadNovoProd_laySample_imgView).setImageResource(0)
            findViewById<Button>(R.id.cadNovoProd_help).setOnClickListener { null }
            findViewById<Button>(R.id.cadNovoProd_btnEnviarImagem).setOnClickListener { null }
            findViewById<Button>(R.id.cadNovoProd_btnCadastrar).setOnClickListener { null }
            btnFechar.setOnClickListener { null }
            clicksCadNovosProdutos()




        }

        //radio buttons
        val rRacao: RadioButton = findViewById(R.id.cadProd_radioBtnRacoes)
        val rServ: RadioButton = findViewById(R.id.cadProd_radioBtnServicos)
        val rAcess: RadioButton = findViewById(R.id.cadProd_radioBtnAcessorios)
        val rEstet: RadioButton = findViewById(R.id.cadProd_radioBtnEstetica)
        val rRemed: RadioButton = findViewById(R.id.cadProd_radioBtnRemedios)

        var tipo = "nao"
        rRacao.setOnClickListener {
            rServ.isChecked=false
            rAcess.isChecked=false
            rEstet.isChecked=false
            rRemed.isChecked=false
            tipo="racao"
            hideKeyboard()
        }

        rServ.setOnClickListener {
            rRacao.isChecked=false
            rAcess.isChecked=false
            rEstet.isChecked=false
            rRemed.isChecked=false
            tipo="servicos"
            hideKeyboard()
        }

        rAcess.setOnClickListener {
            rServ.isChecked=false
            rRacao.isChecked=false
            rEstet.isChecked=false
            rRemed.isChecked=false
            tipo="acessorios"
            hideKeyboard()
        }
        rEstet.setOnClickListener {
            rServ.isChecked=false
            rAcess.isChecked=false
            rRacao.isChecked=false
            rRemed.isChecked=false
            tipo="estetica"
            hideKeyboard()
        }
        rRemed.setOnClickListener {
            rServ.isChecked=false
            rAcess.isChecked=false
            rEstet.isChecked=false
            rRacao.isChecked=false
            tipo="remedios"
            hideKeyboard()
        }

        //btn help
        val btnHelp : Button = findViewById(R.id.cadNovoProd_help)
        btnHelp.setOnClickListener {
            openPopUp("Dica", "Dê preferência para fotos com fundo branco. Elas darão destaque melhor ao seu produto e deixará a imagem da sua loja muito melhor para o cliente.", false, "n", "n", "n")
        }

        btnNovaImagem.setOnClickListener {
            if (etNome.text.isEmpty()){
                etNome.requestFocus()
                etNome.setError("Informe o nome do produto primeiro")
            } else if (etPreco.text.isEmpty()){
                etPreco.requestFocus()
                etPreco.setError("Informe o preço do produto primeiro")
            } else if (etPreco.text.equals("R$0,00")){
                etPreco.requestFocus()
                //etPreco.setError("informe um valor ao produto")
                Toast.makeText(this, "informe um valor ao produto", Toast.LENGTH_SHORT).show()
            } else if (etPreco.text.equals("R$00,0")){
                etPreco.requestFocus()
                //etPreco.setError("informe um valor ao produto")
                Toast.makeText(this, "informe um valor ao produto", Toast.LENGTH_SHORT).show()
            } else if (AreaLojistaModels.itens_venda.toInt()>=AreaLojistaModels.inventario_size){ //caso já tenha atingido o limite.
                hideKeyboard()
                openPopUp2("Limite atingido", "Você atingiu o limite de itens a venda. Você pode expandir a quantidade pagando uma pequena taxa ou apagar itens antigos.", false, "aa", "bb", "aa")

            } else if (tipo=="nao"){
                Toast.makeText(this, "Informe o tipo de produto que você está cadastrando", Toast.LENGTH_SHORT).show()
            } else {
                hideKeyboard()
                //rola pro final para exibir o botão
                val scrollView: ScrollView = findViewById(R.id.scrollCadProd)
                val max = scrollView.scrollY
                scrollView.scrollY=max
                AreaLojistaModels.processo = "cadNovoProd"
                openPopUp2("Envio de imagem", "Selecione o modo de envio da imagem:", true, "Tirar foto", "foto do celular", "fotoNovoProd")
            }
        }


    }

    /*
    //prepara os cliques dos botões do cadastro de produtos
    fun clicksCadNovosProdutos (){

        if (AreaLojistaModels.itens_venda.equals("nao")){ //se for diferente de nao é pq já fez a query antes.
            queryProdutosDoPet()
        }
        atualizaProdutos()


        //todo procedimento abaixo é para cadastro de produtos pré-fabricados num banco de dados interno do app

        //metodos do cadastro proprio de produtos
        //primeiro carregar os dados no array
        //var list_of_nomes = AreaLojistaModels.getAllItems()

        var list_of_nomes = AreaLojistaModels.getAllItems()
        var list_of_images = AreaLojistaModels.getAllImages()
        var list_of_desc = AreaLojistaModels.getAllDesc()
        var list_of_tipo = AreaLojistaModels.getAllTipos()

        //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
        val adapter: produtosDoBancoRecyclerAdapter = produtosDoBancoRecyclerAdapter(this, list_of_nomes, list_of_images)
        //chame a recyclerview
        val recyclerView: RecyclerView = findViewById(R.id.cadProd_RecyclerView)
        //define o tipo de layout (linerr, grid)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)
        //coloca o adapter na recycleview
        recyclerView.adapter = adapter
        recyclerView.layoutManager = linearLayoutManager
        // Notify the adapter for data change.
        adapter.notifyDataSetChanged()


        val etPrecoBanco: EditText = findViewById(R.id.etSearchEngine)

        //s: CharSequence, start: Int,  count: Int, after: Int
        //parte de filtragem
        etPrecoBanco.addTextChangedListener(object : TextWatcher {
            var changed: Boolean = false

            override fun afterTextChanged(p0: Editable?) {
                changed = false

                if (p0!!.length==0) {
                    list_of_nomes = AreaLojistaModels.getAllItems()
                    list_of_images = AreaLojistaModels.getAllImages()
                    list_of_desc = AreaLojistaModels.getAllDesc()
                    list_of_tipo = AreaLojistaModels.getAllTipos()
                    adapter.notifyDataSetChanged()
                }


            }

            override fun beforeTextChanged(p0: CharSequence?, start: Int,count: Int, after: Int) {
                changed=false

                /*
                   list_of_nomes = AreaLojistaModels.getAllItems()
                   list_of_images = AreaLojistaModels.getAllImages()
                   list_of_desc = AreaLojistaModels.getAllDesc()
                   list_of_tipo = AreaLojistaModels.getAllTipos()
                    adapter.notifyDataSetChanged()


                 */

            }

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                if (!changed) {
                    changed = true

                    var cont=0
                        while (cont<list_of_nomes.size){
                            if (list_of_nomes.get(cont).contains(p0.toString())){
                                //arrayIndexes.add(cont)
                                //do nothing
                            } else {
                                list_of_nomes.removeAt(cont)
                                list_of_images.removeAt(cont)
                                list_of_desc.removeAt(cont)
                                list_of_tipo.removeAt(cont)
                            }
                            cont++
                        }
                        adapter.notifyDataSetChanged()



                }


            }
        })



        recyclerView.addOnItemTouchListener(RecyclerTouchListener(this, recyclerView!!, object: ClickListener{
            override fun onClick(view: View, position: Int) {

                val pop: ConstraintLayout= findViewById(R.id.cadProdDoBanco_PopUp)
                pop.visibility = View.VISIBLE

                //val etPreco: EditText = findViewById(R.id.cadProdDoBanco_Preco)
                AreaLojistaController.CurrencyWatcherNew(etPrecoBanco)

                val btnFechar: Button = findViewById(R.id.cadProdDoBanco_btnFechar)
                btnFechar.setOnClickListener { pop.visibility = View.GONE }

                val tvNome: TextView = findViewById(R.id.cadProdDoBanco_etNome)
                val img: ImageView = findViewById(R.id.cadProdDoBanco_img)

                tvNome.setText(list_of_nomes.get(position))
                Glide.with(this@arealojista).load("").placeholder(list_of_images.get(position)).into(img)

                val btnCad: Button = findViewById(R.id.cadProdDoBanco_btnCad)
                btnCad.setOnClickListener {
                    btnCad.isEnabled=false
                    if (etPrecoBanco.text.isEmpty()){
                        etPrecoBanco.setError("Informe o preço")
                        btnCad.isEnabled = true
                    } else if (etPrecoBanco.text.equals("R$0,00")){
                        etPrecoBanco.requestFocus()
                        //etPreco.setError("informe um valor ao produto")
                        Toast.makeText(this@arealojista, "informe um valor ao produto", Toast.LENGTH_SHORT).show()
                    } else if (etPrecoBanco.text.equals("R$00,0")){
                        etPrecoBanco.requestFocus()
                        //etPreco.setError("informe um valor ao produto")
                        Toast.makeText(this@arealojista, "informe um valor ao produto", Toast.LENGTH_SHORT).show()
                    } else if (AreaLojistaModels.itens_venda.toInt()>=AreaLojistaModels.inventario_size){ //caso já tenha atingido o limite.
                        hideKeyboard()
                        openPopUp2("Limite atingido", "Você atingiu o limite de itens. Você pode expandir a quantidade pagando uma pequena taxa ou apagar itens antigos.", false, "aa", "bb", "aa")

                    } else {

                        var str:String = etPrecoBanco.text.toString().replace("R$", "")
                        str = str.replace(",", "").trim()
                        str = str.replace(".", "").trim()

                        val newCad: DatabaseReference = databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("produtos").push()

                        val bm = BitmapFactory.decodeResource(this@arealojista.getResources(), list_of_images.get(position)) //conver o drawable em um bitmap
                        uploadImafeFromBanco(bm, newCad, list_of_nomes.get(position))

                        newCad.child("nome").setValue(list_of_nomes.get(position))
                        newCad.child("desc").setValue(list_of_desc.get(position))
                        newCad.child("preco").setValue(str)
                        ///newCad.child("img").setValue(link) tem q fazer upload
                        newCad.child("controle").setValue("item")
                        newCad.child("tipo").setValue(list_of_tipo.get(position))

                        AreaLojistaModels.itens_venda = (AreaLojistaModels.itens_venda.toInt()+1).toString()
                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("itens_venda").setValue(AreaLojistaModels.itens_venda)
                        atualizaProdutos()
                        btnFechar.performClick()
                        Toast.makeText(this@arealojista, "Produto cadastrado!", Toast.LENGTH_SHORT).show()

                        btnCad.isEnabled = true
                    }


                }

            }
            override fun onLongClick(view: View?, position: Int) {

            }
        }))





        ExibeEscondePreview("n", "n", "n", "n")

        val btnNovaImagem : Button = findViewById(R.id.cadNovoProd_btnEnviarImagem)
        val etPreco : EditText = findViewById(R.id.cadNovoProd_preco)
        val btnFechar: Button = findViewById(R.id.cadNovoProd_Fechar)
        val etNome: EditText = findViewById(R.id.cadNovoProd_nome)
        //val etDesc: EditText = findViewById(R.id.cadNovoProd_descric)

        etPreco.setInputType(InputType.TYPE_CLASS_NUMBER)
        //so colocar o currencyTextWatcher se o campo estiver vazio. Se for R$0,00 é pq o user saiu e voltou. Evitando travar

        if (etPreco.text.toString().isEmpty()){
            AreaLojistaController.CurrencyWatcherNew(etPreco)
        } else {

        }

        btnFechar.setOnClickListener {
            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layCadProd : ConstraintLayout = findViewById(R.id.layCadProdutos)

            layIndex.visibility = View.VISIBLE
            layCadProd.visibility = View.GONE

            //limpando tudo
            findViewById<EditText>(R.id.cadNovoProd_nome).setText("")
            findViewById<EditText>(R.id.cadNovoProd_descric).setText("")
            //etPreco.removeTextChangedListener(CurrencyWatcherNew(etPreco))
            findViewById<ImageView>(R.id.cadNovoProd_laySample_imgView).setImageResource(0)
            findViewById<Button>(R.id.cadNovoProd_help).setOnClickListener { null }
            findViewById<Button>(R.id.cadNovoProd_btnEnviarImagem).setOnClickListener { null }
            findViewById<Button>(R.id.cadNovoProd_btnCadastrar).setOnClickListener { null }
            btnFechar.setOnClickListener { null }
            clicksCadNovosProdutos()




        }

        //radio buttons
        val rRacao: RadioButton = findViewById(R.id.cadProd_radioBtnRacoes)
        val rServ: RadioButton = findViewById(R.id.cadProd_radioBtnServicos)
        val rAcess: RadioButton = findViewById(R.id.cadProd_radioBtnAcessorios)
        val rEstet: RadioButton = findViewById(R.id.cadProd_radioBtnEstetica)
        val rRemed: RadioButton = findViewById(R.id.cadProd_radioBtnRemedios)

        var tipo = "nao"
        rRacao.setOnClickListener {
            rServ.isChecked=false
            rAcess.isChecked=false
            rEstet.isChecked=false
            rRemed.isChecked=false
            tipo="racao"
            hideKeyboard()
        }

        rServ.setOnClickListener {
            rRacao.isChecked=false
            rAcess.isChecked=false
            rEstet.isChecked=false
            rRemed.isChecked=false
            tipo="servicos"
            hideKeyboard()
        }

        rAcess.setOnClickListener {
            rServ.isChecked=false
            rRacao.isChecked=false
            rEstet.isChecked=false
            rRemed.isChecked=false
            tipo="acessorios"
            hideKeyboard()
        }
        rEstet.setOnClickListener {
            rServ.isChecked=false
            rAcess.isChecked=false
            rRacao.isChecked=false
            rRemed.isChecked=false
            tipo="estetica"
            hideKeyboard()
        }
        rRemed.setOnClickListener {
            rServ.isChecked=false
            rAcess.isChecked=false
            rEstet.isChecked=false
            rRacao.isChecked=false
            tipo="remedios"
            hideKeyboard()
        }

        //btn help
        val btnHelp : Button = findViewById(R.id.cadNovoProd_help)
        btnHelp.setOnClickListener {
            openPopUp("Dica", "Dê preferência para fotos com fundo branco. Elas darão destaque melhor ao seu produto e deixará a imagem da sua loja muito melhor para o cliente.", false, "n", "n", "n")
        }

        btnNovaImagem.setOnClickListener {
            if (etNome.text.isEmpty()){
                etNome.requestFocus()
                etNome.setError("Informe o nome do produto primeiro")
            } else if (etPreco.text.isEmpty()){
                etPreco.requestFocus()
                etPreco.setError("Informe o preço do produto primeiro")
            } else if (etPreco.text.equals("R$0,00")){
                etPreco.requestFocus()
                //etPreco.setError("informe um valor ao produto")
                Toast.makeText(this, "informe um valor ao produto", Toast.LENGTH_SHORT).show()
            } else if (etPreco.text.equals("R$00,0")){
                etPreco.requestFocus()
                //etPreco.setError("informe um valor ao produto")
                Toast.makeText(this, "informe um valor ao produto", Toast.LENGTH_SHORT).show()
            } else if (AreaLojistaModels.itens_venda.toInt()>=AreaLojistaModels.inventario_size){ //caso já tenha atingido o limite.
                hideKeyboard()
                openPopUp2("Limite atingido", "Você atingiu o limite de itens a venda. Você pode expandir a quantidade pagando uma pequena taxa ou apagar itens antigos.", false, "aa", "bb", "aa")

            } else if (tipo=="nao"){
                Toast.makeText(this, "Informe o tipo de produto que você está cadastrando", Toast.LENGTH_SHORT).show()
            } else {
                hideKeyboard()
                //rola pro final para exibir o botão
                val scrollView: ScrollView = findViewById(R.id.scrollCadProd)
                val max = scrollView.scrollY
                scrollView.scrollY=max
                AreaLojistaModels.processo = "cadNovoProd"
                openPopUp2("Envio de imagem", "Selecione o modo de envio da imagem:", true, "Tirar foto", "foto do celular", "fotoNovoProd")
            }
        }


    }


     */
    //prepara os cliques e monta a recycleview
    fun ClicksGerenciaProdutos () {

        val btnFechar : Button = findViewById(R.id.layGerencia_btnVoltar)
        btnFechar.setOnClickListener {
            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layGerenciaProd : ConstraintLayout = findViewById(R.id.layGerenciaProd)

            layIndex.visibility = View.VISIBLE
            layGerenciaProd.visibility = View.GONE
            arrayNomes.clear()
            arrayDesc.clear()
            arrayPreco.clear()
            arrayImg.clear()
            arrayBD.clear()
        }

        if (AreaLojistaModels.itens_venda.equals("nao")){ //se for diferente de nao é pq já fez a query antes.
            queryProdutosDoPet()
        }

        queryItensDaLojaParaRecycleView()


    }

    //prepara os cliques e monta a recycleview
    fun ClicksConfigEntrega (){

        val etValorEntrega : EditText = findViewById(R.id.layEntregas_etValor)
        val btnHelp : Button = findViewById(R.id.layEntregasHelp)
        val btnSalvar: Button = findViewById(R.id.layEntregas_btnSalvar)
        val cbGratuita: CheckBox = findViewById(R.id.layEntregas_cbGratis)
        val cbNaoEntrega: CheckBox = findViewById(R.id.layEntregas_cbNaoEntrega)


        etValorEntrega.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus){
                cbGratuita.isChecked= false
                cbNaoEntrega.isChecked= false
            }
        }

        cbGratuita.setOnClickListener {
            etValorEntrega.setText("")

        }

        cbNaoEntrega.setOnClickListener {
            etValorEntrega.setText("")
        }

        val btnFechar : Button = findViewById(R.id.layEntregas_btnFechar)
        btnFechar.setOnClickListener {
            var lay: ConstraintLayout = findViewById(R.id.layEntregas)
            lay.visibility = View.GONE
            lay = findViewById(R.id.lay_arealojista_index)
            lay.visibility = View.VISIBLE

            btnHelp.setOnClickListener { null }
            btnSalvar.setOnClickListener { null }

        }

        val layEntrega: ConstraintLayout = findViewById(R.id.layEntregas)
        layEntrega.visibility = View.VISIBLE

        var fazEntrega = false
        var valorEntrega = "nao"

        if (etValorEntrega.text.toString().isEmpty()){
            AreaLojistaController.CurrencyWatcherNew(etValorEntrega)
        } else {

        }


        ChamaDialog()
        val rootRef = databaseReference.child("petshops").child(AreaLojistaModels.petBD)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")
                var values: String
                values = p0.child("servicos").child("entrega").getValue().toString()

                if (values.equals("sim")){
                    fazEntrega = true
                }
                values = p0.child("entrega").getValue().toString()
                valorEntrega = values

                if (fazEntrega==false){
                    cbNaoEntrega.isChecked = true
                }

                if (valorEntrega.equals("nao") || valorEntrega.equals("gratis")){
                    //do nothing
                } else {
                    etValorEntrega.setText(valorEntrega)
                }

                if (valorEntrega.equals("gratis")){
                    cbGratuita.isChecked = true

                    etValorEntrega.setText("R$0,00")
                }


                if (fazEntrega==false){
                    cbNaoEntrega.isChecked = true
                }

                if (valorEntrega.equals("nao") || valorEntrega.equals("gratis")){
                    //do nothing
                } else {
                    etValorEntrega.setText(valorEntrega)
                }

                if (valorEntrega.equals("gratis")){
                    cbGratuita.isChecked = true
                    //etValorEntrega.setText("")
                    etValorEntrega.setText("R$0,00")
                }

                cbGratuita.setOnClickListener {
                    cbNaoEntrega.isChecked = false
                    //etValorEntrega.setText("")
                    etValorEntrega.setText("R$0,00")

                }

                cbNaoEntrega.setOnClickListener {
                    cbGratuita.isChecked = false
                    etValorEntrega.setText("")

                }

                btnHelp.setOnClickListener {
                    openPopUp("Configuração da entrega", "Informe ao lado o valor que sua empresa cobra para entregas. Para o caso de entregas gratuitas ou de não oferecer serviçod e entrega marque nos botões abaixo.", false, "n", "n", "n")
                }

                EncerraDialog()

            }
        })

        val etDistancia: EditText = findViewById(R.id.etDistanciaEntrega)
        etDistancia.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val distanciaEmInt = (etDistancia.text.toString()).toInt()
                if (distanciaEmInt>60){
                    etDistancia.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_wrong, 0);
                } else {
                    etDistancia.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_done, 0);
                }
            }

        })


        btnSalvar.setOnClickListener {

            if (cbNaoEntrega.isChecked){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("entrega").setValue("nao")
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("entrega").setValue("nao")
            } else if (cbGratuita.isChecked){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("entrega").setValue("gratis")
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("entrega").setValue("sim")
            } else if (etValorEntrega.text.isEmpty() || etValorEntrega.text.equals("R$0,00")){
                etValorEntrega.requestFocus()
                etValorEntrega.setError("Informe um valor")
            } else {
                var str:String = etValorEntrega.text.toString().replace("R$", "")
                str = str.replace(",", "").trim()
                str = str.replace(".", "").trim()
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("entrega").setValue(str)
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("entrega").setValue("sim")
            }
            layEntrega.visibility = View.GONE


            if (!etDistancia.text.toString().isEmpty()){
                val distanciaEmInt = (etDistancia.text.toString()).toInt()
                if (distanciaEmInt >= 60){
                    Toast.makeText(this, "Distância muito grande. A informação não foi salva", Toast.LENGTH_SHORT).show()
                } else {
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("raio_entrega").setValue(etDistancia.text.toString())
                }

            }
            openPopUp("Pronto", "A configuração da sua entrega foi salva com sucesso.", false, "n", "n", "n")


        }



    }

    //prepara os cliques de gerenciamento de formas de pagamento
    fun ClicksFormasPagamento (){

        var mFazQuery2 = 0 //0 nao precisa 1 tem qe fazer

        val layPagamentos : ConstraintLayout = findViewById(R.id.layFormaPagamento)
        layPagamentos.visibility = View.VISIBLE

        var cbElo: CheckBox = findViewById(R.id.pagamentos_cbElo)
        var cbMaster: CheckBox = findViewById(R.id.pagamentos_cbMaster)
        var cbVisa: CheckBox = findViewById(R.id.pagamentos_cbVisa)
        var cbOutros: CheckBox = findViewById(R.id.pagamentos_cbOutros)

        val btnFechar : Button = findViewById(R.id.layPagamentos_btnFechar)
        btnFechar.setOnClickListener {
            layPagamentos.visibility = View.GONE
            val layIndex : ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            layIndex.visibility = View.VISIBLE
        }

        val cbCredito : CheckBox = findViewById(R.id.pagamentos_cbCredito)
        val cbDebito : CheckBox = findViewById(R.id.pagamentos_cbDebito)

        val layDetalhesDosCartoes: ConstraintLayout = findViewById(R.id.lay_formasDePagamento2)

        cbDebito.setOnClickListener {
            if (cbDebito.isChecked){
                layDetalhesDosCartoes.visibility = View.VISIBLE
            } else {
                if (!cbCredito.isChecked)
                    layDetalhesDosCartoes.visibility = View.INVISIBLE
            }
        }

        cbCredito.setOnClickListener {
            if (cbCredito.isChecked){
                layDetalhesDosCartoes.visibility = View.VISIBLE
            } else {
                if (!cbDebito.isChecked){
                    layDetalhesDosCartoes.visibility = View.INVISIBLE
                }
            }
        }

        val btnSalvar : Button = findViewById(R.id.pagamentos_btnSalvar)
        btnSalvar.setOnClickListener {

            var aceitaCartao = false
            if (cbCredito.isChecked){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("aceita_credito").setValue("sim")
                aceitaCartao = true
            }
            if (cbDebito.isChecked){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("aceita_debito").setValue("sim")
                aceitaCartao = true
            }

            if (aceitaCartao){

                var checkBox: CheckBox
                checkBox = findViewById(R.id.pagamentos_cbElo)

                if (checkBox.isChecked){
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("cartoes").child("elo").setValue("sim")
                }
                checkBox = findViewById(R.id.pagamentos_cbMaster)
                if (checkBox.isChecked){
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("cartoes").child("master").setValue("sim")
                }
                checkBox = findViewById(R.id.pagamentos_cbVisa)
                if (checkBox.isChecked){
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("cartoes").child("visa").setValue("sim")
                }
                checkBox = findViewById(R.id.pagamentos_cbOutros)
                if (checkBox.isChecked){
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("cartoes").child("outros").setValue("sim")
                }
            }

            layPagamentos.visibility = View.GONE
            openPopUp("Pronto", "As informações foram salvas", false, "n", "n", "n")

        }


        ChamaDialog()
        //este é o primeiro passo: Vamos pegar os dados prévios do user
        val rootRef = databaseReference.child("petshops").child(AreaLojistaModels.petBD)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {

                var values = "nao"
                values = p0.child("aceita_credito").value.toString()
                if (values.equals("sim")){
                    cbCredito.isChecked=true
                    //se aceita credito ou débito, então vai abrir o layout das bandeiras dos cartões
                    //layDetalhesDosCartoes.visibility = View.VISIBLE
                } else {
                    cbCredito.isChecked = false
                }
                values = p0.child("aceita_debito").value.toString()
                if (values.equals("sim")){
                    cbDebito.isChecked=true
                    //layDetalhesDosCartoes.visibility = View.VISIBLE
                } else {
                    cbDebito.isChecked = false
                }

                if (cbCredito.isChecked==true || cbDebito.isChecked==true){
                    layDetalhesDosCartoes.visibility = View.VISIBLE

                    //vamos então pegar quais cartões são aceitos, já que ele aceita cartão
                    values = p0.child("cartoes").child("elo").value.toString()
                    if (values.equals("sim")){
                        cbElo.isChecked=true
                        //layDetalhesDosCartoes.visibility = View.VISIBLE
                    } else {
                        cbElo.isChecked = false
                    }

                    values = p0.child("cartoes").child("master").value.toString()
                    if (values.equals("sim")){
                        cbMaster.isChecked=true
                        //layDetalhesDosCartoes.visibility = View.VISIBLE
                    } else {
                        cbMaster.isChecked = false
                    }

                    values = p0.child("cartoes").child("visa").value.toString()
                    if (values.equals("sim")){
                        cbVisa.isChecked=true
                        //layDetalhesDosCartoes.visibility = View.VISIBLE
                    } else {
                        cbVisa.isChecked = false
                    }

                    values = p0.child("cartoes").child("outros").value.toString()
                    if (values.equals("sim")){
                        cbOutros.isChecked=true
                        //layDetalhesDosCartoes.visibility = View.VISIBLE
                    } else {
                        cbOutros.isChecked = false
                    }

                    EncerraDialog()

                } else {
                    EncerraDialog()
                }

            }
        })



    }

    fun ClicksCancelamento (){
        val layCancela : ConstraintLayout = findViewById(R.id.layCancelaPetShop)
        layCancela.visibility = View.VISIBLE

        val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
        layIndex.visibility = View.GONE

        val btn: Button = findViewById(R.id.layCancela_btnVoltar)
        btn.setOnClickListener {
            layCancela.visibility = View.GONE
            layIndex.visibility = View.VISIBLE
            layCancela_btnSugestao.setOnClickListener { null }
            layCancela_layExibe.visibility = View.VISIBLE
            layCancela_btnCancelaFinal.setOnClickListener { null }
        }

        val etSugestao: EditText = findViewById(R.id.layCancela_etSugestao)

        //se apertar no botao de sugestao exibe o EtSugestao
        layCancela_btnSugestao.setOnClickListener {
            etSugestao.visibility = View.VISIBLE
        }

        //exibir o etProblema
        btnCancelarConfirma1.setOnClickListener {
            layCancela_layExibe.visibility = View.VISIBLE
        }

        //apaga tudo. Abre uma janela perguntando se confirma, e se sim, apaga tudo.
        layCancela_btnCancelaFinal.setOnClickListener {

            if (layCancela_etMotivo.text.isEmpty()){
                layCancela_etMotivo.setError("Informe o motivo de sua saída. Usamos esta informoação para tentar melhorar")
            } else {

                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setMessage("Você tem certeza que deseja remover sua empresa do mapa e apagar a loja, juntamente com todos seus produtos? Informamos que este procedimento não tem volta pois todos os dados serão apagados")
                    .setTitle("Atenção")
                    .setCancelable(true)
                    .setPositiveButton("Sim, apagar tudo", DialogInterface.OnClickListener { dialog, which ->

                        //apagando os dados da loja. Primeiro do storage
                        ChamaDialog()
                        val arrayInfos: MutableList<String> = ArrayList()

                        FirebaseDatabase.getInstance().reference.child("petshops").child(AreaLojistaModels.petBD).child("produtos").orderByChild("controle").equalTo("item")
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    for (querySnapshot in dataSnapshot.children) {

                                        if (dataSnapshot == null) {
                                            //EncerraDialog()
                                            //criar usuário
                                            //createUser()
                                        } else {

                                            //carregar infos
                                            var values: String
                                            //pega o endereço de cada iamgem de cada item para apagar a imagem
                                            values = querySnapshot.child("img").getValue().toString()
                                            arrayInfos.add(values)

                                        }
                                    }



                                    EncerraDialog()
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    // Getting Post failed, log a message
                                    EncerraDialog()
                                    // ...
                                }
                            })


                        var cont=0
                        while (cont<arrayInfos.size){

                            val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(arrayInfos.get(cont)) //urifinal is a String variable with the url
                            storageReference.delete().addOnSuccessListener {
                                //File deleted

                            }.addOnFailureListener {
                                //failed to delete
                            }

                            cont++
                        }

                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).removeValue()
                        databaseReference.child("Baixas").child(AreaLojistaModels.petBD).child("motivo").setValue(layCancela_etMotivo.text.toString())
                        if (!etSugestao.text.isEmpty()){
                            databaseReference.child("Baixas").child(AreaLojistaModels.petBD).child("sugestao").setValue(etSugestao.text.toString())
                        }
                        var teste: String
                        teste = databaseReference.child("Baixas").child(AreaLojistaModels.petBD).child("motivo").toString()
                        Log.d("teste", "o valor é "+teste)
                        databaseReference.child("usuarios").child(AreaLojistaModels.userBD).child("tipo").setValue("usuario")
                        Toast.makeText(this, "Toda informação da sua empresa foi removida", Toast.LENGTH_SHORT).show()
                        finish()

                    })
                // Display a negative button on alert dialog
                builder.setNegativeButton("Não"){dialog,which ->
                    //do nothing
                }
                val alert : AlertDialog = builder.create()
                alert.show()
            }
        }
    }

    fun noWatcher (editText: EditText){

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
    }

    //seta a recycleview
    fun SetUpRecycleViewForGerenciaDeTitulos (){

        //INICIO DO CODIGO DA RECYCLEVIEW
        var adapter: MinhaLojaRecyclerAdapter = MinhaLojaRecyclerAdapter(this, arrayNomes, arrayImg, arrayDesc, arrayPreco, arrayBD)

        //chame a recyclerview
        var recyclerView: RecyclerView = findViewById(R.id.recyclerView_gerenciaProds)

        //define o tipo de layout (linerr, grid)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)

        //coloca o adapter na recycleview
        recyclerView.adapter = adapter

        recyclerView.layoutManager = linearLayoutManager

        // Notify the adapter for data change.
        adapter.notifyDataSetChanged()

        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView.addOnItemTouchListener(RecyclerTouchListener(this, recyclerView!!, object: ClickListener{

            override fun onClick(view: View, position: Int) {
                FazerMudancas(arrayNomes.get(position), arrayDesc.get(position), arrayPreco.get(position), arrayBD.get(position), arrayImg.get(position), position)
            }

            override fun onLongClick(view: View?, position: Int) {

            }
        }))
        //FIM DA RECYCLEVIEW

    }

    //click listener da primeira recycleview
    interface ClickListener {
        fun onClick(view: View, position: Int)

        fun onLongClick(view: View?, position: Int)
    }

    //gerencia do click da recycleview
    internal class RecyclerTouchListener(context: Context, recyclerView: RecyclerView, private val clickListener: ClickListener?) : RecyclerView.OnItemTouchListener {

        private val gestureDetector: GestureDetector

        init {
            gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    val child = recyclerView.findChildViewUnder(e.x, e.y)
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildPosition(child))
                    }
                }
            })
        }

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {

            val child = rv.findChildViewUnder(e.x, e.y)
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildPosition(child))
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {

        }
    }

    //metodo para gerenciar as mudanças nos produtos chamados a partir da recycleview  GERENCIA DE PRODUTOS
    fun FazerMudancas (nome: String, desc: String, preco: String, bd: String, linkImg: String, posicao: Int){

        val etNome: EditText = findViewById(R.id.layGerenciaProd_etNome)
        val etDesc: EditText = findViewById(R.id.layGerenciaProd_etDesc)
        val etPreco: EditText = findViewById(R.id.layGerenciaProd_etPreco)
        val btnSalvarEsair: Button = findViewById(R.id.layGerenciaProd_btnSalvarMudancas)

        val layEdit : ConstraintLayout = findViewById(R.id.layGerenciaProd_layEdit)
        layEdit.visibility = View.VISIBLE

        layEdit.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                // ignore all touch events
                return true
            }
        })

        AreaLojistaController.CurrencyWatcherNew(etPreco)

        etNome.setText(nome)
        if (desc.equals("nao")){
            etDesc.setText("")
        } else {
            etDesc.setText(desc)
        }

        etPreco.setText(preco) //preco já está formato pq ele é armazenado no array já no formato correto.


        btnSalvarEsair.setOnClickListener {
            val layEdit : ConstraintLayout = findViewById(R.id.layGerenciaProd_layEdit)
            layEdit.visibility = View.GONE

            if (!nome.equals(etNome.text.toString())){
                //databaseReference.child("petshops").child(petBD).child("produtos").child(bd).child("nome").setValue(etNome.text)
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("produtos").child(bd).child("nome").setValue(etNome.text.toString())
                arrayNomes.add(posicao, etNome.text.toString())
            }
            if (!desc.equals(etDesc.text.toString())){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("produtos").child(bd).child("desc").setValue(etDesc.text.toString())
                arrayDesc.add(posicao, etDesc.text.toString())

            } else if (etDesc.text.isEmpty()){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("produtos").child(bd).child("desc").setValue("nao")
                arrayDesc.add(posicao, "nao")
            }

            if (!preco.equals(etPreco.text.toString())){
                var str:String = etPreco.text.toString().replace("R$", "")
                str = str.replace(",", "").trim()
                str = str.replace(".", "").trim()
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("produtos").child(bd).child("preco").setValue(str)
                arrayPreco.add(posicao, AreaLojistaController.currencyTranslation(str))
            }

        }

        val btnDeletarProduto: Button = findViewById(R.id.layGerenciaProd_btnApagarProduto)
        btnDeletarProduto.setOnClickListener {
            openPopUpApagaProduto("Atenção", "Você deseja remover este produto da loja? Todos os dados serão apagados.", true, "Sim, remover", "Cancelar", bd, linkImg, posicao)
        }

    }

    //este método e chamado de dentro de upload foto após o upload ter finalizado. Ele vem aqui para lidar com o clique final do botao de cadastrar para finalizar e salva no bd da loja.
    fun cadNovoProdFinal (link: String){

        //criar novo campo no bd
        val etNome: EditText = findViewById(R.id.cadNovoProd_nome)
        val etDesc: EditText = findViewById(R.id.cadNovoProd_descric)
        val etprec: EditText = findViewById(R.id.cadNovoProd_preco)
        val btnCadFinalizar: Button = findViewById(R.id.cadNovoProd_btnCadastrar)
        val btnFechar : Button = findViewById(R.id.cadNovoProd_Fechar)

        //ajusta o valor do preço para salvar no bd
        var str:String = etprec.text.toString().replace("R$", "")
        str = str.replace(",", "").trim()
        str = str.replace(".", "").trim()


        val newCad: DatabaseReference = databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("produtos").push()

        btnCadFinalizar.setOnClickListener {

            var str:String = etprec.text.toString().replace("R$", "")
            str = str.replace(",", "").trim()
            str = str.replace(".", "").trim()

            //petBD = newCad.key.toString()
            var nome = AreaLojistaController.PrimeiraLetraMaiuscula(etNome.text.toString())

            //newCad.child("nome").setValue(etNome.text.toString())
            newCad.child("nome").setValue(nome)
            if (etDesc.text.isEmpty()){
                newCad.child("desc").setValue("nao")
            } else {
                newCad.child("desc").setValue(etDesc.text.toString())
            }

            newCad.child("preco").setValue(str)
            newCad.child("img").setValue(link)
            newCad.child("controle").setValue("item") //ferramenta de controle. Vai ser por isto que iremos buscar para saber quantos produtos já tem.

            //botoes radio de categoria
            val rRacao: RadioButton = findViewById(R.id.cadProd_radioBtnRacoes)
            val rServ: RadioButton = findViewById(R.id.cadProd_radioBtnServicos)
            val rAcess: RadioButton = findViewById(R.id.cadProd_radioBtnAcessorios)
            val rEstet: RadioButton = findViewById(R.id.cadProd_radioBtnEstetica)
            val rRemed: RadioButton = findViewById(R.id.cadProd_radioBtnRemedios)

            if(rRacao.isChecked){
                newCad.child("tipo").setValue("racao") //ferramenta de controle. Vai ser por isto que iremos buscar para saber quantos produtos já tem.
            } else if (rServ.isChecked){
                newCad.child("tipo").setValue("servicos") //ferramenta de controle. Vai ser por isto que iremos buscar para saber quantos produtos já tem.
            } else if (rAcess.isChecked){
                newCad.child("tipo").setValue("acessorios") //ferramenta de controle. Vai ser por isto que iremos buscar para saber quantos produtos já tem.
            } else if (rEstet.isChecked){
                newCad.child("tipo").setValue("estetica") //ferramenta de controle. Vai ser por isto que iremos buscar para saber quantos produtos já tem.
            } else {
                newCad.child("tipo").setValue("remedios") //ferramenta de controle. Vai ser por isto que iremos buscar para saber quantos produtos já tem.
            }


            AreaLojistaModels.itens_venda = (AreaLojistaModels.itens_venda.toInt()+1).toString()
            databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("itens_venda").setValue(AreaLojistaModels.itens_venda)
            atualizaProdutos()

            etNome.setText("")
            etDesc.setText("")
            etprec.setText("")

            //fecha a janela
            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layCadProd : ConstraintLayout = findViewById(R.id.layCadProdutos)
            layIndex.visibility = View.VISIBLE
            layCadProd.visibility = View.GONE

            //manda um aviso pro usuário que foi finalizado com sucesso.
            //openPopUp("Sucesso!", "O produto foi cadastrado e já aparece para seus clientes.", false, "aa", "aa", "aa")
            Toast.makeText(this, "O produto foi cadastrado com sucesso!", Toast.LENGTH_SHORT).show()

            finish()
            //btnCadFinalizar.setOnClickListener (null)
        }


        btnFechar.setOnClickListener(null) //para retirar o clicklistener antigo


        //btn fechar e backspace cancela e apaga do bd.
        btnFechar.setOnClickListener {

            openPopUp2("As informações foram descartadas.", "Você não concluiu o processo de cadastro do novo item.", false, "ss", "ss", "ss")

            //AQUI ELE resolveu sair. Apagar tudo do bd.
            //apagando no bd
            newCad.removeValue()

            val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(urifinal) //urifinal is a String variable with the url
            storageReference.delete().addOnSuccessListener {
                //File deleted

            }.addOnFailureListener {
                //failed to delete

            }

            val layIndex :ConstraintLayout = findViewById(R.id.lay_arealojista_index)
            val layCadProd : ConstraintLayout = findViewById(R.id.layCadProdutos)
            layIndex.visibility = View.VISIBLE
            layCadProd.visibility = View.GONE

        }

        //exibe o preview de como vai ficar o anuncio
        ExibeEscondePreview (link, etNome.text.toString(), etDesc.text.toString(), str)
        EncerraDialog()

    }

    //exibe o preview no cadastro do produto
    fun ExibeEscondePreview(linkImg: String, nome: String, desc: String, preco: String ){

        val imgPrev : ImageView = findViewById(R.id.cadNovoProd_laySample_imgView)
        val tvNome : TextView = findViewById(R.id.cadNovoProd_laySample_tvNome)
        val tvDesc : TextView = findViewById(R.id.cadNovoProd_laySample_tvDesc)
        val tvPreco : TextView = findViewById(R.id.cadNovoProd_laySample_tvPreco)

        if (!linkImg.isEmpty()){
            Glide.with(this@arealojista).load(linkImg).apply(
                RequestOptions.circleCropTransform()).into(imgPrev)
        } else {
            imgPrev.visibility = View.GONE
        }

        tvNome.setText(nome)
        val precoEditado = AreaLojistaController.currencyTranslation(preco)
        tvPreco.setText(precoEditado)
        if (desc.isEmpty()){
            tvDesc.setText("")
        }

        if (tvNome.isVisible){
            imgPrev.visibility = View.GONE
            tvNome.visibility = View.GONE
            tvDesc.visibility = View.GONE
            tvPreco.visibility = View.GONE
        } else {
            imgPrev.visibility = View.VISIBLE
            tvNome.visibility = View.VISIBLE
            tvDesc.visibility = View.VISIBLE
            tvPreco.visibility = View.VISIBLE
        }

    }

    //se o usuario for empresário, pega aqui informações do petshop dele
    fun queryItensDaLojaParaRecycleView () {

        ChamaDialog()

        FirebaseDatabase.getInstance().reference.child("petshops").child(AreaLojistaModels.petBD).child("produtos").orderByChild("controle").equalTo("item")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //EncerraDialog()
                            //criar usuário
                            //createUser()
                        } else {

                            Log.d("teste", "entrou aqui na quary normal. petBD é "+AreaLojistaModels.petBD)

                            //carregar infos
                            var values: String
                            //abrir aqui a tela de gerenciamento do usuario

                            values = querySnapshot.child("nome").getValue().toString()
                            arrayNomes.add(values)
                            values = querySnapshot.child("desc").getValue().toString()
                            arrayDesc.add(values)
                            values = querySnapshot.child("preco").getValue().toString()

                            val precoProv = AreaLojistaController.currencyTranslation(values)

                            arrayPreco.add(precoProv)
                            values = querySnapshot.child("img").getValue().toString()
                            arrayImg.add(values)
                            values = querySnapshot.key.toString()
                            arrayBD.add(values)

                            atualizaProdutos()

                        }
                    }
                    SetUpRecycleViewForGerenciaDeTitulos()
                    EncerraDialog()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })


    }

    //se o usuario for empresário, pega aqui informações do petshop dele
    fun queryProdutosDoPet() {

        FirebaseDatabase.getInstance().reference.child("petshops").orderByChild("BDdoDono").equalTo(AreaLojistaModels.userBD)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //EncerraDialog()
                            //criar usuário
                            //createUser()
                        } else {

                            //carregar infos
                            var values: String
                            //abrir aqui a tela de gerenciamento do usuario
                            values = querySnapshot.child("itens_venda").getValue().toString()
                            AreaLojistaModels.itens_venda = values
                            values = querySnapshot.child("tamanho_inventario").getValue().toString()
                            AreaLojistaModels.inventario_size = values.toInt()

                            atualizaProdutos()

                        }
                    }

                    //EncerraDialog()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })


    }

    fun atualizaProdutos (){

        val tvItens : TextView = findViewById(R.id.cadNovoProd_tvItens)
        tvItens.setText("Itens: "+AreaLojistaModels.itens_venda+"/"+AreaLojistaModels.inventario_size)

    }

    //QueryPet para pegar as informações necessárias para editar layout
    fun queryPetLayoutEdit() {

        FirebaseDatabase.getInstance().reference.child("petshops").orderByChild("BDdoDono").equalTo(AreaLojistaModels.userBD)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //EncerraDialog()
                            //criar usuário
                            //createUser()
                        } else {

                            //carregar infos
                            var values: String

                            //abrir aqui a tela de gerenciamento do usuario
                            /*
                            values = querySnapshot.child("banner").getValue().toString()
                            if (values!= null){
                                if (values.equals("nao")){

                                } else {
                                    val imageView: ImageView = findViewById(R.id.lay_edit_layout_loja_bannerImgView)
                                    Glide.with(this@arealojista).load(values).into(imageView)
                                }

                            }

                             */
                            values = querySnapshot.child("logo").getValue().toString()
                            if (values!= null){
                                if (values.equals("nao")){

                                } else {
                                    val imageView: ImageView = findViewById(R.id.lay_edit_layout_loja_logoImageView)
                                    Glide.with(this@arealojista).load(values).apply(RequestOptions.circleCropTransform()).into(imageView)
                                    findViewById<TextView>(R.id.textView24).visibility = View.GONE
                                }

                            }

                            var cb: CheckBox = findViewById(R.id.servicosCB_BanhoTosa)

                            var servicos = ""
                            values = querySnapshot.child("servicos").child("banhoTosa").getValue().toString()
                            if (values=="nao" || values == null){
                                cb.isChecked=false
                            } else {
                                cb.isChecked=true
                            }
                            values = querySnapshot.child("servicos").child("farmacia").getValue().toString()
                            cb = findViewById(R.id.servicosCB_farmacia)
                            if (values=="nao" || values == null){
                                cb.isChecked=false
                            } else {
                                cb.isChecked=true
                            }
                            values = querySnapshot.child("servicos").child("veterinario").getValue().toString()
                            cb = findViewById(R.id.servicosCB_Veterinario)
                            if (values=="nao" || values == null){
                                cb.isChecked=false
                            } else {
                                cb.isChecked=true
                            }
                            values = querySnapshot.child("servicos").child("hospedagem").getValue().toString()
                            cb = findViewById(R.id.servicosCB_hospedagem)
                            if (values=="nao" || values == null){
                                cb.isChecked=false
                            } else {
                                cb.isChecked=true
                            }
                            values = querySnapshot.child("servicos").child("24hrs").getValue().toString()
                            cb = findViewById(R.id.servicosCB_24hrs)
                            if (values=="nao" || values == null){
                                cb.isChecked=false
                            } else {
                                cb.isChecked=true
                            }
                            values = querySnapshot.child("servicos").child("vetAtendDom").getValue().toString()
                            cb = findViewById(R.id.servicosCB_VetAtendDomicilio)
                            if (values=="nao" || values == null){
                                cb.isChecked=false
                            } else {
                                cb.isChecked=true
                            }
                            values = querySnapshot.child("servicos").child("entrega").getValue().toString()
                            cb = findViewById(R.id.servicosCB_entrega)
                            if (values=="nao" || values == null){
                                cb.isChecked=false
                            } else {
                                cb.isChecked=true
                            }

                            ListernersDosCheckBoxesServios()

                        }
                    }

                    //EncerraDialog()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })


    }

    //listeners dos checkboxes dos serviços
    fun ListernersDosCheckBoxesServios (){
        val cbBanhoTosa : CheckBox = findViewById(R.id.servicosCB_BanhoTosa)
        val cbFarmacia : CheckBox = findViewById(R.id.servicosCB_farmacia)
        val cbVet : CheckBox = findViewById(R.id.servicosCB_Veterinario)
        val cbHospedagem : CheckBox = findViewById(R.id.servicosCB_hospedagem)
        val cb24Hrs : CheckBox = findViewById(R.id.servicosCB_24hrs)
        val cbVetAtendDom : CheckBox = findViewById(R.id.servicosCB_VetAtendDomicilio)
        val cbEntrega : CheckBox = findViewById(R.id.servicosCB_entrega)

        val btnSalvarEsair: Button = findViewById(R.id.servicos_btnFechar)
        btnSalvarEsair.setOnClickListener {

            Toast.makeText(this, "Informações salvas", Toast.LENGTH_SHORT).show()
            if (cbBanhoTosa.isChecked){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("banhoTosa").setValue("sim")
            } else {
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("banhoTosa").setValue("nao")
            }

            if (cbFarmacia.isChecked){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("farmacia").setValue("sim")
            } else {
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("farmacia").setValue("nao")
            }

            if (cbVet.isChecked){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("veterinario").setValue("sim")
            } else {
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("veterinario").setValue("nao")
            }

            if (cbHospedagem.isChecked){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("hospedagem").setValue("sim")
            } else {
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("hospedagem").setValue("nao")
            }

            if (cb24Hrs.isChecked){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("24hrs").setValue("sim")
            } else {
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("24hrs").setValue("nao")
            }

            if (cbVetAtendDom.isChecked){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("vetAtendDom").setValue("sim")
            } else {
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("vetAtendDom").setValue("nao")
            }

            if (cbEntrega.isChecked){
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("entrega").setValue("sim")
            } else {
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("servicos").child("entrega").setValue("nao")
            }

        }
    }

    fun takePictureFromCamera() {
        val layPraFechar: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)
        val layPraFechar2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
        val layPraFechar3: ConstraintLayout = findViewById(R.id.lay_arealojista_index)

        layPraFechar.visibility = View.GONE
        layPraFechar2.visibility = View.GONE
        layPraFechar3.visibility = View.GONE

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

    //retorno da imagem
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //retorno da camera
        //primeiro if resultado da foto tirada pela camera
        if (requestCode == 100) {
            //if (resultCode == RESULT_OK) {
            //processo="alvara"

            val photo: Bitmap = data?.extras?.get("data") as Bitmap
            compressImage(photo)

            //}

        } else {
            //resultado da foto pega na galeria
            if (resultCode == RESULT_OK
                && data != null && data.getData() != null
            ) {

                filePath = data.getData()!!
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                compressImage(bitmap)


            }
        }
    }

    fun loadImage(glide: RequestManager, url: String?, view: ImageView?) {
        view?.let { glide.load(url).into(it) }
    }

    private fun compressImage(image: Bitmap) {

        ChamaDialog()
        //agora sabemos as dimensões da imagem.
        //neste exemplo queremos que caiba em um banner de 100x400
        //é alterando o tamanho aqui que o tamanho total da imagem cresce ao final**************************************
        var imageNova: Bitmap = image
        if (AreaLojistaModels.processo.equals("alvara")){
            imageNova = calculateInSizeSampleToFitImageView(image, 1000, 1000)
        }
        /* tirei o banner. Para voltar é só buscar aqui
        else if (processo.equals("banner")) {
            imageNova = calculateInSizeSampleToFitImageView(image, 1200, 300)
            val imageviewBanne:ImageView = findViewById(R.id.lay_edit_layout_loja_bannerImgView)
            imageviewBanne.setImageBitmap(imageNova)
        }*/
        else if (AreaLojistaModels.processo.equals("logo")){

            imageNova = calculateInSizeSampleToFitImageView(image, 500, 500)
            val imageviewLogo:ImageView = findViewById(R.id.lay_edit_layout_loja_logoImageView)
            imageviewLogo.setImageBitmap(imageNova)
            val txtlogo: TextView = findViewById(R.id.textView24)
            txtlogo.visibility = View.GONE

        } else if (AreaLojistaModels.processo.equals("cadNovoProd")){
            imageNova = calculateInSizeSampleToFitImageView(image, 200, 200)
        }

        //val imageProvisoria: Bitmap = calculateInSizeSampleToFitImageView(image, 800, 200)

        //image provisoria pode ser colocada se quiser no imageview pois já é pequena suficiente.

        val baos = ByteArrayOutputStream()
        var optionsCompress = 50  //taxa de compressao. 100 significa nenhuma compressao
        try {
            //Code here
            while (baos.toByteArray().size / 1024 > 50) {  //Loop if compressed picture is greater than 50kb, than to compression
                baos.reset() //Reset baos is empty baos
                image.compress(
                    Bitmap.CompressFormat.JPEG,
                    optionsCompress,
                    baos
                ) //The compression options%, storing the compressed data to the baos
                optionsCompress -= 25 //Every time reduced by 10
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        val tempUri: Uri = getImageUri(this, imageNova)
        filePath = tempUri
        if (AreaLojistaModels.processo.equals("alvara")){
            uploadImageToAlvara()
        } else {

            EncerraDialog()
            val imageProvisoria: Bitmap = calculateInSizeSampleToFitImageView(image, 250, 250)
            val ivProvisorio: ImageView = findViewById(R.id.layFotoPreview_ivPreview)
            ivProvisorio.setImageBitmap(imageProvisoria)

            val layPreviewFoto: ConstraintLayout = findViewById(R.id.layFotoPreview)
            layPreviewFoto.visibility = View.VISIBLE
            val btnRotate: Button = findViewById(R.id.layFotoPreview_btnRotate)
            btnRotate.setOnClickListener {

                val newProvisoria: Bitmap? = RotateBitmap(imageProvisoria, 90F)
                //ivProvisorio.setImageBitmap(newProvisoria)
                val bit: Bitmap = newProvisoria!!
                compressImage(bit)
            }

            val btnFinalizarEdicao: Button = findViewById(R.id.layFotoPreview_btnSalvarFoto)
            btnFinalizarEdicao.setOnClickListener {

                ChamaDialog()
                uploadImage()
                layPreviewFoto.visibility = View.GONE

            }

        }


        /*
        else if (processo.equals("logo")){

            //vamos travar logo aqui pra aplicar possibilidade de rodar a imagem

            val imageProvisoria: Bitmap = calculateInSizeSampleToFitImageView(image, 150, 150)
            val ivProvisorio: ImageView = findViewById(R.id.layFotoPreview_ivPreview)
            ivProvisorio.setImageBitmap(imageProvisoria)

            val layPreviewFoto: ConstraintLayout = findViewById(R.id.layFotoPreview)
            layPreviewFoto.visibility = View.VISIBLE
            val btnRotate: Button = findViewById(R.id.layFotoPreview_btnRotate)
            btnRotate.setOnClickListener {

                val newProvisoria: Bitmap? = RotateBitmap(imageProvisoria, 90F)
                //ivProvisorio.setImageBitmap(newProvisoria)
                val bit: Bitmap = newProvisoria!!
                compressImage(bit)
            }

            val btnFinalizarEdicao: Button = findViewById(R.id.layFotoPreview_btnSalvarFoto)
            btnFinalizarEdicao.setOnClickListener {

                ChamaDialog()
                uploadImage()
                layPreviewFoto.visibility = View.GONE

            }





        } else {
            uploadImage()
        }

         */

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
            adaptedHeight = imageHeight
        }

        if (imageWidth > imageViewWidth){

            adaptedWidh = imageWidth / 2
            while (adaptedWidh > imageViewWidth){
                adaptedWidh = adaptedWidh/2
            }
        } else {
            adaptedWidh = imageWidth
        }

        val newBitmap = Bitmap.createScaledBitmap(image, adaptedWidh, adaptedHeight, false)
        return newBitmap

    }


    // Method to save an bitmap to a file
    private fun bitmapToFile(bitmap:Bitmap): Uri {
        // Get the context wrapper
        val wrapper = ContextWrapper(applicationContext)

        // Initialize a new file instance to save bitmap object
        var file = wrapper.getDir("Images",Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")

        try{
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e:IOException){
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

    //metodo exclusivo pro alvará
    fun uploadImageToAlvara(){
        mFireBaseStorage = FirebaseStorage.getInstance()
        mphotoStorageReference = mFireBaseStorage.reference
        mphotoStorageReference = mFireBaseStorage.getReference().child(AreaLojistaModels.petBD).child("alvara").child("alvara")

        val bmp: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath)
        val baos: ByteArrayOutputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 55, baos)

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
                    Toast.makeText(this, "Ocorreu um erro", Toast.LENGTH_SHORT).show()
                }
            }
            return@Continuation mphotoStorageReference.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                urifinal = downloadUri.toString()

                EncerraDialog()

                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("alvara").setValue(urifinal)
                    AreaLojistaModels.alvaraOk=1
                    AreaLojistaModels.alvara="sim"
                    Toast.makeText(this, "O alvará foi enviado com sucesso.", Toast.LENGTH_SHORT).show()
                    loadImage(Glide.with(this), urifinal, findViewById(R.id.cadEmpIvPreview))
                    val layPraFechar2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                    layPraFechar2.visibility = View.VISIBLE

                    val mySharedPrefs: mySharedPrefs = mySharedPrefs(this)
                    mySharedPrefs.setValue("tipo", "empresario")
                    AreaLojistaModels.tipo = "empresario"


                    liberaBotoesDoEmpresario ()
                    checkAlvara ()


                    val btnFinalizaCad: Button = findViewById(R.id.cadEmpBtnFinalizaCad)
                    btnFinalizaCad.isEnabled = true
                    btnFinalizaCad.setOnClickListener {

                        val layCad2: ConstraintLayout =
                            findViewById(R.id.lay_cadastrarEmpreendimento2)
                        layCad2.visibility = View.GONE
                        val layIndexAreaLoja: ConstraintLayout =
                            findViewById(R.id.lay_arealojista_index)
                        layIndexAreaLoja.visibility = View.VISIBLE

                        openPopUp("Muito bem!", "Vamos cadastrar seu primeiro produto? Demora menos de 30 segundos.", false, "n", "n", "n")
                        val btnCadProd: Button = findViewById(R.id.arealojistaIndexBtnCadProd)
                        btnCadProd.performClick()

                    }

            } else {
                // Handle failures
                EncerraDialog()
                val layPraFechar2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                layPraFechar2.visibility = View.VISIBLE
                Toast.makeText(this, "um erro ocorreu.", Toast.LENGTH_SHORT).show()
                // ...
            }
        }.addOnFailureListener {
            // Uh-oh, an error occurred.
        }


    }

    //envio da foto
    //existe uma opção especial aqui para o caso de ser alvará
    fun uploadImage(){

        mFireBaseStorage = FirebaseStorage.getInstance()
        mphotoStorageReference = mFireBaseStorage.reference
        mphotoStorageReference = mFireBaseStorage.getReference().child(AreaLojistaModels.petBD).child("alvara").child("alvara")

        //ficaram aqui os códigos, mas na verdade agora o alvará é feito num processo proprio chamado uploadImageAlvara.
        Log.d("teste", "o valor de processo é "+AreaLojistaModels.processo)

        if (AreaLojistaModels.processo.equals("alvara")){

            mphotoStorageReference = mFireBaseStorage.getReference().child(AreaLojistaModels.petBD).child("alvara").child("alvara")
        } else if (AreaLojistaModels.processo.equals("banner")){
            mphotoStorageReference = mFireBaseStorage.getReference().child(AreaLojistaModels.petBD).child("banner").child("banner")
        } else if (AreaLojistaModels.processo.equals("logo")){
            Toast.makeText(this, "Ajustando imagem", Toast.LENGTH_SHORT).show()
            mphotoStorageReference = mFireBaseStorage.getReference().child(AreaLojistaModels.petBD).child("logo").child("logo")
        } else if (AreaLojistaModels.processo.equals("cadNovoProd")){
            val etNome: EditText = findViewById(R.id.cadNovoProd_nome)
            mphotoStorageReference = mFireBaseStorage.getReference().child(AreaLojistaModels.petBD).child("produtos").child("img_"+etNome.text.toString())
        }

        Log.d("teste", "mphotostorage é "+mphotoStorageReference.toString())
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
                    Toast.makeText(this, "Ocorreu um erro", Toast.LENGTH_SHORT).show()
                }
            }
            return@Continuation mphotoStorageReference.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                urifinal = downloadUri.toString()

                EncerraDialog()
                if (AreaLojistaModels.processo.equals("alvara")){
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("alvara").setValue(urifinal)
                    AreaLojistaModels.alvaraOk=1
                    AreaLojistaModels.alvara="sim"
                    Toast.makeText(this, "O alvará foi enviado com sucesso.", Toast.LENGTH_SHORT).show()
                    loadImage(Glide.with(this), urifinal, findViewById(R.id.cadEmpIvPreview))
                    val layPraFechar2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                    layPraFechar2.visibility = View.VISIBLE


                    val btnFinalizaCad: Button = findViewById(R.id.cadEmpBtnFinalizaCad)
                    btnFinalizaCad.setOnClickListener {

                        if (!AreaLojistaModels.alvara.equals("nao")) {
                            val layCad2: ConstraintLayout =
                                findViewById(R.id.lay_cadastrarEmpreendimento2)
                            layCad2.visibility = View.GONE
                            val layIndexAreaLoja: ConstraintLayout =
                                findViewById(R.id.lay_arealojista_index)
                            layIndexAreaLoja.visibility = View.VISIBLE

                            val intent = Intent(this, MapsActivity::class.java)
                            //intent.putExtra("userBD", userBD)
                            intent.putExtra("email", AreaLojistaModels.userMail)
                            //intent.putExtra("alvara", alvara)
                            //intent.putExtra("tipo", tipo)
                            if (!AreaLojistaModels.petBD.equals("nao")) {
                                intent.putExtra("petBD", AreaLojistaModels.petBD)
                            }
                            intent.putExtra("endereco", AreaLojistaModels.endereco)
                            intent.putExtra("chamaLatLong", "sim")
                            startActivity(intent)
                            finish()

                            /* não faz mais isso. Agora encerra e manda direto pra mapsActivity para pegar latLong
                            //habilita os botões
                            var btn: Button
                            btn = findViewById(R.id.arealojistaIndexBtneditLayout)
                            btn.isEnabled = true

                            btn = findViewById(R.id.arealojistaIndexBtnCadProd)
                            btn.isEnabled = true

                            btn = findViewById(R.id.arealojistaIndexBtnGerenciaProd)
                            btn.isEnabled = true

                            btn = findViewById(R.id.arealojistaIndexBtnEntrega)
                            btn.isEnabled = true

                            btn = findViewById(R.id.arealojistaIndexBtnFormaPagamento)
                            btn.isEnabled = true

                            btn = findViewById(R.id.arealojistaIndexBtnCadastrarEmpreendimento)
                            btn.setText("Atualizar informações")

                             */
                        }
                    }

                }
                if (AreaLojistaModels.processo.equals("banner")){

                    //val imageView: ImageView = findViewById(R.id.lay_edit_layout_loja_bannerImgView)
                    //Glide.with(this@arealojista).load(urifinal).into(imageView)  //agora a imagem é colocada diretamente no método de compressimage
                    //imageView.visibility = View.VISIBLE
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("banner").setValue(urifinal)
                }
                if (AreaLojistaModels.processo.equals("logo")){

                    val imageView: ImageView = findViewById(R.id.lay_edit_layout_loja_logoImageView)
                    Glide.with(this@arealojista).load(urifinal).apply(RequestOptions.circleCropTransform()).into(imageView)
                    imageView.visibility = View.VISIBLE
                    databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("logo").setValue(urifinal)
                }
                if (AreaLojistaModels.processo.equals("cadNovoProd")){
                    cadNovoProdFinal(urifinal)
                }


            } else {
                // Handle failures
                EncerraDialog()
                val layPraFechar2: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento2)
                layPraFechar2.visibility = View.VISIBLE
                Toast.makeText(this, "um erro ocorreu.", Toast.LENGTH_SHORT).show()
                // ...
            }
        }.addOnFailureListener {
            // Uh-oh, an error occurred.
        }


    }

    fun openPopUp (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, call: String) {
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
                popupWindow.dismiss()
            }

            buttonPopupS.setOnClickListener {
                if (call.equals("vaiPraPlano")){
                    findViewById<Button>(R.id.btnPlanos).performClick()
                    popupWindow.dismiss()
                }
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
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.layPaizao)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)

        /*
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

         */


    }

    //Método que exibe uma pergunta, se quer tirar foto ou enviar do celular
    fun openPopUp2 (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, call: String) {
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

            /*
            // Set a click listener for popup's button widget
            buttonPopupN.setOnClickListener{
                // Dismiss the popup window
                popupWindow.dismiss()
            }

             */

            buttonPopupN.setOnClickListener {
                takePictureFromGallery()
                popupWindow.dismiss()
                //ChamaDialog()  se chamar aqui e o usuario cancelar, fica travada a tela pra sempre
            }

            buttonPopupS.setOnClickListener {
                takePictureFromCamera()
                popupWindow.dismiss()
                ChamaDialog() //ChamaDialog()  se chamar aqui e o usuario cancelar, fica travada a tela pra sempre
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
        val lay_root: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )


    }

    //confirma apagar o produto
    fun openPopUpApagaProduto (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, bd: String, linkFirebase: String, posicao: Int) {
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
                popupWindow.dismiss()
            }

            buttonPopupS.setOnClickListener {

                ChamaDialog()
                val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(linkFirebase)
                storageReference.delete().addOnSuccessListener {
                    //File deleted
                }.addOnFailureListener {
                    //failed to delete
                }

                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("produtos").child(bd).removeValue()
                Toast.makeText(this, "O item foi apagado", Toast.LENGTH_SHORT).show()

                popupWindow.dismiss()
                val layEditProd :ConstraintLayout = findViewById(R.id.layGerenciaProd_layEdit)
                layEditProd.visibility = View.GONE

                val recycleview : RecyclerView = findViewById(R.id.recyclerView_gerenciaProds)
                val adapter = recycleview.adapter
                val layoutmanager = recycleview.layoutManager

                AreaLojistaModels.itens_venda = ((AreaLojistaModels.itens_venda).toInt()-1).toString()
                databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("itens_venda").setValue(AreaLojistaModels.itens_venda)

                arrayBD.removeAt(posicao)
                arrayImg.removeAt(posicao)
                arrayPreco.removeAt(posicao)
                arrayDesc.removeAt(posicao)
                arrayNomes.removeAt(posicao)

                layoutmanager?.removeAllViews()
                adapter?.notifyItemRemoved(posicao)
                adapter?.notifyDataSetChanged()
                EncerraDialog()
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
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.lay_cadastrarEmpreendimento)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }

    //remove itens do array
    fun removeitensSafetely(array: MutableList<String> = ArrayList()): MutableList<String>{

        var arrayProv: MutableList<String> = ArrayList()
        var i =0
        //vamos primeiro passar todo dado pro array provisório
        while (i<array.size){
            arrayProv.add(array.get(i))
            i++
        }

        //agora todos os dados já foram duplicados. Vamos apagar o bd antigo
        array.clear()

        //return@ArrayList
        return arrayProv
    }

    fun queryimpulsionamentosAtivos() {


        FirebaseDatabase.getInstance().reference.child("impulsionamentos").orderByChild("pet").equalTo(AreaLojistaModels.petBD)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.exists()) {


                        for (querySnapshot in dataSnapshot.children) {

                            var txt: TextView = findViewById(R.id.impulso_tvNome)
                            val imgIV: ImageView = findViewById(R.id.impulso_imageView)
                            var values: String

                            values = querySnapshot.child("nome_prod").value.toString()  //pos0
                            //val txt: TextView = findViewById(R.id.impulsionado_nome)
                            txt.setText(values)

                            values = querySnapshot.child("img_prod").value.toString() //pos1
                            Glide.with(this@arealojista).load(values).centerCrop()
                                .into(imgIV)

                            values =querySnapshot.child("preco").value.toString() //pos2
                            txt = findViewById(R.id.impulso_tvPreco)
                            txt.setText(values)

                            values = querySnapshot.child("data_inicio").value.toString()  //pos0
                            txt = findViewById(R.id.impulso_tvDataInicio)
                            txt.setText("início: "+values)

                            values = querySnapshot.child("data_final").value.toString()  //pos0
                            txt = findViewById(R.id.impulso_tvDataFinal)
                            val datafinal = values
                            values = querySnapshot.child("hora_inicio").value.toString()  //pos0
                            txt.setText("Termina em: "+datafinal+" às "+values)

                            values = querySnapshot.key.toString()  //pos0

                            val btnCancelar: Button = findViewById(R.id.impulso_btnCancelar)
                            btnCancelar.setOnClickListener {

                                val builder: AlertDialog.Builder = AlertDialog.Builder(this@arealojista)
                                builder.setMessage("Tem certeza que deseja cancelar este impulsionamento? As compras que já foram feitas não serão canceladas.")
                                    .setTitle("Atenção")
                                    .setCancelable(false)
                                    .setPositiveButton("Sim, cancelar", DialogInterface.OnClickListener { dialog, which ->

                                        //Aqui cancela o impulsionamento
                                        databaseReference.child("impulsionamentos").child(values).removeValue()

                                        //aqui libera o petshop para novos
                                        databaseReference.child("petshops").child(AreaLojistaModels.petBD).child("impulsionamentos").child("nao")

                                        openPopUp("Pronto", "Este impulsionamento foi cancelado e você já pode fazer novos", false, "n", "n", "n")

                                        val layInfos: ConstraintLayout = findViewById(R.id.layImpulso_exibe)
                                        layInfos.visibility = View.GONE
                                        val laySemNada: ConstraintLayout = findViewById(R.id.layImpulso_addNova)
                                        laySemNada.visibility = View.VISIBLE

                                    })
                                // Display a negative button on alert dialog
                                builder.setNegativeButton("Não"){dialog,which ->
                                    dialog.dismiss()
                                }
                                val alert : AlertDialog = builder.create()
                                alert.show()

                            }

                        }
                    } else {
                        val tvImpulsos : TextView = findViewById(R.id.tvImpulsos)
                        tvImpulsos.setText("Não existem impulsionamentos ativos para você")
                        val layExibeInfor: ConstraintLayout = findViewById(R.id.layImpulso_exibe)
                        layExibeInfor.visibility = View.GONE
                        val laySemNada: ConstraintLayout = findViewById(R.id.layImpulso_addNova)
                        laySemNada.visibility = View.VISIBLE
                    }


                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })


    }


    //Máscara de edição de texto
    /*
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


     */

    private fun setupPermissions() {

        cameraPermissions.checkPermission(this, CAMERA_PERMISSION_CODE)
        readFilesPermissions.checkPermission(this, READ_PERMISSION_CODE)
        writeFilesPermissions.checkPermission(this, WRITE_PERMISSION_CODE)

        /*
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
         */
    }

    //obsoleto pois foi agora pra casses.
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        if (requestCode==CAMERA_PERMISSION_CODE){
            cameraPermissions.handlePermissionsResult(requestCode, permissions, grantResults, CAMERA_PERMISSION_CODE)
        }
        if (requestCode==READ_PERMISSION_CODE){
            cameraPermissions.handlePermissionsResult(requestCode, permissions, grantResults, READ_PERMISSION_CODE)
        }
        if (requestCode==WRITE_PERMISSION_CODE){
            cameraPermissions.handlePermissionsResult(requestCode, permissions, grantResults, WRITE_PERMISSION_CODE)
        }

        /*
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
         */
    }



    fun ChamaDialog() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        ) //este serve para bloquear cliques que pdoeriam dar erros
        val layout = findViewById(R.id.LayoutProgressBar) as RelativeLayout
        layout.visibility = View.VISIBLE
        val spinner = findViewById(R.id.progressBar1) as ProgressBar
        spinner.visibility = View.VISIBLE
    }

    //este método torna invisivel um layout e encerra o dialogbar spinner.
    fun EncerraDialog() {
        val layout = findViewById(R.id.LayoutProgressBar) as RelativeLayout
        val spinner = findViewById(R.id.progressBar1) as ProgressBar
        layout.visibility = View.GONE
        spinner.visibility = View.GONE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) //libera os clicks
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

    internal inner class MyFailureListener : OnFailureListener {
        override fun onFailure(exception: Exception) {
            val errorCode = (exception as StorageException).errorCode
            val errorMessage = exception.message
            Log.d("storage", "Error Code: "+errorCode)
            Log.d("storage", "Error Message: "+errorMessage)
            // test the errorCode and errorMessage, and handle accordingly
        }
    }

    /*
    CAMINHO DO UPLOAD DO ALVARA

    OnCreate----ClicksIniciais----SetupPermissions

                            =-----clickCadastroEmpresa----BtnProximo
                                                    ------btnSendAlvara ----processo=alvara--takePictureCamera-------StartActivityForResult-------OnActivityForResult------compressImage-----upLoadImage (aquui salva)
                                                    ------BtnFinaliza                           code=100

     */

}
