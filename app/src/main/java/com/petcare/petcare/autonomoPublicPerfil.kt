package com.petcare.petcare

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class autonomoPublicPerfil : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference

    //envio de imagem
    private lateinit var filePath: Uri
    private var urifinal: String = "nao"
    private lateinit var mphotoStorageReference: StorageReference
    private lateinit var mFireBaseStorage: FirebaseStorage

    var bdDoAutonomo: String = "nao"
    var usermail: String = "nao"
    var userBD: String = "nao"
    var whatsAppNumber: String = "nao"
    var servico: String = "nao"
    var apelido:String = "nao"

    val arrayFotos: MutableList<String> = ArrayList()
    var fotoLimit: Int = 1
    var qntFotos=0
    var libera="nao"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_autonomo_public_perfil)


        ChamaDialog()
        databaseReference = FirebaseDatabase.getInstance().reference

        val delim = "!!??!!lukako"


        bdDoAutonomo = intent.getStringExtra("autonomoBD")
        userBD = intent.getStringExtra("userBD")
        usermail = intent.getStringExtra("userMail")
        libera = intent.getStringExtra("liberado")

        if (bdDoAutonomo.contains(delim)) {
            bdDoAutonomo = bdDoAutonomo.replace(delim, "")
        }

        var planoPremium = "nao"
        if (intent.hasExtra("planoPremium")) {
            planoPremium =  intent.getStringExtra("planoPremium")
        }

        if (planoPremium.equals("sim")) {
            fotoLimit = 10
        }
        /*
        val planoPremium =  intent.getStringExtra("planoPremium")
        if (planoPremium.equals("sim")){
            fotoLimit=10
        }

         */

        if (bdDoAutonomo.equals(userBD)){
            //significa que é o usuário entrando na sua página para ver
            //btnContratar continua enabled false
            //Libera o botao de upload de foto
            val btnUpload:Button = findViewById(R.id.autonomoPublicPerfil_btnUpload)
            btnUpload.visibility = View.VISIBLE
        } else {
            val btnContratar: Button = findViewById(R.id.autonomoPublicPerfil_btnContratar)
            btnContratar.isEnabled=true
        }
        queryInfos()

        metodosIniciais()

    }

    fun metodosIniciais(){

        mFireBaseStorage = FirebaseStorage.getInstance()
        mphotoStorageReference = mFireBaseStorage.reference

        //exibir o codigo
        val tvCu : TextView = findViewById(R.id.autonomoPublicPerfil_Cu)
        tvCu.setText(bdDoAutonomo)

        val btnContratar: Button = findViewById(R.id.autonomoPublicPerfil_btnContratar)
        btnContratar.setOnClickListener {
            if (libera.equals("nao")){
                Toast.makeText(this, "Você ainda não tem privilégio para contratar um serviço. Você precisa fazer pelo menos uma compra em um petshop para confirmação da sua existência. Esta é uma medida de segurança para todos nossos parceiros e também para você.", Toast.LENGTH_LONG).show()
            } else {
                //openpopup
                openPopUp("Atenção!", "Ao clicar em concordar será gerado um pedido de serviço, colocaremos você em contato. Uma mensagem automática será gerada e enviada ao número do prestador de serviço.", true, "Sim, iniciar conversas", "Cancelar")
            }
        }

        //se for verdade é pq é plano premium
        if (fotoLimit>=2){
            val btnUpload: Button = findViewById(R.id.autonomoPublicPerfil_btnUpload)
            btnUpload.setOnClickListener {
                if (qntFotos<fotoLimit){
                    openPopUpUpload("Upload de fotos", "fazer upload a partir de:", true, "Tirar foto", "Do celular")
                } else {
                    makeToast("Você já atingiu o limite de fotos")
                }
            }
        }

        val btnVoltar: Button = findViewById(R.id.autonomoPublicPerfil_btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }

    }


    //Neste método salvamos tudo que iremos precisar depois
    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Save the user's current game state

        savedInstanceState.putString("bdDoAutonomo", bdDoAutonomo)
        savedInstanceState.putString("usermail", usermail)
        savedInstanceState.putString("userBD", userBD)
        savedInstanceState.putString("whatsAppNumber", whatsAppNumber)
        savedInstanceState.putString("servico", servico)
        savedInstanceState.putString("apelido", apelido)
        savedInstanceState.putString("fotoLimit", fotoLimit.toString())
        savedInstanceState.putString("qntFotos", qntFotos.toString())
        savedInstanceState.putString("libera", libera)

        super.onSaveInstanceState(savedInstanceState)
    }

