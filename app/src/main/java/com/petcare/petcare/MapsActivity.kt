package com.petcare.petcare

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.FirebaseDatabase.getInstance
import com.petcare.petcare.Controller.MapsController
import com.petcare.petcare.Models.MapsModels
import com.petcare.petcare.Utils.*
import kotlinx.android.synthetic.main.activity_maps.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    //to do
    //criar listagem de tudo que está na tela
    //busca do autonomo por código (BD)
    //3 - criar clusters (pega a distancia entre marks - basta usar o arrayPetFriendMarkers e medir as distancias....se forem pequenas esconder em cluster   ref: https://github.com/googlemaps/android-maps-utils/blob/master/demo/src/main/java/com/google/maps/android/utils/demo/CustomMarkerClusteringDemoActivity.java
    //SERVICO: TINDER
    //ADICIONAR AS PERMISSOES EM TODAS ACTIVITIES NVOAS QUE USAM CAMERA E FOOT

    private val FINE_LOCATION_CODE = 721

    private val CAMERA_PERMISSION_CODE = 100
    private val READ_PERMISSION_CODE = 101
    private val WRITE_PERMISSION_CODE = 102


    //upadte automático
    private val appUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val appUpdatedListener: InstallStateUpdatedListener by lazy {
        object : InstallStateUpdatedListener {
            override fun onStateUpdate(installState: InstallState) {
                when {
                    installState.installStatus() == InstallStatus.DOWNLOADED -> popupSnackbarForCompleteUpdate()
                    installState.installStatus() == InstallStatus.INSTALLED -> appUpdateManager.unregisterListener(this)
                    else ->   MapsController.makeToast("Instalando atualização. %"+installState.installStatus(), this@MapsActivity) //Timber.d("InstallStateUpdatedListener: state: %s", installState.installStatus())
                }
            }
        }
    }

    //val mySharedPrefs: mySharedPrefs = mySharedPrefs(this)

    lateinit var mAdView : AdView

    private lateinit var mMap: GoogleMap
    private var getLocal: Boolean = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location

    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    val enderecoUser: MutableList<String> = ArrayList()
    val petShops: MutableList<String> = ArrayList()
    val lojaInfo: MutableList<String> = ArrayList()


    //arrays da loja para o recycleview
    val arrayNomes: MutableList<String> = ArrayList()
    val arrayImg: MutableList<String> = ArrayList()
    val arrayDesc: MutableList<String> = ArrayList()
    val arrayPreco: MutableList<String> = ArrayList()
    val arrayBD: MutableList<String> = ArrayList()
    val arrayTipo: MutableList<String> = ArrayList()

    //arrays da loja para o recycleview
    val arrayNomesCarrinho: MutableList<String> = ArrayList()
    val arrayImgCarrinho: MutableList<String> = ArrayList()
    val arrayDescCarrinho: MutableList<String> = ArrayList()
    val arrayPrecoCarrinho: MutableList<String> = ArrayList()
    val arrayBDCarrinho: MutableList<String> = ArrayList()
    val arrayTipoCarrinho: MutableList<String> = ArrayList()


    //variaveis dos petFriends
    val arrayPetFriendMarker: MutableList<Marker> = ArrayList()

    val arrayStories: MutableList<String> = ArrayList()  //Todos stories num unico lugar. Utilizado para retirar os markers no mapa
    val arrayImgStories: MutableList<String> = ArrayList()  //Todos stories num unico lugar. Utilizado para retirar os markers no mapa

    val arrayAutonomos: MutableList<Marker> = ArrayList()  //Todos autonomos num unico lugar. Este array é usado para tirar os markers do mapa

    val arrayAutonomosNomeBdParaLista: MutableList<String> = ArrayList()  //Todos autonomos num unico lugar. Este array é usado para tirar os markers do mapa


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        /*
        val mySharedPrefs:mySharedPrefs= mySharedPrefs(this)
        mySharedPrefs.setValue("userBdInicial", "testando")
        val valor = mySharedPrefs.getValue("userBdInicial")
        Log.d("teste", "O valor é "+valor)
         */

        //recupera o email do usuário
        MapsModels.userMail = intent.getStringExtra("email")

        MapsModels.setupInicial(this@MapsActivity)

        databaseReference = FirebaseDatabase.getInstance().reference
        FacebookSdk.sdkInitialize(getApplicationContext())
        checkForAppUpdate()
        // Sample AdMob app ID: ca-app-pub-3940256099942544~3347511713

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        MobileAds.initialize(this, "ca-app-pub-6912617107153681~1282961500")
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)


        //tudo que tinha aqui foi movido pra onMapsReady para começar tudo junto
        if (fineLocationPermission.hasPermissions(this)==false){
            fineLocationPermission.checkPermission(this, FINE_LOCATION_CODE)
        }

    }

    override fun onStart() {
        super.onStart()
        val menu: ConstraintLayout = findViewById(R.id.lay_menu)
        val btnMenu: ImageView = findViewById(R.id.lay_Maps_MenuBtn)  //btnMenu é um imageview nao btn

        if (menu.isVisible==false){

            //view is not visible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                (btnMenu.drawable as AnimatedVectorDrawable).reset()
            } else {
                (btnMenu.drawable as AnimatedVectorDrawable).start()
            }
        } else {
            //view is visible
            (btnMenu.drawable as AnimatedVectorDrawable).start()
        }

    }

    override fun onResume() {
        super.onResume()

        val menu: ConstraintLayout = findViewById(R.id.lay_menu)
        val btnMenu: ImageView = findViewById(R.id.lay_Maps_MenuBtn)  //btnMenu é um imageview nao btn

        if (menu.isVisible==false){
            //view is not visible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                (btnMenu.drawable as AnimatedVectorDrawable).reset()
            } else {
                (btnMenu.drawable as AnimatedVectorDrawable).start()
            }
        } else {
            //view is visible
            (btnMenu.drawable as AnimatedVectorDrawable).start()
        }


        val btnExibeLista: Button = findViewById(R.id.btnShowHideLista)
        btnExibeLista.setOnClickListener {

            montaRecyclerViewListaPet()
            montaRecyclerViewListaAutonomo()

        }


        //app update
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->

                // If the update is downloaded but not installed,
                // notify the user to complete the update.
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate()
                }

                //Check if Immediate update is required
                try {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        // If an in-app update is already running, resume the update.
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            262)
                    }
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                }
            }
    }

    override fun onMapReady(googleMap: GoogleMap) {
//        setUpMap()

        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false

        // centralBtnApenasLocaliza ()

        //tudo que veio de onCreate
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        metodosIniciais()
    }

    //este método é chamado quando a app termina de carregar o mapa
    fun metodosIniciais(){


        //requestToOpenGpsLikeWaze()
        MapsController.requestToOpenGpsLikeWaze(this)

        //ChamaDialog()

        databaseReference = FirebaseDatabase.getInstance().reference

        //essa query não tem função, só serve para corrigir possiveis petshops emq ualquer lugar que nao tenha latlong (erro no cadastro)
        queryPetsSemLatLong()

        //só entra aqui quando acaba de criar a loja e volta pra cá.
        if (intent.getStringExtra("chamaLatLong")!=null){
            val endereco = intent.getStringExtra("endereco")
            val bd = intent.getStringExtra("petBD")
            MapsController.getLatLong(endereco, bd, this)
            //getLatLong(endereco, bd)

            val toast = Toast.makeText(this@MapsActivity, "Aguarde alguns segundos, estamos configurando sua loja.", Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
            metodosIniciais()
        } else {

            if (intent.getStringExtra("voltaDoUser")!=null){
                //userMail = intent.getStringExtra("email")
                MapsModels.userMail = intent.getStringExtra("email")
            }


            //aqui é para o caso dele ter entrado sem login (fazer login depois), vamos mudar o texto do botão de logout
            //if (userMail.equals("semLogin")){
            if (MapsModels.userMail.equals("semLogin")){
                val btnLogout: Button = findViewById(R.id.mapsLogoutBtn)
                btnLogout.setText("Fazer login")
            }

            //centralBtnApenasLocaliza() //clique inicial do botão central. Depois ele vai assumir outras funções

            if (MapsController.isNetworkAvailable(this)) {
                //antigamente ficava aqui queries iniciais. Não existe mais este método. tudo que havia lá foi movido para outro momento
            } else {
                MapsController.makeToast("Você está sem conexão de internet. Não foi possível buscar os estabelecimentos próximos", this)
            }

            if (fineLocationPermission.hasPermissions(this)){
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            }

            menuClicks()

            if (isNetworkAvailable(this)) {
                btnBuscaPorEndClick()
            } else {
                MapsController.makeToast("Você está sem conexão de internet. Não foi possível buscar os estabelecimentos próximos", this)
            }

            if (fineLocationPermission.hasPermissions(this)){
                getUserLocation(MapsModels.raioUser, 0)
            } else {
                fineLocationPermission.checkPermission(this, FINE_LOCATION_CODE)
            }

            if (fineLocationPermission.hasPermissions(this)){

                if (MapsModels.userBD.equals("nao")) {
                    queryUserInitial()
                } else {
                    placeUserInMap()
                    if (!MapsModels.userBD.equals("usuario")){
                        //se for autonomo ou proprietário vai fazer query para pegar os dados
                        queryUserInitial()
                    }

                }


            }


        }


        centralBtnApenasLocaliza ()

        EncerraDialog()


    }

    fun montaRecyclerViewListaPet(){

        val layLista: ConstraintLayout = findViewById(R.id.layLista)
        layLista.visibility = View.VISIBLE


        var adapter: listaDePetsRecyclerViewAdapter = listaDePetsRecyclerViewAdapter(this, petShops)

//chame a recyclerview
        var recyclerView: RecyclerView = findViewById(R.id.layLista_recyclerView_Pets)

//define o tipo de layout (linerr, grid)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)

//coloca o adapter na recycleview
        recyclerView.adapter = adapter

        recyclerView.layoutManager = linearLayoutManager

// Notify the adapter for data change.
        adapter.notifyDataSetChanged()

        //constructor: context, nomedarecycleview, object:ClickListener
        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView.addOnItemTouchListener(
            arealojista.RecyclerTouchListener(
                this,
                recyclerView,
                object : arealojista.ClickListener {

                    override fun onClick(view: View, position: Int) {

                        //bd
                        val plus = position*7
                        val bdPet = petShops.get(plus+3)
                        MapsModels.bdDoPet =bdPet
                        //este método está ajustando o botão central que muda de imagem. Dentro dele está o click da loja
                        //aqui abre a loja
                        centralBtnMarkerToSeta_MapaToLoja("user")
                        layLista.visibility = View.GONE


                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                })
        )


        val btnVoltar: Button = findViewById(R.id.layLista_btnVoltar)
        btnVoltar.setOnClickListener {
            layLista.visibility = View.GONE
            
        }

    }

    fun montaRecyclerViewListaAutonomo(){


        val layLista: ConstraintLayout = findViewById(R.id.layLista)
        layLista.visibility = View.VISIBLE


        var adapter: listaDeAutonomosRecyclerViewAdapter = listaDeAutonomosRecyclerViewAdapter(this, arrayAutonomosNomeBdParaLista)

//chame a recyclerview
        var recyclerView: RecyclerView = findViewById(R.id.layLista_recyclerView_autonomos)

//define o tipo de layout (linerr, grid)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)

//coloca o adapter na recycleview
        recyclerView.adapter = adapter

        recyclerView.layoutManager = linearLayoutManager

// Notify the adapter for data change.
        adapter.notifyDataSetChanged()

        //constructor: context, nomedarecycleview, object:ClickListener
        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView.addOnItemTouchListener(
            arealojista.RecyclerTouchListener(
                this,
                recyclerView,
                object : arealojista.ClickListener {

                    override fun onClick(view: View, position: Int) {

                        //bd
                        val tokens = StringTokenizer(arrayAutonomosNomeBdParaLista.get(position), "!?!00!")
                        val nome = tokens.nextToken() // aqui sai o nome
                        val bd = tokens.nextToken()

                        val intent = Intent(this@MapsActivity, autonomoPublicPerfil::class.java)
                        intent.putExtra("autonomoBD", bd)
                        intent.putExtra("userBD", MapsModels.userBD)
                        intent.putExtra("userMail", MapsModels.userMail)
                        var libera = "nao"
                        if (MapsModels.liberaServico){
                            libera="sim"
                        }
                        intent.putExtra("liberado", libera)
                        startActivity(intent)
                        layLista.visibility = View.GONE
                        finish()


                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                })
        )


        val btnVoltar: Button = findViewById(R.id.layLista_btnVoltar)
        btnVoltar.setOnClickListener {
            layLista.visibility = View.GONE

        }

    }

    fun btnBuscaPorEndClick (){

        val btnBuscaEnd: Button = findViewById(R.id.btnInserirEndereco)
        val layTotal: ConstraintLayout = findViewById(R.id.laySearchAdress)
        val laypopup: ConstraintLayout = findViewById(R.id.constraintLayout21)
        val animCames = AnimationUtils.loadAnimation(this, R.anim.anim_comes_from_verybottom)
        val animGoes = AnimationUtils.loadAnimation(this, R.anim.anim_goes_to_verybottom)


        btnBuscaEnd.setOnClickListener {

            if (layTotal.visibility == View.VISIBLE){

                laypopup.startAnimation(animGoes)
                animGoes.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(animation: Animation?) {

                    }

                    override fun onAnimationEnd(animation: Animation?) {

                        layTotal.visibility = View.GONE //esta tela é o fundo transparente
                    }

                    override fun onAnimationStart(animation: Animation?) {

                    }

                })

            } else {
                layTotal.visibility = View.VISIBLE
                laypopup.startAnimation(animCames)
                metBuscaPorEndereco()
            }

        }

    }

    fun metBuscaPorEndereco (){


        val btnBuscar : Button = findViewById(R.id.laySearchbtnBuscar)
        btnBuscar.setOnClickListener {

            val etEnd: EditText = findViewById(R.id.laySearchEnd)

            if (etEnd.text.isEmpty()){
                etEnd.requestFocus()
                etEnd.setError("Informe o endereço")
            } else {
                hideKeyboard()
                val location = MapsController.findLocationFromAdress(etEnd.text.toString(), this)
                if (location!=null){
                    markLocationFromAdress(location)
                } else {
                    MapsController.makeToast("Endereço não foi encontrado.", this)
                }

            }

        }


        val btnFechar: Button = findViewById(R.id.laySearchBtnFechar)
        btnFechar.setOnClickListener {
            val btnBuscaEnd: Button = findViewById(R.id.btnInserirEndereco)
            btnBuscaEnd.performClick()
        }

    }

    fun markLocationFromAdress(location: Address){

        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
        val latLong = LatLng(location.latitude, location.longitude)

        var circle: Circle
        circle= mMap.addCircle(

            CircleOptions()
                .center(latLong)
                .radius((10).toDouble()) //=10 km
                .strokeColor(Color.BLUE)
                .fillColor(Color.BLUE)
        )


        circle= mMap.addCircle(

            CircleOptions()
                .center(latLong)
                .radius((MapsModels.raioUser).toDouble()) //=10 km
                .strokeColor(Color.CYAN)
                .fillColor(ContextCompat.getColor(this!!, R.color.azulClarotransp))
        )

        val btnBuscaEnd: Button = findViewById(R.id.btnInserirEndereco)
        btnBuscaEnd.performClick()//fecha a janela


        queryPetsNerbyFromGiverLocation(location.latitude, location.longitude, 1)



        val paisanuncio: String = MapsController.getAddressOnlyPaisParaAnuncioProprio(latLong, this)
        if (paisanuncio.equals("nao")){
            //mantem anuncio adMob pois nao identificou corretamente o país
        } else {
            queryAnunciosPropriosNivelPais(paisanuncio, latLong)
        }

    }

    fun queryPetsNerbyFromGiverLocation(lat: Double, long: Double, multiple: Int) {

        //o valor 0.01f equivale a 1 km em latlent (soma de latitude e longitude)

        var latlong = lat + long  //esta latlong é um double para calculos

        var startAtval = latlong-(0.01f*0.6*multiple)
        var endAtval = latlong+(0.01f*0.6*multiple)

        /*
        Log.d("teste", "inicio testes")
        Log.d("teste", "startAtVal com 6 km"+startAtval)
        Log.d("teste", "endatVal com 6 km"+endAtval)
        Log.d("teste", "latlong do user"+latlong)
        var dif1 = latlong-endAtval
        Log.d("teste", "diferença entre posicao user e endatval"+dif1)
        dif1 = latlong-startAtval
        Log.d("teste", "diferença entre posicao user e startAtVal"+dif1)

        var doublehere =-65.976439    //-65.976439‬
        dif1 = latlong-doublehere
        Log.d("teste", "diferença entre posicao user o local mais longe a esquerda"+dif1)
        Log.d("teste", "startAtVal somado com a diferença acima para ver se chega no ponto certo é "+(dif1-startAtval))

        doublehere =-65.862351
        dif1 = latlong-doublehere
        Log.d("teste", "diferença entre posicao user o local mais longe a direita"+dif1)



        Log.d("teste", "neste latlong...4 km para esquerda no google maps seria "+"-65.940220")
        Log.d("teste", "neste latlong...4 km para direita no google maps seria "+"-65862351")
        Log.d("teste", "fim dos testes")
         */

        //nova regra de ouro
        //Por conta das características da latitude e longitude, nao podemos usar o mesmo valor para startAtVal (pois fica a esquerda) e endAtVal(que fica a direita).
        //O que ocorre é que itens que ficam a esquerda acumulam a soma de valores negativos de latitude e longitude. Já os que ficam em endVal pegam o valor negativo da longitude mas as vezes pega positivo de latitude. Isso dava resulltado no final.
        //OBS: Isso é verdade no ocidente. Se um dia quem sabe passar pro oriente.
        //Então agora o que vamos fazer.
        //a val dif armazena a diferença que encontramos entre startatVal e até onde faria 6km no mapa. Se alguim dia for mudar o raio (agora é 0.6) vai ter que mexer nisso.
        //entao basta adiconar essa diferença a startAtVal antes da busca para ele corrigir o erro. A verificar se isto também precisa ser feito para endAtAval.


        startAtval = (MapsModels.dif+startAtval) //ajuste

        getInstance().reference.child("petshops").orderByChild("latlong").startAt(startAtval).endAt(endAtval)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.exists()) {
                        for (querySnapshot in dataSnapshot.children) {

                            Log.d("teste", "entrou")

                            var values: String
                            values = querySnapshot.child("nome").value.toString()  //pos0
                            petShops.add(values)
                            values = querySnapshot.child("lat").value.toString() //pos1
                            petShops.add(values)
                            values = querySnapshot.child("long").value.toString() //pos2
                            petShops.add(values)
                            values = querySnapshot.key.toString() //pos3
                            petShops.add(values)
                            values = querySnapshot.child("raio_entrega").value.toString() //pos4
                            petShops.add(values)
                            values = querySnapshot.child("plano").value.toString() //pos4
                            petShops.add(values)
                            values = querySnapshot.child("impulsionamentos").value.toString() //pos4
                            petShops.add(values)

                            //IMPORTANTE
                            //ao aumentar o número de itens aqui, aumente o contador em placePetShopsInMap()
                            /*
                            PetShop     pos     item
                                        0       nome
                                        1       latitude
                                        2       longitude
                                        3       bd
                                        4       raio_entrega
                                        5       plano
                             */

                            //posicao++ reforma 3

                        }
                    } else {

                        if (multiple==1){
                            queryPetsNerbyFromGiverLocation(lat, long, 2)
                        } else {
                            openPopUp(
                                "Que pena",
                                "Não existem clinicas ou petshops cadastrados perto do endereço informado.",
                                false,
                                "n",
                                "n",
                                "n"
                            )
                        }

                    }

                    //EncerraDialog()
                    placePetShopsInMap("fromAddress")

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })  //addValueEventListener


    }

    fun menuClicks (){

        val menu: ConstraintLayout = findViewById(R.id.lay_menu)

        val btnMenu: ImageView = findViewById(R.id.lay_Maps_MenuBtn) //é um imageview porque tem uma animação drawable
        //menu
        //val mMenuBtn : ImageView = findViewById(R.id.lay_Maps_MenuBtn)
        btnMenu.setOnClickListener {

            val menuInAnim = AnimationUtils.loadAnimation(this, R.anim.menuin)
            val menuOutAnim = AnimationUtils.loadAnimation(this, R.anim.menuout)

            if (menu.visibility == View.GONE){
                menu.startAnimation(menuInAnim)
                menu.visibility = View.VISIBLE
                (btnMenu.drawable as AnimatedVectorDrawable).start()
            } else {
                menu.startAnimation(menuOutAnim)
                menu.visibility = View.GONE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    (btnMenu.drawable as AnimatedVectorDrawable).reset()
                } else {
                    (btnMenu.drawable as AnimatedVectorDrawable).start()
                }
                val btnImpulso : Button = findViewById(R.id.btnAbreImpulso)
                btnImpulso.setBackgroundResource(R.drawable.ic_sms_failed_black_24dp)
            }

            /*
            if (state==0){
                menu.visibility = View.VISIBLE
                state = 1
                (btnMenu.drawable as AnimatedVectorDrawable).start()
            } else {
                menu.visibility = View.GONE
                state = 0
                (btnMenu.drawable as AnimatedVectorDrawable).reset()
            }
             */

        }

        val btnFecharMenuInside: Button = findViewById(R.id.layMeny_btnFecharInside)
        btnFecharMenuInside.setOnClickListener {
            btnMenu.performClick()
        }

        val btnLogout: Button = findViewById(R.id.mapsLogoutBtn)
        btnLogout.setOnClickListener {
            btnMenu.performClick()
            val mySharedPrefs: mySharedPrefs = mySharedPrefs(this)
            mySharedPrefs.clearSharedPreference()
            logout()
        }

        val btnMinhasCompras: Button = findViewById(R.id.menu_btnMinhasCompras)
        btnMinhasCompras.setOnClickListener {

            if (MapsModels.userMail.equals("semLogin")){
                //precisa fazer o login
                openPopUpLogin("Você não está logado", "Para acessar esta função você precisa se registrar.", "Fazer login", "Cancelar")
                btnMenu.performClick()
            } else {
                btnMenu.performClick()
                val intent = Intent(this, minhasComprasActivity::class.java)
                intent.putExtra("userBD", MapsModels.userBD)
                startActivity(intent)
            }

        }

        val btnProprietario:Button = findViewById(R.id.menu_btn1)
        btnProprietario.setOnClickListener {
            if (MapsModels.userBD!="nao"){


                //vamos testar as permissões já aqui para não dar erro depois dentro da classe
                if (cameraPermissions.hasPermissions(this)){
                    //proceed with code

                    btnMenu.performClick()
                    val intent = Intent(this, arealojista::class.java)
                    intent.putExtra("userBD", MapsModels.userBD)
                    intent.putExtra("alvara", MapsModels.alvara)
                    intent.putExtra("tipo", MapsModels.tipo)
                    intent.putExtra("email", MapsModels.userMail)
                    if (!MapsModels.petBDseForEmpresario.equals("nao")){
                        intent.putExtra("petBD", MapsModels.petBDseForEmpresario)
                    }
                    startActivity(intent)
                    finish()

                } else {
                    cameraPermissions.requestPermission(this, CAMERA_PERMISSION_CODE)
                    //writeFilesPermissions.requestPermission(this, WRITE_PERMISSION_CODE)
                    //readFilesPermissions.requestPermission(this, READ_PERMISSION_CODE)
                }

            } else {
                Toast.makeText(this, "Aguarde, suas informações ainda não foram carregadas. Isto depende de sua conexão com a internet.", Toast.LENGTH_SHORT).show()
            }
        }

        val btnMinhasVendas:Button = findViewById(R.id.menu_btnMinhasVendas)
        btnMinhasVendas.setOnClickListener {
            btnMenu.performClick()//fecha o menu e ajusta animação
            val intent = Intent(this, minhasVendas::class.java)
            intent.putExtra("userBD", MapsModels.userBD)
            intent.putExtra("tipo", MapsModels.tipo)
            intent.putExtra("petBD", MapsModels.petBDseForEmpresario)
            startActivity(intent)

        }

        val btnMinhaLoja: Button = findViewById(R.id.menu_btnMinhaLoja)
        btnMinhaLoja.setOnClickListener {
            if (!MapsModels.petBDseForEmpresario.equals("nao")){
                btnMenu.performClick()
                centralBtnMarkerToSeta_MapaToLoja("prop")
                queryDetalhesDaLoja(MapsModels.petBDseForEmpresario, "prop")

            }
        }

        val btnAutonomos: Button = findViewById(R.id.menu_btnAutonomo)
        btnAutonomos.setOnClickListener {
            val intent = Intent(this, autonomosActivity::class.java)
            intent.putExtra("userBD", MapsModels.userBD)
            intent.putExtra("tipo", MapsModels.tipo)
            //intent.putExtra("petBD", petBDseForEmpresario)
            startActivity(intent)

        }

        val btnAdocao: Button = findViewById(R.id.menu_btnAdocao)
        btnAdocao.setOnClickListener {
            val intent = Intent(this, adocaoActivity::class.java)
            intent.putExtra("userBD", MapsModels.userBD)
            val currentLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
            val mycity = MapsController.getAddressOnlyCidadeParaAnuncioProprio(currentLatLng, this)
            if (mycity.isEmpty()){
                //OBS: se for nao informar que nao tem animais na sua cidade
                intent.putExtra("cidade", "nao")
            } else {
                intent.putExtra("cidade", mycity)
            }
            var libera = "nao"
            if (MapsModels.liberaServico){
                libera="sim"
            }
            intent.putExtra("liberado", libera)
            startActivity(intent)
        }


        val btnPerfil: Button = findViewById(R.id.menu_btnPerfil)
        btnPerfil.setOnClickListener {

            if (this@MapsActivity::lastLocation.isInitialized && !MapsModels.userBD.equals("nao")) {

                val intent = Intent(this, userPerfilActivity::class.java)
                intent.putExtra("userBD", MapsModels.userBD)
                intent.putExtra("img", MapsModels.imgDoUser)
                intent.putExtra("userMail", MapsModels.userMail)
                intent.putExtra("tipo", "meuPerfil")
                val lat: String = lastLocation.latitude.toString()
                val long: String = lastLocation.longitude.toString()
                val latlong: String = ((lat).toDouble() + (long).toDouble()).toString()
                intent.putExtra("latlong", latlong)
                startActivity(intent)
                finish()

            } else{
                MapsController.makeToast("Aguarde. Ainda estamos carregando suas informações", this)
            }

        }
    }


    //busca informações iniciais do usuario
    fun queryUserInitial() {


            Log.d("teste", "entrou na query mesmo assim")

            val rootRef = databaseReference.child("usuarios")
            rootRef.orderByChild("email").equalTo(MapsModels.userMail).limitToFirst(1)
                //getInstance().reference.child("usuarios").orderByChild("email").equalTo(userMail)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (querySnapshot in dataSnapshot.children) {

                            if (dataSnapshot == null) {
                                //EncerraDialog()
                                //criar usuário
                                //createUser()
                            } else {

                                //carregar infos
                                val mySharedPrefs: mySharedPrefs = mySharedPrefs(this@MapsActivity)
                                var values: String
                                values = querySnapshot.child("userBD").value.toString()
                                MapsModels.userBD = values
                                mySharedPrefs.setValue("userBdInicial", values)


                                values = querySnapshot.child("tipo").value.toString()
                                MapsModels.tipo = values
                                mySharedPrefs.setValue("tipo", values)

                                values = querySnapshot.child("avaliacoes").value.toString()
                                val qntAval = values.toInt()
                                if (qntAval > 0) {
                                    MapsModels.liberaServico = true
                                    mySharedPrefs.setValue("liberaServicoInicial", "1")
                                } else {
                                    mySharedPrefs.setValue("liberaServicoInicial", "0")
                                }



                                if (!querySnapshot.child("img").exists()){
                                    databaseReference.child("usuarios").child(MapsModels.userBD).child("img").setValue("nao")
                                    MapsModels.imgDoUser="nao"
                                    mySharedPrefs.setValue("imgInicial", "nao")
                                } else {
                                    MapsModels.imgDoUser = querySnapshot.child("img").value.toString()
                                    mySharedPrefs.setValue("imgInicial", MapsModels.imgDoUser)
                                }

                                if (MapsModels.tipo.equals("autonomo")) {

                                    val sit = querySnapshot.child("situacao").value.toString()

                                    if (sit.equals("analise")) {  //se estiver em analise nao vamos exibi-lo
                                            //do nothing
                                        MapsController.makeToast("Seu pedido ainda está em análise. Você ainda não aparece no mapa.", this@MapsActivity)
                                    } else {

                                        val servico =
                                                querySnapshot.child("servico").value.toString()

                                        values = querySnapshot.child("apelido").value.toString()

                                        liberaBotoesAutonomo()
                                        MapsModels.updateAutonomosStatus("online", servico, values, lastLocation)
                                        queryGetAutonomoAditionalInfo()
                                        if (this@MapsActivity::lastLocation.isInitialized){
                                                MapsModels.updateUserStatus("offline", "bla", lastLocation, this@MapsActivity)
                                        }
                                        fimDeTudo()
                                        val btn: Button = findViewById(R.id.menu_btnMinhasVendas)
                                        btn.visibility = View.VISIBLE
                                        findViewById<Button>(R.id.menu_btnAutonomo).setText("Editar informações de prestação de serviço")

                                    }
                                } else {
                                    if (this@MapsActivity::lastLocation.isInitialized){
                                        fimDeTudo()

                                            //se chegou aqui ao ponto de colocar o user online, todos os metodos iniciais ja foram apliados
                                            MapsModels.updateUserStatus("online", MapsModels.imgDoUser, lastLocation, this@MapsActivity)
                                            //placeUserInMap()
                                    }
                                        //placeUserInMap()
                                }



                                findUsersNerby(lastLocation.latitude, lastLocation.longitude)
                                findAutonomosNerby(lastLocation.latitude, lastLocation.longitude)


                                //values = querySnapshot.child("tipo").value.toString()
                                //tipo = values  //reforma1
                                if (MapsModels.tipo.equals("empresario")) {
                                    MapsModels.tipo = "empresario"
                                    values = querySnapshot.child("petBD").value.toString()
                                    MapsModels.petBDseForEmpresario = values
                                    mySharedPrefs.setValue("petBdSeForEmpresarioInicial", MapsModels.petBDseForEmpresario)

                                    var btn: Button
                                    btn = findViewById(R.id.menu_btn1)
                                    btn.visibility = View.VISIBLE
                                    btn = findViewById(R.id.menu_btnMinhasVendas)
                                    btn.visibility = View.VISIBLE

                                    //pega os dados do petshop
                                    queryPetDoEmpresario()
                                    QueryPedidosParaFinalizarDoEmpresario()
                                } else {
                                    val btn: Button = findViewById(R.id.menu_btn1)
                                    btn.setText("    É proprietário?")
                                    btn.visibility = View.VISIBLE
                                    QueryPedidosParaFinalizar()
                                }

                                rootRef.removeEventListener(this)
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

    //se o usuario for empresário, pega aqui informações do petshop dele
    fun queryPetDoEmpresario() {

        getInstance().reference.child("petshops").orderByChild("BDdoDono").equalTo(MapsModels.userBD).limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
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
                            values = querySnapshot.child("lat").value.toString()
                            if (values.equals("nao")){

                                val logradouro = querySnapshot.child("logradouro").value.toString()
                                val numero = querySnapshot.child("numero").value.toString()
                                val bairro = querySnapshot.child("bairro").value.toString()
                                val cidade = querySnapshot.child("cidade").value.toString()
                                val estado = querySnapshot.child("estado").value.toString()
                                val petbd = querySnapshot.child("bd").value.toString()
                                val address = logradouro+" "+numero+", "+bairro+", "+cidade+" - "+estado
                                MapsController.getLatLong(address, petbd, this@MapsActivity)
                            }

                            values = querySnapshot.child("plano").value.toString()
                            MapsModels.plano = values

                            values = querySnapshot.child("alvara").value.toString()
                            MapsModels.alvara = values

                            val itens = querySnapshot.child("itens_venda").value.toString()
                            val logo = querySnapshot.child("logo").value.toString()
                            val raioEntrega = querySnapshot.child("raio_entrega").value.toString()

                            var percent = 0  //20% são as infos básicas como endereço e tal. Então ele sempre parte de 20%.
                            percent = percent+20 //estes 20 são os descritos acima
                            if (!MapsModels.alvara.equals("nao")){
                                percent = percent+20
                            }
                            if (itens.toInt()!=0){
                                percent = percent+20
                            }
                            if (!logo.equals("nao")){
                                percent = percent+20
                            }
                            if (!raioEntrega.equals("nao")){
                                percent=percent+20
                            }

                            val key = querySnapshot.key.toString()

                            if (MapsModels.alvara.equals("nao")){
                                //openPopUp("Atenção", "Você ainda não finalizou o cadastro da sua empresa. Está faltando o alvará. Sem este documento sua empresa não aparece no mapa para os clientes. Clique no menu e depois no botão proprietário. Em seguida entre em Cadastrar Empreendimento para enviar as informações que ainda faltam.", false, "sim", "nao", "lalala")
                                openPopUpNotifyPetShopInfoFaltando("Vamos melhorar sua presença?", "Você ainda não finalizou o cadastro da sua empresa e ficou faltando o alvará. Sem este documento sua empresa não aparece no mapa para os clientes.", true, "Completar", "deixar assim", "alvara", percent, key)
                            } else if (logo.equals("nao")){
                                openPopUpNotifyPetShopInfoFaltando("Vamos melhorar sua presença?", "Percebemos que você ainda não colocou o logo da sua empresa na loja virtual. Vamos fazer isso agora?", true, "Sim, vamos!", "Deixar assim", "logo", percent, key)
                            } else if (itens.toInt()==0){
                                openPopUpNotifyPetShopInfoFaltando("Vamos melhorar sua presença?", "Percebemos que você ainda não cadastrou nenhum produto. Você pode estar perdendo vendas. Vamos corrigir isto agora?", true, "Sim, vamos!", "Faço depois", "itens", percent, key)
                            } else if (raioEntrega.equals("nao")){
                                openPopUpNotifyPetShopInfoFaltando("Vamos melhorar sua presença?", "Você ainda não definiu o seu raio de entrega. Você pode estar perdendo vendas. Vamos fazer isso agora?", true, "Sim, vamos!", "Faço depois", "raio", percent, key)
                            } else {
                                //do nothing
                            }
                            //libera botões do user
                            liberaBotoesProp()


                            /*
                            if (alvara.equals("nao")){
                                //openPopUp("Atenção", "Você ainda não finalizou o cadastro da sua empresa. Está faltando o alvará. Sem este documento sua empresa não aparece no mapa para os clientes. Clique no menu e depois no botão proprietário. Em seguida entre em Cadastrar Empreendimento para enviar as informações que ainda faltam.", false, "sim", "nao", "lalala")
                                openPopUpNotifyPetShopInfoFaltando("Vamos melhorar sua presença?", "Você ainda não finalizou o cadastro da sua empresa. Está faltando o alvará. Sem este documento sua empresa não aparece no mapa para os clientes.", true, "Sim, completar", "Não, deixar assim", "alvara", percent, key)
                            } else {
                                //libera botões do user
                                liberaBotoesProp()

                            }

                             */


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

    fun liberaBotoesProp(){
        var btn:Button = findViewById(R.id.menu_btnMinhasVendas)
        btn.visibility = View.VISIBLE
        btn = findViewById(R.id.menu_btnMinhaLoja)
        btn.visibility = View.VISIBLE

    }

    fun liberaBotoesAutonomo(){
        var btn:Button = findViewById(R.id.menu_btnMinhasVendas)
        btn.setText("    Pedidos de serviços")
        btn.visibility = View.VISIBLE
        btn.setOnClickListener { null }
        btn.setOnClickListener {
            abreMinhasNotificacoes(1)
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
        val lay_root: ConstraintLayout = findViewById(R.id.lay_maps)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

        //aqui colocamos os ifs com cada call de cada vez que a popup for chamada
        if (call.equals("alvara")) {

        }

    }

    fun openPopUpLogin (titulo: String, texto:String, btnSim: String, btnNao: String) {
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


        txtTitulo.text = titulo
        txtTexto.text = texto

        buttonPopupN.setText(btnNao)
        buttonPopupS.setText(btnSim)
        buttonPopupOk.visibility = View.GONE

        buttonPopupN.setOnClickListener {
            popupWindow.dismiss()
        }

        buttonPopupS.setOnClickListener {
            finish()
        }


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora
            popupWindow.dismiss()
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.lay_maps)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )


    }

    fun logout(){
        auth = FirebaseAuth.getInstance()
        auth.signOut()
        LoginManager.getInstance().logOut()
        val sharedPref: SharedPreferences = getSharedPreferences(getString(R.string.sharedpreferences), 0) //0 é private mode
        val editor = sharedPref.edit()
        editor.clear().apply()

        finish()

    }

    //pega a posição do usuário e marca o circulo no mapa
    private fun getUserLocation(raio: Int, situacao: Int) {

        if (temPermissaoParaGps()) {

            // 1
            if (fineLocationPermission.hasPermissions(this)==false){
                fineLocationPermission.checkPermission(this, FINE_LOCATION_CODE)
            } else {

                mMap.isMyLocationEnabled = true

                // 2
                fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                    // Got last known location. In some rare situations this can be null.
                    // 3

                    if (location != null) {

                        getLocal=true
                        //minha localização
                        lastLocation = location
                        val currentLatLng = LatLng(location.latitude, location.longitude)

                        //vai verificar se tem anuncio no pais e dentro dele vai chamando os outros métodos
                        //estava fazendo verificação várias vezes, todas as vezes que opassava aqui. ENtão vamos fazer uma trava
                        if (MapsModels.checkAds==0) {
                            MapsModels.checkAds=1
                            val paisanuncio: String =
                                MapsController.getAddressOnlyPaisParaAnuncioProprio(currentLatLng, this)
                            if (paisanuncio.equals("nao")) {
                                //mantem anuncio adMob pois nao identificou corretamente o país
                            } else {
                                queryAnunciosPropriosNivelPais(paisanuncio, currentLatLng)
                            }
                        }


                        /*
                        //este bloco é para o anuncio proprio. Verifica a cidade do usuario e faz uma query para ver se tem anuncio nesta cidade. Se tiver, desativa o admob. Dentro desta query ele faz um clicklistener para mandar pro site do anunciante
                        val cidadeAnuncio: String = getAddressOnlyCidadeParaAnuncioProprio(currentLatLng)
                        if (cidadeAnuncio.equals("nao")){
                            //mantem anuncio adMob pois nao achou a cidade
                        } else {
                            //verifica se tem um anuncio proprio. Se tiver vai trocar o adMob pelo anuncio proprio
                            queryAnunciosPropriosNivelCidade(cidadeAnuncio, currentLatLng)
                        }

                         */

                        //ajusta o zoom para aparecer o petshop mais distante na tela do user
                        if (situacao == 3){

                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))
                        }

                        //se situação não for 0, é o priemiro acesso do user. Se for 2, é o botão de centralizar;  obs: Situacao 1 é que nao achou petshops e aumentou o raio de busca. Nao precisa aproximar a cam
                        if (situacao == 0 || situacao == 2) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))
                        }

                        if (situacao == 0) {
                            val circle: Circle
                            circle = mMap.addCircle(

                                CircleOptions()
                                    .center(currentLatLng)
                                    .radius((raio).toDouble()) //=10 km
                                    .strokeColor(Color.CYAN)
                                    .fillColor(ContextCompat.getColor(this!!, R.color.azulClarotransp))
                            )



                            //se for primeiro acesso também achar os usuarios proximos PetFriends
                            //vou tentar levar pra dentro da qiery inicial  REFORMA
                            //findUsersNerby(location.latitude, location.longitude)
                            //findAutonomosNerby(location.latitude, location.longitude)
                        }



                        /*
                    val markerOptions = MarkerOptions()
                    val titleStr = getAddress(currentLatLng)  // add these two lines
                    markerOptions.title(titleStr)
                    mMap.addMarker(markerOptions)

                     */

                        //tirando o caso do user clicar no botão do meio, nao precisa buscar novamente os pets proximos
                        if (situacao != 2) {
                            if (MapsModels.petsNerbyWhereAlredyQueried==false) { //assim garantimos que nao entre duas vezes
                                queryPetsNerby(location.latitude, location.longitude)
                            }
                            MetodosDoImpulsionamentoGerencia(location.latitude, location.longitude)
                            //queryStories(location.latitude, location.longitude)

                        }

                        placeUserInMap()
                        EncerraDialog() //fecha o loading que iniciou em onCreate
                    } else {
                        //para aparelhos antigos não estava encontrando a localização
                        val toast = Toast.makeText(
                            this@MapsActivity,
                            "Não foi possível encontrar sua localização. Ligue o GPS ou Digite o endereço",
                            Toast.LENGTH_LONG
                        )
                        toast.setGravity(Gravity.CENTER, 0, 100)
                        toast.show()
                        val btnBuscaEnd: Button = findViewById(R.id.btnInserirEndereco)
                        btnBuscaEnd.performClick()
                    }
                }


            }

        } else {
            //Não tem permissão
            RequestGpsPermission()
        }

    }

    //esta query não tem função para o usuário. Ela está aqui para corrigir erros. Pets que por algum motivo tenham cadastro mas ainda não tenham latlong (e por isso nao aparecem na busca) serão corrigidos. Nenhuma função depende desta busca e nem precisa aparecer para o usuário os pets que forem corrigidos aqui
    fun queryPetsSemLatLong() {

        getInstance().reference.child("petshops").orderByChild("latlong")
            .equalTo("nao")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //EncerraDialog()
                        } else {

                            var values: String
                            values = querySnapshot.key.toString() //pos3

                            var endereco: String
                            var vez :String
                            vez = querySnapshot.child("logradouro").value.toString()
                            endereco = vez
                            vez = querySnapshot.child("numero").value.toString()
                            endereco = endereco+" "+vez
                            vez = querySnapshot.child("bairro").value.toString()
                            endereco = endereco+", "+vez
                            vez = querySnapshot.child("cidade").value.toString()
                            endereco = endereco+", "+vez
                            vez = querySnapshot.child("estado").value.toString()
                            endereco = endereco+"- "+vez
                            //val address = logradouro+" "+numero+", "+bairro+", "+cidade+" - "+estado
                            values = querySnapshot.key.toString() //pos3

                            MapsController.getLatLong(endereco, values, this@MapsActivity)

                            //getLatLong(endereco, values) reforma


                        }
                    }

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })


    }

    //query para achar os petshops próximos.
    //aqui nós somamos a latitude e longitude. Buscamos por esse valor somado. E adicionamos e reduzimos valores para fazer a busca
    //0,01 representa 1km em latitude e longitude.
    fun queryPetsNerby(lat: Double, long: Double) {

        MapsModels.petsNerbyWhereAlredyQueried=true

        //o valor 0.01f equivale a 1 km em latlent (soma de latitude e longitude)

        var latlong = lat + long
        //inicio codigo antigo
        //val startAtval = latlong-(0.01f*0.7)  //0.7 vai equivaler a 7 km  0.8 = 8km
        //val endAtval = latlong+(0.01f*0.7)

        var startAtval = latlong-(0.01f*MapsModels.raioBusca)
        var endAtval = latlong+(0.01f*MapsModels.raioBusca)


        if (isNetworkAvailable(this)) {
            //getInstance().reference.child("petshops").orderByChild("latlong").startAt(latlong - (0.01f * raio)).endAt(latlong + (0.01f * raio))
            animaLoad()
            //nova regra de ouro
            //Por conta das características da latitude e longitude, nao podemos usar o mesmo valor para startAtVal (pois fica a esquerda) e endAtVal(que fica a direita).
            //O que ocorre é que itens que ficam a esquerda acumulam a soma de valores negativos de latitude e longitude. Já os que ficam em endVal pegam o valor negativo da longitude mas as vezes pega positivo de latitude. Isso dava resulltado no final.
            //OBS: Isso é verdade no ocidente. Se um dia quem sabe passar pro oriente.
            //Então agora o que vamos fazer.
            //a val dif armazena a diferença que encontramos entre startatVal e até onde faria 6km no mapa. Se alguim dia for mudar o raio (agora é 0.6) vai ter que mexer nisso.
            //entao basta adiconar essa diferença a startAtVal antes da busca para ele corrigir o erro. A verificar se isto também precisa ser feito para endAtAval.


            startAtval = (MapsModels.dif+startAtval) //ajuste
            getInstance().reference.child("petshops").orderByChild("latlong").startAt(startAtval)
                .endAt(endAtval)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        if (dataSnapshot.exists()) {
                            for (querySnapshot in dataSnapshot.children) {

                                var values: String
                                var nome: String
                                values = querySnapshot.child("nome").value.toString()  //pos0
                                petShops.add(values)
                                nome = values
                                values = querySnapshot.child("lat").value.toString() //pos1
                                petShops.add(values)
                                values = querySnapshot.child("long").value.toString() //pos2
                                petShops.add(values)
                                values = querySnapshot.key.toString() //pos3
                                petShops.add(values)
                                values = querySnapshot.child("raio_entrega").value.toString() //pos4
                                petShops.add(values)
                                values = querySnapshot.child("plano").value.toString() //pos4
                                petShops.add(values)
                                values =
                                    querySnapshot.child("impulsionamentos").value.toString() //pos4
                                petShops.add(values)

                                values = querySnapshot.child("long").value.toString() //pos2


                                //IMPORTANTE
                                //ao aumentar o número de itens aqui, aumente o contador em placePetShopsInMap()
                                /*
                            PetShop     pos     item
                                        0       nome
                                        1       latitude
                                        2       longitude
                                        3       bd
                                        4       raio_entrega
                                        5       plano
                             */


                                //aqui é para o caso do petshop estar sem latitude ou longitude definido. Então vai fazer este gadastro
                                if (petShops.get(MapsModels.posicao + 1).toString().equals("nao")) {

                                    var endereco: String
                                    var vez: String
                                    vez = querySnapshot.child("logradouro").value.toString()
                                    endereco = vez
                                    vez = querySnapshot.child("numero").value.toString()
                                    endereco = endereco + " " + vez
                                    vez = querySnapshot.child("bairro").value.toString()
                                    endereco = endereco + ", " + vez
                                    vez = querySnapshot.child("cidade").value.toString()
                                    endereco = endereco + ", " + vez
                                    vez = querySnapshot.child("estado").value.toString()
                                    endereco = endereco + "- " + vez
                                    //val address = logradouro+" "+numero+", "+bairro+", "+cidade+" - "+estado

                                    //aqui dentro ele salva e faz tudo
                                    MapsController.getLatLong(endereco, petShops.get(MapsModels.posicao + 3), this@MapsActivity)
                                    EncerraDialog()
                                }

                                //posicao++ reforma 3

                            }
                        } else {

                            EncerraDialog()
                            //openPopUp("Que pena", "Não existem clinicas ou petshops perto de você. Vamos aumentar o raio da busca?", false, "n", "n","n")
                            if (MapsModels.raioBusca < 10.0) {

                                //ChamaDialog()
                                //val km = (raioBusca * 10).toInt()
                                MapsModels.raioBusca = MapsModels.raioBusca + 5.0
                                MapsModels.raioUser = MapsModels.raioUser + 7000
                                //ao chamar aqui coloca os petshops no mapa.
                                MapsModels.petsNerbyWhereAlredyQueried=false
                                //getUserLocation(MapsModels.raioUser, 1)
                          //      animaLoad()
                                queryPetsNerby(lat, long)

                            } else {
                                openPopUp(
                                    "Que pena",
                                    "Não existem clinicas ou petshops perto de você num raio de 10 km.",
                                    false,
                                    "n",
                                    "n",
                                    "n"
                                )

                                //animaLoad()
                            }

                        }

                        //EncerraDialog()
                        placePetShopsInMap("normal")
                        //animaLoad()
                        if (MapsModels.raioBusca > 10.0) {
                            //AGORA EXISTE UM METODO QUE TRATA DISTO EM CALCULATEZOOMTOFIT. ELE É CHAMADO DE PLACEPETSHOPSONMAP
                            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlong, 15f))
                            // mMap.animateCamera(CameraUpdateFactory.zoomTo((raioBusca * 1.5).toFloat()))
                            //mMap.animateCamera(CameraUpdateFactory.zoomTo((17.0f).toFloat()))
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Getting Post failed, log a message

                        // ...
                    }
                })

            animaLoad() //este fica

        } else {
            animaLoad()  //este fica
            MapsController.makeToast("Você está sem internet", this)
        }

    }

    //Métodos DO IMPULSIONAMENTO

    fun MetodosDoImpulsionamentoGerencia (lat: Double, long: Double){

        val lay: ConstraintLayout = findViewById(R.id.lay_impulsionado)
        val btnFecharImpulso: Button = findViewById(R.id.impulsionamentos_btnfechar)

        //ao ser clicado abre a loja e coloca o produto já no carrinho para ser comprado
        lay.setOnClickListener {
            //abre o pet com este bd
            centralBtnMarkerToSeta_MapaToLoja("impulso")
            queryDetalhesDaLoja(MapsModels.bdDoImpulsionamento, "user")

            //vamos adicionar ao carrinho com o preço promocional
            var txt: TextView = findViewById(R.id.impulsionado_nome)
            arrayNomesCarrinho.add(txt.text.toString())

            //separando desc de img
            txt = findViewById(R.id.impulsionado_desc) //aqui armazenei descricao:!:Imagemlink
            val tokens = StringTokenizer(txt.text.toString(), "*")
            val desc = tokens.nextToken() // this will contain "Fruit"
            val img = tokens.nextToken() // this will contain " they
            arrayImgCarrinho.add(img)

            txt = findViewById(R.id.impulsionando_preco)
            arrayPrecoCarrinho.add(txt.text.toString())

            arrayDescCarrinho.add(desc)

            arrayBDCarrinho.add(MapsModels.bdDoImpulsionamento)  //que é igual do pet


            val layCarrinho : ConstraintLayout = findViewById(R.id.lay_carrinho)
            //layCarrinho.visibility = View.VISIBLE
            var btnAbrirCarrinho : Button = findViewById(R.id.lay_loja_btnAbrirCarrinho)
            btnAbrirCarrinho.performClick()

            //verificar aqui tipoProdParaImpulso
            CalculaTotalCompra(MapsModels.bdDoImpulsionamento)

            layCarrinho.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    // ignore all touch events
                    return true
                }
            })

            ChamaDialog()
            val rootRef = databaseReference.child("petshops").child(MapsModels.bdDoImpulsionamento).child("produtos")
            rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    //TODO("Not yet implemented")
                    EncerraDialog()
                }

                override fun onDataChange(p0: DataSnapshot) {
                    //TODO("Not yet implemented")
                    var values: String
                    values = p0.child("servicos").child("entrega").getValue().toString()


                    SetUpRecycleViewDoCarrinho(MapsModels.tipoProdParaImpulso)
                    EncerraDialog()
                }
            })

            MapsModels.popupAberta = false

            //fim de adicionar ao carrinho
            btnFecharImpulso.performClick()
        }

        val btnAbreImpulso: Button = findViewById(R.id.btnAbreImpulso)
        btnFecharImpulso.setOnClickListener {

            lay.visibility = View.GONE
            btnAbreImpulso.visibility = View.VISIBLE
        }

        btnAbreImpulso.setOnClickListener {
            val lay: ConstraintLayout = findViewById(R.id.lay_impulsionado)
            lay.visibility = View.VISIBLE
            btnAbreImpulso.visibility = View.GONE
        }

        val txt: TextView = findViewById(R.id.impulsionado_nome)
        txt.setText("testando novo")
        queryimpulsionamentoNerby(lat, long)
    }

    fun queryimpulsionamentoNerby(lat: Double, long: Double) {

        //o valor 0.01f equivale a 1 km em latlent (soma de latitude e longitude)


        var latlong = lat + long

        var startAtval = latlong-(0.01f*(MapsModels.raioBusca*2))
        var endAtval = latlong+(0.01f*(MapsModels.raioBusca*2))

        //nova regra de ouro
        //Por conta das características da latitude e longitude, nao podemos usar o mesmo valor para startAtVal (pois fica a esquerda) e endAtVal(que fica a direita).
        //O que ocorre é que itens que ficam a esquerda acumulam a soma de valores negativos de latitude e longitude. Já os que ficam em endVal pegam o valor negativo da longitude mas as vezes pega positivo de latitude. Isso dava resulltado no final.
        //OBS: Isso é verdade no ocidente. Se um dia quem sabe passar pro oriente.
        //Então agora o que vamos fazer.
        //a val dif armazena a diferença que encontramos entre startatVal e até onde faria 6km no mapa. Se alguim dia for mudar o raio (agora é 0.6) vai ter que mexer nisso.
        //entao basta adiconar essa diferença a startAtVal antes da busca para ele corrigir o erro. A verificar se isto também precisa ser feito para endAtAval.


        startAtval = (MapsModels.dif+startAtval) //ajuste

        getInstance().reference.child("impulsionamentos").orderByChild("latlong").startAt(startAtval).endAt(endAtval)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.exists()) {

                        var cont = 0
                        var maxN = dataSnapshot.childrenCount.toInt()


                        //sortear um numero randomizado
                        val min = 0 //valor minimo que você quer.
                        val max = dataSnapshot.childrenCount.toInt()

                        val n: Int
                        if (max==1){
                            n = 0
                        } else {
                            n = Random().nextInt(max - min + 1) + min
                        }


                        for (querySnapshot in dataSnapshot.children) {

                            if (cont == n) {
                                //pega os valores que eu quero. assim so vai o que sorteei

                                val nome: String
                                val img: String
                                val preco: String
                                var txt: TextView = findViewById(R.id.impulsionado_nome)
                                val imgIV: ImageView = findViewById(R.id.impulsionado_img)
                                var values: String

                                //primeiro vamos verificar se o anuncio já venceu
                                val dataHojeStr = MapsController.GetDate()
                                values = querySnapshot.child("data_final").value.toString()  //pos0

                                val format = SimpleDateFormat("dd/MM/yyyy")

                                val date1 = format.parse(dataHojeStr)
                                val date2 = format.parse(values)

                                //codigo antigo
                                //compara a hora. Se a data 1 (hoje) for maior que  a data_final do anuncio, significa que já venceu. Então tem que apagar o bd.
                                // se retorna 0 é pq é o mesmo dia
                                //se retorna menos que 0, é antes
                                //se retorna mais que 0 é depois
                                if (date1.compareTo(date2) > 0) {

                                    //apagar bd
                                    val key = querySnapshot.key.toString()
                                    //temos que testar isso
                                    databaseReference.child("impulsionamentos").child(key).removeValue()


                                } else if (date1.compareTo(date2) == 0){ //se a data for igual, vamos verifica a hora.

                                    val horaAgoraStr = MapsController.GetHour()
                                    val formathour = SimpleDateFormat("hh:mm")
                                    values = querySnapshot.child("hora_inicio").value.toString()  //pos0
                                    val hora1 = formathour.parse(horaAgoraStr)
                                    val hora2 = formathour.parse(values)

                                    //se a hora de agora for maior do que a hora que começou o anuncio, apagar do bd.
                                    if (hora1.compareTo(hora2) >0){
                                        //apagar, a hora já é maior que a do inicio
                                        val key = querySnapshot.key.toString()
                                        //temos que testar isso
                                        databaseReference.child("impulsionamentos").child(key).removeValue()

                                    } else {


                                        //ATENCAO ESSE CODIGO É COPIADO DO ELSE ABAIXO. eLE OCORRE QUANDO A DATE É A MESMA MAS A HORA AINDA É MENOR. ENTAO TEM QUE EXIBIR. SE ALTERAR ALGO LÁ, ALTERE AQUI
                                        values =
                                            querySnapshot.child("nome_prod").value.toString()  //pos0
                                        //val txt: TextView = findViewById(R.id.impulsionado_nome)
                                        nome = values
                                        txt.setText(nome)
                                        values =
                                            querySnapshot.child("img_prod").value.toString() //pos1
                                        img = values
                                        Glide.with(this@MapsActivity).load(img).centerCrop()
                                            .into(imgIV)
                                        values =
                                            querySnapshot.child("preco").value.toString() //pos2

                                        preco = values
                                        txt = findViewById(R.id.impulsionando_preco)
                                        txt.setText(preco)

                                        values =
                                            querySnapshot.child("preco").value.toString() //pos2


                                        values = querySnapshot.child("desc_prod").value.toString() //pos2
                                        txt = findViewById(R.id.impulsionado_desc)
                                        txt.setText(values.toString()+"*"+img) //este valor é passado apenas para o caso do user clicar e querer comprar. Vai ser pelo txt que pegaremos os valores

                                        values = querySnapshot.child("pet").value.toString() //pos2
                                        MapsModels.bdDoImpulsionamento = values
                                        val layImpulso: ConstraintLayout =
                                            findViewById(R.id.lay_impulsionado)
                                        layImpulso.visibility = View.VISIBLE



                                        values =
                                            querySnapshot.child("tipo").value.toString()
                                        MapsModels.tipoProdParaImpulso = values

                                        val horaInicio = querySnapshot.child("hora_inicio").value.toString()  //vai definir a hora final
                                        val dataLimite = querySnapshot.child("hora_inicio").value.toString()  //vai definir a hora final
                                        val horaAgora = MapsController.GetHour()
                                        val dataAgora = MapsController.GetDate()

                                        val tempoRestante = MapsController.getDifferenceInTwoDates(dataAgora, dataLimite, horaAgora, horaInicio, this@MapsActivity)
                                        val tvHoraRestante: TextView = findViewById(R.id.lay_impulsionado_txtTempo)
                                        tvHoraRestante.setText("Tempo restante: "+tempoRestante)

                                        cont++

                                    }

                                } else {

                                    //ATENCAO, ESTE CODIGO ESTA REPETIDO ACIMA. SE ALTERAR ALGO AQUI, ALTERE LÁ TAMBÉM
                                    values =
                                        querySnapshot.child("nome_prod").value.toString()  //pos0
                                    //val txt: TextView = findViewById(R.id.impulsionado_nome)
                                    nome = values
                                    txt.setText(nome)
                                    values =
                                        querySnapshot.child("img_prod").value.toString() //pos1
                                    img = values
                                    Glide.with(this@MapsActivity).load(img).centerCrop()
                                        .into(imgIV)
                                    values =
                                        querySnapshot.child("preco").value.toString() //pos2

                                    preco = values
                                    txt = findViewById(R.id.impulsionando_preco)
                                    txt.setText(preco)

                                    values = querySnapshot.child("desc_prod").value.toString() //pos2
                                    txt = findViewById(R.id.impulsionado_desc)
                                    txt.setText(values.toString()+"*"+img) //este valor é passado apenas para o caso do user clicar e querer comprar. Vai ser pelo txt que pegaremos os valores


                                    values = querySnapshot.child("pet").value.toString() //pos2
                                    MapsModels.bdDoImpulsionamento = values
                                    val layImpulso: ConstraintLayout =
                                        findViewById(R.id.lay_impulsionado)
                                    layImpulso.visibility = View.VISIBLE

                                    values =
                                        querySnapshot.child("tipo").value.toString()
                                    MapsModels.tipoProdParaImpulso = values

                                    cont++
                                }
                            } else {
                                cont++
                            }


                        }

                        val btnCentraliza : Button = findViewById(R.id.btnLocalizaNovamente)
                        btnCentraliza.performClick()

                    } else {

                        EncerraDialog()
                        //openPopUp("Que pena", "Não existem clinicas ou petshops perto de você. Vamos aumentar o raio da busca?", false, "n", "n","n")
                        if (MapsModels.raioBusca <= 10.0) {

                            if (MapsModels.raioBusca == 0.3) {
                                //showToast()
                            }
                            //ChamaDialog()
                            MapsModels.raioBusca = MapsModels.raioBusca + 5.0
                            MapsModels.raioUser = MapsModels.raioUser + 7000
                            //getUserLocation(raioUser, 1)

                        }

                        //EncerraDialog()
                        //val layImpulso : ConstraintLayout = findViewById(R.id.lay_impulsionado)
                        //layImpulso.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })   //addValueEventListener


    }

    //FIM DOS METODOS DE IMPULSIONAMENTO

    /*
    fun queryPetsNerby(lat: Double, long: Double, raio: Int) {

        //o valor 0.01f equivale a 1 km em latlent (soma de latitude e longitude)

        var latlong = lat + long
        //inicio codigo antigo
        //val startAtval = latlong-(0.01f*0.7)  //0.7 vai equivaler a 7 km  0.8 = 8km
        //val endAtval = latlong+(0.01f*0.7)

        var startAtval = latlong-(0.01f*raioBusca)
        var endAtval = latlong+(0.01f*raioBusca)

        //getInstance().reference.child("petshops").orderByChild("latlong").startAt(latlong - (0.01f * raio)).endAt(latlong + (0.01f * raio))
        getInstance().reference.child("petshops").orderByChild("latlong").startAt(startAtval).endAt(endAtval)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot.exists()) {



                            EncerraDialog()
                            openPopUp("Que pena", "Não existem clinicas ou petshops perto de você. Vamos aumentar o raio da busca?", false, "n", "n","n")
                            raioBusca=raioBusca+0.2
                            raioUser= raioUser+2000
                        } else {


                            var values: String
                            values = querySnapshot.child("nome").value.toString()  //pos0
                            petShops.add(values)
                            values = querySnapshot.child("lat").value.toString() //pos1
                            petShops.add(values)
                            values = querySnapshot.child("long").value.toString() //pos2
                            petShops.add(values)
                            values = querySnapshot.key.toString() //pos3
                            petShops.add(values)

                            values = querySnapshot.child("latlong").value.toString() //pos2

                            //aqui é para o caso do petshop estar sem latitude ou longitude definido. Então vai fazer este gadastro
                            if (petShops.get(posicao+1).toString().equals("nao")){

                                var endereco: String
                                var vez :String
                                vez = querySnapshot.child("logradouro").value.toString()
                                endereco = vez
                                vez = querySnapshot.child("numero").value.toString()
                                endereco = endereco+" "+vez
                                vez = querySnapshot.child("bairro").value.toString()
                                endereco = endereco+", "+vez
                                vez = querySnapshot.child("cidade").value.toString()
                                endereco = endereco+", "+vez
                                vez = querySnapshot.child("estado").value.toString()
                                endereco = endereco+"- "+vez
                                //val address = logradouro+" "+numero+", "+bairro+", "+cidade+" - "+estado
                                getLatLong(endereco, petShops.get(posicao+3))
                            }

                            posicao++
                            //IMPORTANTE
                            //ao aumentar o número de itens aqui, aumente o contador em placePetShopsInMap()
                            /*
                            PetShop     pos     item
                                        0       nome
                                        1       latitude
                                        2       longitude
                                        3       bd
                             */

                        }
                    }

                    //EncerraDialog()
                    placePetShopsInMap()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })


    }


     */
    //coloca os markers dos petshops na tela
    fun placePetShopsInMap(option: String) {

        //criar uma função para colocar um marker desse para cada petshop

        val size = petShops.size
        var cont = 0
        while (size > cont) {

            val latLng =
                LatLng(petShops.get(cont + 1).toDouble(), petShops.get(cont + 2).toDouble())

            //se for a partir do endereço, não deve ajustar a camera. Pois o ajuste da camera leva em consideração lastlocation e este dado é a posição do usuário. Ou seja, ajustava a camera onde o usuário está e não onde buscou
            if (!option.equals("fromAddress")) {
                if (cont + 7 >= size) { //aqui movimenta o zoom até achar o pet. Neste caso, só vamos fazer isso no ultimo pet carregado, que deve ser o mais longe*** falta verificar


                    val locationPet =
                        Location(LocationManager.NETWORK_PROVIDER) // OR GPS_PROVIDER based on the requirement
                    locationPet.latitude = latLng.latitude
                    locationPet.longitude = latLng.longitude
                    val distancia = lastLocation.distanceTo(locationPet)
                    val zoomAdjusted = MapsController.calculateZoomToFit(distancia)
                    val currentLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)

                    //ajusta o zoom para aparecer o petshop mais distante na tela do user
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            currentLatLng,
                            zoomAdjusted
                        )
                    )
                }
            }

            if (petShops.get(cont+6).equals("sim")){
                val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title(petShops.get(cont+3)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.markericopremiumemoferta)))
                //se quiser adicionar o endereço e telefone na infowindows use .snippet na linha acima
                mark1.tag=0
            } else if (petShops.get(cont+5).equals("premium")){
                val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title(petShops.get(cont+3)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.markericopremium)))
                //se quiser adicionar o endereço e telefone na infowindows use .snippet na linha acima
                mark1.tag=0
            } else {
                val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title(petShops.get(cont+3)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.markerico)))
                //se quiser adicionar o endereço e telefone na infowindows use .snippet na linha acima
                mark1.tag=0
            }

            //adjust zoomm para aparecer todas lojas
            //12 encaixa prra 5km
            //adjustZoomToFit()

            //place raio_entrega as circle
            if (petShops.get(cont+4).toString().equals("nao")) {
                //do nothing
            }else {

                var circle: Circle
                circle= mMap.addCircle(

                    CircleOptions()
                        .center(latLng)
                        .radius((petShops.get(cont+4).toInt()*1000).toDouble())
                        .strokeColor(ContextCompat.getColor(this!!, R.color.laranja))
                        .fillColor(ContextCompat.getColor(this!!, R.color.vermelhotransp))
                )
            }



            cont = cont + 7

        }

        mMap.setOnMarkerClickListener (this)
    }


    //aquyi estão os cliques dos markers vindos do mapa
    override fun onMarkerClick(p0: Marker?): Boolean {
        // Retrieve the data from the marker.
        //https://developers.google.com/maps/documentation/android-sdk/marker


        val bd = p0?.title

        if (bd != null){

            if (bd.equals("Você")) {  //significa que o usuario clicou na imagem dele no mapa. Abrir a página de perfil do user

                //nao precisa disso abaixo pois o titulo é apenas "você"
                //val tokens = StringTokenizer(bd.toString(), "!?!")
                //val descart = tokens.nextToken() // this will contain "Fruit"
                //val bdDoUser = tokens.nextToken() // this will contain " they
                //val img = tokens.nextToken()

                //abrir nova intent com este BD bdDoUser
                /* parece que este codigo eu usei pra testar o autonomo e depois esqueci de apagar
                val intent = Intent(this, autonomoPublicPerfil::class.java)
                intent.putExtra("autonomoBD", userBD)
                intent.putExtra("userBD", userBD)
                intent.putExtra("userMail", userMail) //aqui é o email do usuario mesmo. É para quando voltar a activity
                val lat: String = lastLocation.latitude.toString()
                val long: String = lastLocation.longitude.toString()
                val latlong: String = ((lat).toDouble() + (long).toDouble()).toString()
                intent.putExtra("latlong", latlong)
                var libera = "nao"
                if (liberaServico){
                    libera="sim"
                }
                intent.putExtra("liberado", libera) //aqui é o email do usuario mesmo. É para quando voltar a activity
                 */

                val intent = Intent(this, userPerfilActivity::class.java)
                intent.putExtra("userBD", MapsModels.userBD)
                intent.putExtra("img", MapsModels.imgDoUser)
                intent.putExtra("userMail", MapsModels.userMail)
                intent.putExtra("tipo", "meuPerfil")
                val lat: String = lastLocation.latitude.toString()
                val long: String = lastLocation.longitude.toString()
                val latlong: String = ((lat).toDouble() + (long).toDouble()).toString()
                intent.putExtra("latlong", latlong)
                startActivity(intent)




            } else if (bd.equals("EuAutonomo")) {
                //codigo aqui
                /*
                val intent = Intent(this, autonomosActivity::class.java)
                intent.putExtra("userBD", userBD)
                intent.putExtra("tipo", tipo)
                //intent.putExtra("petBD", petBDseForEmpresario)
                startActivity(intent)
                 */

                //abrir nova intent com este BD bdDoUser
                val intent = Intent(this, autonomoPublicPerfil::class.java)
                intent.putExtra("autonomoBD", MapsModels.userBD)
                intent.putExtra("userBD", MapsModels.userBD)
                intent.putExtra("userMail", MapsModels.userMail) //aqui é o email do usuario mesmo. É para quando voltar a activity
                var libera: String = "nao"
                if (MapsModels.liberaServico){
                    libera = "sim"
                }
                intent.putExtra("liberado", libera)
                //intent.putExtra("tipo", "visitante")
                if (MapsModels.autonomoPlanoPremium){
                    intent.putExtra("planoPremium", "sim")
                } else {
                    intent.putExtra("planoPremium", "nao")
                }

                startActivity(intent)

            } else if (bd.contains("autonomoWorker!?!")){

                val tokens = StringTokenizer(bd.toString(), "!?!")
                val descart = tokens.nextToken() // this will contain "Fruit"
                val bdDoUser = tokens.nextToken() // this will contain " they
                //val img = tokens.nextToken()

                //abrir nova intent com este BD bdDoUser
                val intent = Intent(this, autonomoPublicPerfil::class.java)
                intent.putExtra("autonomoBD", bdDoUser)
                intent.putExtra("userBD", MapsModels.userBD)
                intent.putExtra("userMail", MapsModels.userMail) //aqui é o email do usuario mesmo. É para quando voltar a activity
                var libera = "nao"
                if (MapsModels.liberaServico){
                    libera="sim"
                }
                intent.putExtra("liberado", libera) //aqui é o email do usuario mesmo. É para quando voltar a activity

                //intent.putExtra("tipo", "visitante")
                startActivity(intent)


            } else if (bd.contains("petFriend!?!")){  //significa que o usuário clicou em outro usuário
                //separando desc de img
                //txt = findViewById(R.id.impulsionado_desc) //aqui armazenei descricao:!:Imagemlink
                val tokens = StringTokenizer(bd.toString(), "!?!")
                val descart = tokens.nextToken() // this will contain "Fruit"
                val bdDoUser = tokens.nextToken() // this will contain " they
                val img = tokens.nextToken()
                val latlong = tokens.nextToken()

                //abrir nova intent com este BD bdDoUser
                val intent = Intent(this, userPerfilActivity::class.java)
                intent.putExtra("userBD", bdDoUser)
                intent.putExtra("img", img)
                intent.putExtra("userMail", MapsModels.userMail) //aqui é o email do usuario mesmo. É para quando voltar a activity
                intent.putExtra("tipo", "visitante")
                intent.putExtra("latlong", latlong)
                startActivity(intent)

            } else {  //clicou numa loja

                ChamaDialog()
                MapsModels.bdDoPet = bd
                //este método está ajustando o botão central que muda de imagem. Dentro dele está o click da loja
                centralBtnMarkerToSeta_MapaToLoja("user")

            }

        }

        //return false
        return true
    }

    //aqui ele apenas centraliza a tela. Não faz mais nada, nao muda imagem. NADA
    fun centralBtnApenasLocaliza () {
        val btnCentral: Button = findViewById(R.id.btnLocalizaNovamente)
        //btnCentral.setOnClickListener { null }
        btnCentral.setOnClickListener {


            if (MapsModels.userBD.equals("nao")){

                //vamos ver se tem coisa arquivada e se queremos usar o sharedprefs pra evitar queries desnecessárias o tempo todo
                val sharedPref: SharedPreferences = getSharedPreferences(getString(R.string.sharedpreferences), 0) //0 é private mode
                val editor = sharedPref.edit()

                val x  = sharedPref.getString("userBdInicial", "nao")
                if (!x.equals("nao")) {
                    //O user ainda nao tem sharedprefs ou apagou
                    //verificar tudo aqui
                    MapsModels.userBD = sharedPref.getString("userBdInicial", "nao").toString()
                    MapsModels.tipo = sharedPref.getString("tipoInicial", "nao").toString()
                    val servico = sharedPref.getString("liberaServicoInicial", "0").toString()

                    if (servico.toInt() > 0){
                        MapsModels.liberaServico = true
                    } else {
                        MapsModels.liberaServico = false
                    }

                    if (!MapsModels.tipo.equals("autonomo")){ //se for autonomo vai fazer a query. Isto está la no else. Entao nao precisamos fazer nenhum destes métodos


                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {

                            fineLocationPermission.checkPermission(this, FINE_LOCATION_CODE)

                        } else {

                            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                                // Got last known location. In some rare situations this can be null.
                                // 3

                                if (location != null) {

                                    lastLocation = location

                                    findUsersNerby(lastLocation.latitude, lastLocation.longitude)
                                    findAutonomosNerby(lastLocation.latitude, lastLocation.longitude)
                                    getUserLocation(MapsModels.raioUser, 0)

                                }
                            }

                        }

                    }

                    MapsModels.imgDoUser = sharedPref.getString("imgInicial", "nao").toString()

                    if (this@MapsActivity::lastLocation.isInitialized){
                        MapsModels.updateUserStatus("online", MapsModels.imgDoUser, lastLocation)
                    }

                    placeUserInMap()

                    if (MapsModels.tipo.equals("empresario")){
                        MapsModels.petBDseForEmpresario = sharedPref.getString("petBdSeForEmpresarioInicial", "nao").toString()
                        var btn: Button
                        btn = findViewById(R.id.menu_btn1)
                        btn.visibility = View.VISIBLE
                        btn = findViewById(R.id.menu_btnMinhasVendas)
                        btn.visibility = View.VISIBLE

                        queryPetDoEmpresario()
                        QueryPedidosParaFinalizarDoEmpresario()
                    } else {
                        val btn: Button = findViewById(R.id.menu_btn1)
                        btn.setText("    É proprietário?")
                        btn.visibility = View.VISIBLE
                        QueryPedidosParaFinalizar()
                    }

                } else if (MapsModels.userBD.equals("nao")){
                    queryUserInitial()
                } else if (MapsModels.tipo.equals("autonomo")){ //o autonomo precisa de info atualizada. Então faz query
                    queryUserInitial()
                }


                /*
            if (userBD.equals("nao")){
                queryUserInitial()
            }

                 */


            }

             getUserLocation(MapsModels.raioUser, 2)


        }
    }

    //este método muda o botão do meio para voltar da loja. Proprietários e users tem comportamento diferentes aqui dentro, mas a maior parte do processo é igual. Só diferencia a query
    fun centralBtnMarkerToSeta_MapaToLoja(userOuProp: String){ //user, prop ou impulso (quando o click vier do impulso)

        //chegando aqui significa que o user clicou na loja no mapa. então chama a qieru para começar o processo de montar a loja.
        val btnIVcentral: ImageView = findViewById(R.id.imageView4)
        (btnIVcentral.drawable as AnimatedVectorDrawable).start() //volta a ser icone Marker

        //quando entra na loja esconde estes botões
        val btnMenu: ImageView = findViewById(R.id.lay_Maps_MenuBtn)  //imageview que e o botão de menu
        val btnLupa: Button = findViewById(R.id.btnInserirEndereco) //imagem da busca de endereço
        btnMenu.visibility = View.GONE
        btnLupa.visibility = View.GONE

        if (userOuProp.equals("user")) {
            queryDetalhesDaLoja(MapsModels.bdDoPet, "user")
        }

        btnIVcentral.setImageResource(R.drawable.seta_to_marker)
        //seta um novo clicklistener, agora ao ser clicado ele vai jogar de volta pro maps activity e ele volta a ter funçao apenas de centralizar a tela.
        val btnCentral: Button = findViewById(R.id.btnLocalizaNovamente)
        btnCentral.setOnClickListener { null }

        //chegando aqui
        btnCentral.setOnClickListener {

            var lay: ConstraintLayout
            lay = findViewById(R.id.lay_loja)
            lay.visibility = View.GONE

            //quando sai da loja voltam os botões
            val btnMenu: ImageView = findViewById(R.id.lay_Maps_MenuBtn)  //imageview que e o botão de menu
            val btnLupa: Button = findViewById(R.id.btnInserirEndereco)
            btnMenu.visibility = View.VISIBLE
            btnLupa.visibility = View.VISIBLE

            arrayNomesCarrinho.clear()
            arrayBDCarrinho.clear()
            arrayDescCarrinho.clear()
            arrayImgCarrinho.clear()
            arrayPrecoCarrinho.clear()

            lay = findViewById(R.id.lay_maps)
            lay.visibility = View.VISIBLE

            lojaInfo.clear()

            val btnIVcentral: ImageView = findViewById(R.id.imageView4)
            (btnIVcentral.drawable as AnimatedVectorDrawable).start()

            centralBtnApenasLocaliza()
            btnIVcentral.setImageResource(R.drawable.marker_to_seta)
        }
    }

    fun queryDetalhesDaLoja(BdEscolhido: String, userOuProp: String) {
        //este método pode ser chamado pelo clique normal do usuário no mapa, então no caso o bdEscolhido vem derivado da variavel global bdDoPet.
        //Mas também pode vir do clique do botão "minha loja" no menu. Neste caso é o proprietário que quer olhar a loja. Aí vem do bdDoEmpresario


        ChamaDialog()
        //val rootRef = databaseReference.child("petshops").child(bdDoPet)
        val rootRef = databaseReference.child("petshops").child(BdEscolhido)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")
                var values: String
                values = p0.child("nome").value.toString()
                lojaInfo.add(values)
                values = p0.child("banner").value.toString()
                lojaInfo.add(values)
                values = p0.child("logo").value.toString()
                lojaInfo.add(values)
                values = p0.child("telefone").value.toString()
                lojaInfo.add(values)
                values = p0.child("servicos").child("24hrs").value.toString()
                lojaInfo.add(values)
                values = p0.child("servicos").child("banhoTosa").value.toString()
                lojaInfo.add(values)
                values = p0.child("servicos").child("entrega").value.toString()
                lojaInfo.add(values)
                values = p0.child("servicos").child("farmacia").value.toString()
                lojaInfo.add(values)
                values = p0.child("servicos").child("hospedagem").value.toString()
                lojaInfo.add(values)
                values = p0.child("servicos").child("vetAtendDom").value.toString()
                lojaInfo.add(values)
                values = p0.child("servicos").child("veterinario").value.toString()
                lojaInfo.add(values)

                //verifica se é o dono através do bd. Poderia ser o dono entrando pelo mapa normalmente como um user. Mas ele nao pode comprar o proprio produto
                values = p0.child("BDdoDono").value.toString()
                if (values.equals(MapsModels.userBD)){  //se o bddo usuario for igual ao do pet, é pq
                    queryItensDaLojaParaRecycleView(BdEscolhido, "prop")
                } else {
                    queryItensDaLojaParaRecycleView(BdEscolhido, userOuProp)


                    //se não for o dono, vamos contabilizar uma visita
                    if (p0.child("visitas").exists()){ //primeiro verifica se existe
                        values = p0.child("visitas").value.toString() //se existe pegamos o valor
                        databaseReference.child("petshops").child(BdEscolhido).child("visitas").setValue(values.toInt()+1)
                    } else {
                        //e aqui caso não exista (usuarios de versões antigas), ele cria o campo e coloca 1 visita lá
                        databaseReference.child("petshops").child(BdEscolhido).child("visitas").setValue(1)
                    }
                }

                setupLoja(BdEscolhido)

                values = p0.child("impulsionamentos").value.toString()
                if (values.equals("sim")){
                    queryVerificaImpulsionamentoNaLoja(BdEscolhido)
                }
                //EncerraDialog()

            }

        })



    }

    fun queryVerificaImpulsionamentoNaLoja(bdDaLoja: String) {

        getInstance().reference.child("impulsionamentos").orderByChild("pet").equalTo(bdDaLoja)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.exists()) {


                        for (querySnapshot in dataSnapshot.children) {

                            val nome: String
                            val img: String
                            val preco: String
                            var txt: TextView = findViewById(R.id.impulsionado_nome)
                            val imgIV: ImageView = findViewById(R.id.impulsionado_img)
                            var values: String

                            //se a hora de agora for maior do que a hora que começou o anuncio, apagar do bd.

                            //ATENCAO ESSE CODIGO É COPIADO DO ELSE ABAIXO. eLE OCORRE QUANDO A DATE É A MESMA MAS A HORA AINDA É MENOR. ENTAO TEM QUE EXIBIR. SE ALTERAR ALGO LÁ, ALTERE AQUI
                            values =
                                querySnapshot.child("nome_prod").value.toString()  //pos0
                            //val txt: TextView = findViewById(R.id.impulsionado_nome)
                            nome = values
                            txt.setText(nome)
                            values =
                                querySnapshot.child("img_prod").value.toString() //pos1
                            img = values
                            Glide.with(this@MapsActivity).load(img).centerCrop()
                                .into(imgIV)
                            values =
                                querySnapshot.child("preco").value.toString() //pos2

                            preco = values
                            txt = findViewById(R.id.impulsionando_preco)
                            txt.setText(preco)

                            values = querySnapshot.child("desc_prod").value.toString() //pos2
                            txt = findViewById(R.id.impulsionado_desc)
                            txt.setText(values.toString() + "*" + img) //este valor é passado apenas para o caso do user clicar e querer comprar. Vai ser pelo txt que pegaremos os valores

                            values = querySnapshot.child("pet").value.toString() //pos2
                            MapsModels.bdDoImpulsionamento = values
                            val layImpulso: ConstraintLayout =
                                findViewById(R.id.lay_impulsionado)
                            layImpulso.visibility = View.VISIBLE
                            val btnAbreImpulso: Button = findViewById(R.id.btnAbreImpulso)
                            btnAbreImpulso.setBackgroundResource(R.mipmap.icon_oferta)


                        }

                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })   //addValueEventListener

    }

    fun clicksDolay_loja (){

        var posicaoBtn = 0
        var imgNoBtn: ImageView = findViewById(R.id.carrinho_ImageView)
        var btnAbrirCarrinho : Button = findViewById(R.id.lay_loja_btnAbrirCarrinho)
        var cL = findViewById(R.id.lay_carrinho) as ConstraintLayout

        imgNoBtn.setImageResource(R.drawable.carrinho_para_seta)

        btnAbrirCarrinho.setOnClickListener {

            //pegando o tamanho da tela do celular
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            val width: Int = size.x


            //mudando os parametros do width do layoutCarrinho para encaixar no tamanho exato que queremos na tela. O botão do carrinho vai acompanhar pois ele está encostado nesta view.

            var lp = cL.layoutParams as ConstraintLayout.LayoutParams


            if (lp.width <100){  //vamos usar o width para saber se o botão está no formato de carrinho ou seta. Ou seja, abrir ou fechar o layout
                //se for menor do que 100 é pq ele está recolhido
                imgNoBtn.setImageResource(R.drawable.carrinho_para_seta)
                bottomBar.visibility = View.GONE

                lp.width = width - 150
                cL.layoutParams = lp
                lay_carrinho.visibility = View.VISIBLE
                val btnCentral : Button = findViewById(R.id.btnLocalizaNovamente)
                val img: ImageView = findViewById(R.id.imageView4)
                btnCentral.visibility = View.GONE
                img.visibility = View.GONE
                (imgNoBtn.drawable as AnimatedVectorDrawable).start()



            } else {

                imgNoBtn.setImageResource(R.drawable.seta_para_carrinho)
                bottomBar.visibility = View.VISIBLE
                //(imgNoBtn.drawable as AnimatedVectorDrawable).reset()
                lp.width = 8
                cL.layoutParams = lp
                lay_carrinho.visibility = View.INVISIBLE
                (imgNoBtn.drawable as AnimatedVectorDrawable).start()
                val btnCentral : Button = findViewById(R.id.btnLocalizaNovamente)
                val img: ImageView = findViewById(R.id.imageView4)
                btnCentral.visibility = View.VISIBLE
                img.visibility = View.VISIBLE
            }


        }

        val layLoja: ConstraintLayout = findViewById(R.id.lay_loja)
        layLoja.visibility = View.VISIBLE


    }

    fun setupLoja(bdDestaLoja: String){

        //esconde impulsionamento  = se a loja tiver promoção, vai voltar a aparecer já com simbolo trocado lá na queryImpulsionamentoDaLoja()
        val layImpulsionamento: ConstraintLayout = findViewById(R.id.lay_impulsionado)
        layImpulsionamento.visibility = View.GONE

        val banner = findViewById<ImageView>(R.id.lay_loja_bannerImageView)
        //banner.setImageResource(R.drawable.bannersample)
        banner.setImageResource(R.mipmap.banner)
        val logo : ImageView = findViewById(R.id.lay_loja_imgLogo)
        //logo.setImageResource(R.drawable.logosample)
        logo.setImageResource(R.drawable.ic_logoportrait)
        val etServicos: TextView = findViewById(R.id.lay_loja_servicos)

        //lojaInfo.get(0) é o nome do Pet
        if (!lojaInfo.get(1).equals("nao")){ //se for nao, deixa o padrão que já foi colocado acima.
            Glide.with(this@MapsActivity).load(lojaInfo.get(1)).centerCrop().into(banner)
        }
        if (!lojaInfo.get(2).equals("nao")){
            Glide.with(this@MapsActivity).load(lojaInfo.get(2)).apply(RequestOptions.circleCropTransform()).into(logo)
            val txNome: TextView = findViewById(R.id.loja_nomePet)
            txNome.visibility= View.GONE
        } else {
            val txNome: TextView = findViewById(R.id.loja_nomePet)
            txNome.setText(lojaInfo.get(0).toString())
        }

        //nome2 é o nome que fica exibido fixo
        val txtNome2 : TextView = findViewById(R.id.txtNome2)
        txtNome2.setText(lojaInfo.get(0).toString())


        val txCu : TextView = findViewById(R.id.loja_cu)
        txCu.setText("Código único: "+bdDestaLoja)


        //botao que exibe e esconde os checkboxs com serviços
        val btnExibeEsconde: ImageView = findViewById(R.id.lay_loja_btnExibeEsconde)
        btnExibeEsconde.setOnClickListener {
            //dentro do método ele verifica se precisa esconder ou exibir
            ExibeEscondeCheckBox()
        }


        var cb: CheckBox //esta variavel vai recebendo os diferentes checkboxs, para não ter que declarar muitas variáveis

        //colocar os serviços
        cb = findViewById(R.id.loja_cb24hrs)  //funciona 24 hrs
        var values = lojaInfo.get(4)
        cb.isChecked = !(values=="nao" || values == null)

        cb = findViewById(R.id.loja_cbBanhoTosa) //banho e tosa
        values = lojaInfo.get(5)
        cb.isChecked = !(values=="nao" || values == null)

        cb = findViewById(R.id.loja_cbEntrega) //faz entrega
        values = lojaInfo.get(6)
        cb.isChecked = !(values=="nao" || values == null)

        cb = findViewById(R.id.loja_cbFarmacia) //faz entrega
        values = lojaInfo.get(7)
        cb.isChecked = !(values=="nao" || values == null)

        cb = findViewById(R.id.loja_cbHospedagem) //faz entrega
        values = lojaInfo.get(8)
        cb.isChecked = !(values=="nao" || values == null)

        cb = findViewById(R.id.loja_cbAtendDom) //atendimento em domicilio aka veterinario em domicilio
        values = lojaInfo.get(9)
        cb.isChecked = !(values=="nao" || values == null)

        cb = findViewById(R.id.loja_cbVeterinario) //atendimento veterinário
        values = lojaInfo.get(10)
        cb.isChecked = !(values=="nao" || values == null)


        //colocar as outras informações na loja
        //ajustar as imagens pra obedecer o layout e nao expandir


        clicksDolay_loja()

        /* agora isso é feito apenas em clicksdolaydaloja
        var lay: ConstraintLayout
        lay = findViewById(R.id.lay_loja)
        lay.visibility = View.VISIBLE

        lay = findViewById(R.id.lay_maps)
        lay.visibility = View.GONE

         */
        val btn: Button = findViewById(R.id.lay_loja_btnAbrirCarrinho)
        val img: ImageView = findViewById(R.id.carrinho_ImageView)
        ListenersDoBotaoQueAcompanhaTela(btn, img)


        EncerraDialog()

        setClicksDoMenuCategoria()
    }

    fun setClicksDoMenuCategoria(){

        val lay1Rac: ConstraintLayout = findViewById(R.id.lay1)
        val lay2Serc: ConstraintLayout = findViewById(R.id.lay2)
        val lay3Est: ConstraintLayout = findViewById(R.id.lay3)
        val lay4Med: ConstraintLayout = findViewById(R.id.lay4)
        val lay5Acess: ConstraintLayout = findViewById(R.id.lay5)

        val txtRac: TextView = findViewById(R.id.txtTitleRacao)
        val txtServ: TextView = findViewById(R.id.txtTitleServicos)
        val txtEst: TextView = findViewById(R.id.txtTitleEstetica)
        val txtMed: TextView = findViewById(R.id.txtTitleMedicamento)
        val txtAcess: TextView = findViewById(R.id.txtTitleAcessorios)

        val recycleRacao: RecyclerView = findViewById(R.id.loja_recycleView)
        val recycleServ: RecyclerView = findViewById(R.id.loja_recyclerViewServicos)
        val recycleEst: RecyclerView = findViewById(R.id.loja_recyclerViewEstetica)
        val recycleMed: RecyclerView = findViewById(R.id.loja_recyclerViewRemedios)
        val recycleAcess: RecyclerView = findViewById(R.id.loja_recyclerViewProdutos)

        recycleRacao.visibility = View.INVISIBLE
        recycleServ.visibility = View.INVISIBLE
        recycleEst.visibility = View.INVISIBLE
        recycleMed.visibility = View.INVISIBLE
        recycleAcess.visibility = View.INVISIBLE

        lay1Rac.setOnClickListener {
            lay1Rac.setBackgroundColor(Color.parseColor("#ffffff"))
            lay2Serc.setBackgroundColor(Color.parseColor("#ffffff"))
            lay3Est.setBackgroundColor(Color.parseColor("#ffffff"))
            lay4Med.setBackgroundColor(Color.parseColor("#ffffff"))
            lay5Acess.setBackgroundColor(Color.parseColor("#ffffff"))
            txtRac.setTextColor(Color.parseColor("#a2d5f2"))
            txtServ.setTextColor(Color.parseColor("#a2d5f2"))
            txtEst.setTextColor(Color.parseColor("#a2d5f2"))
            txtMed.setTextColor(Color.parseColor("#a2d5f2"))
            txtAcess.setTextColor(Color.parseColor("#a2d5f2"))
            recycleRacao.visibility = View.INVISIBLE
            recycleServ.visibility = View.INVISIBLE
            recycleEst.visibility = View.INVISIBLE
            recycleMed.visibility = View.INVISIBLE
            recycleAcess.visibility = View.INVISIBLE

            lay1Rac.setBackgroundColor(Color.parseColor("#a2d5f2"))
            txtRac.setTextColor(Color.parseColor("#ffffff"))
            recycleRacao.visibility = View.VISIBLE
        }

        lay2Serc.setOnClickListener {
            lay1Rac.setBackgroundColor(Color.parseColor("#ffffff"))
            lay2Serc.setBackgroundColor(Color.parseColor("#ffffff"))
            lay3Est.setBackgroundColor(Color.parseColor("#ffffff"))
            lay4Med.setBackgroundColor(Color.parseColor("#ffffff"))
            lay5Acess.setBackgroundColor(Color.parseColor("#ffffff"))
            txtRac.setTextColor(Color.parseColor("#a2d5f2"))
            txtServ.setTextColor(Color.parseColor("#a2d5f2"))
            txtEst.setTextColor(Color.parseColor("#a2d5f2"))
            txtMed.setTextColor(Color.parseColor("#a2d5f2"))
            txtAcess.setTextColor(Color.parseColor("#a2d5f2"))
            recycleRacao.visibility = View.INVISIBLE
            recycleServ.visibility = View.INVISIBLE
            recycleEst.visibility = View.INVISIBLE
            recycleMed.visibility = View.INVISIBLE
            recycleAcess.visibility = View.INVISIBLE

            lay2Serc.setBackgroundColor(Color.parseColor("#a2d5f2"))
            txtServ.setTextColor(Color.parseColor("#ffffff"))
            recycleServ.visibility = View.VISIBLE
        }

        lay3Est.setOnClickListener {
            lay1Rac.setBackgroundColor(Color.parseColor("#ffffff"))
            lay2Serc.setBackgroundColor(Color.parseColor("#ffffff"))
            lay3Est.setBackgroundColor(Color.parseColor("#ffffff"))
            lay4Med.setBackgroundColor(Color.parseColor("#ffffff"))
            lay5Acess.setBackgroundColor(Color.parseColor("#ffffff"))
            txtRac.setTextColor(Color.parseColor("#a2d5f2"))
            txtServ.setTextColor(Color.parseColor("#a2d5f2"))
            txtEst.setTextColor(Color.parseColor("#a2d5f2"))
            txtMed.setTextColor(Color.parseColor("#a2d5f2"))
            txtAcess.setTextColor(Color.parseColor("#a2d5f2"))
            recycleRacao.visibility = View.INVISIBLE
            recycleServ.visibility = View.INVISIBLE
            recycleEst.visibility = View.INVISIBLE
            recycleMed.visibility = View.INVISIBLE
            recycleAcess.visibility = View.INVISIBLE

            lay3Est.setBackgroundColor(Color.parseColor("#a2d5f2"))
            txtEst.setTextColor(Color.parseColor("#ffffff"))
            recycleEst.visibility = View.VISIBLE
        }

        lay4Med.setOnClickListener {
            lay1Rac.setBackgroundColor(Color.parseColor("#ffffff"))
            lay2Serc.setBackgroundColor(Color.parseColor("#ffffff"))
            lay3Est.setBackgroundColor(Color.parseColor("#ffffff"))
            lay4Med.setBackgroundColor(Color.parseColor("#ffffff"))
            lay5Acess.setBackgroundColor(Color.parseColor("#ffffff"))
            txtRac.setTextColor(Color.parseColor("#a2d5f2"))
            txtServ.setTextColor(Color.parseColor("#a2d5f2"))
            txtEst.setTextColor(Color.parseColor("#a2d5f2"))
            txtMed.setTextColor(Color.parseColor("#a2d5f2"))
            txtAcess.setTextColor(Color.parseColor("#a2d5f2"))
            recycleRacao.visibility = View.INVISIBLE
            recycleServ.visibility = View.INVISIBLE
            recycleEst.visibility = View.INVISIBLE
            recycleMed.visibility = View.INVISIBLE
            recycleAcess.visibility = View.INVISIBLE

            lay4Med.setBackgroundColor(Color.parseColor("#a2d5f2"))
            txtMed.setTextColor(Color.parseColor("#ffffff"))
            recycleMed.visibility = View.VISIBLE
        }

        lay5Acess.setOnClickListener {
            lay1Rac.setBackgroundColor(Color.parseColor("#ffffff"))
            lay2Serc.setBackgroundColor(Color.parseColor("#ffffff"))
            lay3Est.setBackgroundColor(Color.parseColor("#ffffff"))
            lay4Med.setBackgroundColor(Color.parseColor("#ffffff"))
            lay5Acess.setBackgroundColor(Color.parseColor("#ffffff"))
            txtRac.setTextColor(Color.parseColor("#a2d5f2"))
            txtServ.setTextColor(Color.parseColor("#a2d5f2"))
            txtEst.setTextColor(Color.parseColor("#a2d5f2"))
            txtMed.setTextColor(Color.parseColor("#a2d5f2"))
            txtAcess.setTextColor(Color.parseColor("#a2d5f2"))
            recycleRacao.visibility = View.INVISIBLE
            recycleServ.visibility = View.INVISIBLE
            recycleEst.visibility = View.INVISIBLE
            recycleMed.visibility = View.INVISIBLE
            recycleAcess.visibility = View.INVISIBLE

            lay5Acess.setBackgroundColor(Color.parseColor("#a2d5f2"))
            txtAcess.setTextColor(Color.parseColor("#ffffff"))
            recycleAcess.visibility = View.VISIBLE
        }
    }

    //seta a recycleview e prepara o cliqueListener chamando openPopUpCarrinho. Lá no popup começa o processo de colocar os dados nos arrays próprios.
    fun SetUpRecycleViewDaLoja (userOuProp: String){

        var tipoProduto: String = "nao"
        var cont =0

        val arrayRecyclerList1: MutableList<String> = ArrayList()
        arrayRecyclerList1.clear()

        while (cont<arrayTipo.size){
            if (arrayTipo.get(cont).equals("racao")){
                arrayRecyclerList1.add(arrayNomes.get(cont))
                arrayRecyclerList1.add(arrayImg.get(cont))
                arrayRecyclerList1.add(arrayPreco.get(cont))
                arrayRecyclerList1.add(arrayDesc.get(cont))
                arrayRecyclerList1.add(arrayBD.get(cont))
                cont++

                //ajuste da primeira recycleview que vai aparecer
                //se tem pelo menos 1 tipo ração, é a ração que será a guia inicial

                //atenção embora eu tenha numerado, esta ordem segue a ordem de aparição aqui. Mas não no app na aparencia.
                //a ordem no app é: Ração, serviços, estética, medicamentos e acessórios

                //val lay1Rac: ConstraintLayout = findViewById(R.id.lay1)
                //lay1Rac.performClick()

                val recycleRacao: RecyclerView = findViewById(R.id.loja_recycleView)
                recycleRacao.visibility = View.VISIBLE

            } else {
                cont++
            }

        }


        //INICIO DO CODIGO DA RECYCLEVIEW
        var adapter: MinhaLojaVertcialRacaoRecyclerAdapter = MinhaLojaVertcialRacaoRecyclerAdapter(this, arrayRecyclerList1)

        //chame a recyclerview
        var recyclerView: RecyclerView = findViewById(R.id.loja_recycleView)


        //define o tipo de layout (linerr, grid)
        var linearLayoutManager: CenterZoomLayoutManager = CenterZoomLayoutManager(this, RecyclerView.HORIZONTAL, false)

        //coloca o adapter na recycleview
        recyclerView.adapter = adapter

        recyclerView.layoutManager = linearLayoutManager

        // Notify the adapter for data change.
        adapter.notifyDataSetChanged()

        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView.addOnItemTouchListener(
            arealojista.RecyclerTouchListener(
                this,
                recyclerView,
                object : arealojista.ClickListener {

                    override fun onClick(view: View, position: Int) {

                        val layLoja :ConstraintLayout= findViewById(R.id.lay_loja)
                        layLoja.setOnTouchListener(object : View.OnTouchListener {
                            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                                // ignore all touch events
                                return true
                            }
                        })

                        tipoProduto="racao"
                        if (userOuProp.equals("user")){
                            //se for usuario vai abrir popup para adicionar prod. no carrinho
                            if (MapsModels.popupAberta==false){
                                if (MapsModels.userMail.equals("semLogin")){
                                    openPopUpLogin("Você não está logado", "Para acessar esta função você precisa fazer login.", "Fazer login", "Cancelar")
                                } else {

                                    openPopUpAddItemCarrinho("Adicionando ao carrinho", "Deseja adicionar este item?", "Sim, adicionar", "Não", position*5, arrayRecyclerList1, tipoProduto)
                                }
                            }
                        } else {

                            //se for proprietário ao clicar no item vai perguntar se quiser impulsionar
                            if (MapsModels.userMail.equals("semLogin")){
                                openPopUpLogin("Você não está logado", "Para acessar esta função você precisa se registrar.", "Fazer login", "Cancelar")
                            } else {
                                openPopUpPromocao("Impulsionar vendas", "Deseja impulsionar as vendas deste produto?", "Sim, impulsionar", "Não", position*5, arrayRecyclerList1, tipoProduto)
                            }

                        }



                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                })
        )

        //se o arr


        //setupLoja()





        //setup Recyclerview do Serviços

        val arrayRecycleListServ: MutableList<String> = ArrayList()

        cont =0
        while (cont<arrayTipo.size){
            if (arrayTipo.get(cont).equals("servicos")){
                arrayRecycleListServ.add(arrayNomes.get(cont))
                arrayRecycleListServ.add(arrayImg.get(cont))
                arrayRecycleListServ.add(arrayPreco.get(cont))
                arrayRecycleListServ.add(arrayDesc.get(cont))
                arrayRecycleListServ.add(arrayBD.get(cont))
                cont++


                val recycleRacao: RecyclerView = findViewById(R.id.loja_recycleView)
                if (recycleRacao.isVisible==false){

                    val recycleServ: RecyclerView = findViewById(R.id.loja_recyclerViewServicos)
                    recycleServ.visibility = View.VISIBLE

                }

                /*
                val recycleRacao: RecyclerView = findViewById(R.id.loja_recycleView)
                val lay1Rac: ConstraintLayout = findViewById(R.id.lay1)  //ja foi

                if (lay1Rac.isVisible==false){
                    val lay2Serc: ConstraintLayout = findViewById(R.id.lay2)
                    lay2Serc.performClick()

                }

                 */
            }
            cont++
        }

        //INICIO DO CODIGO DA RECYCLEVIEW
        var adapter4: MinhaLojaVertcialRemediosRecyclerAdapter = MinhaLojaVertcialRemediosRecyclerAdapter(this, arrayRecycleListServ)

        //chame a recyclerview
        var recyclerView4: RecyclerView = findViewById(R.id.loja_recyclerViewServicos)


        //define o tipo de layout (linerr, grid)
        var linearLayoutManager4: LinearLayoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        //coloca o adapter na recycleview
        recyclerView4.adapter = adapter4

        recyclerView4.layoutManager = linearLayoutManager4

        // Notify the adapter for data change.
        adapter4.notifyDataSetChanged()

        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView4.addOnItemTouchListener(
            arealojista.RecyclerTouchListener(
                this,
                recyclerView4,
                object : arealojista.ClickListener {

                    override fun onClick(view: View, position: Int) {

                        val layLoja :ConstraintLayout= findViewById(R.id.lay_loja)
                        layLoja.setOnTouchListener(object : View.OnTouchListener {
                            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                                // ignore all touch events
                                return true
                            }
                        })
                        tipoProduto = "servicos"
                        if (userOuProp.equals("user")){
                            //se for usuario vai abrir popup para adicionar prod. no carrinho
                            if (MapsModels.popupAberta==false){
                                if (MapsModels.userMail.equals("semLogin")){
                                    openPopUpLogin("Você não está logado", "Para acessar esta função você precisa fazer login.", "Fazer login", "Cancelar")
                                } else {

                                    openPopUpAddItemCarrinho("Adicionando ao carrinho", "Deseja adicionar este item?", "Sim, adicionar", "Não", position*5, arrayRecycleListServ, tipoProduto)
                                }
                            }
                        } else {

                            //se for proprietário ao clicar no item vai perguntar se quiser impulsionar
                            if (MapsModels.userMail.equals("semLogin")){
                                openPopUpLogin("Você não está logado", "Para acessar esta função você precisa se registrar.", "Fazer login", "Cancelar")
                            } else {
                                openPopUpPromocao("Impulsionar vendas", "Deseja impulsionar as vendas deste produto?", "Sim, impulsionar", "Não", position*5, arrayRecycleListServ, tipoProduto)
                            }

                        }



                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                })
        )



        //FIM DA RECYCLEVIEW


        //setup Recyclerview de estetica

        val arrayRecycleListEstetica: MutableList<String> = ArrayList()

        cont =0
        while (cont<arrayTipo.size){
            if (arrayTipo.get(cont).equals("estetica")){
                arrayRecycleListEstetica.add(arrayNomes.get(cont))
                arrayRecycleListEstetica.add(arrayImg.get(cont))
                arrayRecycleListEstetica.add(arrayPreco.get(cont))
                arrayRecycleListEstetica.add(arrayDesc.get(cont))
                arrayRecycleListEstetica.add(arrayBD.get(cont))
                cont++



                val recycleRacao: RecyclerView = findViewById(R.id.loja_recycleView)
                val recycleServ: RecyclerView = findViewById(R.id.loja_recyclerViewServicos)

                if (recycleRacao.isVisible==false && recycleServ.isVisible==false){
                    val recycleEst: RecyclerView = findViewById(R.id.loja_recyclerViewEstetica)
                    recycleEst.visibility = View.VISIBLE

                }

                /*
                val lay1Rac: ConstraintLayout = findViewById(R.id.lay1)  //ja foi
                val lay2Serc: ConstraintLayout = findViewById(R.id.lay2)

                if (lay1Rac.isVisible==false && lay2Serc.isVisible==false){
                    val lay3Est: ConstraintLayout = findViewById(R.id.lay3)
                    lay3Est.performClick()
                }

                 */

            }
            cont++
        }

        //INICIO DO CODIGO DA RECYCLEVIEW
        var adapter5: MinhaLojaVertcialRemediosRecyclerAdapter = MinhaLojaVertcialRemediosRecyclerAdapter(this, arrayRecycleListEstetica)

        //chame a recyclerview
        var recyclerView5: RecyclerView = findViewById(R.id.loja_recyclerViewEstetica)


        //define o tipo de layout (linerr, grid)
        var linearLayoutManager5: LinearLayoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        //coloca o adapter na recycleview
        recyclerView5.adapter = adapter5

        recyclerView5.layoutManager = linearLayoutManager5

        // Notify the adapter for data change.
        adapter5.notifyDataSetChanged()

        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView5.addOnItemTouchListener(
            arealojista.RecyclerTouchListener(
                this,
                recyclerView5,
                object : arealojista.ClickListener {

                    override fun onClick(view: View, position: Int) {
                        //Log.d("teste", arrayNomes.get(position))

                        val layLoja :ConstraintLayout= findViewById(R.id.lay_loja)
                        layLoja.setOnTouchListener(object : View.OnTouchListener {
                            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                                // ignore all touch events
                                return true
                            }
                        })
                        tipoProduto = "estetica"
                        if (userOuProp.equals("user")){
                            //se for usuario vai abrir popup para adicionar prod. no carrinho
                            if (MapsModels.popupAberta==false){
                                if (MapsModels.userMail.equals("semLogin")){
                                    openPopUpLogin("Você não está logado", "Para acessar esta função você precisa fazer login.", "Fazer login", "Cancelar")
                                } else {

                                    openPopUpAddItemCarrinho("Adicionando ao carrinho", "Deseja adicionar este item?", "Sim, adicionar", "Não", position*5, arrayRecycleListEstetica, tipoProduto)
                                }
                            }
                        } else {

                            //se for proprietário ao clicar no item vai perguntar se quiser impulsionar
                            if (MapsModels.userMail.equals("semLogin")){
                                openPopUpLogin("Você não está logado", "Para acessar esta função você precisa se registrar.", "Fazer login", "Cancelar")
                            } else {
                                openPopUpPromocao("Impulsionar vendas", "Deseja impulsionar as vendas deste produto?", "Sim, impulsionar", "Não", position*5, arrayRecycleListEstetica, tipoProduto)
                            }

                        }


                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                })
        )




        //setup Recyclerview do remedio - medicamentos

        val arrayRecycleListRem: MutableList<String> = ArrayList()

        //ajuste da primeira recycleview que vai aparecer


        cont =0
        while (cont<arrayTipo.size){
            if (arrayTipo.get(cont).equals("remedios")){
                arrayRecycleListRem.add(arrayNomes.get(cont))
                arrayRecycleListRem.add(arrayImg.get(cont))
                arrayRecycleListRem.add(arrayPreco.get(cont))
                arrayRecycleListRem.add(arrayDesc.get(cont))
                arrayRecycleListRem.add(arrayBD.get(cont))
                cont++

                //ajuste da primeira recycleview que vai aparecer


                val recycleRacao: RecyclerView = findViewById(R.id.loja_recycleView)
                val recycleServ: RecyclerView = findViewById(R.id.loja_recyclerViewServicos)
                val recycleEst: RecyclerView = findViewById(R.id.loja_recyclerViewEstetica)

                if (recycleRacao.isVisible==false && recycleServ.isVisible==false && recycleEst.isVisible==false){

                    val recycleMed: RecyclerView = findViewById(R.id.loja_recyclerViewRemedios)
                    recycleMed.visibility = View.VISIBLE
                }

                /*
                val lay1Rac: ConstraintLayout = findViewById(R.id.lay1)
                val lay2Serv: ConstraintLayout = findViewById(R.id.lay2) //ja foi
                val lay3Est: ConstraintLayout = findViewById(R.id.lay3)

                if (lay1Rac.isVisible==false && lay2Serv.isVisible==false &&lay3Est.isVisible==false){
                    val lay4Med: ConstraintLayout = findViewById(R.id.lay4)
                    lay4Med.performClick()
                }

                 */
            }
            cont++
        }

        //INICIO DO CODIGO DA RECYCLEVIEW
        var adapter2: MinhaLojaVertcialRemediosRecyclerAdapter = MinhaLojaVertcialRemediosRecyclerAdapter(this, arrayRecycleListRem)

        //chame a recyclerview
        var recyclerView2: RecyclerView = findViewById(R.id.loja_recyclerViewRemedios)


        //define o tipo de layout (linerr, grid)
        var linearLayoutManager2: LinearLayoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        //coloca o adapter na recycleview
        recyclerView2.adapter = adapter2

        recyclerView2.layoutManager = linearLayoutManager2

        // Notify the adapter for data change.
        adapter2.notifyDataSetChanged()

        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView2.addOnItemTouchListener(
            arealojista.RecyclerTouchListener(
                this,
                recyclerView2,
                object : arealojista.ClickListener {

                    override fun onClick(view: View, position: Int) {
                        //Log.d("teste", arrayNomes.get(position))

                        val layLoja :ConstraintLayout= findViewById(R.id.lay_loja)
                        layLoja.setOnTouchListener(object : View.OnTouchListener {
                            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                                // ignore all touch events
                                return true
                            }
                        })
                        tipoProduto="remedios"
                        if (userOuProp.equals("user")){
                            //se for usuario vai abrir popup para adicionar prod. no carrinho
                            if (MapsModels.popupAberta==false){
                                if (MapsModels.userMail.equals("semLogin")){
                                    openPopUpLogin("Você não está logado", "Para acessar esta função você precisa fazer login.", "Fazer login", "Cancelar")
                                } else {

                                    openPopUpAddItemCarrinho("Adicionando ao carrinho", "Deseja adicionar este item?", "Sim, adicionar", "Não", position*5, arrayRecycleListRem, tipoProduto)
                                }
                            }
                        } else {

                            //se for proprietário ao clicar no item vai perguntar se quiser impulsionar
                            if (MapsModels.userMail.equals("semLogin")){
                                openPopUpLogin("Você não está logado", "Para acessar esta função você precisa se registrar.", "Fazer login", "Cancelar")
                            } else {
                                openPopUpPromocao("Impulsionar vendas", "Deseja impulsionar as vendas deste produto?", "Sim, impulsionar", "Não", position*5, arrayRecycleListRem, tipoProduto)
                            }

                        }



                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                })
        )


        //FIM DO RECYCLERVIEW DE MEDICAMENTOS

        //setup RecyclerView do produto---agora é acessórios

        val arrayRecycleListProd: MutableList<String> = ArrayList()

        cont =0
        while (cont<arrayTipo.size){
            if (arrayTipo.get(cont).equals("acessorios")){
                arrayRecycleListProd.add(arrayNomes.get(cont))
                arrayRecycleListProd.add(arrayImg.get(cont))
                arrayRecycleListProd.add(arrayPreco.get(cont))
                arrayRecycleListProd.add(arrayDesc.get(cont))
                arrayRecycleListProd.add(arrayBD.get(cont))
                cont++

                //ajuste da primeira recycleview que vai aparecer



                val recycleRacao: RecyclerView = findViewById(R.id.loja_recycleView)
                val recycleServ: RecyclerView = findViewById(R.id.loja_recyclerViewServicos)
                val recycleEst: RecyclerView = findViewById(R.id.loja_recyclerViewEstetica)
                val recycleMed: RecyclerView = findViewById(R.id.loja_recyclerViewRemedios)
                val recycleAcess: RecyclerView = findViewById(R.id.loja_recyclerViewProdutos)
                if (recycleRacao.isVisible==false && recycleServ.isVisible==false && recycleEst.isVisible==false && recycleMed.isVisible==false){
                    recycleAcess.visibility = View.VISIBLE
                }

                /*
                val lay1Rac: ConstraintLayout = findViewById(R.id.lay1)
                val lay2Serv: ConstraintLayout = findViewById(R.id.lay2)
                val lay3Est: ConstraintLayout = findViewById(R.id.lay3)
                val lay4Med: ConstraintLayout = findViewById(R.id.lay4)
                //ajuste da primeira recycleview que vai aparecer

                //ajuste da primeira recycleview que vai aparecer
                //verifica. Se a primeira recycle (ração) está invisivel, então exibe essa, caso tenha algo pra exibir (estando aqui dentro tem algo)
                if (lay1Rac.isVisible==false && lay2Serv.isVisible==false && lay3Est.isVisible==false  && lay4Med.isVisible==false){
                    //ajuste da primeira recycleview que vai aparecer

                    val lay5Access: ConstraintLayout = findViewById(R.id.lay5)
                    lay5Access.performClick()
                    lay4Med.visibility= View.INVISIBLE
                }

                 */
            }
            cont++
        }

        //INICIO DO CODIGO DA RECYCLEVIEW
        var adapter3: MinhaLojaVertcialRemediosRecyclerAdapter = MinhaLojaVertcialRemediosRecyclerAdapter(this, arrayRecycleListProd)

        //chame a recyclerview
        var recyclerView3: RecyclerView = findViewById(R.id.loja_recyclerViewProdutos)


        //define o tipo de layout (linerr, grid)
        var linearLayoutManager3: LinearLayoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        //coloca o adapter na recycleview
        recyclerView3.adapter = adapter3

        recyclerView3.layoutManager = linearLayoutManager3

        // Notify the adapter for data change.
        adapter3.notifyDataSetChanged()

        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView3.addOnItemTouchListener(
            arealojista.RecyclerTouchListener(
                this,
                recyclerView3,
                object : arealojista.ClickListener {

                    override fun onClick(view: View, position: Int) {
                        //Log.d("teste", arrayNomes.get(position))

                        val layLoja :ConstraintLayout= findViewById(R.id.lay_loja)
                        layLoja.setOnTouchListener(object : View.OnTouchListener {
                            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                                // ignore all touch events
                                return true
                            }
                        })
                        tipoProduto = "acessorios"
                        if (userOuProp.equals("user")){
                            //se for usuario vai abrir popup para adicionar prod. no carrinho
                            if (MapsModels.popupAberta==false){
                                if (MapsModels.userMail.equals("semLogin")){
                                    openPopUpLogin("Você não está logado", "Para acessar esta função você precisa fazer login.", "Fazer login", "Cancelar")
                                } else {

                                    openPopUpAddItemCarrinho("Adicionando ao carrinho", "Deseja adicionar este item?", "Sim, adicionar", "Não", position*5, arrayRecycleListProd, tipoProduto)
                                }
                            }
                        } else {

                            //se for proprietário ao clicar no item vai perguntar se quiser impulsionar
                            if (MapsModels.userMail.equals("semLogin")){
                                openPopUpLogin("Você não está logado", "Para acessar esta função você precisa se registrar.", "Fazer login", "Cancelar")
                            } else {
                                openPopUpPromocao("Impulsionar vendas", "Deseja impulsionar as vendas deste produto?", "Sim, impulsionar", "Não", position*5, arrayRecycleListProd, tipoProduto)
                            }

                        }



                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                })
        )

        //FIM DO RECYCLERVIEW DE ACESSORIOS



        //se nenhum array tiver conteúdo, exibir mensagem dizendo que não tem nada dentrp
        //aqui se todas recycleviews estao invisivels, é pq estão todas vazias

        val recycleRacao: RecyclerView = findViewById(R.id.loja_recycleView)
        val recycleServ: RecyclerView = findViewById(R.id.loja_recyclerViewServicos)
        val recycleEst: RecyclerView = findViewById(R.id.loja_recyclerViewEstetica)
        val recycleMed: RecyclerView = findViewById(R.id.loja_recyclerViewRemedios)
        val recycleAcess: RecyclerView = findViewById(R.id.loja_recyclerViewProdutos)

        if (recycleRacao.isVisible==false && recycleServ.isVisible==false && recycleEst.isVisible==false && recycleMed.isVisible==false && recycleAcess.isVisible==false){
            val txtAvisaVazio: TextView = findViewById(R.id.txtAvisaVazio)
            txtAvisaVazio.visibility = View.VISIBLE
        }

        /*
        val lay1Rac: ConstraintLayout = findViewById(R.id.lay1)  //ja foi
        val lay2Serc: ConstraintLayout = findViewById(R.id.lay2)
        val lay4Med: ConstraintLayout = findViewById(R.id.lay4)  //ja foi
        val lay5Acess: ConstraintLayout = findViewById(R.id.lay5) //ja foi
        val lay3Est: ConstraintLayout = findViewById(R.id.lay3)

        if (lay1Rac.isVisible==false && lay5Acess.isVisible==false && lay4Med.isVisible==false && lay2Serc.isVisible==false && lay3Est.isVisible==false){
            val
            txtAvisaVazio: TextView = findViewById(R.id.
            txtAvisaVazio)
            txtAvisaVazio.visibility = View.VISIBLE
        }
         */


        //FIM DA RECYCLEVIEW


    }

    //aqui verifica se os checkbox com os serviços oferecidos estão abertos ou fechados e abre ou fecha, aumentando ou diminuindo o tamanho do layout
    fun ExibeEscondeCheckBox (){

        val cb1: CheckBox = findViewById(R.id.loja_cb24hrs)  //funciona 24 hrs
        val cb2: CheckBox = findViewById(R.id.loja_cbBanhoTosa) //banho e tosa
        val cb3: CheckBox = findViewById(R.id.loja_cbEntrega) //faz entrega
        val cb4: CheckBox = findViewById(R.id.loja_cbFarmacia) //faz entrega
        val cb5: CheckBox = findViewById(R.id.loja_cbHospedagem) //faz entrega
        val cb6: CheckBox = findViewById(R.id.loja_cbAtendDom) //atendimento em domicilio aka veterinario em domicilio
        val cb7: CheckBox = findViewById(R.id.loja_cbVeterinario) //atendimento veterinário
        val btn : ImageView = findViewById(R.id.lay_loja_btnExibeEsconde)

        if (cb1.isVisible){
            cb1.visibility = View.GONE
            cb2.visibility = View.GONE
            cb3.visibility = View.GONE
            cb4.visibility = View.GONE
            cb5.visibility = View.GONE
            cb6.visibility = View.GONE
            cb7.visibility = View.GONE
            btn.setImageResource(R.drawable.ic_expand_more_black_24dp)

        } else {
            cb1.visibility = View.VISIBLE
            cb2.visibility = View.VISIBLE
            cb3.visibility = View.VISIBLE
            cb4.visibility = View.VISIBLE
            cb5.visibility = View.VISIBLE
            cb6.visibility = View.VISIBLE
            cb7.visibility = View.VISIBLE
            btn.setImageResource(R.drawable.ic_expand_less_black_24dp)
        }
    }

    //esta popup é exclusiva do proprietário e vai servir para impulsionar promoção e chama a activity ImpulsionarPromo
    fun openPopUpPromocao (titulo: String, texto:String, btnSim: String, btnNao: String, posicao: Int, array: MutableList<String>, tipoProd: String) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao

        MapsModels.popupAberta=true

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
            slideOut.slideEdge = Gravity.LEFT
            popupWindow.exitTransition = slideOut

        }


        // Get the widgets reference from custom view
        val buttonPopupN = view.findViewById<Button>(R.id.btnReclamar)
        val buttonPopupS = view.findViewById<Button>(R.id.BtnRecebimento)
        val buttonPopupOk = view.findViewById<Button>(R.id.popupBtnOk)
        val txtTitulo = view.findViewById<TextView>(R.id.popupTitulo)
        val txtTexto = view.findViewById<TextView>(R.id.popupTexto)

        //vai exibir os botões com textos e esconder o btn ok
        buttonPopupOk.visibility = View.GONE
        //exibe e ajusta os textos dos botões
        buttonPopupN.text = btnNao
        buttonPopupS.text = btnSim

        // Set a click listener for popup's button widget
        buttonPopupN.setOnClickListener{
            // Dismiss the popup window
            MapsModels.popupAberta = false
            popupWindow.dismiss()
        }

        buttonPopupS.setOnClickListener {

            //gerenciaImpulsionamentos(posicao)


            val intent = Intent(this, impulsionarPromoActivity::class.java)
            intent.putExtra("nome", array.get(posicao))
            intent.putExtra("img", array.get(posicao+1))
            intent.putExtra("plano", MapsModels.plano)
            intent.putExtra("bdPet", MapsModels.petBDseForEmpresario)
            intent.putExtra("preco", array.get(posicao+2))
            intent.putExtra("email", MapsModels.userMail)
            intent.putExtra("desc", array.get(posicao+3))
            intent.putExtra("tipo", tipoProd)
            startActivity(intent)

            MapsModels.popupAberta = false
            popupWindow.dismiss()
        }


        txtTitulo.text = titulo
        txtTexto.text = texto


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            popupWindow.dismiss()
            MapsModels.popupAberta = false
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.lay_maps)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }

    //click listener da recycleview da loja
    interface ClickListener {
        fun onClick(view: View, position: Int)

        fun onLongClick(view: View?, position: Int)
    }

    //gerencia do click da recycleview da loja
    internal class RecyclerTouchListener(context: Context, recyclerView: RecyclerView, private val clickListener: arealojista.ClickListener?) : RecyclerView.OnItemTouchListener {

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

    //Métodos finais, de verificar se o usuário tem pedidos em aberto e se tiver, leva-lo para a página.
    //este método será chamado após todas as queries terem sido feitas.
    fun QueryPedidosParaFinalizar (){

        FirebaseDatabase.getInstance().reference.child("compras").orderByChild("cliente").equalTo(MapsModels.userBD)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //EncerraDialog()
                            //criar usuário
                            //createUser()
                        } else {

                            var values: String

                            values = querySnapshot.child("status").value.toString()
                            if (values.equals("aguardando confirmacao") || values.equals("vendedor confirma entrega")){
                                val btnTemPedidoParaFechar : ImageView = findViewById(R.id.btnTemPedidoParaFechar)
                                btnTemPedidoParaFechar.visibility = View.VISIBLE


                                //(btnTemPedidoParaFechar.drawable as AnimatedVectorDrawable).start()


                                btnTemPedidoParaFechar.setOnClickListener {
                                    val btnMinhasCompras:Button = findViewById(R.id.menu_btnMinhasCompras)
                                    //btnMinhasCompras.performClick()
                                    abreMinhasNotificacoes(0)
                                }

                                /*
                                val btnAbreMinhasCompras: Button = findViewById(R.id.menu_btnMinhasCompras)
                                btnAbreMinhasCompras.performClick()

                                 */
                            } else {
                                btnTemPedidoParaFechar.visibility=View.GONE
                            }

                        }
                    }

                    //SetUpRecycleViewDaLoja()
                    //EncerraDialog()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })    //addValueEventListener

    }

    fun abreMinhasNotificacoes (opcao: Int){

        if (opcao==0){
            val intent = Intent(this, minhasComprasActivity::class.java)
            intent.putExtra("userBD", MapsModels.userBD)
            startActivity(intent)
        } else {
            //a diferença entre autonomo e usuario será verificada dentro da activity
            val intent = Intent(this, minhasVendas::class.java)
            intent.putExtra("userBD", MapsModels.userBD)
            intent.putExtra("tipo", MapsModels.tipo)
            intent.putExtra("petBD", MapsModels.petBDseForEmpresario)
            startActivity(intent)
        }
    }

    //Métodos finais, de verificar se o usuário tem pedidos em aberto e se tiver, leva-lo para a página.
    //este método será chamado após todas as queries terem sido feitas.
    fun QueryPedidosParaFinalizarDoEmpresario (){

        FirebaseDatabase.getInstance().reference.child("compras").orderByChild("petshop").equalTo(MapsModels.petBDseForEmpresario)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //EncerraDialog()
                            //criar usuário
                            //createUser()
                        } else {

                            var values: String

                            values = querySnapshot.child("status").value.toString()
                            if (values.equals("aguardando confirmacao") || values.equals("cliente confirma recebimento")){

                                val btnTemPedidoParaFechar : ImageView = findViewById(R.id.btnTemPedidoParaFechar)
                                btnTemPedidoParaFechar.visibility = View.VISIBLE
                                //(btnTemPedidoParaFechar.drawable as AnimatedVectorDrawable).start()

                                btnTemPedidoParaFechar.setOnClickListener {
                                    //val btnMinhasVendas:Button = findViewById(R.id.menu_btnMinhasVendas)
                                    //btnMinhasVendas.performClick()
                                    abreMinhasNotificacoes(1)
                                }
                                /*
                                val btnMinhasVendas:Button = findViewById(R.id.menu_btnMinhasVendas)
                                btnMinhasVendas.performClick()

                                 */
                            }

                        }
                    }

                    //SetUpRecycleViewDaLoja()
                    //EncerraDialog()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })    //addValueEventListener


    }

    fun ListenersDoBotaoQueAcompanhaTela (btn: Button, img: ImageView){

        var myScroll: ScrollView = findViewById(R.id.scrollView3)
        //val btn: Button = findViewById(R.id.lay_loja_btnAbrirCarrinho)

        myScroll.viewTreeObserver.addOnScrollChangedListener {
            var posicaoAntiga = MapsModels.posicao
            MapsModels.posicao = myScroll.scrollY
            if (MapsModels.posicao<posicaoAntiga) {
                val animset = AnimatorSet()
                val botaoMovendo = ObjectAnimator.ofFloat(
                    btn,
                    "translationY",
                    posicaoAntiga.toFloat(),
                    MapsModels.posicao.toFloat()
                )
                val imgMovendo = ObjectAnimator.ofFloat(
                    img,
                    "translationY",
                    posicaoAntiga.toFloat(),
                    MapsModels.posicao.toFloat()
                )
                animset.play(botaoMovendo).with(imgMovendo)
                animset.setDuration(100)
                animset.start()
            } else {

                val animset = AnimatorSet()
                val botaoMovendo = ObjectAnimator.ofFloat(
                    btn,
                    "translationY",
                    MapsModels.posicao.toFloat(),
                    posicaoAntiga.toFloat()
                )
                val imgMovendo = ObjectAnimator.ofFloat(
                    img,
                    "translationY",
                    posicaoAntiga.toFloat(),
                    MapsModels.posicao.toFloat()
                )
                animset.play(botaoMovendo).with(imgMovendo)
                animset.play(botaoMovendo)
                animset.setDuration(100)
                animset.start()

            }
        }
    }
















    //MEtodos da venda Apenas

    //clique da recyclewview dentro do carrinho está aqui. Aqui apaga o item da recycleview
    fun SetUpRecycleViewDoCarrinho (tipo: String){

        //INICIO DO CODIGO DA RECYCLEVIEW
        var adapter: MinhaLojaRecyclerAdapter = MinhaLojaRecyclerAdapter(this, arrayNomesCarrinho, arrayImgCarrinho, arrayDescCarrinho, arrayPrecoCarrinho, arrayBDCarrinho)

        //chame a recyclerview
        var recyclerView: RecyclerView = findViewById(R.id.carrinho_recycleview)

        //define o tipo de layout (linerr, grid)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)

        //coloca o adapter na recycleview
        recyclerView.adapter = adapter

        recyclerView.layoutManager = linearLayoutManager

        // Notify the adapter for data change.
        adapter.notifyDataSetChanged()

        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView.addOnItemTouchListener(
            arealojista.RecyclerTouchListener(
                this,
                recyclerView,
                object : arealojista.ClickListener {

                    override fun onClick(view: View, position: Int) {
                        //Log.d("teste", arrayNomes.get(position))

                        val layCarrinho :ConstraintLayout= findViewById(R.id.lay_carrinho)
                        layCarrinho.setOnTouchListener(object : View.OnTouchListener {
                            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                                // ignore all touch events
                                return true
                            }
                        })

                        if (MapsModels.popupAberta==false){
                            //verificar aqui
                            openPopUpRemoverDoCarrinho("Atenção", "Deseja remover este item do seu carrinho?", "Sim, remover", "Não", position, tipo)
                        }


                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                })
        )
        //FIM DA RECYCLEVIEW

    }

    fun queryItensDaLojaParaRecycleView (bdEscolhido: String, userOuProp: String) {

        arrayNomes.clear()
        arrayBD.clear()
        arrayDesc.clear()
        arrayImg.clear()
        arrayPreco.clear()
        arrayTipo.clear()

        if (userOuProp.equals("prop")) {
            openPopUp(
                "Olá proprietário",
                "As edições na loja devem ser feitas na área especial do proprietário. Aqui você pode conferir como o cliente vê seu espaço. Você também pode impulsionar uma promoção clicando nos ites à venda. A promoção impulsionada aparece em destaque na tela do usuário.",
                false,
                "n",
                "n",
                "n"
            )
        }
        FirebaseDatabase.getInstance().reference.child("petshops").child(bdEscolhido).child("produtos").orderByChild("controle").equalTo("item")
            //FirebaseDatabase.getInstance().reference.child("petshops").child(bdDoPet).child("produtos").orderByChild("controle").equalTo("item")
            .addListenerForSingleValueEvent(object : ValueEventListener {
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

                            values = querySnapshot.child("nome").value.toString()
                            arrayNomes.add(values)
                            values = querySnapshot.child("desc").value.toString()
                            arrayDesc.add(values)
                            values = querySnapshot.child("preco").value.toString()

                            val precoProv = MapsController.currencyTranslation(values)

                            arrayPreco.add(precoProv)
                            values = querySnapshot.child("img").value.toString()
                            arrayImg.add(values)
                            values = querySnapshot.child("tipo").value.toString()
                            arrayTipo.add(values)
                            values = querySnapshot.key.toString()
                            arrayBD.add(values)

                        }
                    }

                    setClicksDoMenuCategoria()
                    SetUpRecycleViewDaLoja(userOuProp)
                    //SetUpRecycleViewDaLoja()
                    //EncerraDialog()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })  //addValueEventListener


    }

    //esta popup abre quando pergunta se quer adicionar um item ao carrinho
    fun openPopUpAddItemCarrinho (titulo: String, texto:String, btnSim: String, btnNao: String, posicao: Int, array: MutableList<String>, tipoProd:String) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao


        MapsModels.popupAberta=true

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
            slideOut.slideEdge = Gravity.LEFT
            popupWindow.exitTransition = slideOut

        }


        // Get the widgets reference from custom view
        val buttonPopupN = view.findViewById<Button>(R.id.btnReclamar)
        val buttonPopupS = view.findViewById<Button>(R.id.BtnRecebimento)
        val buttonPopupOk = view.findViewById<Button>(R.id.popupBtnOk)
        val txtTitulo = view.findViewById<TextView>(R.id.popupTitulo)
        val txtTexto = view.findViewById<TextView>(R.id.popupTexto)

        //vai exibir os botões com textos e esconder o btn ok
        buttonPopupOk.visibility = View.GONE
        //exibe e ajusta os textos dos botões
        buttonPopupN.text = btnNao
        buttonPopupS.text = btnSim

        // Set a click listener for popup's button widget
        buttonPopupN.setOnClickListener{
            // Dismiss the popup window
            MapsModels.popupAberta = false
            popupWindow.dismiss()
        }

        buttonPopupS.setOnClickListener {

            //rola a tela pro topo da loja pra ver o carrinho
            val scrollViewLoja: ScrollView = findViewById(R.id.scrollView3)
            scrollView3.scrollY = 0

            arrayNomesCarrinho.add(array.get(posicao))
            arrayImgCarrinho.add(array.get(posicao+1))
            arrayPrecoCarrinho.add(array.get(posicao+2))
            arrayDescCarrinho.add(array.get(posicao+3))
            arrayBDCarrinho.add(array.get(posicao+4))
            arrayTipoCarrinho.add(tipoProd)

            val layCarrinho : ConstraintLayout = findViewById(R.id.lay_carrinho)
            //layCarrinho.visibility = View.VISIBLE
            var btnAbrirCarrinho : Button = findViewById(R.id.lay_loja_btnAbrirCarrinho)
            btnAbrirCarrinho.performClick()

            CalculaTotalCompra(MapsModels.bdDoPet)

            layCarrinho.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    // ignore all touch events
                    return true
                }
            })

            SetUpRecycleViewDoCarrinho(tipoProd)
            MapsModels.popupAberta = false
            popupWindow.dismiss()
        }


        txtTitulo.text = titulo
        txtTexto.text = texto


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            popupWindow.dismiss()
            MapsModels.popupAberta = false
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.lay_maps)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }
    /*
    //esta popup abre quando pergunta se quer adicionar um item ao carrinho
    fun openPopUpAddItemCarrinho (titulo: String, texto:String, btnSim: String, btnNao: String, posicao: Int) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao

        popupAberta=true

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
            slideOut.slideEdge = Gravity.LEFT
            popupWindow.exitTransition = slideOut

        }


        // Get the widgets reference from custom view
        val buttonPopupN = view.findViewById<Button>(R.id.btnReclamar)
        val buttonPopupS = view.findViewById<Button>(R.id.BtnRecebimento)
        val buttonPopupOk = view.findViewById<Button>(R.id.popupBtnOk)
        val txtTitulo = view.findViewById<TextView>(R.id.popupTitulo)
        val txtTexto = view.findViewById<TextView>(R.id.popupTexto)

        //vai exibir os botões com textos e esconder o btn ok
        buttonPopupOk.visibility = View.GONE
        //exibe e ajusta os textos dos botões
        buttonPopupN.text = btnNao
        buttonPopupS.text = btnSim

        // Set a click listener for popup's button widget
        buttonPopupN.setOnClickListener{
            // Dismiss the popup window
            popupAberta = false
            popupWindow.dismiss()
        }

        buttonPopupS.setOnClickListener {

            arrayNomesCarrinho.add(arrayNomes.get(posicao))
            arrayBDCarrinho.add(arrayBD.get(posicao))
            arrayDescCarrinho.add(arrayDesc.get(posicao))
            arrayImgCarrinho.add(arrayImg.get(posicao))
            arrayPrecoCarrinho.add(arrayPreco.get(posicao))

            val layCarrinho : ConstraintLayout = findViewById(R.id.lay_carrinho)
            //layCarrinho.visibility = View.VISIBLE
            var btnAbrirCarrinho : Button = findViewById(R.id.lay_loja_btnAbrirCarrinho)
            btnAbrirCarrinho.performClick()

            CalculaTotalCompra(bdDoPet)

            layCarrinho.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    // ignore all touch events
                    return true
                }
            })

            SetUpRecycleViewDoCarrinho()
            popupAberta = false
            popupWindow.dismiss()
        }


        txtTitulo.text = titulo
        txtTexto.text = texto


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            popupWindow.dismiss()
            popupAberta = false
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.lay_maps)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }
     */

    //esta popup abre e pergunta se quer remover um item que está no carrinho.
    fun openPopUpRemoverDoCarrinho (titulo: String, texto:String, btnSim: String, btnNao: String, posicao: Int, tipoProd: String) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao

        MapsModels.popupAberta=true
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

        val layCarrinho :ConstraintLayout= findViewById(R.id.lay_carrinho)
        layCarrinho.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                // ignore all touch events
                return true
            }
        })

        // Get the widgets reference from custom view
        val buttonPopupN = view.findViewById<Button>(R.id.btnReclamar)
        val buttonPopupS = view.findViewById<Button>(R.id.BtnRecebimento)
        val buttonPopupOk = view.findViewById<Button>(R.id.popupBtnOk)
        val txtTitulo = view.findViewById<TextView>(R.id.popupTitulo)
        val txtTexto = view.findViewById<TextView>(R.id.popupTexto)

        //vai exibir os botões com textos e esconder o btn ok
        buttonPopupOk.visibility = View.GONE
        //exibe e ajusta os textos dos botões
        buttonPopupN.text = btnNao
        buttonPopupS.text = btnSim

        // Set a click listener for popup's button widget
        buttonPopupN.setOnClickListener{
            // Dismiss the popup window
            popupWindow.dismiss()
            MapsModels.popupAberta = false
        }

        buttonPopupS.setOnClickListener {

            var size = arrayNomesCarrinho.size
            arrayNomesCarrinho.removeAt(posicao)
            arrayBDCarrinho.removeAt(posicao)
            arrayDescCarrinho.removeAt(posicao)
            arrayImgCarrinho.removeAt(posicao)
            arrayPrecoCarrinho.removeAt(posicao)

            size = arrayNomesCarrinho.size
            SetUpRecycleViewDoCarrinho("anything")

            popupWindow.dismiss()
            MapsModels.popupAberta = false
            CalculaTotalCompra(MapsModels.bdDoPet)
        }


        txtTitulo.text = titulo
        txtTexto.text = texto


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            popupWindow.dismiss()
            MapsModels.popupAberta = false
            //Fecha a janela ao clicar fora também
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.lay_maps)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }

    //Envia mensagem pro whatsapp
    fun openWhatsApp(whatsApp: String, produtos: String, formaDePagamento: String, bandeira: String, endereco: String, valorVenda: String){

        val pm:PackageManager = packageManager
        try {
            val waIntent: Intent = Intent(Intent.ACTION_SEND)
            waIntent.setPackage("com.whatsapp")
            //sendIntent.setPackage("com.whatsapp");
            waIntent.type = "text/plain"

            var formaDePagamentoFinal : String
            if (bandeira.equals("nao")){
                formaDePagamentoFinal = formaDePagamento
            } else {
                formaDePagamentoFinal = formaDePagamento+" "+bandeira
            }
            val text: String  = "Nova venda.\n\nProdutos: "+produtos+"\n\nForma de pagamento: "+formaDePagamentoFinal+"\n\nEndereço da entrega: "+endereco+"\n"+valorVenda+"\nJá inclusas taxas de entrega, caso existam.\n\nObs: Esta é uma mensagem automática gerada pelo aplicativo, mas pode ter sido editada pelo dono da empresa."

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



    /*
    fun SomaEstaVenda (petBD: String, data:String, preco: String){

        //FirebaseDatabase.getInstance().reference.child("produtos").child(cidade).child(nomeProduto).orderByChild("controle").equalTo("controle")
        val rootRef = databaseReference.child("vendaCadaPet").child(petBD).child(data)
        //val rootRef = databaseReference.child("compras").child(bdDaCompra)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                // EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()){

                    var values: String
                    values = p0.child("valor").value.toString()
                    values = MapsController.CalculeEstesValoresEmDinheiro(values, preco, 1)
                    databaseReference.child("vendaCadaPet").child(petBD).child(data).child("valor").setValue(values)

                } else {

                    databaseReference.child("vendaCadaPet").child(petBD).child(data).child("valor").setValue(preco)

                }

            }

        })


    }
     */


    fun CalculaTotalCompra (bdDoPet: String){  //tipoProd armazena o tipo de produto. Vai ser usado caso seja ração para colocar um observador e lembrar o user se quer comrpar ração novamente

        var total = 0
        val size = arrayPrecoCarrinho.size
        var cont=0
        var str:String = "inicial"
        var string:String = "inicial"

        if (size>0) {
            while (cont < size) {

                str = arrayPrecoCarrinho.get(cont)
                str = str.toString().replace("R$", "")
                str = str.replace(",", "").trim()
                str = str.replace(".", "").trim()

                /*
                var eCentavo = false
                val firstChar: Char = str.get(0)
                 string = firstChar.toString()

                //se for centavo eCentavo vai ser true
                if (string.equals("0")){
                    eCentavo = true
                }

                total = total+str.toInt()
                //se for centavo, vai receber o 0 na frente
                if (eCentavo){
                    string = "0"+total
                }

                cont++

                 */

                val firstChar: Char = str.get(0)
                string = firstChar.toString()
                if (string.equals("0")){
                    total = total + (str).toInt()
                    if (total<100){
                        string = "0"+total
                    } else {
                        string = total.toString()
                    }
                } else {
                    total = total + (str).toDouble().toInt()
                    string = total.toString()
                }

                //total = total + (str).toDouble().toInt()
                cont++

            }

            val tvTotal: TextView = findViewById(R.id.carrinho_tvTotal)
            tvTotal.text = "Total: "+MapsController.currencyTranslation(string)


            /*
            val tvTotal: TextView = findViewById(R.id.carrinho_tvTotal)
            tvTotal.setText("Total: "+currencyTranslation(string))
             */

            //val tvTotal: TextView = findViewById(R.id.carrinho_tvTotal)
            //tvTotal.setText("Total: "+currencyTranslation((total).toString()))


        } else {
            val tvTotal: TextView = findViewById(R.id.carrinho_tvTotal)
            tvTotal.text = "Total: R$0,00"
        }

        val btnFinalizarCompra: Button = findViewById(R.id.carrinho_btnFinalizaCompra)
        btnFinalizarCompra.setOnClickListener {
            val tvTotal: TextView = findViewById(R.id.carrinho_tvTotal)
            if (tvTotal.text.equals("Total: R$0,00")){
                Toast.makeText(this, "Você não tem nenhum item no carrinho.", Toast.LENGTH_SHORT).show()
            } else {
                openPopUpConfirmaCompra("Finalizando compra", "Ao confirmar você vai combinar o pagamento com o vendedor. O valor total da sua compra foi de "+MapsController.currencyTranslation(string)+". Você confirma que quer fazer esta compra?", "Confirmar compra", "Não", MapsController.currencyTranslation(string), bdDoPet)
            }
        }
    }

    //esta popup abre e pergunta se quer remover um item que está no carrinho.
    fun openPopUpConfirmaCompra (titulo: String, texto:String, btnSim: String, btnNao: String, total: String, bdDoPet: String) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao

        MapsModels.popupAberta=true
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

        val layCarrinho :ConstraintLayout= findViewById(R.id.lay_carrinho)
        layCarrinho.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                // ignore all touch events
                return true
            }
        })

        // Get the widgets reference from custom view
        val buttonPopupN = view.findViewById<Button>(R.id.btnReclamar)
        val buttonPopupS = view.findViewById<Button>(R.id.BtnRecebimento)
        val buttonPopupOk = view.findViewById<Button>(R.id.popupBtnOk)
        val txtTitulo = view.findViewById<TextView>(R.id.popupTitulo)
        val txtTexto = view.findViewById<TextView>(R.id.popupTexto)

        //vai exibir os botões com textos e esconder o btn ok
        buttonPopupOk.visibility = View.GONE
        //exibe e ajusta os textos dos botões
        buttonPopupN.text = btnNao
        buttonPopupS.text = btnSim

        // Set a click listener for popup's button widget
        buttonPopupN.setOnClickListener{
            // Dismiss the popup window
            popupWindow.dismiss()
            MapsModels.popupAberta = false
        }

        buttonPopupS.setOnClickListener {

            fechaCompra(total, bdDoPet)
            popupWindow.dismiss()
            MapsModels.popupAberta = false
        }


        txtTitulo.text = titulo
        txtTexto.text = texto


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            popupWindow.dismiss()
            MapsModels.popupAberta = false
            //Fecha a janela ao clicar fora também
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.lay_maps)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }

    //pega os dados finais do pet para fechar a compra
    fun fechaCompra (total: String, bdDoPet: String){

        //aqui finalizar a compra. Mostrar formas de pagamento, confirmar endereço e mandar msg pro whatzapp.
        //cria nova acitivty gerenciar isso para aliviar esta
        var fazEntrega = false
        var valorEntrega = "nao"
        var whatsAppNumber = "nao"
        var nomePet = "nao"
        var bairro = "nao"
        var cidade = "nao"
        var estado = "nao"

        ChamaDialog()
        val rootRef = databaseReference.child("petshops").child(bdDoPet)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")
                var values: String
                values = p0.child("servicos").child("entrega").value.toString()
                if (values.equals("sim")){
                    fazEntrega = true
                }

                //dados georeferenciados para relatórios depois
                values = p0.child("bairro").value.toString()
                bairro = values
                values = p0.child("cidade").value.toString()
                cidade = values
                values = p0.child("estado").value.toString()
                estado = values

                values = p0.child("dddCel").value.toString()
                whatsAppNumber = values
                values = p0.child("cel").value.toString()
                whatsAppNumber = whatsAppNumber+values

                values = p0.child("nome").value.toString()
                nomePet = values

                values = p0.child("entrega").value.toString()
                valorEntrega = values

                val layCompras : ConstraintLayout = findViewById(R.id.lay_compras)
                layCompras.visibility = View.VISIBLE

                val textView: TextView

                val radioEntrega : RadioButton = findViewById(R.id.compras_radioEntrega)
                val radioBuscar : RadioButton = findViewById(R.id.compras_radioBuscaNaLoja)

                textView = findViewById(R.id.compras_tvStatusEntrega)
                val tvTotalCompra : TextView = findViewById(R.id.compras_tvTotalCompra)
                if (fazEntrega){
                    if (valorEntrega.equals("gratis")){
                        textView.text = "A entrega é grátis"
                        val tvTotalCompra : TextView = findViewById(R.id.compras_tvTotalCompra)
                        tvTotalCompra.text = "O valor total da sua compra é: "+total
                    } else {
                        textView.text = "O valor da entrega é "+MapsController.currencyTranslation(valorEntrega)


                        var str:String = total.replace("R$", "")
                        str = str.replace(",", "").trim()
                        str = str.replace(".", "").trim()

                        var str2:String = valorEntrega.replace("R$", "")
                        str2 = str2.replace(",", "").trim()
                        str2 = str2.replace(".", "").trim()

                        val totalizando = (str).toInt()+(str2).toInt()
                        tvTotalCompra.text = "O valor total da sua compra é: "+MapsController.currencyTranslation(
                            totalizando.toString()
                        )
                    }

                    radioEntrega.isEnabled = true

                } else {
                    textView.text = "Este estabelecimento não faz entregas."
                    val tvTotalCompra : TextView = findViewById(R.id.compras_tvTotalCompra)
                    //tvTotalCompra.text = "O valor total da sua compra é: "+currencyTranslation(total)
                    tvTotalCompra.text = "O valor total da sua compra é: "+total   //reforma1
                    radioEntrega.isEnabled = false
                }



                radioEntrega.isChecked = true

                radioEntrega.setOnClickListener {
                    radioBuscar.isChecked = false
                    val layEndereco : ConstraintLayout = findViewById(R.id.layEndEntrega)
                    layEndereco.visibility = View.VISIBLE

                    //devolver o valor da entrega para o preço final. Isso acontece quando o usuário clicou em buscar na loja, entao retirou o valor da entrega. E aí agora vamos devolver esse valor.
                    var str:String = total.replace("R$", "")
                    str = str.replace(",", "").trim()
                    str = str.replace(".", "").trim()

                    var str2:String = valorEntrega.replace("R$", "")
                    str2 = str2.replace(",", "").trim()
                    str2 = str2.replace(".", "").trim()

                    val totalizando = (str).toInt()+(str2).toInt()
                    tvTotalCompra.text = "O valor total da sua compra é: "+MapsController.currencyTranslation(
                        totalizando.toString())

                }

                radioBuscar.setOnClickListener {
                    radioEntrega.isChecked = false
                    val layEndereco : ConstraintLayout = findViewById(R.id.layEndEntrega)
                    layEndereco.visibility = View.GONE

                    //retirar o valor da entrega
                    if (valorEntrega.equals("gratis")){
                        textView.text = "A entrega é grátis"
                        val tvTotalCompra : TextView = findViewById(R.id.compras_tvTotalCompra)
                        tvTotalCompra.text = "O valor total da sua compra é: "+MapsController.currencyTranslation(total)
                    } else {
                        textView.text = "O valor da entrega é "+MapsController.currencyTranslation(valorEntrega)

                        val tvTotalCompra : TextView = findViewById(R.id.compras_tvTotalCompra)
                        var str:String = total.replace("R$", "")
                        str = str.replace(",", "").trim()
                        str = str.replace(".", "").trim()

                        tvTotalCompra.text = "O valor total da sua compra é: "+MapsController.currencyTranslation(
                            str
                        )
                    }

                }

                val cbCredito : CheckBox = findViewById(R.id.pagamentos_cbCredito)
                val cbDebito : CheckBox = findViewById(R.id.pagamentos_cbDebito)
                val cbDinheiro: CheckBox = findViewById(R.id.pagamentos_cbDinheiro)

                values = p0.child("servicos").child("aceita_debito").value.toString()

                if (values.equals("nao")){
                    cbDebito.isEnabled = false
                    cbDebito.text = "Não aceita\ndébito"
                }

                values = p0.child("servicos").child("aceita_credito").value.toString()

                if (values.equals("nao")){
                    cbCredito.isEnabled = false
                    cbCredito.text = "Não aceita\ncrétido"
                }

                val cbMaster: CheckBox = findViewById(R.id.pagamentos_cbMaster)
                val cbVisa: CheckBox = findViewById(R.id.pagamentos_cbVisa)
                val cbElo: CheckBox = findViewById(R.id.pagamentos_cbElo)
                val cbOutros: CheckBox = findViewById(R.id.pagamentos_cbOutros)

                var bandeira = "nao"
                cbMaster.setOnClickListener {
                    cbVisa.isChecked = false
                    cbElo.isChecked = false
                    cbOutros.isChecked = false
                    bandeira = "MasterCard"
                }

                cbVisa.setOnClickListener {
                    cbMaster.isChecked = false
                    cbElo.isChecked = false
                    cbOutros.isChecked = false
                    bandeira = "Visa"
                }
                cbElo.setOnClickListener {
                    cbMaster.isChecked = false
                    cbVisa.isChecked = false
                    cbOutros.isChecked = false
                    bandeira = "Elo"
                }
                cbOutros.setOnClickListener {
                    cbMaster.isChecked = false
                    cbElo.isChecked = false
                    cbVisa.isChecked = false
                    bandeira = "Outros"
                }


                val layDetalhesDosCartoes: ConstraintLayout = findViewById(R.id.lay_formasDePagamento2)

                cbDebito.setOnClickListener {
                    if (cbDebito.isChecked){
                        layDetalhesDosCartoes.visibility = View.VISIBLE
                    } else {
                        if (!cbCredito.isChecked)
                            layDetalhesDosCartoes.visibility = View.GONE
                    }
                    cbCredito.isChecked = false
                    cbDinheiro.isChecked = false
                }

                cbCredito.setOnClickListener {
                    if (cbCredito.isChecked){
                        layDetalhesDosCartoes.visibility = View.VISIBLE
                    } else {
                        if (!cbDebito.isChecked){
                            layDetalhesDosCartoes.visibility = View.GONE
                        }
                    }
                    cbDebito.isChecked = false
                    cbDinheiro.isChecked = false
                }

                cbDinheiro.setOnClickListener {

                    layDetalhesDosCartoes.visibility = View.GONE
                    cbCredito.isChecked = false
                    cbDebito.isChecked = false

                }

                val currentLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                val endereco = MapsController.getAddress(currentLatLng, this@MapsActivity)
                val etEndereco: EditText = findViewById(R.id.entrega_etEndereco)
                etEndereco.setText(endereco)

                val btnFinalizar: Button = findViewById(R.id.compra_btnFinalizar)
                val btnVoltar: Button = findViewById(R.id.compra_btnVoltar)

                btnVoltar.setOnClickListener {
                    layCompras.visibility = View.GONE
                    radioBuscar.isChecked = false
                    radioEntrega.isChecked = false
                    cbCredito.isChecked=false
                    cbDebito.isChecked=false
                    cbDinheiro.isChecked=false
                    cbElo.isChecked=false
                    cbMaster.isChecked=false
                    cbVisa.isChecked=false
                    cbOutros.isChecked=false
                    entrega_etEndereco.setText("")
                    entrega_etNumero.setText("")
                    entrega_etComplemento.setText("")
                    hideKeyboard()
                }




                btnFinalizar.setOnClickListener {
                    //criar um Bd chamado compras e salvar lá o bd do pet e o bd do comprador para futuras buscas.
                    //salvar lá as informações

                    if (entrega_etEndereco.text.isEmpty() && radioEntrega.isChecked){
                        hideKeyboard()
                        entrega_etEndereco.requestFocus()
                        entrega_etEndereco.error = "Informe o endereço."
                    } else if (entrega_etNumero.text.isEmpty() && radioEntrega.isChecked){
                        hideKeyboard()
                        entrega_etNumero.requestFocus()
                        entrega_etNumero.error = "Informe o número da casa"
                    } else {

                        var buscaOuEntrega = "nao"
                        if (radioEntrega.isChecked){
                            buscaOuEntrega = "Entrega em domicílio"
                        }else {
                            buscaOuEntrega = "Busca na loja"
                        }
                        var formaPagamento = "nao"
                        if (cbDinheiro.isChecked){
                            formaPagamento = "Pagamento em dinheiro"
                        } else if (cbDebito.isChecked){
                            formaPagamento = "Pagamento em débito"
                        } else {
                            formaPagamento = "Pagamento em crédito"
                        }

                        hideKeyboard()
                        VendaEfetuada(buscaOuEntrega, formaPagamento, bandeira, entrega_etEndereco.text.toString(), entrega_etNumero.text.toString(), entrega_etComplemento.text.toString(), whatsAppNumber, tvTotalCompra.text.toString(), nomePet, bairro, cidade, estado, bdDoPet)
                        openPopUp("Compra realizada!", "Seu pedido foi enviado para a loja, maiores contatos via whatsapp.", false, "n", "n", "m")
                        layCompras.visibility = View.GONE
                        //limpar form
                        radioBuscar.isChecked = false
                        radioEntrega.isChecked = false
                        cbCredito.isChecked=false
                        cbDebito.isChecked=false
                        cbDinheiro.isChecked=false
                        cbElo.isChecked=false
                        cbMaster.isChecked=false
                        cbVisa.isChecked=false
                        cbOutros.isChecked=false
                        entrega_etEndereco.setText("")
                        entrega_etNumero.setText("")
                        entrega_etComplemento.setText("")

                    }



                }

                EncerraDialog()

            }
        })

    }



    //após o usuário confirmar a compra, chama este método que grava os dados no BD
    /* codigo original de backup
    fun VendaEfetuada (buscaOuEntrega: String, formaPagamento: String, bandeira: String, endereco: String, numero: String, complemento: String, whatsApp: String, precoFinal: String, nomePet: String, bairro: String, cidade: String, estado: String, bdDoPet: String){

        //compra finalizada
        //criar um BD com a compra onde precisa ter: Bd do user, itens, bddoPet
        //enviar para o whatzapp do pet o resumo
        //Criar a área onde acessa as compras do usuário. Fazer isso em outra activity
        //val newCad: DatabaseReference = databaseReference.child("compras").push()


        val newCad: String = databaseReference.child("compras").push().key.toString()

        //salva no shared para poder
        val sharedPref: SharedPreferences = getSharedPreferences(getString(R.string.sharedpreferences), 0) //0 é private mode
        val editor = sharedPref.edit()
        editor.putString("liberaServicoInicial", "1")
        editor.apply()

        databaseReference.child("compras").child(newCad).child("cliente").setValue(MapsModels.userBD)
        databaseReference.child("compras").child(newCad).child("petshop").setValue(bdDoPet)
        databaseReference.child("compras").child(newCad).child("BuscaOuEntrega").setValue(buscaOuEntrega)
        databaseReference.child("compras").child(newCad).child("FormaPgto").setValue(formaPagamento)
        if (formaPagamento.equals("Pagamento em dinheiro")){ //se nao for dinheiro, anotar a bandeira do cartão
            databaseReference.child("compras").child(newCad).child("bandeira_cartao").setValue("nao usado")
        } else {
            databaseReference.child("compras").child(newCad).child("bandeira_cartao").setValue(bandeira)
        }
        databaseReference.child("compras").child(newCad).child("valor").setValue(precoFinal)
        databaseReference.child("compras").child(newCad).child("Endereco_entrega").setValue(endereco)
        databaseReference.child("compras").child(newCad).child("Endereco_numero").setValue(numero)
        databaseReference.child("compras").child(newCad).child("Endereco").setValue(complemento)
        databaseReference.child("compras").child(newCad).child("status").setValue("aguardando confirmacao")
        databaseReference.child("compras").child(newCad).child("avaliacao_user").setValue("nao")
        databaseReference.child("compras").child(newCad).child("avaliacao_pet").setValue("nao")
        databaseReference.child("compras").child(newCad).child("nomePet").setValue(nomePet)

        //vamos salvar essa info aqui para depois poder exibir os serviços no recycleview. Isso só vai ocorrer se tiver serviços
        val provi = "nao"
        var contHere=0
        while (contHere<arrayTipoCarrinho.size){
            if (arrayTipoCarrinho.get(contHere).equals("servicos")){
                databaseReference.child("compras").child(newCad).child("servico").setValue("servicos")
                contHere=arrayTipoCarrinho.size
            }
            contHere++
        }


        val date = MapsController.GetDate()
        val dateInMillis = MapsController.ConvertDateToMillis(date)

        databaseReference.child("compras").child(newCad).child("data_compra").setValue(dateInMillis.toString())
        databaseReference.child("compras").child(newCad).child("hora_compra").setValue(MapsController.GetHour())

        databaseReference.child("compras").child(newCad).child("bairro").setValue(bairro)
        databaseReference.child("compras").child(newCad).child("cidade").setValue(cidade)
        databaseReference.child("compras").child(newCad).child("estado").setValue(estado)

        var produtos: String = ""
        var cont=0
        while (cont<arrayNomesCarrinho.size){
            if (cont==0){
                produtos = arrayNomesCarrinho.get(cont)
                //girafinha
            } else {
                produtos = produtos+", "+arrayNomesCarrinho.get(cont)
            }
            cont++
        }

        if (buscaOuEntrega.equals("Entrega em domicílio")){
            openWhatsApp(whatsApp, produtos, formaPagamento, bandeira, endereco+", "+numero+" - "+complemento, precoFinal)
        } else {
            val enderecoWhat = "Vai buscar na loja"
            openWhatsApp(whatsApp, produtos, formaPagamento, bandeira, enderecoWhat, precoFinal)
        }

        //vai criar outro no para registrar detalhes das vendas de todos os pets
        //var newVenda: String = databaseReference.child("vendaCadaPet").push().key.toString()
        cont=0

        while (cont<arrayPrecoCarrinho.size){

            //parte da venda
            val newCad2: String = databaseReference.child("compras").child(newCad).child("produtos_vendidos").push().key.toString()

            databaseReference.child("compras").child(newCad).child("produtos_vendidos").child(newCad2).child("item").setValue(arrayNomesCarrinho.get(cont))
            var str:String = arrayPrecoCarrinho.get(cont).replace("R$", "")
            str = str.replace(",", "").trim()
            str = str.replace(".", "").trim()
            databaseReference.child("compras").child(newCad).child("produtos_vendidos").child(newCad2).child("preco").setValue(str)
            databaseReference.child("compras").child(newCad).child("produtos_vendidos").child(newCad2).child("descricao").setValue(arrayDescCarrinho.get(cont))
            databaseReference.child("compras").child(newCad).child("produtos_vendidos").child(newCad2).child("controle").setValue("item") //para busca numa query
            databaseReference.child("compras").child(newCad).child("produtos_vendidos").child(newCad2).child("tipo").setValue(arrayTipoCarrinho.get(cont)) //para busca numa query
            //fim da venda

            //Agora vamos salvar o valor da venda. Isso vai servir para retirar extrato depois
            val path: String = MapsController.GetMonthWithYear()
            val precofinal2: String = precoFinal.replace("O valor total da sua compra é: R$", "")  //retira o texto para ficar apenas o valor
            SomaEstaVenda(bdDoPet, path, precofinal2) // aqui ele faz a soma das vendas e salva no BD


            //parte nova da adm, registrar as vendas de cada produto
            //E agora salvar os produtos mais vendidos
            ExisteEsteProduto(cidade, arrayNomesCarrinho.get(cont), arrayPrecoCarrinho.get(cont))

            cont++
        }

        /*
        newCad.child("cliente").setValue(userBD)
        newCad.child("petshop").setValue(bdDoPet)
        //newCad.child("localDaEntrega").setValue(lastLocation)
        newCad.child("BuscaOuEntrega").setValue(buscaOuEntrega)
        newCad.child("FormaPgto").setValue(formaPagamento)
        if (formaPagamento.equals("Pagamento em dinheiro")){ //se nao for dinheiro, anotar a bandeira do cartão
            newCad.child("bandeira_cartao").setValue("nao usado")
        } else {
            newCad.child("bandeira_cartao").setValue(bandeira)
        }
        newCad.child("valor").setValue(precoFinal)
        newCad.child("Endereco_entrega").setValue(endereco)
        newCad.child("Endereco_numero").setValue(numero)
        newCad.child("Endereco").setValue(complemento)
        newCad.child("status").setValue("aguardando confirmacao")
        newCad.child("avaliacao_user").setValue("nao")
        newCad.child("avaliacao_pet").setValue("nao")
        newCad.child("nomePet").setValue(nomePet)

        newCad.child("data_compra").setValue(GetDate())
        newCad.child("hora_compra").setValue(GetHour())

        var produtos: String = ""
        var cont=0
        while (cont<arrayNomesCarrinho.size){
            if (cont==0){
                produtos = arrayNomesCarrinho.get(cont)
                //girafinha
            } else {
                produtos = produtos+", "+arrayNomesCarrinho.get(cont)
            }
            cont++
        }

        if (buscaOuEntrega.equals("Entrega em domicílio")){
            val enderecoWhats = endereco
            openWhatsApp(whatsApp, produtos, formaPagamento, bandeira, endereco+", "+numero+" - "+complemento, precoFinal)
        } else {
            val enderecoWhat = "Vai buscar na loja"
            openWhatsApp(whatsApp, produtos, formaPagamento, bandeira, enderecoWhat, precoFinal)
        }


        cont=0
        while (cont<arrayPrecoCarrinho.size){

            newCad.child("produtos_vendidos").child("item").setValue(arrayNomesCarrinho.get(cont))
            var str:String = arrayPrecoCarrinho.get(cont).replace("R$", "")
            str = str.replace(",", "").trim()
            str = str.replace(".", "").trim()
            newCad.child("produtos_vendidos").child("preco").setValue(str)
            newCad.child("produtos_vendidos").child("descricao").setValue(arrayDescCarrinho.get(cont))
            cont++
        }

         */

        //vamos registrar agora o alerta caso seja compra de ração
        cont=0
        while (cont<arrayTipoCarrinho.size){
            if (arrayTipoCarrinho.get(cont).equals("racao")){ //se tiver ração ele chama o metodo
                ativarLembrete(bdDoPet, arrayNomesCarrinho.get(cont))
                cont=arrayTipoCarrinho.size //se achou uma ração, ele acaba com esse while pois nao preciso verificar mais de uma vez. Mesmo se o user comprou varias rações, nao importa pois nao vou avisar varias vezes. Vai ser somente um aviso.
            }
            cont++
        }

        arrayPrecoCarrinho.clear()
        arrayImgCarrinho.clear()
        arrayDescCarrinho.clear()
        arrayBDCarrinho.clear()
        arrayNomesCarrinho.clear()
        arrayTipoCarrinho.clear()

        val layCarrinho : ConstraintLayout = findViewById(R.id.lay_carrinho)
        layCarrinho.visibility = View.GONE
        val layLoja: ConstraintLayout = findViewById(R.id.lay_loja)
        layLoja.visibility = View.GONE

        val checkboxDin: CheckBox = findViewById(R.id.pagamentos_cbDinheiro)
        checkboxDin.isChecked=true //deixa o diheiro marcado como padrão para a próxima compra. Antes passava direto sem a pessoa informar

        val bottomBar: ConstraintLayout = findViewById(R.id.bottomBar)
        val imgNoBtn: ImageView = findViewById(R.id.imageView4)
        imgNoBtn.setImageResource(R.drawable.seta_para_carrinho)
        bottomBar.visibility = View.VISIBLE

        val btnMenu: ImageView = findViewById(R.id.lay_Maps_MenuBtn)  //imageview que e o botão de menu
        val btnLupa: Button = findViewById(R.id.btnInserirEndereco) //imagem da busca de endereço
        btnMenu.visibility = View.VISIBLE
        btnLupa.visibility = View.VISIBLE
        val btnCentral: Button = findViewById(R.id.btnLocalizaNovamente)
        btnCentral.visibility = View.VISIBLE
        imgNoBtn.visibility = View.VISIBLE
        btnCentral.performClick()
        //(imgNoBtn.drawable as AnimatedVectorDrawable).start()


    }
     */

    fun VendaEfetuada (buscaOuEntrega: String, formaPagamento: String, bandeira: String, endereco: String, numero: String, complemento: String, whatsApp: String, precoFinal: String, nomePet: String, bairro: String, cidade: String, estado: String, bdDoPet: String){

        val produtos = MapsModels.saveVendaNoBd(this, buscaOuEntrega, formaPagamento, bandeira, endereco, numero, complemento, whatsApp, precoFinal, nomePet, bairro, cidade, estado, bdDoPet, arrayTipoCarrinho, arrayNomesCarrinho, arrayPrecoCarrinho, arrayDescCarrinho)

        if (buscaOuEntrega.equals("Entrega em domicílio")){
            openWhatsApp(whatsApp, produtos, formaPagamento, bandeira, endereco+", "+numero+" - "+complemento, precoFinal)
        } else {
            val enderecoWhat = "Vai buscar na loja"
            openWhatsApp(whatsApp, produtos, formaPagamento, bandeira, enderecoWhat, precoFinal)
        }

        arrayPrecoCarrinho.clear()
        arrayImgCarrinho.clear()
        arrayDescCarrinho.clear()
        arrayBDCarrinho.clear()
        arrayNomesCarrinho.clear()
        arrayTipoCarrinho.clear()

        val layCarrinho : ConstraintLayout = findViewById(R.id.lay_carrinho)
        layCarrinho.visibility = View.GONE
        val layLoja: ConstraintLayout = findViewById(R.id.lay_loja)
        layLoja.visibility = View.GONE

        val checkboxDin: CheckBox = findViewById(R.id.pagamentos_cbDinheiro)
        checkboxDin.isChecked=true //deixa o diheiro marcado como padrão para a próxima compra. Antes passava direto sem a pessoa informar

        val bottomBar: ConstraintLayout = findViewById(R.id.bottomBar)
        val imgNoBtn: ImageView = findViewById(R.id.imageView4)
        imgNoBtn.setImageResource(R.drawable.seta_para_carrinho)
        bottomBar.visibility = View.VISIBLE

        val btnMenu: ImageView = findViewById(R.id.lay_Maps_MenuBtn)  //imageview que e o botão de menu
        val btnLupa: Button = findViewById(R.id.btnInserirEndereco) //imagem da busca de endereço
        btnMenu.visibility = View.VISIBLE
        btnLupa.visibility = View.VISIBLE
        val btnCentral: Button = findViewById(R.id.btnLocalizaNovamente)
        btnCentral.visibility = View.VISIBLE
        imgNoBtn.visibility = View.VISIBLE
        btnCentral.performClick()
        //(imgNoBtn.drawable as AnimatedVectorDrawable).start()


    }


    //Fim dos métodos de venda






    //METODOS DOS STORIES
    fun queryStories(lat: Double, long: Double) {

        val latlong = lat + long
        var startAtval = latlong-(0.01f*MapsModels.raioBusca)
        var endAtval = latlong+(0.01f*MapsModels.raioBusca)

        //nova regra de ouro
        //Por conta das características da latitude e longitude, nao podemos usar o mesmo valor para startAtVal (pois fica a esquerda) e endAtVal(que fica a direita).
        //O que ocorre é que itens que ficam a esquerda acumulam a soma de valores negativos de latitude e longitude. Já os que ficam em endVal pegam o valor negativo da longitude mas as vezes pega positivo de latitude. Isso dava resulltado no final.
        //OBS: Isso é verdade no ocidente. Se um dia quem sabe passar pro oriente.
        //Então agora o que vamos fazer.
        //a val dif armazena a diferença que encontramos entre startatVal e até onde faria 6km no mapa. Se alguim dia for mudar o raio (agora é 0.6) vai ter que mexer nisso.
        //entao basta adiconar essa diferença a startAtVal antes da busca para ele corrigir o erro. A verificar se isto também precisa ser feito para endAtAval.


        startAtval = (MapsModels.dif+startAtval) //ajuste

        getInstance().reference.child("stories").orderByChild("latlong").startAt(startAtval).endAt(endAtval)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //EncerraDialog()
                            //criar usuário
                            //createUser()
                        } else {


                            //primeiro vamos apagar se o stories já não deveria mais existir
                            val dataLimite = querySnapshot.child("dataLimite").getValue().toString()
                            val dataAgora = MapsController.GetDate()


                            val format = SimpleDateFormat("dd/MM/yyyy")
                            val date1 = format.parse(dataAgora)
                            val date2 = format.parse(dataLimite)

                            if (date1.compareTo(date2) > 0){
                                //data1 maior do que data2. Então tem que apagar
                                //apagar bd
                                val key = querySnapshot.key.toString()
                                //temos que testar isso
                                databaseReference.child("stories").child(key).removeValue()

                            } else if (date1.compareTo(date2) == 0){  //se for igual, vamos comparar as horas pra saber se excedeu
                                val horaLimite = querySnapshot.child("horaLimite").getValue().toString()
                                val horaAgora = MapsController.GetHour()

                                val formatHora = SimpleDateFormat("hh:mm")
                                val hora1 = formatHora.parse(horaAgora)
                                val hora2 = formatHora.parse(horaLimite)

                                if (hora1.compareTo(hora2) >0){
                                    //apagar, a hora já é maior que a do inicio
                                    val key = querySnapshot.key.toString()
                                    //temos que testar isso
                                    databaseReference.child("stories").child(key).removeValue()

                                } else {

                                    //codigo aqui também copiado do de baixo

                                    //codigo aqui
                                    //carregar infos
                                    var values: String
                                    var valorProvi: String =""
                                    val delim = "!?!%&**!"


                                    if (querySnapshot.child("img").exists()){

                                        values= querySnapshot.child("img").getValue().toString()
                                        arrayImgStories.add(values)
                                        //values = querySnapshot.child("img").getValue().toString()
                                        //valorProvi = values+delim
                                    } else {

                                        arrayImgStories.add("nao")
                                        //valorProvi="nao"+delim
                                    }

                                    //aqui imgLink!?!%
                                    if (querySnapshot.child("message").exists()){
                                        values = querySnapshot.child("message").getValue().toString()
                                        valorProvi=valorProvi+values+delim
                                        //arrayStories.add(valorProvi)
                                    } else {
                                        valorProvi=valorProvi+"nao"+delim
                                        //arrayStories.add(valorProvi)
                                    }  //aqui   imgLink!?!%message!?!%

                                    if (querySnapshot.child("textColor").exists()){
                                        values = querySnapshot.child("textColor").getValue().toString()
                                        valorProvi=valorProvi+values+delim
                                        //arrayStories.add(valorProvi)
                                    } else {
                                        valorProvi=valorProvi+"nao"+delim
                                        //arrayStories.add(valorProvi)
                                    }  //aqui   imgLink!?!%message!?!%textColor

                                    if (querySnapshot.child("bgColor").exists()){
                                        values = querySnapshot.child("bgColor").getValue().toString()
                                        valorProvi=valorProvi+values+delim
                                        //arrayStories.add(valorProvi)
                                    } else {
                                        valorProvi=valorProvi+"nao"+delim
                                        //arrayStories.add(valorProvi)
                                    }  //aqui   imgLink!?!%message!?!%textColor!?!%bgColor

                                    if (querySnapshot.child("messagePosition").exists()){
                                        values = querySnapshot.child("messagePosition").getValue().toString()
                                        valorProvi=valorProvi+values+delim
                                        //arrayStories.add(valorProvi)
                                    } else {
                                        valorProvi=valorProvi+"nao"+delim
                                        //arrayStories.add(valorProvi)
                                    }  //aqui   imgLink!?!%message!?!%textColor!?!%bgColor!?!%messagePosition


                                    values = querySnapshot.key.toString()
                                    valorProvi = valorProvi+values+delim
                                    arrayStories.add(valorProvi)
                                    //aqui   imgLink!?!%message!?!%textColor!?!%bgColor!?!%messagePosition!?!%BD

                                    /*
                                    //array: DIVISOR: !?!%
                                    0 - img
                                    1 - message
                                    2 - textColor
                                    3 - bgColor
                                    4 - messagePosition
                                    5 - BD

                                     */


                                }

                            } else {

                                //codigo aqui
                                //carregar infos
                                var values: String
                                var valorProvi: String
                                val delim = "!?!%"

                                if (querySnapshot.child("img").exists()){
                                    values = querySnapshot.child("img").getValue().toString()
                                    //arrayStories.add(values)
                                    //valorProvi = arrayStories.get(0)+delim
                                    valorProvi = values+delim
                                } else {
                                    valorProvi="nao"+delim
                                    //arrayStories.add(valorProvi)
                                }  //aqui   imgLink!?!%

                                //aqui imgLink!?!%
                                if (querySnapshot.child("message").exists()){
                                    values = querySnapshot.child("message").getValue().toString()
                                    valorProvi=valorProvi+values+delim
                                    //arrayStories.add(valorProvi)
                                } else {
                                    valorProvi=valorProvi+"nao"+delim
                                    //arrayStories.add(valorProvi)
                                }  //aqui   imgLink!?!%message!?!%

                                if (querySnapshot.child("textColor").exists()){
                                    values = querySnapshot.child("textColor").getValue().toString()
                                    valorProvi=valorProvi+values+delim
                                    //arrayStories.add(valorProvi)
                                } else {
                                    valorProvi=valorProvi+"nao"+delim
                                    //arrayStories.add(valorProvi)
                                }  //aqui   imgLink!?!%message!?!%textColor

                                if (querySnapshot.child("bgColor").exists()){
                                    values = querySnapshot.child("bgColor").getValue().toString()
                                    valorProvi=valorProvi+values+delim
                                    //arrayStories.add(valorProvi)
                                } else {
                                    valorProvi=valorProvi+"nao"+delim
                                    //arrayStories.add(valorProvi)
                                }  //aqui   imgLink!?!%message!?!%textColor!?!%bgColor

                                if (querySnapshot.child("messagePosition").exists()){
                                    values = querySnapshot.child("messagePosition").getValue().toString()
                                    valorProvi=valorProvi+values+delim
                                    //arrayStories.add(valorProvi)
                                } else {
                                    valorProvi=valorProvi+"nao"+delim
                                    //arrayStories.add(valorProvi)
                                }  //aqui   imgLink!?!%message!?!%textColor!?!%bgColor!?!%messagePosition

                                values = querySnapshot.key.toString()
                                valorProvi = valorProvi+values
                                arrayStories.add(valorProvi)
                                //aqui   imgLink!?!%message!?!%textColor!?!%bgColor!?!%messagePosition!?!%BD

                                /*
                                //array: DIVISOR: !?!%
                                0 - img
                                1 - message
                                2 - textColor
                                3 - bgColor
                                4 - messagePosition
                                5 - BD

                                 */

                            }




                        }
                    }

                setupRecyclerViewStories()

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    EncerraDialog()
                    // ...
                }
            })


    }

    fun setupRecyclerViewStories(){


        //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
        var adapter: MeusStoriesrecyclerAdapter = MeusStoriesrecyclerAdapter(this, arrayStories, arrayImgStories)

        //chame a recyclerview
        var recyclerView: RecyclerView = findViewById(R.id.meusStoriesRecyclerView)

        //define o tipo de layout (linerr, grid)
        //var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        //coloca o adapter na recycleview
        recyclerView.adapter = adapter

        recyclerView.layoutManager = linearLayoutManager

        // Notify the adapter for data change.
        adapter.notifyDataSetChanged()


        //click
        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView.addOnItemTouchListener(
            arealojista.RecyclerTouchListener(
                this,
                recyclerView,
                object : arealojista.ClickListener {

                    override fun onClick(view: View, position: Int) {
                        //Log.d("teste", arrayStories.get(position))

                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                })
        )

        val layStories: ConstraintLayout = findViewById(R.id.layMaps_Stories)
        layStories.visibility = View.VISIBLE
        val btnAbreStories: Button = findViewById(R.id.layMaps_btnAbreStories)
        val btnFechaStories: Button = findViewById(R.id.btnFechaStories)

        btnFechaStories.setOnClickListener {
            layStories.visibility = View.GONE
            btnAbreStories.visibility = View.VISIBLE
        }

        btnAbreStories.setOnClickListener {
            layStories.visibility = View.VISIBLE
            btnAbreStories.visibility = View.GONE
        }

        if (arrayStories.size==0){
            btnFechaStories.performClick()
        }


    }











    /*********************************
     * ****METODOS DE PERMISSAO PARA O GPS**
     **********************************/

    fun upDateAppVerification(){


    }

    /*
    fun permissaoSeguindoGoogle():Boolean{
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                return false
            } else {
                // No explanation needed, we can request the permission.
                //ActivityCompat.requestPermissions(this,
                //    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 171)

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
                return false
            }

        } else {
            //permission granted
            return true
        }
    }

    fun requestTheDamnPermission(){
        ActivityCompat.requestPermissions(this,
           arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        171)

    }

     */

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode==FINE_LOCATION_CODE){

            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                //permissão concedida
                Toast.makeText(this, "Permissão concedida. Reiniciando a aplicação para achar sua localização.", Toast.LENGTH_SHORT).show()
                finish()


            } else {
                Toast.makeText(this, "Permissão não concedida. Não podemos acessar sua localização", Toast.LENGTH_SHORT).show()
                // permission denied, boo! Disable the
                // functionality that depends on this permission.

                //colocando clicks para chamar a permissão em tudo
                val btnMenu: ImageView = findViewById(R.id.lay_Maps_MenuBtn)
                btnMenu.setOnClickListener {
                    fineLocationPermission.checkPermission(this, FINE_LOCATION_CODE)
                }
                val btnInserirEndereco : Button = findViewById(R.id.btnInserirEndereco)
                btnInserirEndereco.setOnClickListener {
                    fineLocationPermission.checkPermission(this, FINE_LOCATION_CODE)
                }
                val btnShowHideLista : Button = findViewById(R.id.btnShowHideLista)
                btnShowHideLista.visibility = View.GONE

            }

        }
        if (requestCode==CAMERA_PERMISSION_CODE){
            cameraPermissions.handlePermissionsResult(requestCode, permissions, grantResults, CAMERA_PERMISSION_CODE)
            readFilesPermissions.requestPermission(this, READ_PERMISSION_CODE)
        }
        if (requestCode==READ_PERMISSION_CODE){
            readFilesPermissions.handlePermissionsResult(requestCode, permissions, grantResults, READ_PERMISSION_CODE)
            writeFilesPermissions.requestPermission(this, WRITE_PERMISSION_CODE)
        }
        if (requestCode==WRITE_PERMISSION_CODE){
            writeFilesPermissions.handlePermissionsResult(requestCode, permissions, grantResults, WRITE_PERMISSION_CODE)
        }


    }

    fun temPermissaoParaGps(): Boolean{
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            //permissão concedida
            return true
        } else {

            return false

        }
    }

    fun RequestGpsPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {


        } else {

            // layPermissaoLocal.visibility = View.VISIBLE
            /*
            btnAutorizar.setOnClickListener {
                //mude a permissão aqui
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    999
                )

                //getUserLocation(raioUser, 0)
                //layPermissaoLocal.visibility = View.GONE
                //noting
            }

             */
        }
    }


    /********************************
     *****FIM DOS METODOS DE PERMISSAO
     ********************************/





    //propagandas próprias
    //primeiro verifica se tem uma propaganda de nível nacional. Se não tiver verifica estadual e ai se nao houve na cidade.
    //COMO FUNCIONA: ELE faz uma query para saber se tem algo a nivel Brasil. Se tiver, coloca num array e chama o método publica anuncio. Lá tem um timer e faz gestão desse array
    //caso não tenha nada a nivel país, procura a nível estado e depois nivel cidade. São impeditivos. Se tiver a nivel brasil, nao exibe nivel estado e cidade.
    fun queryAnunciosPropriosNivelPais(pais: String, latLng: LatLng) {

        val arrayAnuncios: MutableList<String> = ArrayList()

        val rootRef = databaseReference.child("anuncios").child(pais).child("anuncio")
        rootRef.orderByChild("controle").equalTo("anuncio")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (querySnapshot.exists()){

                            val adView: AdView = findViewById(R.id.adView)
                            adView.visibility = View.GONE

                            /*
                            pos 0 - img
                            pos 1 - link
                             */

                            var values: String
                            values = querySnapshot.key.toString()

                            //ajusta visualização
                            var valuesHere = "nao"
                            valuesHere = querySnapshot.child("contador").getValue().toString()
                            MapsModels.contadorAnuncio = valuesHere.toInt()
                            databaseReference.child("anuncios").child(pais).child("anuncio").child(values).child("contador").setValue(MapsModels.contadorAnuncio+1)

                            values = querySnapshot.child("anuncio").getValue().toString()
                            arrayAnuncios.add(values)

                            values = querySnapshot.child("link").getValue().toString()
                            arrayAnuncios.add(values)



                        }
                    }

                    if (arrayAnuncios.size>0){
                        publicaAnuncio(arrayAnuncios)
                    } else {

                        //este bloco é para o anuncio proprio. Verifica a cidade do usuario e faz uma query para ver se tem anuncio nesta cidade. Se tiver, desativa o admob. Dentro desta query ele faz um clicklistener para mandar pro site do anunciante
                        val cidadeAnuncio: String = MapsController.getAddressOnlyEstadoParaAnuncioProprio(latLng, this@MapsActivity) //aqui vai pegar o estado
                        if (cidadeAnuncio.equals("nao")){
                            //mantem anuncio adMob pois nao achou a cidade
                        } else {
                            //verifica se tem um anuncio proprio. Se tiver vai trocar o adMob pelo anuncio proprio
                            queryAnunciosPropriosNivelEstado(cidadeAnuncio, latLng, arrayAnuncios)
                        }


                    }


                    //EncerraDialog()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })


        /*
        val rootRef = databaseReference.child("anuncios").child(pais)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")

                if (p0.exists()){

                    val adView: AdView = findViewById(R.id.adView)
                    adView.visibility = View.GONE

                    var values: String
                    values = p0.child("anuncio").getValue().toString()

                    val imageViewAnuncioProprio : ImageView = findViewById(R.id.anuncioProprioIV)
                    Glide.with(this@MapsActivity).load(values).centerCrop().into(imageViewAnuncioProprio)
                    imageViewAnuncioProprio.visibility = View.VISIBLE
                    imageViewAnuncioProprio.setOnClickListener {
                        var valuesHere = "nao"
                        valuesHere = p0.child("contador").getValue().toString()
                        contadorAnuncio = valuesHere.toInt()
                        databaseReference.child("anuncios").child(pais).child("contador").setValue(contadorAnuncio+1)

                        //now open site
                        var url = p0.child("link").getValue().toString()
                        if (!url.startsWith("http://") && !url.startsWith("https://"))
                            url = "http://" + url;
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                        )
                        startActivity(browserIntent)
                    }


                } else {

                    //este bloco é para o anuncio proprio. Verifica a cidade do usuario e faz uma query para ver se tem anuncio nesta cidade. Se tiver, desativa o admob. Dentro desta query ele faz um clicklistener para mandar pro site do anunciante
                    val cidadeAnuncio: String = getAddressOnlyEstadoParaAnuncioProprio(latLng) //aqui vai pegar o estado
                    if (cidadeAnuncio.equals("nao")){
                        //mantem anuncio adMob pois nao achou a cidade
                    } else {
                        //verifica se tem um anuncio proprio. Se tiver vai trocar o adMob pelo anuncio proprio
                        queryAnunciosPropriosNivelEstado(cidadeAnuncio, latLng)
                    }



                }




            }
        })

         */


    }

    fun queryAnunciosPropriosNivelEstado(Estado: String, latLng: LatLng, arrayAnuncios: MutableList<String>) {

        //vamos usar cidadeFiltrada para manter o código. Mas é na verdade estado
        val estadoFiltrado = MapsController.removeSpecialCharsAndToLowerCase(Estado)

        val rootRef = databaseReference.child("anuncios").child(estadoFiltrado).child("anuncio")
        rootRef.orderByChild("controle").equalTo("anuncio")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (querySnapshot.exists()){

                            val adView: AdView = findViewById(R.id.adView)
                            adView.visibility = View.GONE

                            /*
                            pos 0 - img
                            pos 1 - link
                             */

                            var values: String
                            values = querySnapshot.key.toString()

                            //ajusta visualização
                            var valuesHere = "nao"
                            valuesHere = querySnapshot.child("contador").getValue().toString()
                            MapsModels.contadorAnuncio = valuesHere.toInt()
                            databaseReference.child("anuncios").child(estadoFiltrado).child("anuncio").child(values).child("contador").setValue(MapsModels.contadorAnuncio+1)

                            values = querySnapshot.child("anuncio").getValue().toString()
                            arrayAnuncios.add(values)

                            values = querySnapshot.child("link").getValue().toString()
                            arrayAnuncios.add(values)


                        }
                    }

                    if (arrayAnuncios.size>0){
                        publicaAnuncio(arrayAnuncios)
                    } else {

                        //este bloco é para o anuncio proprio. Verifica a cidade do usuario e faz uma query para ver se tem anuncio nesta cidade. Se tiver, desativa o admob. Dentro desta query ele faz um clicklistener para mandar pro site do anunciante
                        val cidadeAnuncio: String = MapsController.getAddressOnlyCidadeParaAnuncioProprio(latLng, this@MapsActivity) //aqui vai pegar o estado
                        if (cidadeAnuncio.equals("nao")){
                            //mantem anuncio adMob pois nao achou a cidade
                        } else {
                            //verifica se tem um anuncio proprio. Se tiver vai trocar o adMob pelo anuncio proprio
                            queryAnunciosPropriosNivelCidade(cidadeAnuncio, arrayAnuncios)
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

    /*
    fun queryAnunciosPropriosNivelEstado(Estado: String, latLng: LatLng) {

        val cidadeFiltrada = removeSpecialCharsAndToLowerCase(Estado)

        val rootRef = databaseReference.child("anuncios").child(cidadeFiltrada)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")

                if (p0.exists()){

                    val adView: AdView = findViewById(R.id.adView)
                    adView.visibility = View.GONE

                    var values: String
                    values = p0.child("anuncio").getValue().toString()

                    val imageViewAnuncioProprio : ImageView = findViewById(R.id.anuncioProprioIV)
                    Glide.with(this@MapsActivity).load(values).centerCrop().into(imageViewAnuncioProprio)
                    imageViewAnuncioProprio.visibility = View.VISIBLE
                    imageViewAnuncioProprio.setOnClickListener {
                        var valuesHere = "nao"
                        valuesHere = p0.child("contador").getValue().toString()
                        contadorAnuncio = valuesHere.toInt()
                        databaseReference.child("anuncios").child(cidadeFiltrada).child("contador").setValue(contadorAnuncio+1)

                        //now open site
                        var url = p0.child("link").getValue().toString()
                        if (!url.startsWith("http://") && !url.startsWith("https://"))
                            url = "http://" + url;
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                        )
                        startActivity(browserIntent)
                    }


                } else {

                    //este bloco é para o anuncio proprio. Verifica a cidade do usuario e faz uma query para ver se tem anuncio nesta cidade. Se tiver, desativa o admob. Dentro desta query ele faz um clicklistener para mandar pro site do anunciante
                    val cidadeAnuncio: String = getAddressOnlyCidadeParaAnuncioProprio(latLng) //aqui vai pegar o estado
                    if (cidadeAnuncio.equals("nao")){
                        //mantem anuncio adMob pois nao achou a cidade
                    } else {
                        //verifica se tem um anuncio proprio. Se tiver vai trocar o adMob pelo anuncio proprio
                        queryAnunciosPropriosNivelCidade(cidadeAnuncio)
                    }

                }




            }
        })


    }
     */

    fun queryAnunciosPropriosNivelCidade(cidade:String, arrayAnuncios: MutableList<String>) {

        //vamos usar cidadeFiltrada para manter o código. Mas é na verdade estado
        val cidadeFiltrada = MapsController.removeSpecialCharsAndToLowerCase(cidade)

        val rootRef = databaseReference.child("anuncios").child(cidadeFiltrada).child("anuncio")
        rootRef.orderByChild("controle").equalTo("anuncio")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (querySnapshot.exists()){

                            val adView: AdView = findViewById(R.id.adView)
                            adView.visibility = View.GONE

                            /*
                            pos 0 - img
                            pos 1 - link
                             */

                            var values: String
                            values = querySnapshot.key.toString()

                            //ajusta visualização
                            var valuesHere = "nao"
                            valuesHere = querySnapshot.child("contador").getValue().toString()
                            MapsModels.contadorAnuncio = valuesHere.toInt()
                            databaseReference.child("anuncios").child(cidadeFiltrada).child("anuncio").child(values).child("contador").setValue(MapsModels.contadorAnuncio+1)

                            values = querySnapshot.child("anuncio").getValue().toString()
                            arrayAnuncios.add(values)

                            values = querySnapshot.child("link").getValue().toString()
                            arrayAnuncios.add(values)

                        }

                    }

                    if (arrayAnuncios.size>0) {
                        publicaAnuncio(arrayAnuncios)
                    }

                    //EncerraDialog()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })

    }

    /*
    fun queryAnunciosPropriosNivelCidade(cidade: String) {

        val cidadeFiltrada = removeSpecialCharsAndToLowerCase(cidade)

        val rootRef = databaseReference.child("anuncios").child(cidadeFiltrada)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")

                if (p0.exists()){

                    val adView: AdView = findViewById(R.id.adView)
                    adView.visibility = View.GONE

                    var values: String
                    values = p0.child("anuncio").getValue().toString()

                    val imageViewAnuncioProprio : ImageView = findViewById(R.id.anuncioProprioIV)
                    Glide.with(this@MapsActivity).load(values).centerCrop().into(imageViewAnuncioProprio)
                    imageViewAnuncioProprio.visibility = View.VISIBLE
                    imageViewAnuncioProprio.setOnClickListener {
                        var valuesHere = "nao"
                        valuesHere = p0.child("contador").getValue().toString()
                        contadorAnuncio = valuesHere.toInt()
                        databaseReference.child("anuncios").child(cidadeFiltrada).child("contador").setValue(contadorAnuncio+1)

                        //now open site
                        var url = p0.child("link").getValue().toString()
                        if (!url.startsWith("http://") && !url.startsWith("https://"))
                            url = "http://" + url;
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                        )
                        startActivity(browserIntent)
                    }


                }




            }
        })


    }
     */


    fun publicaAnuncio(arrayAnuncios: MutableList<String>){

        //primeiro tem que criar um contador para roletar os anuncios
        /* Assim está o array
                            pos 0 - img
                            pos 1 - link
        */

        var cont=0
        val imageViewAnuncioProprio : ImageView = findViewById(R.id.anuncioProprioIV)
        Glide.with(this@MapsActivity).load(arrayAnuncios.get(cont)).centerCrop().into(imageViewAnuncioProprio)
        imageViewAnuncioProprio.visibility = View.VISIBLE
        imageViewAnuncioProprio.setOnClickListener {

            var url = arrayAnuncios.get(cont+1)
            if (!url.startsWith("http://") && !url.startsWith("https://"))
                url = "http://" + url;
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
            startActivity(browserIntent)

        }

        if (arrayAnuncios.size>2) {  //se o array for menor do que 2 é pq só existe um anuncio. Então não precisa do timer.
            //se for maior, vai mudar o anuncio a cada 20 seg
            val timer = object : CountDownTimer(20000, 1000) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {

                    cont = cont + 2
                    if (cont < arrayAnuncios.size) { //enquanto cont menor do que o tamanho do array, mantem o array
                        cont=cont
                    } else {
                        cont=0  //se for maior, zera ele para começar novamente.
                    }


                        Glide.with(this@MapsActivity).load(arrayAnuncios.get(cont)).centerCrop()
                            .into(imageViewAnuncioProprio)
                        //imageViewAnuncioProprio.visibility = View.VISIBLE
                        imageViewAnuncioProprio.setOnClickListener {

                            var url = arrayAnuncios.get(cont + 1)
                            if (!url.startsWith("http://") && !url.startsWith("https://"))
                                url = "http://" + url;
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                            )
                            startActivity(browserIntent)

                        }
                    this.start()
                    }
                }
                timer.start()


        }



        }
    //fim das propagandas proprias















    //*********************************
    //Metodos de usuários

    //Pega dados do usuário e coloca ele no mapa
    fun placeUserInMap() {


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            fineLocationPermission.checkPermission(this, FINE_LOCATION_CODE)
            return
        } else {

            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                // Got last known location. In some rare situations this can be null.
                // 3

                if (location != null) {

                    getLocal = true
                    //minha localização
                    lastLocation = location


                    //criar uma função para colocar um marker desse para cada petshop
                    val latLng = LatLng(lastLocation.latitude, lastLocation.longitude)

                    var bitmapFinal : Bitmap?

                    //pega o tamanho da tela para ajustar a qualquer celular na mesma proporção
                    val display = windowManager.defaultDisplay
                    val size = Point()
                    display.getSize(size)
                    val width: Int = size.x
                    val height: Int = size.y

                    //aqui é o tamanho total da imagem do user. Alterar aqui se quiser aumentar ou diminuir
                    val withPercent  = ((12*width)/100).toInt()
                    val heigthPercent : Int = ((7*height)/100).toInt()

                    var img = "nao"
                    if (MapsModels.imgDoUser.equals("nao")){
                        img = "https://firebasestorage.googleapis.com/v0/b/farejadorapp.appspot.com/o/imgs_sistema%2Fimgusernoimg.png?alt=media&token=8a119c04-3295-4c5a-8071-dde1fe7849ea"
                    } else {
                        img = MapsModels.imgDoUser
                    }

                    //com o Glide vamos transformar a imagem que vem cmo link do storage firebase em um bitmap que iremos trabalhar
                    Glide.with(this)
                        .asBitmap()
                        .load(img)
                        .apply(RequestOptions().override(withPercent, heigthPercent)) //ajusta o tamanho
                        .apply(RequestOptions.circleCropTransform()) //coloca em formato circulo
                        .into(object : CustomTarget<Bitmap>(){
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                                //pega a imagem do fundo branco
                                val bit = BitmapFactory.decodeResource(
                                    this@MapsActivity.getResources(),
                                    R.drawable.placeholder
                                )

                                //no metodo abaixo ajustamos o tamanho das imagens e juntamos os dois
                                bitmapFinal = createUserBitmapFinalJustRound(resource, bit)  //here we will insert the bitmap we got with the link in a placehold with white border.

                                //coloca a marca com titulo

                                val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("Você").icon(BitmapDescriptorFactory.fromBitmap(bitmapFinal)))

                                mark1.tag=0

                                mMap.setOnMarkerClickListener (this@MapsActivity)

                            }
                            override fun onLoadCleared(placeholder: Drawable?) {
                                // this is called when imageView is cleared on lifecycle call or for
                                // some other reason.
                                // if you are referencing the bitmap somewhere else too other than this imageView
                                // clear it here as you can no longer have the bitmap
                            }
                        })


                }
            }

        }


    }

    //ajusta a imagem para o marker com imagem
    fun createUserBitmapFinalJustRound(bitmapImgUser: Bitmap?, bitmapPlaceHolder: Bitmap?): Bitmap? {

        //vamos ajustar o fundo branco ao tamanho que colocamos na imagem do user
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width: Int = size.x
        //val height: Int = size.y

        val withPercent  = ((18*width)/100).toFloat()   //um pouco maior do que a imagem do user
        val differenceAdjust = ((8*withPercent)/100).toFloat()

        //ajusta ao tamanho que queremos
        val newPlaceHolder = MapsController.scaleDown(bitmapPlaceHolder!!, withPercent, true)

        //agora colocamos a imagem do bolão ao fundo e a imagem do user a frente
        val bmOverlay = Bitmap.createBitmap(newPlaceHolder!!.getWidth(), newPlaceHolder.getHeight(), newPlaceHolder.getConfig())
        val canvas = Canvas(bmOverlay)
        val customMatrix = Matrix()
        customMatrix.setTranslate(differenceAdjust, differenceAdjust)
        canvas.drawBitmap(newPlaceHolder!!, Matrix(), null)
        canvas.drawBitmap(bitmapImgUser!!, customMatrix, null)

        return bmOverlay

    }

    private fun Marker(get: Marker, position: Int): Marker {

        return arrayPetFriendMarker.get(position)

    }

    //procura usuarios proximos online
    fun findUsersNerby(lat: Double, long: Double) {

        var latlong = lat + long

        var startAtval = latlong-(0.01f*MapsModels.raioBusca)
        val endAtval = latlong+(0.01f*MapsModels.raioBusca)

        //nova regra de ouro
        //Por conta das características da latitude e longitude, nao podemos usar o mesmo valor para startAtVal (pois fica a esquerda) e endAtVal(que fica a direita).
        //O que ocorre é que itens que ficam a esquerda acumulam a soma de valores negativos de latitude e longitude. Já os que ficam em endVal pegam o valor negativo da longitude mas as vezes pega positivo de latitude. Isso dava resulltado no final.
        //OBS: Isso é verdade no ocidente. Se um dia quem sabe passar pro oriente.
        //Então agora o que vamos fazer.
        //a val dif armazena a diferença que encontramos entre startatVal e até onde faria 6km no mapa. Se alguim dia for mudar o raio (agora é 0.6) vai ter que mexer nisso.
        //entao basta adiconar essa diferença a startAtVal antes da busca para ele corrigir o erro. A verificar se isto também precisa ser feito para endAtAval.


        startAtval = (MapsModels.dif+startAtval) //ajuste

        getInstance().reference.child("onlineUsers").orderByChild("latlong").startAt(startAtval)
            .endAt(endAtval).limitToFirst(15)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.exists()) {
                        for (querySnapshot in dataSnapshot.children) {

                            if (!querySnapshot.key.toString().equals(MapsModels.userBD)){
                                var values: String
                                var img: String
                                img = querySnapshot.child("img").value.toString()
                                values = querySnapshot.key.toString()
                                val latFriend = querySnapshot.child("lat").value.toString()
                                val longFriend = querySnapshot.child("long").value.toString()

                                //coloca o petFriend no mapa
                                placePetFriendsInMap(img, values, latFriend.toDouble(), longFriend.toDouble())

                            }

                        }
                    } else {

                        EncerraDialog()


                    }

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })   //addValueEventListener

    }

    //coloca os usuarios proximos online no mapa
    //também tem o click do botão que esconde e mostra os usuários no mapa.
    fun placePetFriendsInMap(img: String, BdPetFriend: String, lat: Double, long: Double){

        val latLng = LatLng(lat, long)

        var bitmapFinal : Bitmap?

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width: Int = size.x
        val height: Int = size.y

        val withPercent  = ((12*width)/100).toInt()
        val heigthPercent : Int = ((7*height)/100).toInt()


        var img2 = "nao"
        if (img.equals("nao")){
            img2 = "https://firebasestorage.googleapis.com/v0/b/farejadorapp.appspot.com/o/imgs_sistema%2Fimgusernoimg.png?alt=media&token=8a119c04-3295-4c5a-8071-dde1fe7849ea"
        } else {
            img2 = img
        }

        Glide.with(this)
            .asBitmap()
            .load(img2)
            .apply(RequestOptions().override(withPercent, heigthPercent))
            .apply(RequestOptions.circleCropTransform())
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                    val bit = BitmapFactory.decodeResource(
                        this@MapsActivity.getResources(),
                        R.drawable.placeholder
                    )

                    bitmapFinal = createUserBitmapFinalJustRound(resource, bit)  //here we will insert the bitmap we got with the link in a placehold with white border.

                    val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("petFriend!?!"+BdPetFriend+"!?!"+img+"!?!"+latLng).icon(BitmapDescriptorFactory.fromBitmap(bitmapFinal)))
                    arrayPetFriendMarker.add(mark1)

                    mark1.tag=0

                    mMap.setOnMarkerClickListener (this@MapsActivity)

                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    // this is called when imageView is cleared on lifecycle call or for
                    // some other reason.
                    // if you are referencing the bitmap somewhere else too other than this imageView
                    // clear it here as you can no longer have the bitmap
                }
            })


        //aqui esconde ou mostra os usuarios
        //OBS: SE DER ERRO QUANDO TIVER MAIS MARKERS OLHAR NO METODO GET MARK. PODE SER QUE TENHA QUE MUDAR O CODIGO LA DENTRO, POIS ESTA .get(0) e nao get(position)
        val btnShowHidePetFriends = findViewById<Button>(R.id.btnShowHidePetFriends)
        btnShowHidePetFriends.visibility = View.VISIBLE
        btnShowHidePetFriends.setOnClickListener {
            var cont=0
            while (cont<arrayPetFriendMarker.size){
                if (arrayPetFriendMarker.get(cont).isVisible){
                    arrayPetFriendMarker.get(cont).isVisible=false
                    btnShowHidePetFriends.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.petfriendsnot, 0, 0)
                    MapsController.makeToast("Usuários removidos do mapa", this)
                } else {
                    arrayPetFriendMarker.get(cont).isVisible=true
                    btnShowHidePetFriends.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.petfriends, 0, 0)
                    MapsController.makeToast("Usuários de volta ao mapa", this)
                }
                cont++
            }

        }


    }



    //metodos que foram aposentados para colocar bitmap dentro do marker
    /*
    private fun createUserBitmapFinal(bitmapInicial: Bitmap?): Bitmap? {

        //primeiro vamos pegar o tamanho da tela para este código servir em qualquer celular
        //pegando o tamanho da tela do celular
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width: Int = size.x
        val height: Int = size.y

        val withPercent :Int = ((9*width)/100)//.roundToInt() //aqui a parte branca de trás terá 5% do tamanho da tela (está quebrado para manter as mesmas proporões do exemplo que peguei e funcionava perfeito
        val heigthPercent = ((5.7*height)/100).toFloat()

        Log.d("teste", "o valor de withPercent é "+withPercent)
        Log.d("teste", "o valor de heightPercent é "+heigthPercent)

        var result: Bitmap? = null
        try {
            //result = Bitmap.createBitmap(dp(62f), dp(76f), Bitmap.Config.ARGB_8888)
            result = Bitmap.createBitmap(dp((withPercent).toFloat()), dp((heigthPercent).toFloat()), Bitmap.Config.ARGB_8888)
            result.eraseColor(Color.TRANSPARENT)
            val canvas = Canvas(result)
            val drawable: Drawable = resources.getDrawable(R.drawable.placeholder)
            //drawable.setBounds(0, 0, dp(62f), dp(76f))
            drawable.setBounds(0,0, dp((withPercent).toFloat()-3), dp((heigthPercent).toFloat()-2))
            drawable.draw(canvas)
            val roundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val bitmapRect = RectF()
            canvas.save()
            //este é o inicial que colocar o elo val bitmap = BitmapFactory.decodeResource(resources, R.drawable.elo)

            if (bitmapInicial != null) {
                val shader =
                    BitmapShader(bitmapInicial, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                val matrix = Matrix()

                val descontoPercentwith = ((61*withPercent)/100).toFloat()
                val descontoPercentHeight = ((87*heigthPercent)/100).toFloat()

                Log.d("teste", "o valor de descontoWidth e "+descontoPercentwith)
                Log.d("teste", "o valor de descontoheight é "+descontoPercentHeight)

                val withBitmap = withPercent+descontoPercentwith  //.roundToInt() //aqui a parte branca de trás terá 5% do tamanho da tela (está quebrado para manter as mesmas proporões do exemplo que peguei e funcionava perfeito
                val heigthBitmap = heigthPercent-descontoPercentHeight //.roundToInt()

                Log.d("teste", "o valor de withBitmap que antes era 104 é "+withBitmap)
                Log.d("teste", "o valor de withheight que antes era 10 é "+heigthBitmap)

                val scale: Float = dp(withBitmap) / bitmapInicial.width.toFloat()  //reduzir aqui aumenta e diminui a imagem dentro do circulo pequeno. Não muda o tamanho do circulo. Ao mexer aqui, altere também nas mesmas proporções na linha bitmapRect
                matrix.postTranslate(5f, 5f) // postTranslate(dp(5), dp(5))0
                matrix.postScale(scale, scale)
                roundPaint.shader = shader
                shader.setLocalMatrix(matrix)
                bitmapRect[(heigthBitmap).toFloat(), (heigthBitmap).toFloat(), (withBitmap).toFloat()+(heigthBitmap).toFloat()]=withBitmap.toFloat()+heigthBitmap.toFloat()    //aqui mexe no tamanho da imagem pequena dentro do circulo branco.
                canvas.drawRoundRect(bitmapRect, 56f, 56f, roundPaint)  // (bitmapRect, dp(26), dp(26), roundPaint)
            }

            canvas.restore()
            try {
                canvas.setBitmap(null)
            } catch (e: java.lang.Exception) {
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return result
    }

    private fun createUserBitmapFinalBackup(bitmapInicial: Bitmap?): Bitmap? {

        //primeiro vamos pegar o tamanho da tela para este código servir em qualquer celular
        //pegando o tamanho da tela do celular
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width: Int = size.x
        val height: Int = size.y

        val withPercent :Int = ((9*width)/100)//.roundToInt() //aqui a parte branca de trás terá 5% do tamanho da tela (está quebrado para manter as mesmas proporões do exemplo que peguei e funcionava perfeito
        val heigthPercent = ((5.7*height)/100).toFloat()

        Log.d("teste", "o valor de withPercent é "+withPercent)
        Log.d("teste", "o valor de withPercent é "+heigthPercent)

        var result: Bitmap? = null
        try {
            //result = Bitmap.createBitmap(dp(62f), dp(76f), Bitmap.Config.ARGB_8888)
            result = Bitmap.createBitmap(dp((withPercent).toFloat()), dp((heigthPercent).toFloat()), Bitmap.Config.ARGB_8888)
            result.eraseColor(Color.TRANSPARENT)
            val canvas = Canvas(result)
            val drawable: Drawable = resources.getDrawable(R.drawable.placeholder)
            //drawable.setBounds(0, 0, dp(62f), dp(76f))
            drawable.setBounds(0,0, dp((withPercent).toFloat()-3), dp((heigthPercent).toFloat()-2))
            drawable.draw(canvas)
            val roundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val bitmapRect = RectF()
            canvas.save()
            //este é o inicial que colocar o elo val bitmap = BitmapFactory.decodeResource(resources, R.drawable.elo)

            if (bitmapInicial != null) {
                val shader =
                    BitmapShader(bitmapInicial, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                val matrix = Matrix()

                val withBitmap :Int = ((22*bitmapInicial.width)/100)//.roundToInt() //aqui a parte branca de trás terá 5% do tamanho da tela (está quebrado para manter as mesmas proporões do exemplo que peguei e funcionava perfeito
                val heigthBitmap: Int = ((3*bitmapInicial.height)/100)//.roundToInt()

                Log.d("teste", "o valor de withBitmap que antes era 104 é "+withBitmap)
                Log.d("teste", "o valor de withheight que antes era 10 é "+heigthBitmap)

                val scale: Float = dp((withBitmap).toFloat()) / bitmapInicial.width.toFloat()  //reduzir aqui aumenta e diminui a imagem dentro do circulo pequeno. Não muda o tamanho do circulo. Ao mexer aqui, altere também nas mesmas proporções na linha bitmapRect
                matrix.postTranslate(5f, 5f) // postTranslate(dp(5), dp(5))0
                matrix.postScale(scale, scale)
                roundPaint.shader = shader
                shader.setLocalMatrix(matrix)
                bitmapRect[(heigthBitmap).toFloat(), (heigthBitmap).toFloat(), (withBitmap).toFloat()+(heigthBitmap).toFloat()]=withBitmap.toFloat()+heigthBitmap.toFloat()    //aqui mexe no tamanho da imagem pequena dentro do circulo branco.
                canvas.drawRoundRect(bitmapRect, 56f, 56f, roundPaint)  // (bitmapRect, dp(26), dp(26), roundPaint)
            }

            canvas.restore()
            try {
                canvas.setBitmap(null)
            } catch (e: java.lang.Exception) {
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return result
    }

    fun dp(value: Float): Int {
        return if (value == 0f) {
            0
        } else Math.ceil(resources.displayMetrics.density * value.toDouble()).toInt()
    }
     */

    override fun onDestroy() {
        super.onDestroy()
        if (MapsModels.tipo.equals("autonomo")){
            updateAutonomosStatus("offline", "bla", "n")
        } else {

            if (this@MapsActivity::lastLocation.isInitialized){
                MapsModels.updateUserStatus("offline", "bla", lastLocation) //para apagar nao precisa saber a imagem
            }

        }
    }

    override fun onStop() {
        super.onStop()

        if (MapsModels.tipo.equals("autonomo")){
            updateAutonomosStatus("offline", "bla", "n")
        } else {

            if (this@MapsActivity::lastLocation.isInitialized){
                MapsModels.updateUserStatus("offline", "bla", lastLocation, this) //para apagar nao precisa saber a imagem
            }

        }
    }


    //*************************
    //FIM DE METODOS DE USUARIOS


    //METODOS DE AUTONOMOS
    fun queryGetAutonomoAditionalInfo (){

        ChamaDialog()
        val rootRef = databaseReference.child("autonomos").child(MapsModels.userBD)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()){

                    var values: String

                    //ler infos de autonomo
                    values = p0.child("servico").getValue().toString()
                    placeAutonomoInMap(values)

                    values = p0.child("plano").value.toString()
                    if (values.equals("sim")){
                        MapsModels.autonomoPlanoPremium=true
                    } else {
                        MapsModels.autonomoPlanoPremium=false
                    }

                    EncerraDialog()

                } else {

                    MapsController.makeToast("Ocorreu um erro.", this@MapsActivity)
                }


            }
        })

    }

    //procura usuarios proximos online
    fun findAutonomosNerby(lat: Double, long: Double) {

        var latlong = lat + long

        var startAtval = latlong-(0.01f*MapsModels.raioBusca)
        val endAtval = latlong+(0.01f*MapsModels.raioBusca)

        //nova regra de ouro
        //Por conta das características da latitude e longitude, nao podemos usar o mesmo valor para startAtVal (pois fica a esquerda) e endAtVal(que fica a direita).
        //O que ocorre é que itens que ficam a esquerda acumulam a soma de valores negativos de latitude e longitude. Já os que ficam em endVal pegam o valor negativo da longitude mas as vezes pega positivo de latitude. Isso dava resulltado no final.
        //OBS: Isso é verdade no ocidente. Se um dia quem sabe passar pro oriente.
        //Então agora o que vamos fazer.
        //a val dif armazena a diferença que encontramos entre startatVal e até onde faria 6km no mapa. Se alguim dia for mudar o raio (agora é 0.6) vai ter que mexer nisso.
        //entao basta adiconar essa diferença a startAtVal antes da busca para ele corrigir o erro. A verificar se isto também precisa ser feito para endAtAval.


        startAtval = (MapsModels.dif+startAtval) //ajuste

        getInstance().reference.child("onlineAutonomos").orderByChild("latlong").startAt(startAtval)
            .endAt(endAtval).limitToFirst(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.exists()) {
                        for (querySnapshot in dataSnapshot.children) {

                            if (!querySnapshot.key.toString().equals(MapsModels.userBD)){
                                var values: String
                                //var img: String
                                //img = querySnapshot.child("img").value.toString()
                                values = querySnapshot.child("situacao").value.toString()
                                if (values.equals("analise") || values.equals("atrasado")){
                                    //nao adicionar
                                } else {

                                    values = querySnapshot.key.toString()
                                    val latFriend = querySnapshot.child("lat").value.toString()
                                    val longFriend = querySnapshot.child("long").value.toString()
                                    val servicos  = querySnapshot.child("servico").value.toString()
                                    val apelido = querySnapshot.child("apelido").value.toString()

                                    //coloca o petFriend no mapa
                                    placeAutonomosWorkersInMap(servicos, values, latFriend.toDouble(), longFriend.toDouble())

                                    //monta o array que vai pra lista
                                    arrayAutonomosNomeBdParaLista.add(apelido+"!?!00!"+values+"!?!00!"+servicos)
                                    /*
                                    neste array
                                    pos 0 - nome
                                    pos 1 - bd
                                     */

                                }

                            }

                        }
                    } else {

                        EncerraDialog()

                    }

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })   //addValueEventListener

    }

    //cria e apaga o campo do usuario que está online.
    fun updateAutonomosStatus(state: String, servico:String, apelido: String){

        val lat = lastLocation.latitude
        val long = lastLocation.longitude

        if (state.equals("online")) {
            databaseReference.child("onlineAutonomos").child(MapsModels.userBD).child("latlong")
                .setValue(lat + long)
            databaseReference.child("onlineAutonomos").child(MapsModels.userBD).child("state").setValue(state)
            databaseReference.child("onlineAutonomos").child(MapsModels.userBD).child("servico").setValue(servico)
            databaseReference.child("onlineAutonomos").child(MapsModels.userBD).child("lat").setValue(lat)
            databaseReference.child("onlineAutonomos").child(MapsModels.userBD).child("long").setValue(long)
            databaseReference.child("onlineAutonomos").child(MapsModels.userBD).child("apelido").setValue(apelido)
        } else {
            databaseReference.child("onlineAutonomos").child(MapsModels.userBD).removeValue()
        }
    }

    //Pega dados do usuário e coloca ele no mapa
    fun placeAutonomoInMap(servico: String) {

        //criar uma função para colocar um marker desse para cada petshop
        val latLng = LatLng(lastLocation.latitude, lastLocation.longitude)

        //obs: Ao adicionar outro serviço lembrar de colocar o titulo EuAutonomo
        if (servico.equals("comida")){
            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("EuAutonomo").icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholdercomida)))
            mark1.tag=0

            mMap.setOnMarkerClickListener (this@MapsActivity)

        } else if (servico.equals("passeador")){
            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("EuAutonomo").icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholderpasseador)))
            mark1.tag=0

            mMap.setOnMarkerClickListener (this@MapsActivity)

        } else if (servico.equals("petsitter")){
            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("EuAutonomo").icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholderpetsitter)))
            mark1.tag=0

            mMap.setOnMarkerClickListener (this@MapsActivity)

        } else if (servico.equals("pettaxi")){
            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("EuAutonomo").icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholderpettaxi)))
            mark1.tag=0

            mMap.setOnMarkerClickListener (this@MapsActivity)

        }



    }

    fun placeAutonomosWorkersInMap(servico: String, Bdautonomo: String, lat: Double, long: Double){

        val latLng = LatLng(lat, long)

        //obs: Ao adicionar outro serviço lembrar de colocar o titulo EuAutonomo
        if (servico.equals("comida")){
            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("autonomoWorker!?!"+Bdautonomo).icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholdercomida)))
            arrayAutonomos.add(mark1)
            mark1.tag=0

            mMap.setOnMarkerClickListener (this@MapsActivity)

        } else if (servico.equals("passeador")){
            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("autonomoWorker!?!"+Bdautonomo).icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholderpasseador)))
            arrayAutonomos.add(mark1)
            mark1.tag=0

            mMap.setOnMarkerClickListener (this@MapsActivity)

        } else if (servico.equals("petsitter")){
            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("autonomoWorker!?!"+Bdautonomo).icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholderpetsitter)))
            arrayAutonomos.add(mark1)
            mark1.tag=0

            mMap.setOnMarkerClickListener (this@MapsActivity)

        } else if (servico.equals("pettaxi")){
            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("autonomoWorker!?!"+Bdautonomo).icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholderpettaxi)))
            arrayAutonomos.add(mark1)
            mark1.tag=0

            mMap.setOnMarkerClickListener (this@MapsActivity)

        }

        //aqui esconde ou mostra os usuarios
        //OBS: SE DER ERRO QUANDO TIVER MAIS MARKERS OLHAR NO METODO GET MARK. PODE SER QUE TENHA QUE MUDAR O CODIGO LA DENTRO, POIS ESTA .get(0) e nao get(position)
        val btnShowHideAutonomos = findViewById<Button>(R.id.btnShowHideAutonomos)
        btnShowHideAutonomos.visibility = View.VISIBLE
        btnShowHideAutonomos.setOnClickListener {
            var cont=0
            while (cont<arrayAutonomos.size){
                if (arrayAutonomos.get(cont).isVisible){
                    arrayAutonomos.get(cont).isVisible=false
                    btnShowHideAutonomos.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.petautonomosnot, 0, 0)
                    MapsController.makeToast("Prestadores de serviço removidos do mapa", this)
                } else {
                    arrayAutonomos.get(cont).isVisible=true
                    btnShowHideAutonomos.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.petautonomos, 0, 0)
                    MapsController.makeToast("Prestadores de serviço de volta ao mapa", this)
                }
                cont++
            }
        }


    }
















    //Fim de tudo que era importante. Qualquer coisa extra entrar aqui. Isto ocorre depois da ultima das queries iniciais automáticas
    fun fimDeTudo(){

        temLembreteHoje() //verifica se precisa exibir aviso de 10 dias de compra da ração

        adjustDonationbar()
    }

    //pega os dados do bd e ajusta a barra. Seta o click
    fun adjustDonationbar(){
        val btnHelp: Button = findViewById(R.id.donation_helpBtn)
        val progressBar: ProgressBar = findViewById(R.id.donation_progressBar)
        //progressBar.setProgress(95)
        btnHelp.setOnClickListener {
            openpopup_donation("O que é esta barra?", "Nós assumimos um compromisso com nossos parceiros. A cada 500 transações dentro do aplicativo nós doamos um saco de ração para a Suipa (Sociedade União Internacional Protetora dos Animais). Você pode acompanhar todo o processo nas nossas redes sociais")
            //openPopUp("O que é esta barra?", "Nós assumimos um compromisso com nossos parceiros. A cada 500 transações dentro do aplicativo nós doamos um saco de ração para a Suipa (Sociedade União Internacional Protetora dos Animais). Você pode acompanhar todo o processo nas nossas redes sociais", false, "n", "n", "n")
        }

        databaseReference = FirebaseDatabase.getInstance().reference

        val rootRef = databaseReference.child("donation")
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")
                var vendas= p0.child("vendas").getValue().toString()

                //calcular quantos % de 500 (o valor que definimos) é o que temos?
                val percent : Double = ((vendas.toDouble()/500)*100).toDouble()

                progressBar.setProgress(percent.toInt())

                //atingiu a meta, zera tudo e armazena no bd
                if (percent==100.0){
                    databaseReference.child("donation").child("vendas").setValue(0)
                    val totalDoado = p0.child("totalDoado").getValue().toString()
                    databaseReference.child("donation").child("totalDoado").setValue(totalDoado.toInt()+1)
                    progressBar.setProgress(0)
                }

                //agora vamso fazer mensagem de incentivo
                if (percent>=90.0){
                    openpopup_donation("Estamos quase lá!", "Nossa comunidade já atingiu 90% da meta para doação de mais ração para a Suipa (Sociedade União Internacional Protetora dos Animais). Falta bem pouco! Você pode ajudar fazendo qualquer compra no aplicativo ou chamando seus amigos para nossa comunidade!")
                }

            }
        })

    }


    fun openpopup_donation (titulo: String, texto:String){

        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao

        //EXIBIR POPUP
        // Initialize a new layout inflater instance
        val inflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate a custom view using layout inflater
        val view = inflater.inflate(R.layout.popup_aviso_donation,null)

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
        val buttonPopupOk = view.findViewById<Button>(R.id.popupBtnOk)
        val txtTitulo = view.findViewById<TextView>(R.id.popupTitulo)
        val txtTexto = view.findViewById<TextView>(R.id.popupTexto)


        txtTitulo.text = titulo
        txtTexto.text = texto

        buttonPopupOk.setOnClickListener {
            popupWindow.dismiss()
        }


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            popupWindow.dismiss()
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.lay_maps)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )


    }











    //Metodos de confirmação de preenchimento de informações do dono do petshopp
    fun openPopUpNotifyPetShopInfoFaltando (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, call: String, percent: Int, Bd: String) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao


        //EXIBIR POPUP
        // Initialize a new layout inflater instance
        val inflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate a custom view using layout inflater
        val view = inflater.inflate(R.layout.popup_notifyinfo_petshop,null)


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
        val txtPercent = view.findViewById<TextView>(R.id.layPorcentageTxt)
        val bar: ProgressBar = view.findViewById(R.id.porcentageBar)

        txtTitulo.text = titulo
        txtTexto.text = texto

        //ajustar a barra
        bar.setProgress(percent)

        txtPercent.setText(percent.toString()+"%")

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



        //aqui colocamos os ifs com cada call de cada vez que a popup for chamada
        buttonPopupS.setOnClickListener {

            popupWindow.dismiss()
            if (call.equals("alvara")) {

                val intent = Intent(this, arealojista::class.java)
                intent.putExtra("userBD", MapsModels.userBD)
                intent.putExtra("alvara", MapsModels.alvara)
                intent.putExtra("tipo", MapsModels.tipo)
                intent.putExtra("email", MapsModels.userMail)
                intent.putExtra("petBD", Bd)
                startActivity(intent)
                finish()
            } else if (call.equals("logo")){
                val intent = Intent(this, arealojista::class.java)
                intent.putExtra("userBD", MapsModels.userBD)
                intent.putExtra("alvara", MapsModels.alvara)
                intent.putExtra("tipo", MapsModels.tipo)
                intent.putExtra("email", MapsModels.userMail)
                intent.putExtra("petBD", Bd)
                intent.putExtra("motivo", "logo")
                startActivity(intent)
                finish()
            } else if (call.equals("itens")){

                val intent = Intent(this, arealojista::class.java)
                intent.putExtra("userBD", MapsModels.userBD)
                intent.putExtra("alvara", MapsModels.alvara)
                intent.putExtra("tipo", MapsModels.tipo)
                intent.putExtra("email", MapsModels.userMail)
                intent.putExtra("petBD", Bd)
                intent.putExtra("motivo", "itens")
                startActivity(intent)
                finish()
            } else if (call.equals("raio")){
                val intent = Intent(this, arealojista::class.java)
                intent.putExtra("userBD", MapsModels.userBD)
                intent.putExtra("alvara", MapsModels.alvara)
                intent.putExtra("tipo", MapsModels.tipo)
                intent.putExtra("email", MapsModels.userMail)
                intent.putExtra("petBD", Bd)
                intent.putExtra("motivo", "raio")
                startActivity(intent)
                finish()
            }

        }

        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            popupWindow.dismiss()
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.lay_maps)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )


    }


















    /*
    //Metodo de lembrar ao user que passou um tempo desde que comprou um item e eprguntar se quer repetir a compra
    //este método pega a data de hoje e salva um lembrete daqui a dez dias.
    fun ativarLembrete(bdDoPet: String, nomeProduto: String){
        val dataRemember = MapsController.GetfutureDate(10)

        val sharedPref: SharedPreferences = getSharedPreferences(getString(R.string.sharedpreferences), 0) //0 é private mode
        val editor = sharedPref.edit()

        editor.putString("rememberDate", dataRemember)
        editor.putString("remeberNomeProduto", nomeProduto)
        editor.putString("rememberBdDoPEt", bdDoPet)
        editor.apply()

    }
     */

    fun temLembreteHoje(){

        if (MapsController.temLembreteHoje(this)){
            val mySharedPrefs: mySharedPrefs = mySharedPrefs(this)

            val nomeProduto = mySharedPrefs.getValue("remeberNomeProduto")
            val bdDoPet = mySharedPrefs.getValue("rememberBdDoPEt")
            openPopUpRememberDate("Ração acabando?", "Percebemos que já faz um tempo desde que você comprou ração "+nomeProduto+" pela última vez. Gostaria de comprar mais na mesma loja?", true, "Sim, comprar", "Não", bdDoPet)
        }

    }

    fun openPopUpRememberDate (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, bdDoPetInformado: String) {
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
                MapsModels.bdDoPet = bdDoPetInformado
                //este método está ajustando o botão central que muda de imagem. Dentro dele está o click da loja
                centralBtnMarkerToSeta_MapaToLoja("user")

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
        val lay_root: ConstraintLayout = findViewById(R.id.lay_maps)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }












    //tentativa de cluster
    fun comparaDistancia(){


        //listener de zoom change


        /*
        val zoom: Float = mMap.getCameraPosition().zoom
        val arrayAutonomosClustered: MutableList<Marker> = ArrayList()
         */


        /*
        mMap.setOnCameraMoveListener(OnCameraMoveListener {
            val cameraPosition: CameraPosition = mMap.getCameraPosition()
            if (cameraPosition.zoom > 18.0) {

            } else {

            }
        })

         */

        /*
        val locationPet =
            Location(LocationManager.NETWORK_PROVIDER) // OR GPS_PROVIDER based on the requirement
        locationPet.latitude = latLng.latitude
        locationPet.longitude = latLng.longitude
        val distancia = lastLocation.distanceTo(locationPet)
        val zoomAdjusted = calculateZoomToFit(distancia)
        val currentLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)

         */
    }












    //metodos de update automático do app
    private fun checkForAppUpdate() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Request the update.
                try {
                    val installType = when {
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        else -> null
                    }
                    if (installType == AppUpdateType.FLEXIBLE) appUpdateManager.registerListener(appUpdatedListener)

                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        installType!!,
                        this,
                        262)
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 262) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this,
                    "Update falhou. Por vaor tente novamente da próxima vez que abrir o app.",
                    Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        /*
        val snackbar = Snackbar.make(
            findViewById(R.id.drawer_layout),
            "Update concluído.",
            Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction("RESTART") { appUpdateManager.completeUpdate() }
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.azulClaro))
        snackbar.show()

         */
        MapsController.makeToast("Aplicativo atualizado!", this)
    }










    //procurar pelo código
    fun searchByCode(){

    }



    //métodos de suporte

    //este transforma numero comum em dinheiro


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

    fun animaLoad(){
        val layLoading: ConstraintLayout = findViewById(R.id.layloading)
        val ivFarejaVai : ImageView = findViewById(R.id.ivFarejaVai)
        val ivFarejaVolta: ImageView = findViewById(R.id.ivFarejaVolta)

        if (layLoading.isVisible==false){
            layLoading.visibility = View.VISIBLE
            //comecar animacao
            val farejavai = AnimationUtils.loadAnimation(this, R.anim.movefarejando)
            val farejavolta = AnimationUtils.loadAnimation(this, R.anim.movefarejandocameback)
            ivFarejaVai.visibility = View.VISIBLE
            ivFarejaVai.startAnimation(farejavai)
            //ivFarejaVolta.startAnimation(farejavolta)

        } else {
            layLoading.visibility = View.GONE
            ivFarejaVai.visibility = View.GONE
        }
    }

    /*MAPA DOS PROCESSOS

    OnCreate:  ------------QueryPetSemLatLong ----->getLatLong x
               ------------MenuClicks x
               ------------BtnBuscaPorEnderecoClick ----->MetBuscaPorEndereco---->FindEndFromAdress---->QueryPetsNerbyFromGivenAdress x
               ----QueryUserInitial--->queryPetEmpresario x
                                   --->QueryPedidosParaFinalizarDoEmpresario x
                                   --->queryPedidosParaFinalizar --->AbreMinhasNotificacoes x


     onMapReady (Ele começa sozinho, não é chamado de nenhum lugar)

            -------------getUserLocation  ----------------------->QueryPetShopsNerby--(sem resultado retorna pra getUserLocation e aumenta o raio da busca)
      btnCentralizar-----^                !                                         --->getLatLong x
                                          !                                         --->PlacePetShopsInMap x
                                          !------------------->Metodo de ImpulsionamenotGerencia---->QueryImpulsionamentoNerby


 */

}




