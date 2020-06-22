package com.petcare.petcare

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.*
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.petcare.petcare.Controller.MainController
import com.petcare.petcare.Models.MainModels
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.activity_minhas_vendas.*

class MainActivity : AppCompatActivity() {

    lateinit var mAdView : AdView

    private lateinit var auth: FirebaseAuth
    //variaveis de login do face
    private lateinit var callbackManager: CallbackManager
    //fim das variaveis de login do face

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        MainModels.setupInicial()

        //minha chave ca-app-pub-6912617107153681/4839063139
        //chave para testes ca-app-pub-3940256099942544/6300978111

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        MobileAds.initialize(this, "ca-app-pub-6912617107153681~1282961500")
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        //todos antigos processos no OnCreate foram para o método metodosDeCreate() para acelerar o processo de criação da activity
        metodosDeCreate()

    }

    fun metodosDeCreate (){

        val telainicial = findViewById<ConstraintLayout>(R.id.layInicial)
        val telaDeVerificacao = findViewById<ConstraintLayout>(R.id.layLoginWithMail_VerificationMail)
        val telaLoginMail = findViewById<ConstraintLayout>(R.id.layLoginWithEmail)
        val telaLoginMailNew = findViewById<ConstraintLayout>(R.id.layLoginWithEmail_newUser)

        telaDeVerificacao.visibility = View.GONE

        //fazer login depos
        val btnLoginDepois : Button = findViewById(R.id.btnLoginDepois)
        btnLoginDepois.setOnClickListener {
            if (MainController.isNetworkAvailable(this)){
                ChamaDialog()

                telainicial.visibility = View.GONE
                telaDeVerificacao.visibility= View.GONE
                telaLoginMail.visibility= View.GONE
                telaLoginMailNew.visibility= View.GONE
                telainicial.visibility= View.VISIBLE

                val intent = Intent(this, MapsActivity::class.java)
                intent.putExtra("email", "semLogin" )
                startActivity(intent)
                val telainicial: ConstraintLayout = findViewById(R.id.layInicial)
                val layoutin = AnimationUtils.loadAnimation(this, R.anim.telainicial_comeback_withdelay_paraajustelogin)
                telainicial.startAnimation(layoutin)

            } else {
                Toast.makeText(this, "Você está sem conexão com a internet e não pode logar.", Toast.LENGTH_SHORT).show()
            }

        }


        val btnLoginWithMail = findViewById<Button>(R.id.layInicial_btnSignWithEmail)
        btnLoginWithMail.setOnClickListener {
            if (MainController.isNetworkAvailable(this)) {
                LoginWithEmail() //tem que fazer aqui dentro
            } else {
                Toast.makeText(this, "Você está sem conexão com a internet.", Toast.LENGTH_SHORT).show()
            }
        }

        //login do face
        val loginButton: LoginButton = findViewById(R.id.login_button)
        val loginFaceVisivel = findViewById<Button>(R.id.layInicial_btnSignWithFace)
        loginFaceVisivel.setOnClickListener {
            if (MainController.isNetworkAvailable(this)) {
                layinicialOut()
                ChamaDialog()
                loginButton.performClick()
            } else {
                Toast.makeText(this, "Você está sem conexão com a internet.", Toast.LENGTH_SHORT).show()
            }

        }

        callbackManager = CallbackManager.Factory.create()
        loginButton.visibility = View.GONE
        loginButton.setReadPermissions("email", "public_profile")
        loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onError(error: FacebookException?) {

                var retorno = "nao"
                retorno = MainController.updateUI("nao", "facebook")
                retornoUpdateUi(retorno)
            }

            override fun onCancel() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                var retorno = "nao"
                retorno = MainController.updateUI("nao", "nao")
                retornoUpdateUi(retorno)
            }

            override fun onSuccess(loginResult: LoginResult) {
                //Log.d("teste", "facebook:onSuccess:$loginResult")
                handleFacebookAccessToken(loginResult.accessToken)
            }

        })

        //fim do login do face
        //animacao()
        animacaoXML()
    }

    fun animacaoXML (){
        //objetos
        val ivCel: ImageView = findViewById(R.id.ivCel)
        var ivmarkP: ImageView = findViewById(R.id.ivmark1)
        var ivmarkG: ImageView = findViewById(R.id.ivMark2)
        val ivLogo: ImageView = findViewById(R.id.ivLogo)

        //animacoes
        val narizdireita1 = AnimationUtils.loadAnimation(this, R.anim.movenarizdireita1)
        val cresceMarkers = AnimationUtils.loadAnimation(this, R.anim.animmarkers)
        val moveCel = AnimationUtils.loadAnimation(this, R.anim.animcel)

        //setanimation
        ivLogo.visibility = View.VISIBLE
        ivmarkG.visibility = View.VISIBLE
        ivmarkP.visibility = View.VISIBLE
        ivLogo.startAnimation(narizdireita1)
        ivmarkP.startAnimation(cresceMarkers)
        ivmarkG.startAnimation(cresceMarkers)

        moveCel.fillAfter=true
        ivCel.startAnimation(moveCel)

    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.

        EncerraDialog()

        MainModels.checkAuth()

        var retorno = "nao"
        val user = MainModels.returnUser()
        if (user.equals("nao")){
            retorno = MainController.updateUI("nao", "null")
        } else {
            retorno = MainController.updateUI(user, "unknown")
        }

        retornoUpdateUi(retorno)


        val LoginWithMail = findViewById<Button>(R.id.layInicial_btnSignWithEmail)
        val layInicial = findViewById<ConstraintLayout>(R.id.layInicial)
        LoginWithMail.setOnClickListener {
            if (MainController.isNetworkAvailable(this)){
                LoginWithEmail() //aqui estao os clickes
                val layLoginMail =
                    findViewById<ConstraintLayout>(R.id.layLoginWithEmail)  //pagina inicial
                layLoginMail.visibility = View.VISIBLE
                layInicial.visibility = View.GONE
            } else {
                Toast.makeText(this, "Você está sem conexão com a internet e não pode entrar.", Toast.LENGTH_SHORT).show()
            }

        }

        metodosDeCreate()

    }

    fun retornoUpdateUi(result: String){

        val layNovoUser = findViewById<ConstraintLayout>(R.id.layLoginWithEmail_newUser)
        val layLoginMail = findViewById<ConstraintLayout>(R.id.layLoginWithEmail)  //pagina inicial do login com email


        //preciso refazer os métodos daqui de dentro
        if (result.equals("email_nao_verificado")){
            val telaDeVerificacao = findViewById<ConstraintLayout>(R.id.layLoginWithMail_VerificationMail)
            telaDeVerificacao.visibility = View.VISIBLE
            layNovoUser.visibility = View.GONE
            laygenericoOutCenterToLeft(layNovoUser)
            laygenericoInRightToCenter(telaDeVerificacao)
            MainModels.sendEmailVerification(this)
            val emailVerifyCheck = findViewById<Button>(R.id.verifyEmailButtonCheck) //botao que o user aperta quando ja vericou o email
            emailVerifyCheck.setOnClickListener {
                emailVerificationCheckMeth()
            }

        } else if (result.equals("email_verificado")){
            val email = MainModels.getUserMail()
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)

            val telaDeVerificacao = findViewById<ConstraintLayout>(R.id.layLoginWithMail_VerificationMail)
            telaDeVerificacao.visibility = View.GONE
            val telaLoginMail = findViewById<ConstraintLayout>(R.id.layLoginWithEmail)
            telaLoginMail.visibility = View.GONE
            val telaLoginMailNew = findViewById<ConstraintLayout>(R.id.layLoginWithEmail_newUser)
            telaLoginMailNew.visibility = View.GONE
            val telainicial = findViewById<ConstraintLayout>(R.id.layInicial)
            telainicial.visibility = View.VISIBLE
            val layoutin = AnimationUtils.loadAnimation(this, R.anim.telainicial_comeback_withdelay_paraajustelogin)
            telainicial.startAnimation(layoutin)
        } else if (result.equals("email_logado")){
            layLoginMail.visibility = View.GONE
            val email = MainModels.getUserMail()
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)

        } else if (result.equals("logado")){

            val email = MainModels.getUserMail()
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)

            startActivity(intent)
            val telaDeVerificacao =
                findViewById<ConstraintLayout>(R.id.layLoginWithMail_VerificationMail)
            telaDeVerificacao.visibility = View.GONE
            val telaLoginMail = findViewById<ConstraintLayout>(R.id.layLoginWithEmail)
            telaLoginMail.visibility = View.GONE
            val telaLoginMailNew =
                findViewById<ConstraintLayout>(R.id.layLoginWithEmail_newUser)
            telaLoginMailNew.visibility = View.GONE
            val telainicial = findViewById<ConstraintLayout>(R.id.layInicial)
            telainicial.visibility = View.VISIBLE
            EncerraDialog()

            val layoutin = AnimationUtils.loadAnimation(this, R.anim.telainicial_comeback_withdelay_paraajustelogin)
            telainicial.startAnimation(layoutin)

        } else if (result.equals("facebook")){

            val email = MainModels.getUserMail()
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)

            EncerraDialog()
            val telainicial: ConstraintLayout = findViewById(R.id.layInicial)
            val layoutin = AnimationUtils.loadAnimation(this, R.anim.telainicial_comeback_withdelay_paraajustelogin)
            telainicial.startAnimation(layoutin)

        } else {

            //naoLogado
            val telaDeVerificacao = findViewById<ConstraintLayout>(R.id.layLoginWithMail_VerificationMail)
            telaDeVerificacao.visibility = View.GONE
            val telaLoginMail = findViewById<ConstraintLayout>(R.id.layLoginWithEmail)
            telaLoginMail.visibility = View.GONE
            val telaLoginMailNew = findViewById<ConstraintLayout>(R.id.layLoginWithEmail_newUser)
            telaLoginMailNew.visibility = View.GONE
            val telainicial = findViewById<ConstraintLayout>(R.id.layInicial)
            telainicial.visibility = View.VISIBLE
            layinicialVoltaAoInicio()
            EncerraDialog()

        }

    }

    private fun createAccount(email: String, password: String) {


        val fieldEmail = findViewById<EditText>(R.id.fieldEmail_newUser)
        val fieldPassword = findViewById<EditText>(R.id.fieldPassword_newUser)
        val confirmaPassword = fieldPasswordConfirmation_newUser.text.toString()
            //val email = fieldEmail.text.toString()
        val password = fieldPassword.text.toString()

        if (fieldEmail.text.toString().isEmpty()) {
                fieldEmail.error = "Obrigatório"

        } else if (!fieldEmail.text.toString().contains("@")){
                fieldEmail.error = "E-mail inválido"

        } else if (!fieldEmail.text.toString().contains(".")){
                fieldEmail.error = "E-mail inválido"

        } else if (TextUtils.isEmpty(password)) {
                fieldPassword.error = "Obrigatório"

        } else if (password.length<6){
                fieldPassword.error = "A senha deve ter pelo menos 6 dígitos"

        } else if (TextUtils.isEmpty(confirmaPassword)) {
            fieldPasswordConfirmation_newUser.error = "Obrigatório"

        } else if (!confirmaPassword.equals(password)) {
                fieldPasswordConfirmation_newUser.error = "As senhas são diferentes"

        } else {


            // [START create_user_with_email]
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information

                        var retorno = "nao"
                        val user = MainModels.returnUser()
                        if (user.equals("nao")){
                            retorno = MainController.updateUI("nao", "null")
                        } else {
                            retorno = MainController.updateUI(user, "unknown")
                        }
                        retornoUpdateUi(retorno)

                        val layLoginWithMail_VerificationMail = findViewById<ConstraintLayout>(R.id.layLoginWithMail_VerificationMail)
                        layLoginWithMail_VerificationMail.visibility=View.VISIBLE
                        laygenericoInLeftToCenter(layLoginWithMail_VerificationMail)
                        emailVerificationCheckMeth()
                        val layNovoUser= findViewById<ConstraintLayout>(R.id.layLoginWithEmail_newUser)
                        layNovoUser.visibility = View.GONE
                        laygenericoOutCenterToLeft(layNovoUser)
                        MainModels.createNewUser()

                    } else {
                        // If sign in fails, display a message to the user.
                        //Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "A autenticação falhou",
                            Toast.LENGTH_SHORT).show()
                        retornoUpdateUi("nao")
                    }

                }

        }


    }

    private fun signIn() {

        layinicialOut()

        val fieldEmail = findViewById<EditText>(R.id.fieldEmail)
        val fieldPassword = findViewById<EditText>(R.id.fieldPassword)
        //val email = fieldEmail.text.toString()
        val password = fieldPassword.text.toString()

        if (fieldEmail.text.toString().isEmpty()) {
            fieldEmail.error = "Obrigatório"
        } else if (!fieldEmail.text.toString().contains("@")){
            fieldEmail.error = "E-mail inválido"
        } else if (!fieldEmail.text.toString().contains(".")){
            fieldEmail.error = "E-mail inválido"

        } else if (TextUtils.isEmpty(password)) {
            fieldPassword.error = "Obrigatório"

        } else if (password.length<6){
            fieldPassword.error = "A senha deve conter pelo menos 6 dígitos"

        } else {
            ChamaDialog()
            auth.signInWithEmailAndPassword(fieldEmail.text.toString(), password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information

                        var retorno = "nao"
                        val user2 = MainModels.returnUser()
                        retorno = MainController.updateUI(user2, "mail")
                        retornoUpdateUi(retorno)
                        EncerraDialog()
                    } else {

                        val mtextViewNovoUser = findViewById<TextView>(R.id.tvNovoUser)
                        mtextViewNovoUser.setTextColor(Color.parseColor("#FF8C00"));
                        Toast.makeText(this, "E-mail ou senha inválidos", Toast.LENGTH_SHORT).show()
                        EncerraDialog()
                    }

                    // [START_EXCLUDE]
                    if (!task.isSuccessful) {
                        Toast.makeText(this, "Algo deu errado", Toast.LENGTH_SHORT).show()
                        EncerraDialog()
                    }

                }
        }

        //showProgressDialog()

        // [START sign_in_with_email]

        // [END sign_in_with_email]
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
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.LayPai)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

        //aqui colocamos os ifs com cada call de cada vez que a popup for chamada
        if (call.equals("confirmaNovaSenha")){

            buttonPopupS.setOnClickListener {
                //faz aqui o que quiser
                //precisa informar o e-mail
                val emailField = findViewById<EditText>(R.id.fieldEmail)

                val auth = FirebaseAuth.getInstance()
                //val user: FirebaseUser? = auth.currentUser
                //val emailAddress = user?.email
                emailField.text.toString()?.let { it1 -> auth.sendPasswordResetEmail(it1) }

                val telaDeVerificacao =
                    findViewById<ConstraintLayout>(R.id.layLoginWithMail_VerificationMail)
                telaDeVerificacao.visibility = View.GONE
                val telaLoginMail = findViewById<ConstraintLayout>(R.id.layLoginWithEmail)
                telaLoginMail.visibility = View.GONE
                val telaLoginMailNew =
                    findViewById<ConstraintLayout>(R.id.layLoginWithEmail_newUser)
                telaLoginMailNew.visibility = View.GONE
                val telainicial = findViewById<ConstraintLayout>(R.id.layInicial)
                telainicial.visibility = View.VISIBLE

                openPopUp("E-mail enviado.", "Foi enviado um e-mail para "+emailField.text.toString()+" com sua nova senha.", false, "Ok", "OK", "confirmaNovaSenha")
                popupWindow.dismiss()
            }
        }
        /*else if (call.equals("confirmaNovaSenha")){
            //nada a fazer
        }

         */

    }

    //login do face
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
    // [END on_activity_result]

    // [START auth_with_facebook]
    private fun handleFacebookAccessToken(token: AccessToken) {
        //Log.d("testeHandleFacebook", "handleFacebookAccessToken:$token")
        // [START_EXCLUDE silent]
        //showProgressDialog()
        // [END_EXCLUDE]

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    //Log.d("testeHandleFacebook", "signInWithCredential:success")
                    val user = auth.currentUser
                    val isNewUser =
                        task.result!!.additionalUserInfo?.isNewUser
                    if (isNewUser!!) {
                        MainModels.createNewUser()
                    }
                    var retorno = "nao"
                    val user2 = MainModels.returnUser()
                    retorno = MainController.updateUI(user2, "facebook")
                    retornoUpdateUi(retorno)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("testeHandleFacebook", "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "A autenticação falhou.",
                        Toast.LENGTH_SHORT).show()
                    retornoUpdateUi("nao")
                    layinicialVoltaAoInicio()
                }

                // [START_EXCLUDE]
                //hideProgressDialog()
                // [END_EXCLUDE]
            }
    }
    //fim do login do face

    fun LoginWithEmail (){

        val layLoginMail = findViewById<ConstraintLayout>(R.id.layLoginWithEmail)  //pagina inicial
        val layNovoUser= findViewById<ConstraintLayout>(R.id.layLoginWithEmail_newUser)  //pagina inicial
        layinicialOut()
        laygenericoInRightToCenter(layLoginMail)


        val btnNovoUser = findViewById<TextView>(R.id.tvNovoUser)
        btnNovoUser.setOnClickListener {
            layLoginMail.visibility = View.GONE
            layNovoUser.visibility = View.VISIBLE
            laygenericoOutCenterToLeft(layLoginMail)
            laygenericoInRightToCenter(layNovoUser)

            val fieldEmail_newUser: EditText =findViewById(R.id.fieldEmail_newUser)
            fieldEmail_newUser.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    //TODO("Not yet implemented")
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    //TODO("Not yet implemented")
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    if (fieldEmail_newUser.text.contains("@")){
                        fieldEmail_newUser.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_icon_cadeado, 0);
                    } else {
                        fieldEmail_newUser.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_icon_cadeado_vermelho, 0);
                    }
                }

            }
            )

            val fieldPassword_newUser : EditText = findViewById(R.id.fieldPassword_newUser)
            fieldPassword_newUser.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    //TODO("Not yet implemented")
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    //TODO("Not yet implemented")
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    if (fieldPassword_newUser.text.length==6){
                        fieldPassword_newUser.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_icon_cadeado, 0);
                    } else {
                        fieldPassword_newUser.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_icon_cadeado_vermelho, 0);
                    }
                }

            }
            )

            val fieldPasswordConfirmation_newUser: EditText = findViewById(R.id.fieldPasswordConfirmation_newUser)
            fieldPasswordConfirmation_newUser.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    //TODO("Not yet implemented")
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    //TODO("Not yet implemented")
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    if (fieldPasswordConfirmation_newUser.text.toString().equals(fieldPassword_newUser.text.toString())){
                        fieldPasswordConfirmation_newUser.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_icon_cadeado, 0);
                    } else {
                        fieldPasswordConfirmation_newUser.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_icon_cadeado_vermelho, 0);
                    }
                }

            }
            )

            val btnVoltar: Button = findViewById(R.id.createWithEmail_btnCancel)
            btnVoltar.setOnClickListener {
                layLoginMail.visibility = View.VISIBLE
                layNovoUser.visibility = View.GONE
                laygenericoInLeftToCenter(layLoginMail)
                laygenericoOutCenterToRight(layNovoUser)

                btnVoltar.setOnClickListener { null }
                fieldEmail_newUser.addTextChangedListener(null)
                fieldPassword_newUser.addTextChangedListener(null)
                fieldPasswordConfirmation_newUser.addTextChangedListener(null)
            }

        }

        val signInEmail = findViewById<Button>(R.id.emailSignInButton) //botão de signin
        signInEmail.setOnClickListener {

            signIn()
            hideKeyboard()

        }

        val btnVoltarDoLoginWithMail = findViewById<Button>(R.id.loginWithEmail_Voltarbtn)
        btnVoltarDoLoginWithMail.setOnClickListener {
            val layInicial = findViewById<ConstraintLayout>(R.id.layInicial)
            hideKeyboard()
            //layLoginMail.visibility = View.GONE
            //layInicial.visibility = View.VISIBLE
            layinicialVoltaAoInicio()
            laygenericoOutCenterToRight(layLoginMail)

        }

        //itens da página de criar novo user
        /*  acho que este click estava repetido e errado. Ele invocava o mesmo botão do metodo abaixo e o botao voltar ja funciona, invocado de dentro do outro botao que chama este layout
        val btnVoltar = findViewById<Button>(R.id.emailCreateAccountButton)  //btn voltar para a tela de login com email
        btnVoltar.setOnClickListener {
            hideKeyboard()
            layLoginMail.visibility = View.VISIBLE
            layNovoUser.visibility = View.GONE
        }
         */

        val emailCreateAccountBtn = findViewById<Button>(R.id.emailCreateAccountButton) //cria usuario
        emailCreateAccountBtn.setOnClickListener {
            hideKeyboard()
            val  etEmail = findViewById<EditText>(R.id.fieldEmail_newUser)
            val etPassword = findViewById<EditText>(R.id.fieldPassword_newUser);

            createAccount(etEmail.text.toString(), etPassword.text.toString()) //a verificação é feita dentro de validateForm

        }

        val emailVerifyCheck = findViewById<Button>(R.id.verifyEmailButtonCheck) //botao que o user aperta quando ja vericou o email
        val verifyEmailButton = findViewById<Button>(R.id.verifyEmailButton) //botão para reenviar
        val verifyHelp = findViewById<Button>(R.id.verification_btnHelp)
        val verifyVoltar = findViewById<Button>(R.id.verification_btnVoltar)

        verifyHelp.setOnClickListener {
            openPopUp("Ajuda", "Por que você precisa verificar seu e-mail?\n\nCom a verificação de e-mail garantimos que é o dono do e-mail que está acessando. Assim impedimos terceiros de se aproveitarem de dados de outras pessoas. É para a segurança de toda comunidade.\n\nO que eu devo fazer?\n\nVocê precisa esperar chegar o e-mail de confirmação e clicar no link confirmando.\n\nNão chegou nenhum e-mail. O que eu faço?\n\nVocê pode procurar na pasta de spam ou lixo do seu e-mail. Ou clicar no botão abaixo para reenviar.", false, "n", "n", "n")
        }

        verifyVoltar.setOnClickListener {
            laygenericoOutCenterToRight(layLoginWithMail_VerificationMail)
            val layinicial: ConstraintLayout = findViewById(R.id.layInicial)
            layinicial.visibility = View.VISIBLE
            layinicialVoltaAoInicio()
        }

        verifyEmailButton.setOnClickListener {
            MainModels.sendEmailVerification(this)
        }

        emailVerificationCheckMeth()

        emailVerifyCheck.setOnClickListener {
            val user = auth.currentUser
            if (user != null) {
                user!!.reload()

                if (!user.isEmailVerified){
                    Toast.makeText(this, "O e-mail ainda não foi verificado.", Toast.LENGTH_SHORT)
                } else {
                    var retorno = "nao"
                    val user2 = MainModels.returnUser()
                    retorno = MainController.updateUI(user2, "mail")
                    retornoUpdateUi(retorno)

                }
            }
        }


        val novaSenha = findViewById<TextView>(R.id.tvPerdeuSenha)
        novaSenha.setOnClickListener {

            val emailField = findViewById<EditText>(R.id.fieldEmail)
            if (emailField.text.toString().isEmpty()){
                emailField.requestFocus()
                emailField.setError("Informe o e-mail para enviar o reset da senha.")
            } else if (!emailField.text.toString().contains("@")){
                emailField.requestFocus()
                emailField.setError("Informe um e-mail válido")
            } else {
                openPopUp("ATENÇÃO", "Você deseja receber uma nova senha por e-mail?", true, "Sim, quero", "Não", "confirmaNovaSenha")
            }
        }

    }

    fun emailVerificationCheckMeth () {

        val emailVerifyCheck = findViewById<Button>(R.id.verifyEmailButtonCheck) //botao que o user aperta quando ja vericou o email
        emailVerifyCheck.setOnClickListener {
            val user = auth.currentUser
            if (user != null) {
                user!!.reload()

                var retorno = "nao"
                val user2 = MainModels.returnUser()
                retorno = MainController.updateUI(user2, "mail")

                retornoUpdateUi(retorno)
                }
            }
        }

    //por fim, pegue o retorno dos métodos aqui
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 999){
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                var retorno = "nao"
                val user = MainModels.returnUser()
                if (user.equals("nao")){
                    retorno = MainController.updateUI("nao", "null")
                } else {
                    retorno = MainController.updateUI(user, "unknown")
                }

                retornoUpdateUi(retorno)

            }
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

    /* To hide Keyboard */
    fun hideKeyboard() {
        try {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun layinicialOut(){
        val telainicial: ConstraintLayout = findViewById(R.id.layInicial)
        val layoutin = AnimationUtils.loadAnimation(this, R.anim.layout_slideout)
        telainicial.startAnimation(layoutin)

        val btnLoginSemLogin: Button = findViewById(R.id.btnLoginDepois)
        val btnFace: Button = findViewById(R.id.layInicial_btnSignWithFace)
        val btnMail: Button = findViewById(R.id.layInicial_btnSignWithEmail)
        btnFace.isEnabled = false
        btnMail.isEnabled = false
        btnLoginSemLogin.isEnabled = false
    }

    fun layinicialVoltaAoInicio(){
        val telainicial: ConstraintLayout = findViewById(R.id.layInicial)
        val layoutin = AnimationUtils.loadAnimation(this, R.anim.layout_cameback)
        telainicial.startAnimation(layoutin)

        val btnLoginSemLogin: Button = findViewById(R.id.btnLoginDepois)
        val btnFace: Button = findViewById(R.id.layInicial_btnSignWithFace)
        val btnMail: Button = findViewById(R.id.layInicial_btnSignWithEmail)
        btnFace.isEnabled = true
        btnMail.isEnabled = true
        btnLoginSemLogin.isEnabled = true
    }

    fun laygenericoOutCenterToLeft (layout:ConstraintLayout){

        val layoutMove = AnimationUtils.loadAnimation(this, R.anim.layout_slideout)
        layout.startAnimation(layoutMove)

    }


    fun laygenericoInRightToCenter (layout:ConstraintLayout){

        val layoutMove = AnimationUtils.loadAnimation(this, R.anim.layout_slidein)
        layout.visibility = View.VISIBLE
        layout.startAnimation(layoutMove)

    }

    fun laygenericoInLeftToCenter (layout:ConstraintLayout){

        val layoutMove = AnimationUtils.loadAnimation(this, R.anim.layout_slidein_left_to_center)
        layout.startAnimation(layoutMove)

    }

    fun laygenericoOutCenterToRight (layout:ConstraintLayout){

        val layoutMove = AnimationUtils.loadAnimation(this, R.anim.layout_slideout_centro_to_right)
        layout.startAnimation(layoutMove)

    }

}