//aqui recuperamos tudo.

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState)

        bdDoAutonomo = savedInstanceState.getString("bdDoAutonomo").toString()
        usermail = savedInstanceState.getString("usermail").toString()
        userBD = savedInstanceState.getString("userBD").toString()
        whatsAppNumber = savedInstanceState.getString("whatsAppNumber").toString()
        servico = savedInstanceState.getString("servico").toString()
        apelido = savedInstanceState.getString("apelido").toString()
        var x = savedInstanceState.getString("fotoLimit").toString()
        fotoLimit = x.toInt()
        x = savedInstanceState.getString("qntFotos").toString()
        qntFotos = x.toInt()
        libera = savedInstanceState.getString("libera").toString()

        //val btnAdicionarPet: Button = findViewById(R.id.adocao_btnAnunciar)
        //btnAdicionarPet.performClick()

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
                popupWindow.dismiss()
            }

            buttonPopupS.setOnClickListener {
                fechaContrato()
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
        val lay_root: ConstraintLayout = findViewById(R.id.autonomoPublicPerfil_index)


        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }

    //esta query vai preencher os campos para o usuario editar. São todos automaticos no inicio da activity
    fun queryInfos(){

        var nota=0.0
        var avaliacoes=0

        ChamaDialog()
        Log.d("teste", "o bd do autono para a queyr é"+bdDoAutonomo)
        val rootRef = databaseReference.child("autonomos").child(bdDoAutonomo)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()){

                    var values: String

                    values = p0.child("apelido").getValue().toString()
                    var textView: TextView = findViewById(R.id.autonomoPublicPerfil_Nome)
                    textView.setText(values)
                    apelido = values

                    values = p0.child("servico").getValue().toString()
                    textView = findViewById(R.id.autonomoPublicPerfil_Servico)
                    if (values.equals("passeador")){
                        textView.setText("Passeador de animais")
                    } else if (values.equals("comida")){
                        textView.setText("Comidas naturais")
                    }
                    servico = textView.text.toString()

                    values = p0.child("desc").getValue().toString()
                    textView = findViewById(R.id.autonomoPublicPerfil_etDescricao)
                    textView.setText(values)

                    values = p0.child("cartao").getValue().toString()
                    textView = findViewById(R.id.autonomoPublicPerfil_Pagamento)
                    if (values.equals("sim")){
                        textView.setText("Dinheiro e cartão")
                    } else {
                        textView.setText("Dinheiro apenas")
                    }

                    values = p0.child("imgPerfil").getValue().toString()
                    val imageView: ImageView = findViewById(R.id.autonomoPublicPerfil_ivPerfil)
                    Glide.with(this@autonomoPublicPerfil).load(values).apply(RequestOptions.circleCropTransform()).apply(RequestOptions().override(130, 130)).into(imageView)

                    whatsAppNumber = p0.child("ddd").getValue().toString()
                    values= p0.child("whats").getValue().toString()
                    whatsAppNumber = whatsAppNumber+values


                    values = p0.child("avaliacoes").getValue().toString()
                    textView = findViewById(R.id.autonomoPublicPerfil_Avaliacoes)
                    textView.setText("Avaliacões: "+values)
                    avaliacoes = values.toInt()

                    values = p0.child("nota").getValue().toString()
                    textView = findViewById(R.id.autonomoPublicPerfil_Nota)
                    textView.setText("Nota: "+values)
                    nota = values.toDouble()

                    calculaNota(nota, avaliacoes)


                    //ler as imagens
                    qntFotos = (p0.child("qntFotos").getValue().toString()).toInt()
                    if (qntFotos.toInt()>0){
                        if (p0.child("fotos").child("foto0").exists()) {
                            values = p0.child("fotos").child("foto0").getValue().toString()
                            arrayFotos.add(values)
                            if (qntFotos <= fotoLimit) { //usuario premio pode ter mais fotos
                                var cont = 1
                                while (cont < qntFotos) {
                                    var fieldName = "foto" + cont
                                    values = p0.child("fotos").child(fieldName).getValue().toString()
                                    arrayFotos.add(values)
                                    cont++
                                }

                            }
                        } else {
                            Log.d("teste", "está sem nenhuma foto")
                        }
                    }

                    //colocar no botão a quantidade de fotos
                    val btnUpload: Button = findViewById(R.id.autonomoPublicPerfil_btnUpload)
                    btnUpload.setText("Upload de fotos  "+qntFotos.toString()+"/"+fotoLimit.toString())

                    montaRecyclerView()
                    EncerraDialog()

                } else {
                    EncerraDialog()
                    makeToast("Ocorreu um erro.")
                    finish()
                }


            }
        })

    }

    fun montaRecyclerView(){


        //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
        var adapter: MinhasFotosNoPerfilPublicoAutonomoRecyclerViewAdapter = MinhasFotosNoPerfilPublicoAutonomoRecyclerViewAdapter(this, arrayFotos)

//chame a recyclerview
        var recyclerView: RecyclerView = findViewById(R.id.autonomoPublicPerfil_recyclerView)

        //define o tipo de layout (linerr, grid)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

//coloca o adapter na recycleview
        recyclerView.adapter = adapter

        recyclerView.layoutManager = linearLayoutManager

// Notify the adapter for data change.
        adapter.notifyDataSetChanged()

        recyclerView.addOnItemTouchListener(RecyclerTouchListener(this, recyclerView!!, object: ClickListener{

            override fun onClick(view: View, position: Int) {
                //Log.d("teste", arrayFotos.get(position))
                //Toast.makeText(this@MainActivity, !! aNome.get(position).toString(), Toast.LENGTH_SHORT).show()
                openPopUpApagaFoto("Atenção", "Você deseja apagar esta foto?", true, "Sim, apagar", "Cancelar", position)
            }

            override fun onLongClick(view: View?, position: Int) {

            }
        }))


    }

    fun openPopUpApagaFoto (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, position: Int) {
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
                apagaFoto(position)
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
        val lay_root: ConstraintLayout = findViewById(R.id.autonomoPublicPerfil_index)


        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }

    fun apagaFoto(position: Int){

        mFireBaseStorage = FirebaseStorage.getInstance()
        mphotoStorageReference = mFireBaseStorage.reference

        mphotoStorageReference =mFireBaseStorage.getReference().child(userBD).child("imagensAutonomo")

        // Delete the file
        mphotoStorageReference.delete().addOnSuccessListener {
            // File deleted successfully
            Toast.makeText(this, "Imagem apagada", Toast.LENGTH_SHORT).show()
            arrayFotos.removeAt(position)
        }.addOnFailureListener {
            // Uh-oh, an error occurred!
            Toast.makeText(this, "Um erro ocorreu", Toast.LENGTH_SHORT).show()
        }


    }

    fun calculaNota(nota: Double, avaliacoes: Int){

        var media: Double = 0.0
        if (nota==0.0 && avaliacoes ==0){
            media=0.0
        } else {
            media = (nota/avaliacoes).toDouble()
        }

        val star1:ImageView = findViewById(R.id.autonomoPublicPerfil_star1)
        val star2:ImageView = findViewById(R.id.autonomoPublicPerfil_star2)
        val star3:ImageView = findViewById(R.id.autonomoPublicPerfil_star3)

        if (media==0.0){
            star1.setImageResource(R.drawable.ic_avaliation_branco)
            star2.setImageResource(R.drawable.ic_avaliation_branco)
            star3.setImageResource(R.drawable.ic_avaliation_branco)
        } else if (media<=0.5){
            star1.setImageResource(R.drawable.ic_star_half_black_24dp)
            star2.setImageResource(R.drawable.ic_avaliation_branco)
            star3.setImageResource(R.drawable.ic_avaliation_branco)
        } else if (media<=1.0){
            star1.setImageResource(R.drawable.ic_avaliacao_gold)
            star2.setImageResource(R.drawable.ic_avaliation_branco)
            star3.setImageResource(R.drawable.ic_avaliation_branco)
        } else if (media<=1.5){
            star1.setImageResource(R.drawable.ic_avaliacao_gold)
            star2.setImageResource(R.drawable.ic_star_half_black_24dp)
            star3.setImageResource(R.drawable.ic_avaliation_branco)
        } else if (media<=2.0){
            star1.setImageResource(R.drawable.ic_avaliacao_gold)
            star2.setImageResource(R.drawable.ic_avaliacao_gold)
            star3.setImageResource(R.drawable.ic_avaliation_branco)
        } else if (media<=2.5){
            star1.setImageResource(R.drawable.ic_avaliacao_gold)
            star2.setImageResource(R.drawable.ic_avaliacao_gold)
            star3.setImageResource(R.drawable.ic_star_half_black_24dp)
        } else {
            star1.setImageResource(R.drawable.ic_avaliacao_gold)
            star2.setImageResource(R.drawable.ic_avaliacao_gold)
            star3.setImageResource(R.drawable.ic_avaliacao_gold)
        }

    }


    fun fechaContrato(){

        //A negociação será aberta e enviada para minhas vendas. Para o user é minhas compras.
        val newCad: DatabaseReference = databaseReference.child("servicos").child(bdDoAutonomo).push()
        newCad.child("cliente").setValue(userBD)
        newCad.child("clienteMail").setValue(usermail)
        var x = GetDate()
        newCad.child("data").setValue(x)
        x = GetHour()
        newCad.child("hora").setValue(x)
        newCad.child("controle").setValue("controle")
        newCad.child("apelido").setValue(apelido)
        newCad.child("servico").setValue(servico)
        newCad.child("prestador").setValue(bdDoAutonomo)
        newCad.child("status").setValue("aguardando confirmacao")

        //servico Enrollmanet = cria um campo no bd para o usuario poder encontrar fácilmente depois (pois está dentro do bd do prestador. Então o user nao tem acesso direto)
        //databaseReference.child("servicosEnrollment").child(userBD+"!?!%"+bdDoAutonomo).child("prestadorDoServico").setValue(bdDoAutonomo)
        //databaseReference.child("servicosEnrollment").child(userBD+bdDoAutonomo).child("situacao").setValue("aberto")   nao preciso escrever aberto. Se existe aqui é pq está aberto. Quando encerrar vou apagar
        databaseReference.child("servicosEnrollment").child(bdDoAutonomo+"!?!%"+userBD).child("cliente").setValue(userBD)

        /*
        val newCadEnrollment : DatabaseReference = databaseReference.child("servicosEnrollment").child(userBD+bdDoAutonomo).push()
        newCadEnrollment.child("prestadorDoServico").setValue(bdDoAutonomo)
        newCadEnrollment.child("situacao").setValue("aberto")
         */


        /*
        databaseReference.child("servicos").child(bdDoAutonomo).child("cliente").setValue(userBD)
        databaseReference.child("servicos").child(bdDoAutonomo).child("clienteMail").setValue(usermail)
        var x = GetDate()
        databaseReference.child("servicos").child(bdDoAutonomo).child("data").setValue(x)
        x = GetHour()
        databaseReference.child("servicos").child(bdDoAutonomo).child("hora").setValue(x)
        databaseReference.child("servicos").child(bdDoAutonomo).child("controle").setValue("controle")
        databaseReference.child("servicos").child(bdDoAutonomo).child("status").setValue("aguardando confirmacao")
         */

        openWhatsApp(whatsAppNumber, x)

    }

    //Envia mensagem pro whatsapp
    fun openWhatsApp(whatsApp: String, hora:String){

        val pm: PackageManager = packageManager
        try {
            val waIntent: Intent = Intent(Intent.ACTION_SEND)
            waIntent.setPackage("com.whatsapp")
            //sendIntent.setPackage("com.whatsapp");
            waIntent.type = "text/plain"


            val text: String  = "Olá "+apelido+"\nNovo agendamento de serviço\n\n"+servico+"\n\nHoje, "+GetDate()+"às "+hora+" este cliente fez contato buscando seu serviço. Tenham em mente que ainda precisam ser combinados os horários, os preços finais e as condições. Após o serviço ter sido realizado finalizem para avaliar o cliente/prestador de serviço. Reforçamos que o aplicativo Farejador não ganha comissão nesta transação e por isto não nos responsabilizamos de forma alguma. Esta mensagem foi gerada automáticamente mas pode ter sido editada."

            val toNumber =  "55"+whatsApp// Replace with mobile phone number without +Sign or leading zeros, but with country code
            //Suppose your country is India and your phone number is “xxxxxxxxxx”, then you need to send “91xxxxxxxxxx”.

            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("http://api.whatsapp.com/send?phone=$toNumber&text=$text")
            startActivity(intent)

        } catch (e: PackageManager.NameNotFoundException ) {
            Toast.makeText(this, "WhatsApp não está instalado neste celular", Toast.LENGTH_SHORT)
                .show()
        }catch(e:Exception){

        }


        finish()
    }



    //envio das fotos
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

    private fun compressImage(image: Bitmap) {

        //agora sabemos as dimensões da imagem.
        //neste exemplo queremos que caiba em um banner de 100x400
        //é alterando o tamanho aqui que o tamanho total da imagem cresce ao final**************************************
//pode ser 100x100, depende do formato que você quer exibir
//400x100 fica com 2,5 kb, 800x200 fica com 5 kb
        val imageProvisoria: Bitmap = calculateInSizeSampleToFitImageView(image, 130, 150)

        //image provisoria pode ser colocada no imageview pois já é pequena suficiente.
//        val imageviewBanne:ImageView = findViewById(R.id.lay_edit_layout_loja_bannerImgView)
  //      imageviewBanne.setImageBitmap(imageProvisoria)

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


        mphotoStorageReference =mFireBaseStorage.getReference().child(userBD).child("imagensAutonomo").child(GetHour()+GetDate())


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
                //se quiser salvar, é o urifinal que é o link
                //pra salvar no bd e carregar com glide.
                arrayFotos.add(urifinal)
                val field = "foto"+qntFotos
                qntFotos=qntFotos+1
                databaseReference.child("autonomos").child(userBD).child("fotos").child(field).setValue(urifinal)
                databaseReference.child("autonomos").child(userBD).child("qntFotos").setValue(qntFotos)
                //colocar no botão a quantidade de fotos
                val btnUpload: Button = findViewById(R.id.autonomoPublicPerfil_btnUpload)
                btnUpload.setText("Upload de fotos  "+qntFotos.toString()+"/"+fotoLimit.toString())
                EncerraDialog()


            } else {
                // Handle failures
                Toast.makeText(this, "um erro ocorreu.", Toast.LENGTH_SHORT).show()
                // ...
            }
        }

    }

    fun openPopUpUpload (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String) {
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
               takePictureFromCamera()
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
        val lay_root: ConstraintLayout = findViewById(R.id.autonomoPublicPerfil_index)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )


    }




    //click listener da primeira recycleview
    interface ClickListener {
        fun onClick(view: View, position: Int)

        fun onLongClick(view: View?, position: Int)
    }
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



    //pega  a data
    private fun GetDate () : String {

        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.format(Date())

        return currentDate
    }

    //pega a hora
    private fun GetHour () : String {

        val sdf = SimpleDateFormat("hh:mm")
        val currentDate = sdf.format(Date())

        return currentDate
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

    override fun onBackPressed() {
        intent.putExtra("email", usermail)
        intent.putExtra("voltaDoUser", "volta")
        startActivity(intent)
        finish()
    }
}
