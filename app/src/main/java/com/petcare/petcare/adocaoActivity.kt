    package com.petcare.petcare

    import android.Manifest
    import android.content.Context
    import android.content.ContextWrapper
    import android.content.DialogInterface
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.graphics.Bitmap
    import android.graphics.BitmapFactory
    import android.graphics.Point
    import android.net.Uri
    import android.os.Build
    import android.os.Bundle
    import android.provider.MediaStore
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
    import androidx.core.content.ContextCompat
    import androidx.recyclerview.widget.GridLayoutManager
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

    class adocaoActivity : AppCompatActivity() {

        //envio de imagem
        private lateinit var filePath: Uri
        private var urifinal: String = "nao"
        private lateinit var mphotoStorageReference: StorageReference
        private lateinit var mFireBaseStorage: FirebaseStorage

        var userBd = "nao"
        var libera="nao"
        var userCity = "nao"

        private lateinit var databaseReference: DatabaseReference

        var temFoto=false

        var arrayNome: MutableList<String> = ArrayList()
        var arrayImg: MutableList<String> = ArrayList()
        var arrayUserBd: MutableList<String> = ArrayList()
        var arrayPetBd: MutableList<String> = ArrayList()
        var arrayDesc: MutableList<String> = ArrayList()
        var arrayTipo: MutableList<String> = ArrayList()
        var arrayWhats: MutableList<String> = ArrayList()

        val delim="!?!%*"
        var arrayAcoesNecessarias: MutableList<String> = ArrayList()  //vamos usar o mesmoa array para quando o suer for anunciante e quando for pretendente. Prioridade pro anunciante. Ai se estiver vazio, verificamos a outra possibuilidade
        var arrayMeusAnuncios: MutableList<String> = ArrayList()  //vamos usar o mesmoa array para quando o suer for anunciante e quando for pretendente. Prioridade pro anunciante. Ai se estiver vazio, verificamos a outra possibuilidade

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_adocao)


        }

        override fun onStart() {
            super.onStart()

            databaseReference = FirebaseDatabase.getInstance().reference

            mFireBaseStorage = FirebaseStorage.getInstance()
            mphotoStorageReference = mFireBaseStorage.reference


            userBd = intent.getStringExtra("userBD")
            libera = intent.getStringExtra("liberado")
            userCity = intent.getStringExtra("cidade")

            val btnAdicionarPet: Button = findViewById(R.id.adocao_btnAnunciar)
            btnAdicionarPet.setOnClickListener {
                if (libera.equals("nao")){
                    openPopUp("Que pena", "Você ainda não possui privilégios. Para isto é necessário que faça uma compra em algum petshop pelo aplicativo. Isto serve como uma medida de segurança para todos. Assim nós confirmamos que você realmente existe. Esperamos que entenda.", false, "n", "n")
                } else {
                    val estaLay: ConstraintLayout = findViewById(R.id.adocao_layInicial)
                    estaLay.visibility=View.GONE
                    val adicionarAnimais: ConstraintLayout = findViewById(R.id.adocao_Layanunciar_animal)
                    adicionarAnimais.visibility=View.VISIBLE
                    adicionarAnimais()
                }

            }


            queryAnimaisParaAdocao()

            queryMeusAnuncios()
            queryMinhasAdocoes()

            val btnAbreMeusAnuncios : Button = findViewById(R.id.adocao_btnMeusAnuncios)
            btnAbreMeusAnuncios.setOnClickListener {
                abreMeusAnuncios()
            }

        }

        //passos: Criar um mural com animais para adoção
        //criar possibilidade de fazer doação: Ao fechar a doação abrir um chat no whatzapp
        //obs: Tem que verificar se a pessoa já liberou serviços


        //query dos animais
        fun queryAnimaisParaAdocao(){

            ChamaDialog()
            //FirebaseDatabase.getInstance().reference.child("petshops").orderByChild("latlong").startAt(startAtval).endAt(endAtval)
            FirebaseDatabase.getInstance().reference.child("adocao").orderByChild("cidade").equalTo(userCity).limitToFirst(50)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        if (dataSnapshot.exists()) {
                            for (querySnapshot in dataSnapshot.children) {

                                var values: String
                                values = querySnapshot.child("nome").value.toString()
                                arrayNome.add(values)
                                values = querySnapshot.child("desc").value.toString()
                                arrayDesc.add(values)
                                values = querySnapshot.child("imagem").value.toString()
                                arrayImg.add(values)
                                values = querySnapshot.child("tipo").value.toString()
                                arrayTipo.add(values)
                                values = querySnapshot.child("userBd").value.toString()
                                arrayUserBd.add(values)
                                values = querySnapshot.child("cel").value.toString()
                                arrayWhats.add(values)
                                values = querySnapshot.key.toString()
                                arrayPetBd.add(values)

                            }
                        } else {

                            //openPopUp("Que pena", "Não existem animais para adotar na sua cidade", false, "n", "n")

                        }

                        EncerraDialog()
                        montaRecyclerViewGrid()

                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Getting Post failed, log a message
                        EncerraDialog()
                        // ...
                    }
                })  //addValueEventListener



        }

        //coloca itens na RecyclerView e prepara o click
        fun montaRecyclerViewGrid(){

            //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
            var adapter: adocaoRecyclerViewAdapter = adocaoRecyclerViewAdapter(this, arrayNome, arrayImg, arrayUserBd, arrayPetBd, arrayDesc, arrayTipo, arrayWhats)

    //chame a recyclerview
            var recyclerView: RecyclerView = findViewById(R.id.adocao_recyclerView)


            //calcular aqui o tamanho da tela e dividir em quantos quadradinhos der

            //aqui definimos a quantidade de colunas. Mas vamos calcular o tamanho da tela para saber quantas cabem
            //cada quadradinho tem 162 dp no layout.
            //pegando o tamanho da tela do celular
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            val width: Int = size.x

            var colunas =0
            if (width>900){
                colunas = 3
            } else {
                colunas=2
            }

            recyclerView.layoutManager = GridLayoutManager(this, colunas)

    //coloca o adapter na recycleview
            recyclerView.adapter = adapter

            //recyclerView.layoutManager = linearLayoutManager

    // Notify the adapter for data change.
            adapter.notifyDataSetChanged()

            //constructor: context, nomedarecycleview, object:ClickListener
            recyclerView.addOnItemTouchListener(RecyclerTouchListener(this, recyclerView!!, object: ClickListener{

                override fun onClick(view: View, position: Int) {
                    //Log.d("teste", aNome.get(position))
                    //Toast.makeText(this@MainActivity, !! aNome.get(position).toString(), Toast.LENGTH_SHORT).show()
                    Log.d("teste", "o clique funciona")
                    openMaisInfos(position, 0)
                }

                override fun onLongClick(view: View?, position: Int) {

                }
            }))


        }

        fun adicionarAnimais(){

            temFoto=false

            var list_of_items = arrayOf(
                "Selecione",
                "Cachorro",
                "Gato",
                "Outro"
            )

            var tipoPet = "Selecione"
            val spinner: Spinner = findViewById(R.id.spinner_adocao)
            //Adapter for spinner
            spinner.adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list_of_items)

            //item selected listener for spinner
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                    tipoPet = list_of_items[position]

                }
            }

            val btnUpload: Button = findViewById(R.id.adocao_adicionar_btnUpload)
            btnUpload.setOnClickListener {
                openPopUp("Upload de imagem", "Origem da foto", true, "Tirar foto", "Galeria")
            }

            val etNome: EditText = findViewById(R.id.adocao_adicionar_etNome)
            val etDesc: EditText = findViewById(R.id.adocao_adicionar_etDesc)
            val etDdd: EditText = findViewById(R.id.adocao_adicionar_etDdd)
            val etCel: EditText = findViewById(R.id.adocao_adicionar_etCel)


            val btnFinalizar: Button = findViewById(R.id.adocao_adicionar_btnPublicar)
            btnFinalizar.setOnClickListener {
                ChamaDialog()
                btnFinalizar.isEnabled=false
                //fazer verificação e chama uploadImage
                if (etNome.text.isEmpty()){
                    etNome.requestFocus()
                    etNome.setError("!")
                } else if (etDesc.text.isEmpty()){
                    etDesc.requestFocus()
                    etDesc.setText("!")
                    EncerraDialog()
                    btnFinalizar.isEnabled=true
                } else if (temFoto==false){
                    Toast.makeText(this, "Primeiro faça upload de uma foto", Toast.LENGTH_SHORT).show()
                    EncerraDialog()
                    btnFinalizar.isEnabled=true
                } else if (tipoPet.equals("Selecione")) {
                    Toast.makeText(this, "Informe o tipo do pet", Toast.LENGTH_SHORT).show()
                    EncerraDialog()
                    btnFinalizar.isEnabled=true
                } else if (etDdd.text.isEmpty()){
                    EncerraDialog()
                    etDdd.requestFocus()
                    etDdd.setError("!")
                } else if (etCel.text.isEmpty()){
                    etCel.requestFocus()
                    etCel.setError("!")
                    EncerraDialog()
                } else {
                    uploadImage(tipoPet)
                }

            }
        }

        fun openMaisInfos(position: Int, sit: Int){  //sit 0 pode ser o user ou dono. Nao sabemos, entao exibimos o botao de adotar e verificamos. se for 1 é o dono e o btn nao aparece.

            //ChamaDialog()
            val layAnterior: ConstraintLayout = findViewById(R.id.adocao_layInicial)
            layAnterior.visibility = View.GONE

            val btnVoltar: Button = findViewById(R.id.adocao_resumo_btnVoltar)
            btnVoltar.setOnClickListener {
                val layInfo: ConstraintLayout = findViewById(R.id.adocao_resumoInfo)
                layInfo.visibility = View.GONE
                layAnterior.visibility = View.VISIBLE
            }


            val layInfo: ConstraintLayout = findViewById(R.id.adocao_resumoInfo)
            layInfo.visibility = View.VISIBLE

            val imageview: ImageView = findViewById(R.id.adocao_resumo_iv)
            val tvNome: TextView = findViewById(R.id.adocao_resumo_tvNome)
            val tvDesc: TextView = findViewById(R.id.adocao_resumo_tvDesc)
            val tvTipo: TextView = findViewById(R.id.adocao_resumo_tvTipo)
            val btnAdotar: Button = findViewById(R.id.adocao_resumo_adotar)

            Glide.with(this@adocaoActivity).load(arrayImg.get(position)).apply(RequestOptions.circleCropTransform()).into(imageview)
            tvNome.setText(arrayNome.get(position))
            tvDesc.setText(arrayDesc.get(position))
            tvTipo.setText("Tipo: "+arrayTipo.get(position))

            if (sit==1){
                btnAdotar.visibility = View.GONE
            } else {
                btnAdotar.visibility = View.VISIBLE
            }
            btnAdotar.setOnClickListener {

                if (arrayUserBd.get(position).equals(userBd)){
                    openPopUp("Desculpe", "Você não pode adotar um pet de você mesmo.", false, "n", "m")
                } else if (libera.equals("nao")){
                openPopUp("Que pena", "Você ainda não possui privilégios. Para isto é necessário que faça uma compra em algum petshop pelo aplicativo. Isto serve como uma medida de segurança para todos. Assim nós confirmamos que você realmente existe. Esperamos que entenda.", false, "n", "n")

                } else{
                    consultaSeJaFezInteresse(position)
                }

                //se o user ainda nao tiver demostrando interesse antes nesse pet o processo se dará dentro da query
                /*
                databaseReference.child("processosDeAdocao").child(userBd+arrayPetBd.get(position)).child("nomePet").setValue(arrayNome.get(position))
                databaseReference.child("processosDeAdocao").child(userBd+arrayPetBd.get(position)).child("bdAdotante").setValue(userBd)
                databaseReference.child("processosDeAdocao").child(userBd+arrayPetBd.get(position)).child("bdAnunciante").setValue(arrayUserBd.get(position))
                databaseReference.child("processosDeAdocao").child(userBd+arrayPetBd.get(position)).child("bdAnuncioPet").setValue(arrayPetBd.get(position))  //para apagar depois
                databaseReference.child("processosDeAdocao").child(userBd+arrayPetBd.get(position)).child("confirmaAnunciante").setValue("nao")
                databaseReference.child("processosDeAdocao").child(userBd+arrayPetBd.get(position)).child("confirmaAdotante").setValue("nao")
                databaseReference.child("processosDeAdocao").child(userBd+arrayPetBd.get(position)).child("tag").setValue(userBd+arrayPetBd.get(position))

                 */


            }


            val btnFinalizar : Button = findViewById(R.id.adocao_adicionar_btnPublicar)
            btnFinalizar.isEnabled=true
        }

        fun consultaSeJaFezInteresse(position: Int){

            ChamaDialog()
            val rootRef = databaseReference.child("processosDeAdocao").child(arrayPetBd.get(position))
            rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    //TODO("Not yet implemented")
                    EncerraDialog()
                }

                override fun onDataChange(p0: DataSnapshot) {

                    if (p0.exists()) {

                        //se encontrar algo é pq o usuario ja demonstrou interesse antes e vai exibir um feedback
                        //TODO("Not yet implemented")
                        var values: String

                        var anuncianteJaConfirmou = false
                        //se entrou é pq existe. Então travar o botão
                        values = p0.child("confirmaAnunciante").getValue().toString()
                        if (values.equals("sim")) {
                            anuncianteJaConfirmou = true
                        }
                        var adotanteJaConfirmou = false
                        values = p0.child("confirmaAdotante").getValue().toString()
                        if (values.equals("sim")) {
                            adotanteJaConfirmou = true
                        }

                        //o botão de adotar fica desabilitado.
                        //so vai liberar no else aqui
                        if (anuncianteJaConfirmou) {
                            val tvSituacao: TextView = findViewById(R.id.adocao_resumo_tvSituacao)
                            tvSituacao.visibility = View.VISIBLE
                            tvSituacao.setText("O anunciante já confirmou esta adoção. Ainda falta voocê. Clique no botão abaixo para confirmar e fechar este caso.")
                            finalizaAdocao(position)
                        } else if (adotanteJaConfirmou) {
                            val tvSituacao: TextView = findViewById(R.id.adocao_resumo_tvSituacao)
                            tvSituacao.visibility = View.VISIBLE
                            tvSituacao.setText("Você já confirmou esta adoção. Aguarde a confirmação do anunciante.")
                        } else {
                            val btnAdotar: Button = findViewById(R.id.adocao_resumo_adotar)
                            btnAdotar.isEnabled = true
                        }

                        EncerraDialog()

                    } else {

                        /*
                        val newCad: DatabaseReference = databaseReference.child("processosDeAdocao").push()
                        newCad.child("nomePet").setValue(arrayNome.get(position))
                        newCad.child("bdAdotante").setValue(userBd)
                        newCad.child("bdAnunciante").setValue(arrayUserBd.get(position))
                        newCad.child("bdAnuncioPet").setValue(arrayPetBd.get(position))  //para apagar depois
                        newCad.child("confirmaAnunciante").setValue("nao")
                        newCad.child("confirmaAdotante").setValue("nao")
                        newCad.child("tag").setValue(userBd+arrayPetBd.get(position))
                         */

                        databaseReference.child("processosDeAdocao").child(arrayPetBd.get(position)).child("nomePet").setValue(arrayNome.get(position))
                        databaseReference.child("processosDeAdocao").child(arrayPetBd.get(position)).child("bdAdotante").setValue(userBd)
                        databaseReference.child("processosDeAdocao").child(arrayPetBd.get(position)).child("bdAnunciante").setValue(arrayUserBd.get(position))
                        databaseReference.child("processosDeAdocao").child(arrayPetBd.get(position)).child("bdAnuncioPet").setValue(arrayPetBd.get(position))  //para apagar depois
                        databaseReference.child("processosDeAdocao").child(arrayPetBd.get(position)).child("confirmaAnunciante").setValue("nao")
                        databaseReference.child("processosDeAdocao").child(arrayPetBd.get(position)).child("confirmaAdotante").setValue("nao")



                        openWhatsApp(arrayWhats.get(position), position)
                        openPopUp("Pronto!", "Seu interesse foi registrado. Agora temos que aguardar.", false, "n", "n")
                        //Toast.makeText(this, "Seu interesse foi registrado", Toast.LENGTH_SHORT).show()
                        EncerraDialog()
                    }
                }
            })

        }

        //Envia mensagem pro whatsapp
        fun openWhatsApp(whatsApp: String, position: Int){

            val pm:PackageManager = packageManager
            try {
                val waIntent: Intent = Intent(Intent.ACTION_SEND)
                waIntent.setPackage("com.whatsapp")
                //sendIntent.setPackage("com.whatsapp");
                waIntent.type = "text/plain"

                val text: String  = "Olá! Tenho interesse em adotar "+arrayNome.get(position)

                val toNumber =  "55"+whatsApp// Replace with mobile phone number without +Sign or leading zeros, but with country code
                //Suppose your country is India and your phone number is “xxxxxxxxxx”, then you need to send “91xxxxxxxxxx”.

                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("http://api.whatsapp.com/send?phone=$toNumber&text=$text")
                startActivity(intent)

            } catch (e:PackageManager.NameNotFoundException ) {
                Toast.makeText(this, "WhatsApp não está instalado neste celular", Toast.LENGTH_SHORT)
                    .show()
            }catch(e:Exception){

            }

        }

        fun finalizaAdocao(position: Int){

            val btnFinalizar: Button = findViewById(R.id.adocao_resumo_btnConfirmarEfinalizar)
            btnFinalizar.visibility = View.VISIBLE
            btnFinalizar.isEnabled=true

            btnFinalizar.setOnClickListener {

                //no futuro pegar aqui e colocar no feed
                //**************FEED*****************
                databaseReference.child("processosDeAdocao").child(userBd+arrayPetBd.get(position)).removeValue()
                Toast.makeText(this, "Pronto. Esta adoção foi finalizada!", Toast.LENGTH_SHORT).show()
                //restart activity
                val intent = intent
                finish()
                startActivity(intent)
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
                    if (CheckPermissions()){
                        takePictureFromCamera()
                        popupWindow.dismiss()
                    } else {
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
                popupWindow.dismiss()
            }

            //lay_root é o layout parent que vou colocar a popup
            val lay_root: ConstraintLayout = findViewById(R.id.adocao_layInicial)


            // Finally, show the popup window on app
            TransitionManager.beginDelayedTransition(lay_root)
            popupWindow.showAtLocation(
                lay_root, // Location to display popup window
                Gravity.CENTER, // Exact position of layout to display popup
                0, // X offset
                0 // Y offset
            )

        }

        override fun onSaveInstanceState(savedInstanceState: Bundle) {
            // Save the user's current game state

            val etNome: EditText = findViewById(R.id.adocao_adicionar_etNome)
            val etDesc: EditText = findViewById(R.id.adocao_adicionar_etDesc)
            val etDdd: EditText = findViewById(R.id.adocao_adicionar_etDdd)
            val etCel: EditText = findViewById(R.id.adocao_adicionar_etCel)
            val instanceOfUserBd = userBd
            val instanceOfUserLibera = libera
            val instanceOfUserCity = userCity
            var nome = "nao"
            var desc = "nao"
            var ddd= "nao"
            var cel = "nao"
            if (!etNome.text.isEmpty()){
                nome = etNome.text.toString()
            }
            if (!etDesc.text.isEmpty()){
                desc = etDesc.text.toString()
            }
            if (!etDdd.text.isEmpty()){
                ddd = etDdd.text.toString()
            }
            if (!etCel.text.isEmpty()){
                cel = etCel.text.toString()
            }
            savedInstanceState.putString("userBd", instanceOfUserBd)
            savedInstanceState.putString("libera", instanceOfUserLibera)
            savedInstanceState.putString("userCity", instanceOfUserCity)
            savedInstanceState.putString("nome", nome)
            savedInstanceState.putString("desc", desc)
            savedInstanceState.putString("ddd", ddd)
            savedInstanceState.putString("cel", cel)

            //savedInstanceState.putInt(PLAYER_LEVEL, mCurrentLevel)

            // Always call the superclass so it can save the view hierarchy state
            super.onSaveInstanceState(savedInstanceState)
        }

        override fun onRestoreInstanceState(savedInstanceState: Bundle) {
            // Always call the superclass so it can restore the view hierarchy
            super.onRestoreInstanceState(savedInstanceState)

            val etNome: EditText = findViewById(R.id.adocao_adicionar_etNome)
            val etDesc: EditText = findViewById(R.id.adocao_adicionar_etDesc)
            val etDdd: EditText = findViewById(R.id.adocao_adicionar_etDdd)
            val etCel: EditText = findViewById(R.id.adocao_adicionar_etCel)
            val nome = savedInstanceState.getString("nome")
            val desc = savedInstanceState.getString("desc")
            val ddd = savedInstanceState.getString("ddd")
            val cel = savedInstanceState.getString("cel")
            if (!nome.equals("nao")){
                etNome.setText(nome)
            }
            if (!desc.equals("nao")){
                etDesc.setText(desc)
            }
            if (!ddd.equals("nao")){
                etDdd.setText(desc)
            }
            if (!cel.equals("nao")){
                etCel.setText(desc)
            }

            userBd = savedInstanceState.getString("userBd").toString()
            libera = savedInstanceState.getString("libera").toString()
            userCity = savedInstanceState.getString("userCity").toString()

            val btnAdicionarPet: Button = findViewById(R.id.adocao_btnAnunciar)
            btnAdicionarPet.performClick()

        }

        //metodos das fotos
        fun takePictureFromCamera() {

            //vamos salvar informações importantes para ser restauradas depois. Ocorre que quando está sem memoria o android mata a activituy que ficou no fundo perdendo as infos.
            // Save the user's current game state

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, 100)
            }
            /*
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, 100)
            }

             */
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


                    //as vezes dava erro aqui. Data vinha com "".
                    //val photo : Bitmap  = data?.getExtras()?.get("data") as Bitmap
                        //entao agora pegamos também o uri. Aparentemente o uri sempre vem
                      //  val tempUri: Uri = getImageUri(getApplicationContext(), photo)

                        //val mBitmap: Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, tempUri)
                        //verificamos que se photo esta vazio

                        //compressImage(mBitmap)



                    //ImageView.setImageBitmap(photo);

                    // CALL THIS METHOD TO GET THE URI FROM THE BITMAP


                    // Show Uri path based on Image
                    //Toast.makeText(this,"Here "+ tempUri, Toast.LENGTH_LONG).show();

                }
                else
                {
                    Toast.makeText(this, "Falha ao capturar imagem", Toast.LENGTH_SHORT).show();


                    /*
                    val photo: Bitmap = data?.extras?.get("data") as Bitmap
                    compressImage(photo)

                     */

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

        //aqui vamos reduzir o tamanho antes de enviar pro bd
        private fun compressImage(image: Bitmap) {

            //agora sabemos as dimensões da imagem.
            //neste exemplo queremos que caiba em um banner de 100x400
            //é alterando o tamanho aqui que o tamanho total da imagem cresce ao final**************************************
    //pode ser 100x100, depende do formato que você quer exibir
    //400x100 fica com 2,5 kb, 800x200 fica com 5 kb
            temFoto=true
            val imageProvisoria: Bitmap = calculateInSizeSampleToFitImageView(image, 130, 130)

            //image provisoria pode ser colocada no imageview pois já é pequena suficiente.
            val imageviewBanne: ImageView = findViewById(R.id.adocao_adicionar_imageView)
            Glide.with(this).load(imageProvisoria).apply(
                RequestOptions().override(130, 130)).into(imageviewBanne)
            imageviewBanne.visibility = View.VISIBLE

            //imageviewBanne.setImageBitmap(imageProvisoria)

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
        fun uploadImage(tipoPet:String){

            Log.d("teste", "Chegou em uploadImage")
            mFireBaseStorage = FirebaseStorage.getInstance()
            //mphotoStorageReference = mFireBaseStorage.reference

            val fieldName = GetDate()+GetHour()
            mphotoStorageReference =mFireBaseStorage.getReference().child("animaisAdocao").child(fieldName)
            Log.d("teste", "mPhotoStorage pegou a referencia")

            val bmp: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath)
            val baos: ByteArrayOutputStream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 25, baos)

            Log.d("teste", "comprimiu o bitmap")
    //get the uri from the bitmap
            val tempUri: Uri = getImageUri(this, bmp)
            Log.d("teste", "pegou o uri")
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

                    Log.d("teste", "taskSucessfull")
                    criarEntradaNoBd(tipoPet)

                } else {
                    // Handle failures
                    EncerraDialog()
                    Toast.makeText(this, "um erro ocorreu.", Toast.LENGTH_SHORT).show()
                    // ...
                }
            }

        }

        fun criarEntradaNoBd(tipoPet: String){

            val etNome: EditText = findViewById(R.id.adocao_adicionar_etNome)
            val etDesc: EditText = findViewById(R.id.adocao_adicionar_etDesc)
            val etDdd: EditText = findViewById(R.id.adocao_adicionar_etDdd)
            val etCel: EditText = findViewById(R.id.adocao_adicionar_etCel)

            //criar o campo
            val newCad: DatabaseReference = databaseReference.child("adocao").push()
            //petBD = newCad.key.toString()
            newCad.child("nome").setValue(etNome.text.toString())
            newCad.child("desc").setValue(etDesc.text.toString())
            newCad.child("imagem").setValue(urifinal)
            newCad.child("tipo").setValue(tipoPet)
            newCad.child("userBd").setValue(userBd)
            //newCad.child("controle").setValue("controle")  a busca agora é por cidade
            newCad.child("cidade").setValue(userCity)
            val cel = etDdd.text.toString()+etCel.text.toString()
            newCad.child("cel").setValue(cel)

            Toast.makeText(this, "Anúncio publicado", Toast.LENGTH_SHORT).show()

            //add ao array pra aparecer na recycleview ja agora
            arrayNome.add(etNome.text.toString())
            arrayImg.add(urifinal)
            arrayUserBd.add(userBd)
            arrayPetBd.add(newCad.key.toString())
            arrayDesc.add(etDesc.text.toString())
            arrayTipo.add(tipoPet)
            arrayWhats.add(cel)

            etNome.setText("")
            etDesc.setText("")
            etCel.setText("")
            etDdd.setText("")

            val layAnterior: ConstraintLayout = findViewById(R.id.adocao_layInicial)
            layAnterior.visibility=View.VISIBLE
            val adicionarAnimais: ConstraintLayout = findViewById(R.id.adocao_Layanunciar_animal)
            adicionarAnimais.visibility=View.GONE
            adicionarAnimais()

            EncerraDialog()

        }














        //meus anuncios
        fun abreMeusAnuncios() {

            val layInicial: ConstraintLayout = findViewById(R.id.adocao_layInicial)
            val layMeusAnuncios: ConstraintLayout = findViewById(R.id.adocao_layMeusAnuncios)

            layInicial.visibility = View.GONE
            layMeusAnuncios.visibility = View.VISIBLE

            val btnVoltar: Button = findViewById(R.id.adocao_layMeusAnuncios_btnVoltar)
            btnVoltar.setOnClickListener {
                layInicial.visibility = View.VISIBLE
                layMeusAnuncios.visibility = View.GONE
            }


            var arrayNome2: MutableList<String> = ArrayList()
            var arrayImg2: MutableList<String> = ArrayList()
            var arrayUserBd2: MutableList<String> = ArrayList()
            var arrayPetBd2: MutableList<String> = ArrayList()
            var arrayDesc2: MutableList<String> = ArrayList()
            var arrayTipo2: MutableList<String> = ArrayList()
            var arrayWhats2: MutableList<String> = ArrayList()

            ChamaDialog()

            //FirebaseDatabase.getInstance().reference.child("petshops").orderByChild("latlong").startAt(startAtval).endAt(endAtval)
            FirebaseDatabase.getInstance().reference.child("adocao").orderByChild("userBd").equalTo(userBd)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        if (dataSnapshot.exists()) {
                            for (querySnapshot in dataSnapshot.children) {

                                var values: String
                                values = querySnapshot.child("nome").value.toString()
                                arrayNome2.add(values)
                                values = querySnapshot.child("desc").value.toString()
                                arrayDesc2.add(values)
                                values = querySnapshot.child("imagem").value.toString()
                                arrayImg2.add(values)
                                values = querySnapshot.child("tipo").value.toString()
                                arrayTipo2.add(values)
                                values = querySnapshot.child("userBd").value.toString()
                                arrayUserBd2.add(values)
                                values = querySnapshot.child("cel").value.toString()
                                arrayWhats2.add(values)
                                values = querySnapshot.key.toString()
                                arrayPetBd2.add(values)

                            }
                        } else {

                            //openPopUp("Que pena", "Não existem animais para adotar na sua cidade", false, "n", "n")

                        }

                        EncerraDialog()
                        //vamos aqui montar a recyclerVIew. Foi feito isso pra aproveitar o array aaqui dentro e nao precisar de uma variavel global
                        //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
                        var adapter: adocaoRecyclerViewAdapter = adocaoRecyclerViewAdapter(this@adocaoActivity, arrayNome2, arrayImg2, arrayUserBd2, arrayPetBd2, arrayDesc2, arrayTipo2, arrayWhats2)

                        //chame a recyclerview
                        var recyclerView: RecyclerView = findViewById(R.id.recyclerView_meusAnuncios)


                        //calcular aqui o tamanho da tela e dividir em quantos quadradinhos der

                        //aqui definimos a quantidade de colunas. Mas vamos calcular o tamanho da tela para saber quantas cabem
                        //cada quadradinho tem 162 dp no layout.
                        //pegando o tamanho da tela do celular
                        val display = windowManager.defaultDisplay
                        val size = Point()
                        display.getSize(size)
                        val width: Int = size.x

                        var colunas =0
                        if (width>900){
                            colunas = 3
                        } else {
                            colunas=2
                        }

                        recyclerView.layoutManager = GridLayoutManager(this@adocaoActivity, colunas)

                        //coloca o adapter na recycleview
                        recyclerView.adapter = adapter

                        //recyclerView.layoutManager = linearLayoutManager

                        // Notify the adapter for data change.
                        adapter.notifyDataSetChanged()

                        //constructor: context, nomedarecycleview, object:ClickListener
                        recyclerView.addOnItemTouchListener(RecyclerTouchListener(this@adocaoActivity, recyclerView!!, object: ClickListener{

                            override fun onClick(view: View, position: Int) {
                                //Log.d("teste", aNome.get(position))
                                //Toast.makeText(this@MainActivity, !! aNome.get(position).toString(), Toast.LENGTH_SHORT).show()
                                Log.d("teste", "o clique funciona")
                                openMaisInfos(position, 1) //1 é para indicar que você é o dono e vai esconder o botão de adotar.
                            }

                            override fun onLongClick(view: View?, position: Int) {

                                openPopUpAcaoApagarMeusAnuncios("Apagar este processo de adoção?", "Ao clicar em confirmar você apaga este processo. Significa que a adoção não foi realizada. Você tem certeza que deseja apagar este registro?", true, "Sim, confirmo", "Cancelar", arrayPetBd2.get(position), position, arrayImg.get(position))


                            }
                        }))



                        //fim da recyclerview

                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Getting Post failed, log a message
                        EncerraDialog()
                        // ...
                    }
                })  //addValueEventListener



        }

        fun openPopUpAcaoApagarMeusAnuncios (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, anuncioBd: String, position: Int, imgLink: String) {
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

                    //agora apaga do storage
                    val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imgLink) //urifinal is a String variable with the url
                    storageReference.delete().addOnSuccessListener {
                        //File deleted

                    }.addOnFailureListener {
                        //failed to delete
                    }

                    databaseReference.child("adocao").child(anuncioBd).removeValue()
                    databaseReference.child("processosDeAdocao").child(anuncioBd).removeValue()
                    Toast.makeText(this, "Apagado. Pode demorar alguns segundos para ter efeito.", Toast.LENGTH_SHORT).show()

                    arrayImg.removeAt(position)
                    arrayNome.removeAt(position)
                    arrayWhats.removeAt(position)
                    arrayTipo.removeAt(position)
                    arrayDesc.removeAt(position)
                    arrayPetBd.removeAt(position)
                    arrayUserBd.removeAt(position)
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
            }

            //lay_root é o layout parent que vou colocar a popup
            val lay_root: ConstraintLayout = findViewById(R.id.layPaiAdocao)

            // Finally, show the popup window on app
            TransitionManager.beginDelayedTransition(lay_root)
            popupWindow.showAtLocation(
                lay_root, // Location to display popup window
                Gravity.CENTER, // Exact position of layout to display popup
                0, // X offset
                0 // Y offset
            )


        }













    ///LayACoes

    //as duas queries sao disparadas no inicio no onCreate e nao no apos o click. É assim pois tendo ação necessária vai exibir o botão, senao ele n aparece.
    //query dos anuncios que o user fez
    fun queryMeusAnuncios(){

    ChamaDialog()
    //FirebaseDatabase.getInstance().reference.child("petshops").orderByChild("latlong").startAt(startAtval).endAt(endAtval)
    FirebaseDatabase.getInstance().reference.child("processosDeAdocao").orderByChild("bdAnunciante").equalTo(userBd)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if (dataSnapshot.exists()) {
                    for (querySnapshot in dataSnapshot.children) {

                        //val anuncianteAgiu = querySnapshot.child("confirmaAnunciante").value.toString()
                        //val adotanteAgiu = querySnapshot.child("confirmaAdotante").value.toString()
                        //if (adotanteAgiu.equals("sim")){

                            var values: String
                            var container = "nao"
                            values = querySnapshot.child("nomePet").value.toString()
                            container = values+delim

                            values = querySnapshot.child("bdAnuncioPet").value.toString()
                            container = container+values+delim


                            container = container+"anunciante"+delim
                            arrayAcoesNecessarias.add(container)

                            /*pos 0 - nomepet
                               pos 1 - anuncioPet
                               poa 2 - adotante ou anunciante
                             */

                            val btnAcao: Button = findViewById(R.id.adocao_btnAcoes)
                            btnAcao.visibility = View.VISIBLE
                            btnAcao.setOnClickListener {
                                abreMinhasAcoes()
                            }
                      //  } else {
                            //nao existem ações pendetes novas
                      //  }
                    }
                } else {

                    queryMinhasAdocoes()
                    //openPopUp("Que pena", "Não existem animais para adotar na sua cidade", false, "n", "n")

                }

                EncerraDialog()

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                EncerraDialog()
                // ...
            }
        })  //addValueEventListener



    }

    //query dos anuncios que o user fez
    fun queryMinhasAdocoes(){

    ChamaDialog()
    //FirebaseDatabase.getInstance().reference.child("petshops").orderByChild("latlong").startAt(startAtval).endAt(endAtval)
    FirebaseDatabase.getInstance().reference.child("processosDeAdocao").orderByChild("bdAdotante").equalTo(userBd)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if (dataSnapshot.exists()) {
                    for (querySnapshot in dataSnapshot.children) {

                        //val anuncianteAgiu = querySnapshot.child("confirmaAnunciante").value.toString()

                      //  if (anuncianteAgiu.equals("sim")){

                            var values: String
                            var container = "nao"
                            values = querySnapshot.child("nomePet").value.toString()
                            container = values+delim

                            values = querySnapshot.child("bdAnuncioPet").value.toString()
                            container = container+values+delim


                            container = container+"adotante"+delim
                            arrayAcoesNecessarias.add(container)

                            /*pos 0 - nomepet
                               pos 1 - anuncioPet
                               poa 2 - adotante ou anunciante
                             */

                            clickDeAbrirAcoes()

                    //    } else {

                      //  }



                    }
                } else {

                    //openPopUp("Que pena", "Não existem animais para adotar na sua cidade", false, "n", "n")

                }

                if (arrayAcoesNecessarias.size>0){
                    val btnNotificacao: Button = findViewById(R.id.adocao_btnAcoes)
                    btnNotificacao.setOnClickListener {

                    }
                }
                EncerraDialog()

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                EncerraDialog()
                // ...
            }
        })  //addValueEventListener

    }

        fun clickDeAbrirAcoes(){
            val btnAcao: Button = findViewById(R.id.adocao_btnAcoes)
            btnAcao.visibility = View.VISIBLE
            btnAcao.setOnClickListener {
                abreMinhasAcoes()
            }
        }

    fun abreMinhasAcoes (){

    val layInicial: ConstraintLayout = findViewById(R.id.adocao_layInicial)
    val layAcoesNecessarias: ConstraintLayout = findViewById(R.id.adocao_layAcoesNecessarias)

    layInicial.visibility = View.GONE
    layAcoesNecessarias.visibility = View.VISIBLE

    val btnVoltar: Button = findViewById(R.id.adocao_layAcoes_btnVoltar)
    btnVoltar.setOnClickListener {
        layInicial.visibility = View.VISIBLE
        layAcoesNecessarias.visibility = View.GONE
    }

    montaRecyclerViewAcoesNecessarias()

    }

    fun montaRecyclerViewAcoesNecessarias(){


    //vamos dar qualquer coisa ao adapter primeiro
    var adapter: adocaoAcoesRecyclerViewAdapter = adocaoAcoesRecyclerViewAdapter(this, arrayAcoesNecessarias)

    //chame a recyclerview
    var recyclerView: RecyclerView = findViewById(R.id.recyclerView_acoes)

    //define o tipo de layout (linerr, grid)
    var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)

    //coloca o adapter na recycleview
    recyclerView.adapter = adapter

    recyclerView.layoutManager = linearLayoutManager

    // Notify the adapter for data change.
    adapter.notifyDataSetChanged()

    recyclerView.addOnItemTouchListener(RecyclerTouchListener(this, recyclerView!!, object: ClickListener{

        override fun onClick(view: View, position: Int) {
            //Log.d("teste", aNome.get(position))
            //Toast.makeText(this@MainActivity, !! aNome.get(position).toString(), Toast.LENGTH_SHORT).show()
            /*pos 0 - nomepet
              pos 1 - anuncioPet
              poa 2 - adotante ou anunciante
                */

            val tokens = StringTokenizer(arrayAcoesNecessarias.get(position), delim)
            val nomePet = tokens.nextToken() // this will contain "img link or nao"
            val anuncioBd = tokens.nextToken()
            val adotanteOuAmbulante = tokens.nextToken()

            if (adotanteOuAmbulante.equals("anunciante")){
                openPopUpAcao("Atenção", "Você confirma que "+nomePet+" foi adotado? Ao confirmar o processo será encerrado e as informaçõea também. A outra parte ja confirmou.", true, "Sim, confirmo", "Cancelar", anuncioBd, position, nomePet)
            } else {
                openPopUpAcao("Atenção", "Você confirma que "+nomePet+" foi adotado? Ao confirmar o processo será encerrado e as informaçõea também. A outra parte ja confirmou.", true, "Sim, confirmo", "Cancelar", anuncioBd, position, nomePet)
            }

        }

        override fun onLongClick(view: View?, position: Int) {

            /*pos 0 - nomepet
              pos 1 - anuncioPet
              poa 2 - adotante ou anunciante
                */

            val tokens = StringTokenizer(arrayAcoesNecessarias.get(position), delim)
            val nomePet = tokens.nextToken() // this will contain "img link or nao"
            val anuncioBd = tokens.nextToken()
            val adotanteOuAmbulante = tokens.nextToken()


            openPopUpAcaoApagar("Apagar este processo de adoção?", "Ao clicar em confirmar você apaga este processo. Significa que a adoção não foi realizada. Você tem certeza que deseja apagar este registro?", true, "Sim, confirmo", "Cancelar", anuncioBd, position)

        }
    }))



    }

    fun openPopUpAcao (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, anuncioBd: String, position: Int, nome: String) {
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


            databaseReference.child("processosDeAdocao").child(anuncioBd).removeValue()
            databaseReference.child("adocao").child(anuncioBd).removeValue()
            Toast.makeText(this, "Processo encerrado! Esperamos que "+nome+" seja muito feliz no novo lar!", Toast.LENGTH_LONG).show()


            //ANUNCIAR NO FEED
            //adiciona mais um petadotado ao nosso controle. Isso servirá pra marketing no futuro
            val rootRef = databaseReference.child("petsAdotados")
            rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    //TODO("Not yet implemented")
                    EncerraDialog()
                }

                override fun onDataChange(p0: DataSnapshot) {
                    //TODO("Not yet implemented")
                    var values: String
                    values = p0.child("total").getValue().toString()

                    val total = values.toInt()
                    databaseReference.child("petsAdotados").child("total").setValue(total+1)

                }
            })

            arrayAcoesNecessarias.removeAt(position)
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
    }

    //lay_root é o layout parent que vou colocar a popup
    val lay_root: ConstraintLayout = findViewById(R.id.layPaiAdocao)

    // Finally, show the popup window on app
    TransitionManager.beginDelayedTransition(lay_root)
    popupWindow.showAtLocation(
        lay_root, // Location to display popup window
        Gravity.CENTER, // Exact position of layout to display popup
        0, // X offset
        0 // Y offset
    )


    }

    fun openPopUpAcaoApagar (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, anuncioBd: String, position: Int) {
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

            databaseReference.child("processosDeAdocao").child(anuncioBd).removeValue()
            arrayAcoesNecessarias.removeAt(position) //ajusta o array
            Toast.makeText(this, "Apagado", Toast.LENGTH_SHORT).show()

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












    //PERMISSOES
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
    }
