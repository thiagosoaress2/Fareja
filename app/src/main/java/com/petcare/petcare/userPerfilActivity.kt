package com.petcare.petcare

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class userPerfilActivity : AppCompatActivity() {

    var imgDoUser: String = "nao"
    var userBd = "nao"
    var userMail = "nao"
    var tipo = "nao"
    var latLong = "nao"

    var temFotoNoStories = "nao"

    //envio de imagem
    private lateinit var filePath: Uri
    private var urifinal: String = "nao"
    private lateinit var mphotoStorageReference: StorageReference
    private lateinit var mFireBaseStorage: FirebaseStorage
    private lateinit var databaseReference: DatabaseReference

    private val imgPerfilWidth = 250
    private val imgPerfilHeight = 250


    var mImagemProvisoria: Bitmap? = null
    var StoriesTextColor = "#000000"
    var StoriesBgColor = "#FFFFFF"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_perfil)

        val ivPerfil : ImageView = findViewById(R.id.layPerfil_ivPerfil)
        ivPerfil.setLayoutParams(ConstraintLayout.LayoutParams(imgPerfilWidth, imgPerfilHeight))

        databaseReference = FirebaseDatabase.getInstance().reference

        imgDoUser = intent.getStringExtra("img")
        userBd = intent.getStringExtra("userBD")
        userMail = intent.getStringExtra("userMail")
        tipo = intent.getStringExtra("tipo")
        Log.d("teste", tipo)
        latLong = intent.getStringExtra("latlong")

        Log.d("teste", "imagem"+imgDoUser)

        if (!tipo.equals("visitante")){
            setupPermissions()  //so precisa da permissão se for sua página para você editar a foto. Do visitante nao precisa.
        }


        acoesIniciais()
    }

    fun acoesIniciais(){

        val ivPerfil : ImageView = findViewById(R.id.layPerfil_ivPerfil)

        if (tipo.equals("meuPerfil")){

            //aqui abrir métodos para o caso ser o perfil da pessoa e poder alterar as coisas
            val btnAlteraPerfil: Button = findViewById(R.id.layPerfil_btnMudarFotoPerfil)
            btnAlteraPerfil.visibility = View.VISIBLE
            btnAlteraPerfil.setOnClickListener {
                //codigo para alterar a imagem
                if (CheckPermissions()){ //se for falso lá de dentro ele já chama a requisição
                    //codigo para alterar imagem
                    btnAbrePopUpEnvioFoto()

                }
            }

        } else {
            //é um visitante
            //sem clicklistener
        }
        if (imgDoUser.equals("nao")){
            //deixa a imagem padrão
        } else {
            //coloca a imagem no perfil
            Glide.with(this)
                .load(imgDoUser)
                .apply(RequestOptions().override(imgPerfilWidth, imgPerfilHeight)) //ajusta o tamanho
                .apply(RequestOptions.circleCropTransform()) //coloca em formato circulo
                .into(ivPerfil)
        }

        val btnAddRedesSociais: Button = findViewById(R.id.btnAddRedesSociais)
        btnAddRedesSociais.setOnClickListener {

        }

        queryUserInfoDoPerfil()

        queryStories()
    }

    override fun onStart() {
        super.onStart()

        databaseReference = FirebaseDatabase.getInstance().reference
    }

    override fun onDestroy() {
        super.onDestroy()

        val intent = Intent(this, MapsActivity::class.java)

        intent.putExtra("email", userMail)
        startActivity(intent)
    }






    //pegar dados do user
    //busca informações iniciais do usuario
    fun queryUserInfoDoPerfil() {

        ChamaDialog()

        val rootRef = databaseReference.child("userPerfil").child(userBd)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()){

                    //se existe, vamos atualizar as infos na tela
                    var values: String
                    //values = p0.child("img").getValue().toString()   nao preciso atualizar a imagem pois já vem da mapsActivity esta info

                    //ler infos de redes sociais
                    values = p0.child("redesSociaisFace").getValue().toString()
                    if (!values.equals("nao")){
                        val faceTxt: TextView = findViewById(R.id.layRedesSociais_txFace)
                        faceTxt.setText(values)
                        faceTxt.visibility = View.VISIBLE
                        val et: EditText = findViewById(R.id.layAddredes_etFace)
                        et.setText(values)

                    }
                    values = p0.child("redesSociaisInsta").getValue().toString()
                    if (!values.equals("nao")){
                        val txt: TextView = findViewById(R.id.layRedesSociais_txInstagram)
                        txt.setText(values)
                        txt.visibility = View.VISIBLE
                        val et: EditText = findViewById(R.id.layAddredes_etInsta)
                        et.setText(values)
                    }
                    values = p0.child("redesSociaisTwit").getValue().toString()
                    if (!values.equals("nao")){
                        val txt: TextView = findViewById(R.id.layRedesSociais_txTwitter)
                        txt.setText(values)
                        txt.visibility = View.VISIBLE
                        val et: EditText = findViewById(R.id.layAddredes_etTwit)
                        et.setText(values)
                    }
                    //fim das redes sociais




                } else {

                    if (tipo.equals("meuPerfil")){
                        //criar o perfil dele no bd
                        databaseReference.child("userPerfil").child(userBd).child("img").setValue(imgDoUser)
                        databaseReference.child("userPerfil").child(userBd).child("redesSociaisFace").setValue("nao")
                        databaseReference.child("userPerfil").child(userBd).child("redesSociaisInsta").setValue("nao")
                        databaseReference.child("userPerfil").child(userBd).child("redesSociaisTwit").setValue("nao")

                    } else {
                        //exibir a tela pura, sem nada editado. Este user nunca entrou aqui.
                    }
                }

                if (tipo.equals("meuPerfil")){

                    val btnAddredes: Button = findViewById(R.id.btnAddRedesSociais)
                    btnAddredes.visibility = View.VISIBLE
                    btnAddredes.setOnClickListener {

                        val layAddRedes: ConstraintLayout = findViewById(R.id.layAddRedes)
                        if (layAddRedes.isVisible==false){
                            layAddRedes.visibility = View.VISIBLE
                            //colocar listener no btn salvar e fechar
                            val btnFechar: Button = findViewById(R.id.layAddRedes_btnVoltar)
                            val btnSalvar: Button = findViewById(R.id.layAddRedes_btnSalvar)
                            btnFechar.setOnClickListener {
                                layAddRedes.visibility = View.GONE
                                btnFechar.setOnClickListener { null }
                                btnSalvar.setOnClickListener { null }
                            }

                            btnSalvar.setOnClickListener {

                                //salva as mudanças e fecha
                                val etface: EditText = findViewById(R.id.layAddredes_etFace)
                                val etInsta: EditText = findViewById(R.id.layAddredes_etInsta)
                                val etTwit: EditText = findViewById(R.id.layAddredes_etTwit)
                                databaseReference.child("userPerfil").child(userBd).child("redesSociaisFace").setValue(etface.text.toString())
                                databaseReference.child("userPerfil").child(userBd).child("redesSociaisInsta").setValue(etInsta.text.toString())
                                databaseReference.child("userPerfil").child(userBd).child("redesSociaisTwit").setValue(etTwit.text.toString())

                                val tvFace : TextView = findViewById(R.id.layRedesSociais_txFace)
                                tvFace.setText(etface.text.toString())
                                tvFace.visibility = View.VISIBLE

                                val tvInsta : TextView = findViewById(R.id.layRedesSociais_txInstagram)
                                tvInsta.setText(etInsta.text.toString())
                                tvInsta.visibility = View.VISIBLE

                                val tvTwit : TextView = findViewById(R.id.layRedesSociais_txTwitter)
                                tvTwit.setText(etTwit.text.toString())
                                tvTwit.visibility = View.VISIBLE

                                layAddRedes.visibility = View.GONE
                                btnFechar.setOnClickListener { null }
                                btnSalvar.setOnClickListener { null }
                                makeToast("Informações salvas!")

                            }

                        } else {
                            layAddRedes.visibility = View.GONE
                            val btnFechar: Button = findViewById(R.id.layAddRedes_btnVoltar)
                            val btnSalvar: Button = findViewById(R.id.layAddRedes_btnSalvar)
                            btnFechar.setOnClickListener {
                                layAddRedes.visibility = View.GONE
                                btnFechar.setOnClickListener { null }
                                btnSalvar.setOnClickListener { null }
                            }
                        }
                    }

                }

                EncerraDialog()
            }
        })


    }









    //MEtodos dos stories
    //Coloca o listener do botao de adicionar stories
    fun queryStories() {

        ChamaDialog()
        val txMensagem : TextView = findViewById(R.id.layStories_txSemStories)
        val btnAddStories: Button = findViewById(R.id.layStories_btnAddStorie)
        val imageViewStories: ImageView = findViewById(R.id.layStories_iv)

        val rootRef = databaseReference.child("stories").child(userBd)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()){


                    txMensagem.visibility = View.GONE
                    btnAddStories.visibility = View.GONE //btn add só aparece se não tiver
                    imageViewStories.visibility = View.VISIBLE

                    //se existe, vamos atualizar as infos na tela
                    var values: String
                    //values = p0.child("img").getValue().toString()   nao preciso atualizar a imagem pois já vem da mapsActivity esta info

                    //ler infos de redes sociais
                    if (p0.child("img").exists()) {
                        values = p0.child("img").getValue().toString()
                        Glide.with(this@userPerfilActivity).load(values).into(imageViewStories)
                    }

                    if (p0.child("message").exists()){
                        values = p0.child("message").getValue().toString()
                        val txtNaMensagem : TextView = findViewById(R.id.layStories_message)  //este txt é o que aparece dentro da imagem
                        txtNaMensagem.setText(values)

                        values = p0.child("textColor").getValue().toString()
                        txtNaMensagem.setTextColor(Color.parseColor(values))

                        values = p0.child("bgColor").getValue().toString()
                        if (values.equals("#FFFFFF")){
                            imageViewStories.setColorFilter(ContextCompat.getColor(this@userPerfilActivity,  R.color.branco), android.graphics.PorterDuff.Mode.MULTIPLY)
                        } else if (values.equals("#000000")){
                            imageViewStories.setColorFilter(ContextCompat.getColor(this@userPerfilActivity,  R.color.preto), android.graphics.PorterDuff.Mode.MULTIPLY)
                        } else if (values.equals("#2196F3")){
                            imageViewStories.setColorFilter(ContextCompat.getColor(this@userPerfilActivity,  R.color.azul), android.graphics.PorterDuff.Mode.MULTIPLY)
                        } else if (values.equals("#FFEB3B")){
                            imageViewStories.setColorFilter(ContextCompat.getColor(this@userPerfilActivity,  R.color.amarelo), android.graphics.PorterDuff.Mode.MULTIPLY)
                        } else {
                            imageViewStories.setColorFilter(ContextCompat.getColor(this@userPerfilActivity,  R.color.vermelho), android.graphics.PorterDuff.Mode.MULTIPLY)
                        }


                        //
                        values = p0.child("messagePosition").getValue().toString()
                        if (values.equals("top")){

                            val params = txtNaMensagem.layoutParams as ConstraintLayout.LayoutParams
                            params.topToTop = imageViewStories.id
                            txtNaMensagem.requestLayout()

                        } else if (values.equals("middle")){


                            val params = txtNaMensagem.layoutParams as ConstraintLayout.LayoutParams
                            params.topToTop = imageViewStories.id
                            params.bottomToBottom = imageViewStories.id
                            txtNaMensagem.requestLayout()

                        } else {
                            //bottom
                            val params = txtNaMensagem.layoutParams as ConstraintLayout.LayoutParams
                            params.bottomToBottom = imageViewStories.id
                            txtNaMensagem.requestLayout()

                        }

                        val horaAgora = GetHour()
                        val dataAgora = GetDate()
                        val dataLimite = p0.child("dataLimite").getValue().toString()
                        values = p0.child("horaLimite").getValue().toString()

                        val tempoRestante = getDifferenceInTwoDates(dataAgora, dataLimite, horaAgora, values)
                        val tvHoraRestante: TextView = findViewById(R.id.layStories_txtTempoRestante)
                        tvHoraRestante.setText("Tempo restante: "+tempoRestante)

                    }

                    //calcular a hora+12 horas para saber se deve ser retirado
                    //se for menor, buscar a imagem

                    //verificar se a hora ja expirou.Se expirou, tirar o stories antigo e colocar aqui agora o novo listener.



                } else {

                    if (tipo.equals("meuPerfil")){

                        btnAddStories.visibility = View.VISIBLE
                        txMensagem.setText("Vamos mostrar uma história?")

                        /*
                        //está sem stories e vai colocar o listener do botão
                        btnAddStories.visibility = View.VISIBLE
                        btnAddStories.setOnClickListener {
                            MetodosAddStories()
                        }

                         */

                    } else {
                        ///este user é um visitante mas não tem stories pra mostrar
                        txMensagem.setText("Sem história do dia para exibir")
                    }
                }


                EncerraDialog()
            }
        })


    }

    fun MetodosAddStories(){

        //exibe o layout
        val layAddStories: ConstraintLayout = findViewById(R.id.layCriandoStory)
        layAddStories.visibility = View.VISIBLE

        //coloca os listeners dos primeiros botões
        val btnAddStoriesPorFoto: Button = findViewById(R.id.layCriandoStory_btnFoto)
        val btnAddStoriesPorGaleria: Button = findViewById(R.id.layCriandoStory_btnGaleria)
        //val btnApagarImagem: Button = findViewById(R.id.layCriandoStories_btnApagaImg)

        //inicia o processo de envio de foto aqui
        btnAddStoriesPorFoto.setOnClickListener {
            takePictureFromCameraToStories()
        }
        btnAddStoriesPorGaleria.setOnClickListener {
            takePictureFromGalleryToStories()
        }

        /*
        //apagar a foto que o user escolheu
        btnApagarImagem.setOnClickListener {

            val imageView = findViewById<ImageView>(R.id.layCriandoStory_iv)
            imageView.setImageBitmap(null)
            //imageView.setImageResource(null)
            imageView.setImageDrawable(null)
            Glide.with(imageView.context)
                .clear(imageView);

            btnApagarImagem.visibility = View.GONE

            val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(temFotoNoStories)
            storageReference.delete().addOnSuccessListener {
                //File deleted
            }.addOnFailureListener {
                //failed to delete
            }
        }

         */

        val btnFechar: Button = findViewById(R.id.layCriandoStory_btnFechar)
        val btnAddTxt: Button = findViewById(R.id.layCriandoStory_btnAddText)


        //listener do botao publicas
        //Ao abrir a intent para pegar foto, este método estava ficando desabilitado. Vou copia-lo em outro lugar
        val btnPublicar: Button = findViewById(R.id.layCriandoStory_BtnPublicar)


        //editText que adiona texto ao stories
        val etMensagemToStories: EditText = findViewById(R.id.CriandoStories_etMensagem) //editText para o user colocar uma mensagem
        val txtMensagemNaCriacao: TextView = findViewById(R.id.layCriandoStory_txtMensagem) //este é o txt da edição, ainda não é o final


        val btnTop: Button = findViewById(R.id.layCriandoStories_btnPosicaoTxtTop)
        val btnMid: Button = findViewById(R.id.layCriandoStories_btnPosicaoTxtMiddle)
        val btnBot: Button = findViewById(R.id.layCriandoStories_btnPosicaoTxtBottom)


        var temTxt: Boolean = false
        var messageTxt: String = "nao"
        etMensagemToStories.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()){
                    txtMensagemNaCriacao.visibility = View.GONE
                    txtMensagemNaCriacao.setText("")

                    btnTop.visibility = View.GONE
                    btnMid.visibility = View.GONE
                    btnBot.visibility = View.GONE
                    temTxt = false

                } else {
                    txtMensagemNaCriacao.visibility = View.VISIBLE
                    txtMensagemNaCriacao.setText(s)

                    btnTop.visibility = View.VISIBLE
                    btnMid.visibility = View.VISIBLE
                    btnBot.visibility = View.VISIBLE
                    temTxt = true
                    messageTxt = s.toString()
                    Log.d("text", "messageTxt é "+messageTxt)
                }
            }

        })

        var posicaoTxt = "bottom"
        //ajusta a cor dos botões e também a posição do texto
        btnTop.setOnClickListener {
            btnTop.setBackgroundResource(R.drawable.ic_keyboard_arrow_right_orange)
            btnMid.setBackgroundResource(R.drawable.ic_keyboard_arrow_right_darkblue)
            btnBot.setBackgroundResource(R.drawable.ic_keyboard_arrow_right_darkblue)

            val params = txtMensagemNaCriacao.layoutParams as ConstraintLayout.LayoutParams
            params.bottomToBottom = btnTop.id
            txtMensagemNaCriacao.requestLayout()

            posicaoTxt = "top"
        }

        btnMid.setOnClickListener {
            btnTop.setBackgroundResource(R.drawable.ic_keyboard_arrow_right_darkblue)
            btnMid.setBackgroundResource(R.drawable.ic_keyboard_arrow_right_orange)
            btnBot.setBackgroundResource(R.drawable.ic_keyboard_arrow_right_darkblue)

            val params = txtMensagemNaCriacao.layoutParams as ConstraintLayout.LayoutParams
            params.bottomToBottom = btnMid.id
            txtMensagemNaCriacao.requestLayout()

            posicaoTxt = "middle"
        }

        btnBot.setOnClickListener {
            btnTop.setBackgroundResource(R.drawable.ic_keyboard_arrow_right_darkblue)
            btnMid.setBackgroundResource(R.drawable.ic_keyboard_arrow_right_darkblue)
            btnBot.setBackgroundResource(R.drawable.ic_keyboard_arrow_right_orange)

            val params = txtMensagemNaCriacao.layoutParams as ConstraintLayout.LayoutParams
            params.bottomToBottom = btnBot.id
            txtMensagemNaCriacao.requestLayout()

            posicaoTxt = "bottom"
        }



        btnAddTxt.setOnClickListener {
            val layAddTxtToStories: ConstraintLayout = findViewById(R.id.layCriandoStoryAddText)

            if (layAddTxtToStories.isVisible==false){
                layAddTxtToStories.visibility = View.VISIBLE
                etMensagemToStories.setText("")
            } else {
                layAddTxtToStories.visibility = View.GONE
                hideKeyboard()
            }
        }

        //finalmente publicar
        btnPublicar.setOnClickListener {

                if (mImagemProvisoria==null){  //se nao tem foto criar sem foto
                    createStories(messageTxt, temTxt, posicaoTxt)
                } else {
                    //se tem foto, criar aqui
                    //dentro de uploaImage vai também atualizar a tela inicial do usuario
                    uploadImageToStories(posicaoTxt, temTxt, messageTxt)
                    Log.d("teste", "Chamou uploadImagetoStories")
                    btnFechar.performClick()
                }

        }


        var txtColor: Button = findViewById(R.id.textColorbtnBranco)
        txtColor.setOnClickListener {
            clicksColorsText(0)
        }

        txtColor = findViewById(R.id.textColorbtnPreto)
        txtColor.setOnClickListener {
            clicksColorsText(1)
        }

        txtColor = findViewById(R.id.textColorbtnAzul)
        txtColor.setOnClickListener {
            clicksColorsText(2)
        }
        txtColor = findViewById(R.id.textColorbtnAmarelo)
        txtColor.setOnClickListener {
            clicksColorsText(3)
        }
        txtColor = findViewById(R.id.textColorbtnVermelho)
        txtColor.setOnClickListener {
            clicksColorsText(4)
        }


        var bgColor: Button = findViewById(R.id.bgColorbtnBranco)
        bgColor.setOnClickListener {
            clicksColorsBgIv(0)
        }

        bgColor = findViewById(R.id.bgColorbtnPreto)
        bgColor.setOnClickListener {
            clicksColorsBgIv(1)
        }

        bgColor = findViewById(R.id.bgColorbtnAzul)
        bgColor.setOnClickListener {
            clicksColorsBgIv(2)
        }
        bgColor = findViewById(R.id.bgColorbtnAmarelo)
        bgColor.setOnClickListener {
            clicksColorsBgIv(3)
        }
        bgColor = findViewById(R.id.bgColorbtnVermelho)
        bgColor.setOnClickListener {
            clicksColorsBgIv(4)
        }


        btnFechar.setOnClickListener {
            layAddStories.visibility = View.GONE
            btnPublicar.setOnClickListener { null }
            btnAddStoriesPorFoto.setOnClickListener { null }
            btnAddStoriesPorGaleria.setOnClickListener { null }
            btnFechar.setOnClickListener { null }
            btnAddTxt.setOnClickListener { null }
            btnTop.setOnClickListener { null }
            btnMid.setOnClickListener { null}
            btnBot.setOnClickListener { null }
            txtColor.setOnClickListener { null }
            bgColor.setOnClickListener { null }
        }

    }

    //coloca a cor escolhida pelo user no texto do storie
    fun clicksColorsText(number: Int ){
        // - 0 branco
        // - 1 preto
        // - 2 azul
        // - 3 amarelo
        // - 4 vermelho
        val mensagem : TextView = findViewById(R.id.layCriandoStory_txtMensagem)
        when (number){
            0 -> mensagem.setTextColor(Color.parseColor("#FFFFFF"))
            1 -> mensagem.setTextColor(Color.parseColor("#000000"))
            2 -> mensagem.setTextColor(Color.parseColor("#2196F3"))
            3 -> mensagem.setTextColor(Color.parseColor("#FFEB3B"))
            4 -> mensagem.setTextColor(Color.parseColor("#F44336"))
        }

        when (number){
            0 -> StoriesTextColor = "#FFFFFF"
            1 -> StoriesTextColor = "#000000"
            2 -> StoriesTextColor = "#2196F3"
            3 -> StoriesTextColor = "#FFEB3B"
            4 -> StoriesTextColor = "#F44336"
        }
    }

    //coloca a cor escolhida no background do imageview
    fun clicksColorsBgIv(number: Int ){
        // - 0 branco
        // - 1 preto
        // - 2 azul
        // - 3 amarelo
        // - 4 vermelho
        val ivBg: ImageView = findViewById(R.id.layCriandoStory_iv)
        when (number){
            0 -> ivBg.setColorFilter(ContextCompat.getColor(this,  R.color.branco), android.graphics.PorterDuff.Mode.MULTIPLY)
            1 -> ivBg.setColorFilter(ContextCompat.getColor(this,  R.color.preto), android.graphics.PorterDuff.Mode.MULTIPLY)
            2 -> ivBg.setColorFilter(ContextCompat.getColor(this,  R.color.azul), android.graphics.PorterDuff.Mode.MULTIPLY)
            3 -> ivBg.setColorFilter(ContextCompat.getColor(this,  R.color.amarelo), android.graphics.PorterDuff.Mode.MULTIPLY)
            4 -> ivBg.setColorFilter(ContextCompat.getColor(this,  R.color.vermelho), android.graphics.PorterDuff.Mode.MULTIPLY)
        }


        when (number){
            0 -> StoriesBgColor = "#FFFFFF"
            1 -> StoriesBgColor = "#000000"
            2 -> StoriesBgColor = "#2196F3"
            3 -> StoriesBgColor = "#FFEB3B"
            4 -> StoriesBgColor = "#F44336"
        }
    }

    //envio de foto exclusivo do stories
    fun takePictureFromCameraToStories() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, 200)
        }
    }

    fun takePictureFromGalleryToStories() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/jpeg"
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(Intent.createChooser(intent, "Selecione a foto"), 201)

    }

    //aqui vamos reduzir o tamanho antes de enviar pro bd
    private fun compressImageToStories(image: Bitmap) {

        ChamaDialog()

        //agora sabemos as dimensões da imagem.
        //neste exemplo queremos que caiba em um banner de 100x400
        //é alterando o tamanho aqui que o tamanho total da imagem cresce ao final**************************************
//pode ser 100x100, depende do formato que você quer exibir
//400x100 fica com 2,5 kb, 800x200 fica com 5 kb
        //val imageProvisoria: Bitmap = calculateInSizeSampleToFitImageView(image, 600, 800)

        mImagemProvisoria = null
        /*
        if (!temFotoNoStories.equals("nao")){
            val btnApagafotoAntiga : Button = findViewById(R.id.layCriandoStories_btnApagaImg)
            btnApagafotoAntiga.performClick()
        }

         */

        mImagemProvisoria = calculateInSizeSampleToFitImageView(image, 600, 800)
        //image provisoria pode ser colocada no imageview pois já é pequena suficiente.
        val imageview:ImageView = findViewById(R.id.layCriandoStory_iv)
        //imageview.setImageBitmap(mImagemProvisoria)
        Glide.with(this@userPerfilActivity).load(mImagemProvisoria).centerCrop().apply(RequestOptions().override(600, 800))
            .into(imageview)

        //val btnApagafotoAntiga : Button = findViewById(R.id.layCriandoStories_btnApagaImg)
        //btnApagafotoAntiga.visibility = View.VISIBLE

        //esta parte é do método antigo. Imagino que ele nao tenha função mais
        val baos = ByteArrayOutputStream()
        var optionsCompress = 20  //taxa de compressao. 100 significa nenhuma compressao
        try {
            //Code here
            while (baos.toByteArray().size / 1024 > 50) {  //Loop if compressed picture is greater than 50kb, than to compression
                baos.reset() //Reset baos is empty baos
                mImagemProvisoria!!.compress(
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
        val tempUri: Uri = getImageUri(this, mImagemProvisoria!!)
        filePath = tempUri
        //uploadImageToStories()  vai ser chamado a partir do botão de publicar

        //recoloca os listeners
        //listener no btn publicar
        EncerraDialog()
        MetodosAddStories()


    }

    //existe uma opção especial aqui para o caso de ser alvará
    fun uploadImageToStories(posicao: String, temTxt: Boolean, message: String){

        Log.d("teste", "tem txt é "+temTxt
        )
        mFireBaseStorage = FirebaseStorage.getInstance()
        mphotoStorageReference = mFireBaseStorage.reference

        //mphotoStorageReference.child(userBd).child("imgPerfil")

        /*
        // Delete the file
        mphotoStorageReference.delete().addOnSuccessListener {
            // File deleted successfully
            Log.d("teste", "apagou")
        }.addOnFailureListener {
            // Uh-oh, an error occurred!
            Log.d("teste", "deu erro")
        }
         */

        mphotoStorageReference =mFireBaseStorage.getReference().child("stories").child(userBd)


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
                }
            }
            return@Continuation mphotoStorageReference.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                urifinal = downloadUri.toString()
                databaseReference.child("stories").child(userBd).child("img").setValue(urifinal)
                //agora vamos definir o horario limite. Será a data e hora de hoje +1 dia (24 horas)

                databaseReference.child("stories").child(userBd).child("latlong").setValue(latLong.toDouble())
                val datalAgora = GetDate()
                val dataLimite = GetfutureDate(1)
                databaseReference.child("stories").child(userBd).child("dataLimite").setValue(dataLimite)
                val horaLimite = GetHour()
                databaseReference.child("stories").child(userBd).child("horaLimite").setValue(horaLimite)

                val btnAddStories: Button = findViewById(R.id.layStories_btnAddStorie)
                val imageViewStories: ImageView = findViewById(R.id.layStories_iv)
                Glide.with(this@userPerfilActivity).load(urifinal).into(imageViewStories)
                btnAddStories.visibility = View.GONE
                imageViewStories.visibility = View.VISIBLE
                val txMensagem : TextView = findViewById(R.id.layStories_txSemStories)  //esta é a mensagem que aparece quando não tem stories
                txMensagem.visibility = View.GONE

                if (temTxt){
                    databaseReference.child("stories").child(userBd).child("message").setValue(message)
                    databaseReference.child("stories").child(userBd).child("messagePosition").setValue(posicao)

                    val txtNaMensagem : TextView = findViewById(R.id.layStories_message)  //este txt é o que aparece dentro da imagem
                    txtNaMensagem.setText(message)

                    if (posicao.equals("top")){
                        val params = txtNaMensagem.layoutParams as ConstraintLayout.LayoutParams
                        params.topToTop = imageViewStories.id
                        txtNaMensagem.requestLayout()

                    } else if (posicao.equals("middle")){
                        val params = txtNaMensagem.layoutParams as ConstraintLayout.LayoutParams
                        params.topToTop = imageViewStories.id
                        params.bottomToBottom = imageViewStories.id
                        txtNaMensagem.requestLayout()
                    } else {
                        val params = txtNaMensagem.layoutParams as ConstraintLayout.LayoutParams
                        params.bottomToBottom = imageViewStories.id
                        txtNaMensagem.requestLayout()
                    }

                    databaseReference.child("stories").child(userBd).child("textColor").setValue(StoriesTextColor)

                    val txtNaMensagemOriginal : TextView = findViewById(R.id.layStories_message)  //este txt é o que aparece dentro da imagem
                    txtNaMensagemOriginal.setTextColor(Color.parseColor(StoriesTextColor))
                }

                if (!StoriesBgColor.equals("nao")){
                    databaseReference.child("stories").child(userBd).child("bgColor").setValue(StoriesBgColor)
                }

                temFotoNoStories = urifinal

                val btnFechar: Button = findViewById(R.id.layCriandoStory_btnFechar)
                btnFechar.performClick()
                EncerraDialog()


                } else {
                // Handle failures
                Toast.makeText(this, "um erro ocorreu.", Toast.LENGTH_SHORT).show()
                // ...
            }
        }

    }

    fun createStories (message: String, temTxt: Boolean, posicao: String){


        //agora vamos definir o horario limite. Será a data e hora de hoje +1 dia (24 horas)
        val datalAgora = GetDate()
        val dataLimite = GetfutureDate(1)
        databaseReference.child("stories").child(userBd).child("dataLimite").setValue(dataLimite)
        val horaLimite = GetHour()
        databaseReference.child("stories").child(userBd).child("horaLimite").setValue(horaLimite)

        databaseReference.child("stories").child(userBd).child("latlong").setValue(latLong.toDouble())

        val btnAddStories: Button = findViewById(R.id.layStories_btnAddStorie)
        val imageViewStories: ImageView = findViewById(R.id.layStories_iv)
        btnAddStories.visibility = View.GONE
        imageViewStories.visibility = View.VISIBLE
        val txMensagem : TextView = findViewById(R.id.layStories_txSemStories)  //esta é a mensagem que aparece quando não tem stories
        txMensagem.visibility = View.GONE

        if (temTxt){
            databaseReference.child("stories").child(userBd).child("message").setValue(message)
            databaseReference.child("stories").child(userBd).child("messagePosition").setValue(posicao)

            val txtNaMensagem : TextView = findViewById(R.id.layStories_message)  //este txt é o que aparece dentro da imagem
            txtNaMensagem.setText(message)

            if (posicao.equals("top")){
                val params = txtNaMensagem.layoutParams as ConstraintLayout.LayoutParams
                params.topToTop = imageViewStories.id
                txtNaMensagem.requestLayout()

            } else if (posicao.equals("middle")){
                val params = txtNaMensagem.layoutParams as ConstraintLayout.LayoutParams
                params.topToTop = imageViewStories.id
                params.bottomToBottom = imageViewStories.id
                txtNaMensagem.requestLayout()
            } else {
                val params = txtNaMensagem.layoutParams as ConstraintLayout.LayoutParams
                params.bottomToBottom = imageViewStories.id
                txtNaMensagem.requestLayout()
            }

            databaseReference.child("stories").child(userBd).child("textColor").setValue(StoriesTextColor)

            val txtNaMensagemOriginal : TextView = findViewById(R.id.layStories_message)  //este txt é o que aparece dentro da imagem
            txtNaMensagemOriginal.setTextColor(Color.parseColor(StoriesTextColor))
        }

        if (!StoriesBgColor.equals("nao")){
            databaseReference.child("stories").child(userBd).child("bgColor").setValue(StoriesBgColor)
        }

        //colocar a cor do fundo no imageView do perfil para visitantes
        if (StoriesBgColor.equals("#FFFFFF")){
            imageViewStories.setColorFilter(ContextCompat.getColor(this@userPerfilActivity,  R.color.branco), android.graphics.PorterDuff.Mode.MULTIPLY)
        } else if (StoriesBgColor.equals("#000000")){
            imageViewStories.setColorFilter(ContextCompat.getColor(this@userPerfilActivity,  R.color.preto), android.graphics.PorterDuff.Mode.MULTIPLY)
        } else if (StoriesBgColor.equals("#2196F3")){
            imageViewStories.setColorFilter(ContextCompat.getColor(this@userPerfilActivity,  R.color.azul), android.graphics.PorterDuff.Mode.MULTIPLY)
        } else if (StoriesBgColor.equals("#FFEB3B")){
            imageViewStories.setColorFilter(ContextCompat.getColor(this@userPerfilActivity,  R.color.amarelo), android.graphics.PorterDuff.Mode.MULTIPLY)
        } else {
            imageViewStories.setColorFilter(ContextCompat.getColor(this@userPerfilActivity,  R.color.vermelho), android.graphics.PorterDuff.Mode.MULTIPLY)
        }

        val btnFechar: Button = findViewById(R.id.layCriandoStory_btnFechar)
        btnFechar.performClick()

    }

    private fun GetHour () : String {

        val sdf = SimpleDateFormat("hh:mm")
        val currentDate = sdf.format(Date())

        return currentDate
    }

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










    //metodos de envio de foto do perfil?
    fun btnAbrePopUpEnvioFoto(){

        //animacoes de abrir e fechar menu
        val layPopUp : ConstraintLayout = findViewById(R.id.layPopupfoto)
        if(layPopUp.isVisible){
            //recolher layoyt
            val popUpSai = AnimationUtils.loadAnimation(this, R.anim.layout_slideout)
            layPopUp.startAnimation(popUpSai)
            popUpSai.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {

                }

                override fun onAnimationEnd(animation: Animation?) {

                    layPopUp.visibility = View.GONE //esta tela é o fundo transparente
                }

                override fun onAnimationStart(animation: Animation?) {

                }

            })
        } else {
            //exibir
            val popUpEntra = AnimationUtils.loadAnimation(this, R.anim.layout_slidein_left_to_center)
            layPopUp.startAnimation(popUpEntra)
            layPopUp.visibility=View.VISIBLE
        }

        val btnTirarFoto: Button = findViewById(R.id.layPopupfoto_btnCamera)
        val btnGaleriaFoto: Button = findViewById(R.id.layPopupfoto_btnGaleria)

        btnTirarFoto.setOnClickListener {
            takePictureFromCamera()
            btnAbrePopUpEnvioFoto()
        }

        btnGaleriaFoto.setOnClickListener {
            takePictureFromGallery()
            btnAbrePopUpEnvioFoto()
        }
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

    //retorno da imagem
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //retorno da camera
        //primeiro if resultado da foto tirada pela camera
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {

                val photo: Bitmap = data?.extras?.get("data") as Bitmap
                compressImage(photo)

            }

        } else if (requestCode == 101) {
            //resultado da foto pega na galeria
            if (resultCode == RESULT_OK
                && data != null && data.getData() != null
            ) {

                filePath = data.getData()!!
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                compressImage(bitmap)


            }
        } else if (requestCode == 200){

            if (resultCode == RESULT_OK) {

                val photo: Bitmap = data?.extras?.get("data") as Bitmap
                compressImageToStories(photo)

            }
        } else if (requestCode == 201){

            //resultado da foto pega na galeria
            if (resultCode == RESULT_OK
                && data != null && data.getData() != null
            ) {

                filePath = data.getData()!!
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                compressImageToStories(bitmap)


            }

        }
    }

    //aqui vamos reduzir o tamanho antes de enviar pro bd
    private fun compressImage(image: Bitmap) {

        ChamaDialog()


        val imageProvisoria: Bitmap = calculateInSizeSampleToFitImageView(image, imgPerfilWidth, imgPerfilHeight)
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
            compressImage(bit)
        }

        val btnFinalizarEdicao: Button = findViewById(R.id.layFotoPreview_btnSalvarFoto)
        btnFinalizarEdicao.setOnClickListener {

            ChamaDialog()
            uploadImage()
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
        var file = wrapper.getDir("Images",Context.MODE_PRIVATE)
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

        mFireBaseStorage = FirebaseStorage.getInstance()
        mphotoStorageReference = mFireBaseStorage.reference

        //mphotoStorageReference.child(userBd).child("imgPerfil")

        /*
        // Delete the file
        mphotoStorageReference.delete().addOnSuccessListener {
            // File deleted successfully
            Log.d("teste", "apagou")
        }.addOnFailureListener {
            // Uh-oh, an error occurred!
            Log.d("teste", "deu erro")
        }
         */

        mphotoStorageReference =mFireBaseStorage.getReference().child(userBd).child("imgPerfil")


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
                }
            }
            return@Continuation mphotoStorageReference.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                urifinal = downloadUri.toString()

                databaseReference.child("usuarios").child(userBd).child("img").setValue(urifinal)
                //salvar no shared
                val sharedPref: SharedPreferences = getSharedPreferences(getString(R.string.sharedpreferences), 0) //0 é private mode
                val editor = sharedPref.edit()
                editor.putString("imgInicial", urifinal)
                editor.apply()

                databaseReference.child("onlineUsers").child(userBd).child("img").setValue(urifinal)
                val imageViewPerfil: ImageView = findViewById(R.id.layPerfil_ivPerfil)
                Glide.with(this@userPerfilActivity).load(urifinal).apply(RequestOptions.circleCropTransform()).apply(RequestOptions().override(imgPerfilWidth, imgPerfilHeight)).into(imageViewPerfil) //ajusta o tamanho.into(imageViewPerfil)

                EncerraDialog()

            } else {
                // Handle failures
                Toast.makeText(this, "um erro ocorreu.", Toast.LENGTH_SHORT).show()
                // ...
            }
        }

    }

    //fim do upload foto





    //metodos suporte
    fun getDifferenceInTwoDates (dateStart: String, dateStop: String, hourToStart: String, hourToStop: String) : String {

        //format to imput "01/14/2012 09:29:58";
        //Este exemplo estou assumindo que usei GetHours e GetDate. Então hora é dd/MM/yyyy  e hora é 00:00 sem segundos
        val dataStartComplete = dateStart+" "+hourToStart+":00"
        val dataFinalComplete = dateStop+" "+hourToStop+":00"

        var d1: Date? = null
        var d2: Date? = null

        //HH converts hour in 24 hours format (0-23), day calculation

        //HH converts hour in 24 hours format (0-23), day calculation
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        d1 = format.parse(dataStartComplete)
        Log.d("teste", "d1 "+d1)
        d2 = format.parse(dataFinalComplete)
        Log.d("teste", "d2 "+d2)
        var dateFinal : String = "nao"

        try {

            //in milliseconds
            val diff = d2.getTime() - d1.getTime()
            val diffSeconds = diff / 1000 % 60
            val diffMinutes = diff / (60 * 1000) % 60
            val diffHours = diff / (60 * 60 * 1000) % 24
            val diffDays = diff / (24 * 60 * 60 * 1000)
            Log.d("teste", "diffDays "+diffDays)
            //print("$diffDays days, ")
            //print("$diffHours hours, ")
            //print("$diffMinutes minutes, ")
            //print("$diffSeconds seconds.")
            //output  1 days, 1 hours, 1 minutes, 50 seconds.

            Log.d("teste", "diffDays in String "+diffDays.toString())
            dateFinal = diffDays.toString()+" dias, "+diffHours+" horas e "+diffMinutes+" min"
            Log.d("teste", "datefinal "+dateFinal)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }


        return dateFinal

    }




    //PERMISSOES
    //primeiro chame este méotod que vai gerenciar inicialmente.
//caso o user ja tenha dado permissão antes, ele para aqui.
    private fun CheckPermissions() : Boolean {
        var permissao = 0  //é negado
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
            //permissão concedida
            permissao=1
        } else {
            setupPermissions()
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
            permissao=1
        } else {
            setupPermissions()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
            permissao=1
        } else {
            setupPermissions()
        }

        if (permissao==1){
            return true
        } else {
            return false
        }
    }

    private fun setupPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
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
                Toast.makeText(applicationContext,"Você negou a permissão e não poderá acessar as funcionalidades.",
                    Toast.LENGTH_SHORT).show()
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

    /* To hide Keyboard */
    fun hideKeyboard() {
        try {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onBackPressed() {
        val intent = Intent(this, MapsActivity::class.java)
        //intent.putExtra("userBD", userBD)
        intent.putExtra("email", userMail)
        intent.putExtra("voltaDoUser", "volta")
        startActivity(intent)
        finish()
    }
}
