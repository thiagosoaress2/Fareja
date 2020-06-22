package com.petcare.petcare

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_minhas_vendas.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class minhasComprasActivity : AppCompatActivity() {

    var userBD: String = ""
    var aberto = false

    var travaTela=false

    val delimiter = "!?!%"

    val arrayData: MutableList<String> = ArrayList()
    val arrayHora: MutableList<String> = ArrayList()
    val arrayBdDoPet: MutableList<String> = ArrayList()
    val arrayNomeDoPet: MutableList<String> = ArrayList()
    val arrayValorCompra: MutableList<String> = ArrayList()
    val arrayStatus: MutableList<String> = ArrayList()
    val arrayBdDaCompra: MutableList<String> = ArrayList()

    val arrayServicoPrestado: MutableList<String> = ArrayList()


    val arrayProdutos: MutableList<String> = ArrayList()  //este array não é usado no recyclewview, somente pra exibir detalhes da compra

    private lateinit var databaseReference: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minhas_compras)

        userBD = intent.getStringExtra("userBD")

        databaseReference = FirebaseDatabase.getInstance().reference
        QueryMinhasCompras()

        QueryEnrollmentQualEoBd()
        //QueryMeusServicosContratados()

        val btnVoltar : Button = findViewById(R.id.resumoCompras_btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }

    }

    //busca informações iniciais do usuario
    fun QueryMinhasCompras() {

        //ChamaDialog()
        FirebaseDatabase.getInstance().reference.child("compras").orderByChild("cliente").equalTo(userBD).limitToLast(50)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //não existem compras
                        } else {

                            var values: String
                            values = querySnapshot.child("hora_compra").value.toString()
                            arrayHora.add(values)
                            values = querySnapshot.child("data_compra").value.toString()

                            if (values.contains("/")){  //se tiver / é pq por algum motivo o user estava usando versão antiga e salvou dd/mm/yyy ao invés de em millis.
                                //do nothing
                            } else {
                                values = ConvertMillistoDateInString(values.toLong())
                            }

                            arrayData.add(values)
                            values = querySnapshot.child("valor").value.toString()
                            values = values.replace("O valor total da sua compra é: ", "")
                            arrayValorCompra.add(values)
                            values = querySnapshot.child("petshop").value.toString()
                            arrayBdDoPet.add(values)
                            values = querySnapshot.child("nomePet").value.toString()
                            arrayNomeDoPet.add(values)
                            values = querySnapshot.key.toString()
                            arrayBdDaCompra.add(values)

                            values = querySnapshot.child("status").value.toString()
                            arrayStatus.add(values)

                        }
                    }

                    MontaRecyclerView()

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })

    }

    fun MontaRecyclerView (){

        //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
        var adapter: MinhasComprasRecyclerViewAdapter = MinhasComprasRecyclerViewAdapter(this, arrayNomeDoPet, arrayData, arrayHora, arrayValorCompra, arrayBdDoPet, arrayStatus, arrayBdDaCompra)

//chame a recyclerview
        var recyclerView: RecyclerView = findViewById(R.id.resumoCompras_recyclerView)

//define o tipo de layout (linerr, grid)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)

//coloca o adapter na recycleview
        recyclerView.adapter = adapter

        recyclerView.layoutManager = linearLayoutManager

// Notify the adapter for data change.
        adapter.notifyDataSetChanged()


        //click da recycleview
        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView.addOnItemTouchListener(RecyclerTouchListener(this, recyclerView!!, object: ClickListener{

            override fun onClick(view: View, position: Int) {
                //Log.d("teste", aNome.get(position))
                //Toast.makeText(this@MainActivity, !! aNome.get(position).toString(), Toast.LENGTH_SHORT).show()
                //openPopUp("Finalização de compra", "Você confirma o recebimento do produto?", true, "Sim, confirmo", "Não", "rating", arrayBdDoPet.get(position), position, arrayBdDaCompra.get(position))

                if (aberto==false) { //só abre se for false

                    aberto=true //agora a popup ja esta aberta e recebe true. Assim não pode abrir outra por cima.
                    //verifica o status do pedido e chama a popup com as respectivas opções liberadas.
                    if (arrayStatus.get(position).equals("encerrado")){
                        openPopUpEspecial(arrayBdDoPet.get(position),position, arrayBdDaCompra.get(position), arrayStatus.get(position), false, false)
                    } else if (arrayStatus.get(position).equals("cliente confirma recebimento")){
                        openPopUpEspecial(arrayBdDoPet.get(position),position, arrayBdDaCompra.get(position), arrayStatus.get(position), false, true)
                    } else {
                        openPopUpEspecial(arrayBdDoPet.get(position),position, arrayBdDaCompra.get(position), arrayStatus.get(position), true, true)
                    }

                }


            }

            override fun onLongClick(view: View?, position: Int) {

            }
        }))



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

                if (call.equals("rating")){
                //dar nota

                } else if (call.equals("confirma_entrega")){
                    //confirmar que recebe o produto
                    //databaseReference.child("compras").child(bdDaCompra).child("status").setValue("cliente confirma recebimento")
                }

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

    //especial para tratar das reclamações e recebimento
    fun openPopUpEspecial (bdDoPet: String, position: Int, bdDaCompra:String, status: String, ExibebtnRecebimento: Boolean, ExibebtnReclamacao: Boolean) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao

        //EXIBIR POPUP
        // Initialize a new layout inflater instance
        val inflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate a custom view using layout inflater
        val view = inflater.inflate(R.layout.popup_minhascompras,null)

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
        val btnRecebi = view.findViewById<Button>(R.id.btnRecebi)
        val btnReclamacao = view.findViewById<Button>(R.id.btnReclamacao)
        val btnFechar = view.findViewById<Button>(R.id.btnFecharActivityRelatorio)

        if (!ExibebtnRecebimento){
            btnRecebi.visibility = View.GONE
        }

        if (!ExibebtnReclamacao){
            btnReclamacao.visibility = View.GONE
        }



            //exibe e ajusta os textos dos botões

            // Set a click listener for popup's button widget
            btnFechar.setOnClickListener{
                // Dismiss the popup window
                popupWindow.dismiss()
            }

            btnRecebi.setOnClickListener {

                if (status.equals("cliente confirma recebimento")){ //nesta opção ele ja confirmouq ue recebeu e entrou aqui novamente
                    Toast.makeText(this, "Estamos esperando o vendedor confirmar a entrega para encerrar a transação.", Toast.LENGTH_LONG).show()
                } else if (status.equals("vendedor confirma entrega")){ //aqui o outro lado já deu ok  e encerra quando cliente confirmar.
                    databaseReference.child("compras").child(bdDaCompra).child("status").setValue("encerrado")
                    Toast.makeText(this, "Tudo certo. Esta transação foi encerrada.", Toast.LENGTH_LONG).show()
                    arrayBdDaCompra.removeAt(position)
                    arrayStatus.removeAt(position)
                    arrayNomeDoPet.removeAt(position)
                    arrayValorCompra.removeAt(position)
                    arrayBdDoPet.removeAt(position)
                    arrayData.removeAt(position)
                    arrayHora.removeAt(position)

                } else { //aqui é quando o cliente acusa recebimento mas o outro lado ainda não confirmou
                    databaseReference.child("compras").child(bdDaCompra).child("status").setValue("cliente confirma recebimento")
                    Toast.makeText(this, "O recebimento foi registrado. Aguardando a loja confirmar a entrega para encerrar no negócio", Toast.LENGTH_LONG).show()
                    arrayStatus.set(position, "cliente confirma recebimento")

                }

                popupWindow.dismiss()
            }



        val btnDetalhes: Button = view.findViewById(R.id.btnDetalhes)
        //abre os detalhes da compra
        btnDetalhes.setOnClickListener {
            aberto=false
            popupWindow.dismiss()

            //ChamaDialog
            val layDetalhes: ConstraintLayout = findViewById(R.id.layDetalhesCompra)
            layDetalhes.visibility = View.VISIBLE
            val btnVoltar: Button = findViewById(R.id.detalhes_btnVoltar)
            QueryDetalhamentoDasCompras(bdDaCompra)
            btnVoltar.setOnClickListener {
                layDetalhes.visibility = View.GONE
            }

        }


        btnReclamacao.setOnClickListener {
            aberto=false

                val layReclamacao: ConstraintLayout = findViewById(R.id.layReclamacao)
                layReclamacao.visibility = View.VISIBLE

                layReclamacao.setOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                        // ignore all touch events
                        return true
                    }
                })

                val etDescreveProblema: EditText = findViewById(R.id.etDescreveProblema)
                etDescreveProblema.visibility = View.GONE

                var tipoProblema = "nao"
                var btn: Button

                btn = findViewById(R.id.reclamacao_btnFechar)
                btn.setOnClickListener {
                    layReclamacao.visibility = View.GONE
                    etDescreveProblema.setText("")
                }

                btn = findViewById(R.id.reclamacao_btnProblemaNaEntrega)
                btn.setOnClickListener {
                    tipoProblema = "Problema no produto"
                    etDescreveProblema.visibility = View.VISIBLE
                    etDescreveProblema.requestFocus()
                }

                btn = findViewById(R.id.reclamacao_btnPagamento)
                btn.setOnClickListener {
                    tipoProblema = "Problema no atendimento"
                    etDescreveProblema.visibility = View.VISIBLE
                    etDescreveProblema.requestFocus()
                }

                btn = findViewById(R.id.reclamacao_btnNaoEntregou)
                btn.setOnClickListener {
                    tipoProblema = "Não entregou o produto"
                    etDescreveProblema.visibility = View.VISIBLE
                    etDescreveProblema.requestFocus()
                }

                btn = findViewById(R.id.reclamacao_btnOutros)
                btn.setOnClickListener {
                    tipoProblema = "outros"
                    etDescreveProblema.visibility = View.VISIBLE
                    etDescreveProblema.requestFocus()
                }

                btn = findViewById(R.id.reclamacao_btnEnviar)
                btn.setOnClickListener {
                    if (etDescreveProblema.text.isEmpty()){
                        etDescreveProblema.requestFocus()
                        etDescreveProblema.setError("Informe detalhes do problema")
                    } else {
                        val newCad: DatabaseReference = databaseReference.child("reclamacoes_por_parte_dos_clientes").push()
                        newCad.child("problema").setValue(tipoProblema)
                        newCad.child("descricao").setValue(etDescreveProblema.text.toString())
                        newCad.child("petshop").setValue(bdDoPet)
                        newCad.child("compra_com_problema").setValue(bdDaCompra)
                        newCad.child("bdCliente").setValue(userBD)
                        Toast.makeText(this, "Sua reclamação foi registrada. A empresa reberá uma notificação e nós tomamos ciência. Pedimos desculpa por nossos parceiros e buscaremos melhorar.", Toast.LENGTH_LONG).show()
                        layReclamacao.visibility = View.GONE
                    }
                }

                popupWindow.dismiss()
            }


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            aberto=false
            popupWindow.dismiss()
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.layResumoCompras)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }

    //busca informações detalhadas da venda
    fun QueryDetalhamentoDasCompras(bdDaCompra: String) {

        QueryProdutosComprados(bdDaCompra)

        val rootRef = databaseReference.child("compras").child(bdDaCompra)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                // EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")
                var values: String
                var tv: TextView
                tv = findViewById(R.id.tvCodigo)
                tv.setText("Código: "+bdDaCompra)
                values = p0.child("data_compra").value.toString()
                tv = findViewById(R.id.tvDataEhora)
                tv.setText("Data e hora: "+values)
                values = p0.child("hora_compra").value.toString()
                tv.setText(tv.text.toString()+" "+values)
                //produtos é pego na query a seguir pq estão em outro node
                values = p0.child("BuscaOuEntrega").value.toString()
                tv = findViewById(R.id.tvBuscaOuEntrega)
                tv.setText("Tipo de venda: "+values)
                values = p0.child("Endereco_entrega").value.toString()
                tv = findViewById(R.id.tvEnderecoEntrega)
                tv.setText("Endereço: "+values)
                values = p0.child("valor").value.toString()
                tv = findViewById(R.id.tvValorCompra)
                //tv.setText("Valor: "+currencyTranslation(values))
                tv.setText("Valor: "+values)
                values = p0.child("FormaPgto").value.toString()
                tv = findViewById(R.id.tvFormaPgto)
                tv.setText(values)
                values = p0.child("status").value.toString()
                val tvSituacao: TextView = findViewById(R.id.tvSituacao)
                tvSituacao.setText("situação do pedido: "+values)

            }

        })

    }

    //busca informações iniciais do usuario
    fun QueryProdutosComprados(bdDaCompra: String) {

        //ChamaDialog()
        //vai pegar todos os itens
        FirebaseDatabase.getInstance().reference.child("compras").child(bdDaCompra).child("produtos_vendidos").orderByChild("controle").equalTo("item")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //não existem Vendas
                        } else {

                            var values: String
                            values = querySnapshot.child("item").value.toString()
                            arrayProdutos.add(values)

                        }
                    }

                    val tvProd : TextView = findViewById(R.id.tvProdutos)
                    var str: String = ""
                    var cont=0
                    while (cont<arrayProdutos.size){
                        if (cont==0){
                            str = arrayProdutos.get(cont)
                        } else {
                            str = str+", "+arrayProdutos.get(cont)
                        }
                        cont++
                    }
                    tvProd.setText("Produtos: "+str)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })

    }





    //metodos especiais dos servicos de autonomos

    //O que acontecia: O caminho no bd ficava: Servicos---BDdoPrestador---Pushed ----infos
    //Então na hora da busca aqui pelo user nós só conseguiamos buscar em Serviços já que não sabiamos o bdDoPrestador. Então criamos este campo servicos_Enrollment
    fun QueryEnrollmentQualEoBd (){

        val arrayServicosQueEsteClienteEstaEnvolvido: MutableList<String> = ArrayList()

        //ChamaDialog()
        //FirebaseDatabase.getInstance().reference.child("servicosEnrollment").child(userBD).orderByChild("situacao").equalTo("aberto")
        FirebaseDatabase.getInstance().reference.child("servicosEnrollment").orderByChild("cliente").equalTo(userBD)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //não existem compras
                        } else {

                            var values: String
                            values = querySnapshot.key.toString()

                            val tokens = StringTokenizer(values, delimiter)
                            val bdQueEuQuero: String = tokens.nextToken() // this will contain PrestadorServico bd7

                            arrayServicosQueEsteClienteEstaEnvolvido.add(bdQueEuQuero)

                        }
                    }

                    var cont=0
                    while (cont<arrayServicosQueEsteClienteEstaEnvolvido.size){
                            QueryMeusServicosContratados(arrayServicosQueEsteClienteEstaEnvolvido.get(cont))
                        cont++
                    }


                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })


    }

    fun QueryMeusServicosContratados(bd: String) {


        //ChamaDialog()
        FirebaseDatabase.getInstance().reference.child("servicos").child(bd).orderByChild("cliente").equalTo(userBD) //nao precisa limitar. Agora só aparecem os serviços em aberto
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //não existem compras
                        } else {

                            /*  arrayServicoPrestado
                                pos 0 - data
                                pos 1 - hora
                                pos 2 - status
                                pos 3 - apelido
                                pos 4 - servico
                                pos 6 - bd do servico
                                pos 7 - bd do prestador do serviço
                             */

                            var values: String
                            var textContainer : String

                            values = querySnapshot.child("data").value.toString()
                            textContainer = values+delimiter

                            values = querySnapshot.child("hora").value.toString()
                            textContainer = textContainer+values+delimiter

                            values = querySnapshot.child("status").value.toString()
                            textContainer = textContainer+values+delimiter

                            values = querySnapshot.child("apelido").value.toString()
                            textContainer = textContainer+values+delimiter

                            values = querySnapshot.child("servico").value.toString()
                            textContainer = textContainer+values+delimiter

                            values = querySnapshot.key.toString()
                            textContainer = textContainer+values+delimiter

                            textContainer = textContainer+bd+delimiter

                            arrayServicoPrestado.add(textContainer)

                            /*  arrayServicoPrestado
                                pos 0 - data
                                pos 1 - hora
                                pos 2 - status
                                pos 3 - apelido
                                pos 4 - servico
                                pos 6 - bd do servico
                                pos 7 - bd do prestador de serviço
                             */

                        }
                    }

                    MontaRecyclerViewServicosPrestados()

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })

    }

    /*
    //busca informações iniciais do usuario
    fun QueryMeusServicosContratados() {


        //ChamaDialog()
        FirebaseDatabase.getInstance().reference.child("serviços").orderByChild("cliente").equalTo(userBD).limitToLast(50)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //não existem compras
                        } else {

                            var values: String
                            var textContainer : String

                            values = querySnapshot.child("data").value.toString()
                            textContainer = values+delimiter

                            values = querySnapshot.child("hora").value.toString()
                            textContainer = textContainer+values+delimiter
                            if (values.contains("/")){  //se tiver / é pq por algum motivo o user estava usando versão antiga e salvou dd/mm/yyy ao invés de em millis.
                                //do nothing
                            } else {
                                values = ConvertMillistoDateInString(values.toLong())
                            }

                            textContainer = textContainer+values+delimiter

                            values = querySnapshot.child("status").value.toString()
                            textContainer = textContainer+values+delimiter


                            values = querySnapshot.key.toString()
                            textContainer = textContainer+values+delimiter

                            /*
                            0 - data
                            1 - hora
                            2 - status
                            3 - bd do servico p atualizar ou apagar
                             */

                        }
                    }

                    MontaRecyclerViewServicosPrestados()

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })

    }

     */

    fun MontaRecyclerViewServicosPrestados(){

        //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
        var adapter: MeusServicosContratadosAdapter = MeusServicosContratadosAdapter(this, arrayServicoPrestado)

//chame a recyclerview
        var recyclerView: RecyclerView = findViewById(R.id.resumoCompras_recyclerViewServicos)

//define o tipo de layout (linerr, grid)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)

//coloca o adapter na recycleview
        recyclerView.adapter = adapter

        recyclerView.layoutManager = linearLayoutManager

// Notify the adapter for data change.
        adapter.notifyDataSetChanged()


        //click da recycleview
        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView.addOnItemTouchListener(RecyclerTouchListener(this, recyclerView!!, object: ClickListener{

            override fun onClick(view: View, position: Int) {
                //Log.d("teste", aNome.get(position))
                //Toast.makeText(this@MainActivity, !! aNome.get(position).toString(), Toast.LENGTH_SHORT).show()
                //openPopUp("Finalização de compra", "Você confirma o recebimento do produto?", true, "Sim, confirmo", "Não", "rating", arrayBdDoPet.get(position), position, arrayBdDaCompra.get(position))

                if (aberto==false) { //só abre se for false

                    aberto=true //agora a popup ja esta aberta e recebe true. Assim não pode abrir outra por cima.
                    //verifica o status do pedido e chama a popup com as respectivas opções liberadas.

                    val tokens = StringTokenizer(arrayServicoPrestado.get(position), "!?!%")
                    val data = tokens.nextToken() // this will contain "img link or nao"
                    val hora = tokens.nextToken()
                    val status = tokens.nextToken()
                    val apelido = tokens.nextToken()
                    val servico = tokens.nextToken()
                    val bdDoServico = tokens.nextToken()  //bd do user
                    val bdDoPrestadorDeServico = tokens.nextToken()
                    //val bdDoCliente = tokens.nextToken()  //bd do user

                    if (status.equals("encerrado")){
                        openPopUpServicoPrestado(userBD ,position, bdDoServico, status, false, data, hora, bdDoPrestadorDeServico)
                    } else if (status.equals("cliente confirma serviço prestado")){
                        openPopUpServicoPrestado(userBD, position, bdDoServico, status, false, data, hora, bdDoPrestadorDeServico)
                    } else if (status.equals("O serviço foi cancelado.")){
                        maketoast("Serviço foi cancelado.")
                    } else {
                        openPopUpServicoPrestado(userBD ,position, bdDoServico, status, true, data, hora, bdDoPrestadorDeServico)
                    }

                }

            }

            override fun onLongClick(view: View?, position: Int) {

            }
        }))



    }


    fun maketoast(mensagem: String){
        Toast.makeText(this@minhasComprasActivity, mensagem, Toast.LENGTH_SHORT).show()
    }

    //especial para tratar das reclamações e recebimento
    fun openPopUpServicoPrestado (bduser: String, position: Int, bddoServico:String, status: String, ExibeBtnRecebimento: Boolean, data: String, hora: String, bdDoPrestador: String) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao

        /*
        0 - data
        1 - hora
        2 - status
        3 - bd do servico p atualizar ou apagar
         */

        //EXIBIR POPUP
        // Initialize a new layout inflater instance
        val inflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate a custom view using layout inflater
        val view = inflater.inflate(R.layout.popup_minhascompras,null)

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
        val btnRecebi = view.findViewById<Button>(R.id.btnRecebi)

        btnRecebi.setText("Confirmar serviço prestado")
        val btnCancelarServ = view.findViewById<Button>(R.id.btnReclamacao) //btnReclamação vai ser o botão de cancelar o ticket pois pode ser que após o contato do eles não fecharem o serviço. Aí cancelam
        btnCancelarServ.setText("Cancelar serviço")
        val btnFechar = view.findViewById<Button>(R.id.btnFecharActivityRelatorio)

        btnRecebi.setText("Confirmar serviço prestado")

        if (!ExibeBtnRecebimento){
            btnRecebi.visibility = View.GONE
            btnCancelarServ.visibility = View.GONE
        }


        //exibe e ajusta os textos dos botões

        // Set a click listener for popup's button widget
        btnFechar.setOnClickListener{
            // Dismiss the popup window
            popupWindow.dismiss()
            aberto=false
        }

        btnRecebi.setOnClickListener {

            if (status.equals("cliente confirma serviço prestado")) { //nesta opção ele ja confirmou que recebeu e entrou aqui novamente. Não é pra fazer nada

                Toast.makeText(this, "Aguardando o prestados do serviço confirmar.", Toast.LENGTH_LONG).show()

            } else if (status.equals("Prestador confirma serviço feito")){ //neste caso o cliente está finalizando o serviço e o prestador também ja finalizou. É pra fechar tudo


                val tokens = StringTokenizer(arrayServicoPrestado.get(position), "!?!%")
                val data = tokens.nextToken() // this will contain "img link or nao"
                val hora = tokens.nextToken()
                val status = tokens.nextToken()
                val apelido = tokens.nextToken()
                val servico = tokens.nextToken()
                val bdDoServico = tokens.nextToken()  //bd do user
                val bdDoPrestadorDeServico = tokens.nextToken()
                //val bdDoCliente = tokens.nextToken()  //bd do user

                queryUserNota(bdDoPrestadorDeServico)
                databaseReference.child("servicos").child(bdDoPrestador).child(bddoServico).child("status").setValue("encerrado")
                databaseReference.child("servicosEnrollment").child(bdDoPrestador+delimiter+userBD).removeValue()
                arrayServicoPrestado.set(position, data+delimiter+hora+delimiter+"Encerrado"+delimiter+bddoServico+delimiter+bdDoPrestadorDeServico)
            } else { //cliente está confirmando que o serviço foi prestado
                databaseReference.child("servicos").child(bdDoPrestador).child(bddoServico).child("status").setValue("Cliente confirma serviço feito")
                Toast.makeText(this, "Serviço registrado. Aguardando o prestador confirmar  para encerrar no negócio", Toast.LENGTH_LONG).show()

                val tokens = StringTokenizer(arrayServicoPrestado.get(position), "!?!%")
                val data = tokens.nextToken() // this will contain "img link or nao"
                val hora = tokens.nextToken()
                val status = tokens.nextToken()
                val apelido = tokens.nextToken()
                val servico = tokens.nextToken()
                val bdDoServico = tokens.nextToken()  //bd do user
                val bdDoPrestadorDeServico = tokens.nextToken()
                //val bdDoCliente = tokens.nextToken()  //bd do user

                queryUserNota(bdDoPrestadorDeServico)  //avaliar logo pq o user nao volta mais aqui

                databaseReference.child("servicosEnrollment").child(bdDoPrestador+delimiter+userBD).removeValue()
                arrayServicoPrestado.set(position, data+delimiter+hora+delimiter+"Cliente confirma serviço feito"+delimiter+bddoServico+delimiter+bdDoPrestadorDeServico)
            }
            aberto=false
            popupWindow.dismiss()
        }

        btnCancelarServ.setText("Serviço cancelado")
        btnCancelarServ.setOnClickListener {
            databaseReference.child("servicos").child(bddoServico).child(bduser).child("status").setValue("Cancelado")
            Toast.makeText(this, "Serviço cancelado. Aguardando o cliente confirmar encerrar este negocio", Toast.LENGTH_LONG).show()

            val tokens = StringTokenizer(arrayServicoPrestado.get(position), "!?!%")
            val data = tokens.nextToken() // this will contain "img link or nao"
            val hora = tokens.nextToken()
            val status = tokens.nextToken()
            val apelido = tokens.nextToken()
            val servico = tokens.nextToken()
            val bdDoServico = tokens.nextToken()  //bd do user
            val bdDoPrestadorDeServico = tokens.nextToken()

            arrayServicoPrestado.set(position, data+delimiter+hora+delimiter+"Você cancelou este serviço"+delimiter+bddoServico+delimiter+bdDoPrestadorDeServico)
            databaseReference.child("servicosEnrollment").child(bdDoPrestador+delimiter+userBD).removeValue()
            popupWindow.dismiss()
        }

        //parei aqui
        val btnDetalhes: Button = view.findViewById(R.id.btnDetalhes)
        btnDetalhes.visibility = View.GONE
        //abre os detalhes da compra


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            aberto=false
            popupWindow.dismiss()
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.layResumoCompras)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }

    fun queryUserNota(bdDoPrestador:String){

        travaTela=true

        ChamaDialog()

        val rootRef = databaseReference.child("autonomos").child(bdDoPrestador)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")
                var values: String
                values = p0.child("nota").getValue().toString()
                val notaPrestador = values.toDouble()
                values = p0.child("avaliacoes").getValue().toString()
                val qntAvaliacoes = values.toDouble()

                var media: Double = 0.0
                if (notaPrestador==0.0 && qntAvaliacoes ==0.0){
                    media=0.0
                } else {
                    media = (notaPrestador/qntAvaliacoes).toDouble()
                }

//                databaseReference.child("autonomos").child(bdDoPrestador).child("nota").setValue(media)
  //              databaseReference.child("autonomos").child(bdDoPrestador).child("avaliacoes").setValue(qntAvaliacoes+1)

                AbreAvaliacaoDoServico(bdDoPrestador, notaPrestador, qntAvaliacoes)
                EncerraDialog()

            }
        })
    }

    fun AbreAvaliacaoDoServico (bdDoPrestador: String, notaPrestador:Double, qntAvaliacoes: Double){

        travaTela=true

        val abreAval: ConstraintLayout = findViewById(R.id.layAvaliacao)
        abreAval.visibility = View.VISIBLE

        var votou: Int = 0

        // Get the widgets reference from custom view
        val buttonPronto = findViewById<Button>(R.id.btnPronto)
        val ivNota1: ImageView = findViewById(R.id.ivnota1)
        val ivNota2: ImageView = findViewById(R.id.ivnota2)
        val ivNota3: ImageView = findViewById(R.id.ivnota3)

        ivNota1.setOnClickListener {
            votou=1
            ivnota1.setImageResource(R.drawable.ic_avaliacao_gold)
            ivnota2.setImageResource(R.drawable.ic_avaliation_branco)
            ivnota3.setImageResource(R.drawable.ic_avaliation_branco)
        }

        ivNota2.setOnClickListener {
            votou=2
            ivnota1.setImageResource(R.drawable.ic_avaliacao_gold)
            ivnota2.setImageResource(R.drawable.ic_avaliacao_gold)
            ivnota3.setImageResource(R.drawable.ic_avaliation_branco)
        }

        ivNota3.setOnClickListener {
            votou=2
            ivnota1.setImageResource(R.drawable.ic_avaliacao_gold)
            ivnota2.setImageResource(R.drawable.ic_avaliacao_gold)
            ivnota3.setImageResource(R.drawable.ic_avaliacao_gold)
        }


        // Set a click listener for popup's button widget
        buttonPronto.setOnClickListener{

            if (votou!=0){

                var avaliacoes: Double = qntAvaliacoes.toDouble()+1
                var notafinal: Double = (notaPrestador.toDouble()+votou.toDouble()).toDouble()
                notafinal= notafinal/avaliacoes

                //                databaseReference.child("autonomos").child(bdDoPrestador).child("nota").setValue(media)
                //              databaseReference.child("autonomos").child(bdDoPrestador).child("avaliacoes").setValue(qntAvaliacoes+1)


                databaseReference.child("autonomos").child(bdDoPrestador).child("nota").setValue(notafinal)
                databaseReference.child("autonomos").child(bdDoPrestador).child("avaliacoes").setValue(avaliacoes)
                Toast.makeText(this, "Obrigado pela avaliação", Toast.LENGTH_SHORT).show()
                //armazenar o voto no bd
                // Dismiss the popup window
                travaTela=false
                finish()
            } else {
                Toast.makeText(this, "Vote por favor", Toast.LENGTH_SHORT).show()
            }

        }


    }


    //converte millis para Date em String
    fun ConvertMillistoDateInString (dataArmazenada: Long) :String {

        //primeiro converte o string para Calendar
        var sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.format(dataArmazenada)
        return currentDate

    }


    override fun onBackPressed() {

        if (travaTela){
            //nao deixa sair da tela de votação
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
