package com.petcare.petcare

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_minhas_vendas.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class minhasVendas : AppCompatActivity() {

    var userBD: String = ""
    private var petBD = "nao"
    var aberto = false

    var travaTela = false  //trava a tela na avaliação

    val arrayData: MutableList<String> = ArrayList()
    val arrayHora: MutableList<String> = ArrayList()
    val arrayBdDoPet: MutableList<String> = ArrayList()
    val arrayFormaPgto: MutableList<String> = ArrayList()
    val arrayValorCompra: MutableList<String> = ArrayList()
    val arrayStatus: MutableList<String> = ArrayList()
    val arrayBdDaCompra: MutableList<String> = ArrayList()
    val arrayTipo: MutableList<String> = ArrayList()
    val arrayCliente: MutableList<String> = ArrayList()

    val arrayDataFiltrado: MutableList<String> = ArrayList()
    val arrayHoraFiltrado: MutableList<String> = ArrayList()
    val arrayBdDoPetFiltrado: MutableList<String> = ArrayList()
    val arrayFormaPgtoFiltrado: MutableList<String> = ArrayList()
    val arrayValorCompraFiltrado: MutableList<String> = ArrayList()
    val arrayStatusFiltrado: MutableList<String> = ArrayList()
    val arrayBdDaCompraFiltrado: MutableList<String> = ArrayList()
    val arrayTipoFiltrado: MutableList<String> = ArrayList()
    val arrayClienteFiltrado: MutableList<String> = ArrayList()


    val arrayServicosPrestados: MutableList<String> = ArrayList()
    val arrayServicosPrestadosFiltrado: MutableList<String> = ArrayList()

    val arrayProdutos: MutableList<String> = ArrayList()  //este array não é usado no recyclewview, somente pra exibir detalhes da compra

    private lateinit var databaseReference: DatabaseReference

    var notaCliente: Double = 0.0
    var qntAvaliacoes: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minhas_vendas)

        userBD = intent.getStringExtra("userBD")
        petBD = intent.getStringExtra("petBD")
        val tipo: String = intent.getStringExtra("tipo")
        databaseReference = FirebaseDatabase.getInstance().reference

        if (tipo.equals("autonomo")){
            QueryMeusServicosPrestados()
        } else {
            //situacao de petshops
            QueryMinhasVendas()
        }

        val btnVoltar : Button = findViewById(R.id.resumoVendas_btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }

    }

    //busca informações iniciais do usuario
    fun QueryMinhasVendas() {

        CalculaTotal(arrayValorCompra)

        ChamaDialog()

        val hoje = GetDate()
        Log.d("teste", "hoje em string é "+hoje)
        val semanaPassada = GetPastDate(31)
        val hojeMillis = ConvertDateToMillis(hoje)
        Log.d("teste", "hoje em millis é "+hojeMillis)
        val dataLimiteinMillis = ConvertDateToMillis(semanaPassada)


        //ChamaDialog()
        FirebaseDatabase.getInstance().reference.child("compras").orderByChild("petshop").equalTo(petBD).limitToLast(150)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //não existem Vendas
                            EncerraDialog()
                            val tvMsg : TextView = findViewById(R.id.tvExibeServ)
                            tvMsg.setText("Não existem vendas para exibir")

                        } else {

                            /*
                            ******************************************
                            * ********ATENÇÃO**********************
                            * ***************************************
                            * PARA PODER USAR O MESMO LAYOUT E ADAPTER DA PARTE DE
                            * MINHASCOMPRASACTIVITY, AQUI O NOME DO PETSHOP NAO É IMPORTANTE, JÁ QUE SÓ
                            * MOSTRA AS COISAS DO SEU PRÓPRIO PETSHOP. SENDO ASSIM,
                            * O ARRAYFORMADEPAGTO USARA O ARRAYNOME DENTRO DO ADAPTER E O TVNOME NO LAYOUT
                            * *********************************************************
                             */

                            val data = querySnapshot.child("data_compra").value.toString()
                            var longDate: Long = 0

                            if (data.contains("/")){  //se tiver / é pq por algum motivo o user estava usando versão antiga e salvou dd/mm/yyy ao invés de em millis.
                                longDate = ConvertDateToMillis(data)
                            } else {
                                longDate=data.toLong()
                            }

                            if (longDate.toLong()>dataLimiteinMillis) {

                                var values: String
                                values = querySnapshot.child("hora_compra").value.toString()
                                arrayHora.add(values)
                                Log.d("teste", "valor de hora da compra " + arrayHora.get(0))
                                values = querySnapshot.child("data_compra").value.toString()

                                //converte para string para exibir
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
                                values = querySnapshot.child("FormaPgto").value.toString()
                                arrayFormaPgto.add(values)

                                values = querySnapshot.key.toString()
                                arrayBdDaCompra.add(values)

                                values = querySnapshot.child("status").value.toString()
                                arrayStatus.add(values)

                                values = querySnapshot.child("servico").value.toString()
                                arrayTipo.add(values)

                                values = querySnapshot.child("cliente").value.toString()
                                arrayCliente.add(values)

                            }

                            EncerraDialog()

                        }
                    }

                    MontaRecyclerView()

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })
        EncerraDialog()


        val cbServicos: CheckBox = findViewById(R.id.cBServicos)
        cbServicos.setOnClickListener {
            //se checkar faz o filte, senao aperta o click do btnHoje e exibe tudo de hoje normal
            if (cbServicos.isChecked){
                //vamos verificar o que tem nos arrays filtrados. Se eles estiverem vazios, significa que o cliente quer exibir tudo porém filtrado por serviços
                if (arrayBdDaCompraFiltrado.size==0){
                    //se ele está vazio é pq é pra filtrar tudo da tela
                    var posicao =0
                    var cont=0
                    while (cont<arrayTipo.size){
                        if(arrayTipo.get(cont).equals("servicos")){
                            arrayBdDaCompraFiltrado.add(arrayBdDaCompra.get(cont))
                            arrayBdDoPetFiltrado.add(arrayBdDoPet.get(cont))
                            arrayDataFiltrado.add(arrayData.get(cont))
                            arrayHoraFiltrado.add(arrayHora.get(cont))
                            arrayFormaPgtoFiltrado.add(arrayFormaPgto.get(cont))
                            arrayValorCompraFiltrado.add(arrayValorCompra.get(cont))
                            arrayStatusFiltrado.add(arrayStatus.get(cont))
                            arrayTipoFiltrado.add(arrayTipo.get(cont))
                            arrayClienteFiltrado.add(arrayCliente.get(cont))
                        }
                        cont++
                    }

                    MontaRecyclerViewFiltrada()
                    val txTotal: TextView = findViewById(R.id.layBottom_tvTotalVendas)

                } else {
                    //significa que ele quer filtrar o que já está filtrado em 7 dias ou hoje. Então os arrays não estão vazios
                    var posicao =0
                    var cont=0
                    while (cont<arrayTipoFiltrado.size){
                        if(arrayTipoFiltrado.get(cont).equals("servicos")){
                            //se for serviços fica no bd. Se não for vamos apagar no else lá em baixo

                        } else {
                            arrayBdDaCompraFiltrado.removeAt(cont)
                            arrayBdDoPetFiltrado.removeAt(cont)
                            arrayDataFiltrado.removeAt(cont)
                            arrayHoraFiltrado.removeAt(cont)
                            arrayFormaPgtoFiltrado.removeAt(cont)
                            arrayValorCompraFiltrado.removeAt(cont)
                            arrayStatusFiltrado.removeAt(cont)
                            arrayTipoFiltrado.removeAt(cont)
                            arrayClienteFiltrado.removeAt(cont)
                        }
                        cont++
                    }

                    MontaRecyclerViewFiltrada()


                }
            } else { findViewById<Button>(R.id.btnHoje).performClick()}
        }

        val btnMes: Button = findViewById(R.id.btnMes)
        val btnSemana: Button = findViewById(R.id.btnSemana)
        val btnHoje: Button = findViewById(R.id.btnHoje)
        btnHoje.setOnClickListener {

            val tvExibe: TextView = findViewById(R.id.tvExibeServ)
            tvExibe.setText("resumo de hoje")

            btnMes.isEnabled = true
            btnSemana.isEnabled = true
            btnHoje.isEnabled = false

            arrayBdDaCompraFiltrado.clear()
            arrayBdDoPetFiltrado.clear()
            arrayDataFiltrado.clear()
            arrayHoraFiltrado.clear()
            arrayFormaPgtoFiltrado.clear()
            arrayValorCompraFiltrado.clear()
            arrayStatusFiltrado.clear()
            arrayTipoFiltrado.clear()
            arrayClienteFiltrado.clear()

            var posicao =0
            var cont=0

            while (cont<arrayTipo.size){
                //previnir que datas em formato dd/mm/yyyy crashem o app;
                val data = arrayData.get(cont)
                var longDate: Long = 0

                if (data.contains("/")){  //se tiver / é pq por algum motivo o user estava usando versão antiga e salvou dd/mm/yyy ao invés de em millis.
                    longDate = ConvertDateToMillis(data)
                } else {
                    longDate=data.toLong()
                }
                if(hojeMillis==longDate){
                    arrayBdDaCompraFiltrado.add(arrayBdDaCompra.get(cont))
                    arrayBdDoPetFiltrado.add(arrayBdDoPet.get(cont))
                    arrayDataFiltrado.add(arrayData.get(cont))
                    arrayHoraFiltrado.add(arrayHora.get(cont))
                    arrayFormaPgtoFiltrado.add(arrayFormaPgto.get(cont))
                    arrayValorCompraFiltrado.add(arrayValorCompra.get(cont))
                    arrayStatusFiltrado.add(arrayStatus.get(cont))
                    arrayTipoFiltrado.add(arrayTipo.get(cont))
                    arrayClienteFiltrado.add(arrayCliente.get(cont))
                }
                cont++
            }
            MontaRecyclerViewFiltrada()
        }


        btnSemana.setOnClickListener {

            btnMes.isEnabled = true
            btnSemana.isEnabled = false
            btnHoje.isEnabled = true

            arrayBdDaCompraFiltrado.clear()
            arrayBdDoPetFiltrado.clear()
            arrayDataFiltrado.clear()
            arrayHoraFiltrado.clear()
            arrayFormaPgtoFiltrado.clear()
            arrayValorCompraFiltrado.clear()
            arrayStatusFiltrado.clear()
            arrayTipoFiltrado.clear()
            arrayClienteFiltrado.clear()

            val tvExibe: TextView = findViewById(R.id.tvExibeServ)
            tvExibe.setText("últimos 7 dias")

            var posicao =0
            var cont=0
            while (cont<arrayTipo.size){

                //previnir que datas em formato dd/mm/yyyy crashem o app;
                val data = arrayData.get(cont)
                var longDate: Long = 0

                if (data.contains("/")){  //se tiver / é pq por algum motivo o user estava usando versão antiga e salvou dd/mm/yyy ao invés de em millis.
                    longDate = ConvertDateToMillis(data)
                } else {
                    longDate=data.toLong()
                }

                if(longDate > dataLimiteinMillis ){
                    arrayBdDaCompraFiltrado.add(arrayBdDaCompra.get(cont))
                    arrayBdDoPetFiltrado.add(arrayBdDoPet.get(cont))
                    arrayDataFiltrado.add(arrayData.get(cont))
                    arrayHoraFiltrado.add(arrayHora.get(cont))
                    arrayFormaPgtoFiltrado.add(arrayFormaPgto.get(cont))
                    arrayValorCompraFiltrado.add(arrayValorCompra.get(cont))
                    arrayStatusFiltrado.add(arrayStatus.get(cont))
                    arrayTipoFiltrado.add(arrayTipo.get(cont))
                    arrayClienteFiltrado.add(arrayCliente.get(cont))
                }
                cont++
            }
            MontaRecyclerViewFiltrada()
        }


        btnMes.setOnClickListener {

            btnMes.isEnabled = false
            btnSemana.isEnabled = true
            btnHoje.isEnabled = true

            val tvExibe: TextView = findViewById(R.id.tvExibeServ)
            tvExibe.setText("vendas do mês")

            var posicao =0
            var cont=0

            arrayBdDaCompraFiltrado.clear()
            arrayBdDoPetFiltrado.clear()
            arrayDataFiltrado.clear()
            arrayHoraFiltrado.clear()
            arrayFormaPgtoFiltrado.clear()
            arrayValorCompraFiltrado.clear()
            arrayStatusFiltrado.clear()
            arrayTipoFiltrado.clear()
            arrayClienteFiltrado.clear()

            MontaRecyclerView()

        }


    }

    fun MontaRecyclerView (){



        val recyclerFiltrada: RecyclerView = findViewById(R.id.RecyclerViewFiltrada)
        recyclerFiltrada.visibility = View.GONE

        val recylerInicial: RecyclerView = findViewById(R.id.resumoVendas_recyclerView)
        recylerInicial.visibility = View.VISIBLE


        val adapter2 = recyclerFiltrada.adapter
        recyclerFiltrada.swapAdapter(adapter2, true)


        //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
        var adapter: MinhasVendasRecyclerView = MinhasVendasRecyclerView(this, arrayFormaPgto, arrayData, arrayHora, arrayValorCompra, arrayBdDoPet, arrayStatus, arrayBdDaCompra, arrayTipo, arrayCliente)

//chame a recyclerview
        var recyclerView: RecyclerView = findViewById(R.id.resumoVendas_recyclerView)

//define o tipo de layout (linerr, grid)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)

//coloca o adapter na recycleview
        recyclerView.adapter = adapter

        recyclerView.layoutManager = linearLayoutManager

// Notify the adapter for data change.
        adapter.notifyDataSetChanged()


        //click da recycleview
        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView.addOnItemTouchListener(
            minhasComprasActivity.RecyclerTouchListener(
                this,
                recyclerView!!,
                object : minhasComprasActivity.ClickListener {

                    override fun onClick(view: View, position: Int) {
                        //Log.d("teste", aNome.get(position))
                        //Toast.makeText(this@MainActivity, !! aNome.get(position).toString(), Toast.LENGTH_SHORT).show()
                        //openPopUp("Finalização de compra", "Você confirma o recebimento do produto?", true, "Sim, confirmo", "Não", "rating", arrayBdDoPet.get(position), position, arrayBdDaCompra.get(position))

                        if (aberto==false){ //só abre se for false
                            aberto=true
                            if (arrayStatus.get(position).equals("encerrado")){
                                openPopUpEspecial(arrayBdDoPet.get(position),position, arrayBdDaCompra.get(position), arrayStatus.get(position), false, false)
                            } else if (arrayStatus.get(position).equals("vendedor confirma entrega")){
                                openPopUpEspecial(arrayBdDoPet.get(position),position, arrayBdDaCompra.get(position), arrayStatus.get(position), false, true)
                            } else {
                                openPopUpEspecial(arrayBdDoPet.get(position),position, arrayBdDaCompra.get(position), arrayStatus.get(position), true, true)
                            }
                        }




                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                }))




        val txTotal: TextView = findViewById(R.id.layBottom_tvTotalVendas)
        val total = CalculaTotal(arrayValorCompra)
        txTotal.setText(currencyTranslation(total))

    }

    fun CalculaTotal(array: MutableList<String>) : String{

        var cont=0
        var total: String = "R$0,00"
        while (cont<array.size){
            total = SomeEstesDoisDinheiros(total, array.get(cont).toString())
            cont++
        }

        return  total
    }

    fun MontaRecyclerViewFiltrada (){

        val recylerInicial: RecyclerView = findViewById(R.id.resumoVendas_recyclerView)
        recylerInicial.visibility = View.GONE

        val recyclerFiltrada: RecyclerView = findViewById(R.id.RecyclerViewFiltrada)
        recyclerFiltrada.visibility = View.VISIBLE
        //val recyclerOld: RecyclerView = findViewById(R.id.resumoVendas_recyclerView)
       // val adapterprovi = recyclerOld.adapter
        //adapterprovi?.notifyDataSetChanged()


        //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
        var adapter2: MinhasVendasRecyclerView = MinhasVendasRecyclerView(this, arrayFormaPgtoFiltrado, arrayDataFiltrado, arrayHoraFiltrado, arrayValorCompraFiltrado, arrayBdDoPetFiltrado, arrayStatusFiltrado, arrayBdDaCompraFiltrado, arrayTipoFiltrado, arrayClienteFiltrado)

        //trocando o adaptador para este novo
        recyclerFiltrada.swapAdapter(adapter2,true)

        //var recyclerView: RecyclerView = findViewById(R.id.resumoVendas_recyclerView)

        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)

        recyclerFiltrada.adapter = adapter2

        recyclerFiltrada.layoutManager = linearLayoutManager

        // Notify the adapter for data change.
        adapter2.notifyDataSetChanged()


        //click da recycleview
        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerFiltrada.addOnItemTouchListener(
            minhasComprasActivity.RecyclerTouchListener(
                this,
                recyclerFiltrada!!,
                object : minhasComprasActivity.ClickListener {

                    override fun onClick(view: View, position: Int) {

                        if (aberto==false){

                            if (arrayStatus.get(position).equals("encerrado")){
                                openPopUpEspecial(arrayBdDoPet.get(position),position, arrayBdDaCompra.get(position), arrayStatus.get(position), false, false)
                            } else if (arrayStatus.get(position).equals("vendedor confirma entrega")){
                                openPopUpEspecial(arrayBdDoPet.get(position),position, arrayBdDaCompra.get(position), arrayStatus.get(position), false, true)
                            } else {
                                openPopUpEspecial(arrayBdDoPet.get(position),position, arrayBdDaCompra.get(position), arrayStatus.get(position), true, true)
                            }
                        }


                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                }))


        val txTotal: TextView = findViewById(R.id.layBottom_tvTotalVendas)
        val total = CalculaTotal(arrayValorCompra)
        txTotal.setText(currencyTranslation(total))

    }

    //click listener da primeira recycleview
    interface ClickListener {
        fun onClick(view: View, position: Int)

        fun onLongClick(view: View?, position: Int)
    }

    internal class RecyclerTouchListener(context: Context, recyclerView: RecyclerView, private val clickListener: minhasComprasActivity.ClickListener?) : RecyclerView.OnItemTouchListener {

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

    //especial para tratar das reclamações e recebimento
    fun openPopUpEspecial (bdDoPet: String, position: Int, bdDaCompra:String, status: String, ExibeBtnRecebimento: Boolean, ExibebtnReclamacao: Boolean) {
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

        btnRecebi.setText("Confirmar entrega")

        if (!ExibeBtnRecebimento){
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
            aberto=false
        }

        btnRecebi.setOnClickListener {

            if (status.equals("cliente confirma recebimento")) { //nesta opção ele ja confirmouq ue recebeu e entrou aqui novamente

                //agora nesta situação vai abrir a tela de avaliação enquanto ocorre o salvamento no bd.
                //openAvaliationSystem()
                queryUserNota(position)

                databaseReference.child("compras").child(bdDaCompra).child("status").setValue("encerrado")
                Toast.makeText(this, "Tudo certo. Esta transação foi encerrada.", Toast.LENGTH_LONG).show()
                arrayBdDaCompra.removeAt(position)
                arrayStatus.removeAt(position)
                arrayFormaPgto.removeAt(position)
                arrayValorCompra.removeAt(position)
                arrayBdDoPet.removeAt(position)
                arrayData.removeAt(position)
                arrayHora.removeAt(position)

            } else if (status.equals("vendedor confirma entrega")){ //neste caso o proprio vendedor ja deu como finalizada, mas o cliente ainda não. Ou seja, ele está clicando aqui novamente.
                Toast.makeText(this, "Estamos esperando o cliente confirmar a entrega para encerrar a transação.", Toast.LENGTH_LONG).show()

            } else {
                databaseReference.child("compras").child(bdDaCompra).child("status").setValue("vendedor confirma entrega")
                Toast.makeText(this, "A entrega foi registrada. Aguardando o cliente confirmar o recebimento para encerrar no negócio", Toast.LENGTH_LONG).show()
                arrayStatus.set(position, "vendedor confirma entrega")
            }
            aberto=false
            popupWindow.dismiss()
        }

        val btnDetalhes: Button = view.findViewById(R.id.btnDetalhes)
        //abre os detalhes da compra
        btnDetalhes.setOnClickListener {
            var recyclerInicialAberta = false //vai registrar qual layout estava aberto na hora
            popupWindow.dismiss()
            aberto=false
            //ChamaDialog
            val layDetalhes: ConstraintLayout = findViewById(R.id.layDetalhesVenda)
            layDetalhes.visibility = View.VISIBLE
            val btnVoltar: Button = findViewById(R.id.detalhes_btnVoltar)
            QueryDetalhamentoDasVendas(bdDaCompra)


            val recylerInicial: RecyclerView = findViewById(R.id.resumoVendas_recyclerView)
            if (recylerInicial.isVisible){
                recyclerInicialAberta = true
            } else {
                recyclerInicialAberta = false
            }

            recylerInicial.visibility = View.GONE

            val recyclerFiltrada: RecyclerView = findViewById(R.id.RecyclerViewFiltrada)
            recyclerFiltrada.visibility = View.GONE

            btnVoltar.setOnClickListener {
                layDetalhes.visibility = View.GONE
                if (recyclerInicialAberta==true){
                    recylerInicial.visibility = View.VISIBLE

                } else {
                    recyclerFiltrada.visibility = View.VISIBLE
                }
            }

        }

        btnReclamacao.setOnClickListener {

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
                tipoProblema = "Problema na entrega"
                etDescreveProblema.visibility = View.VISIBLE
                etDescreveProblema.requestFocus()
            }

            btn = findViewById(R.id.reclamacao_btnPagamento)
            btn.setOnClickListener {
                tipoProblema = "Problema no pagamento"
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
                    val newCad: DatabaseReference = databaseReference.child("reclamacoes_por_parte_dos_pets").push()
                    newCad.child("problema").setValue(tipoProblema)
                    newCad.child("descricao").setValue(etDescreveProblema.text.toString())
                    newCad.child("petshop").setValue(bdDoPet)
                    newCad.child("compra_com_problema").setValue(bdDaCompra)
                    newCad.child("bdCliente").setValue(userBD)
                    Toast.makeText(this, "Sua reclamação foi registrada. Em caso de recorrência este usuário será excluído.", Toast.LENGTH_LONG).show()
                    layReclamacao.visibility = View.GONE
                }
            }

            aberto=false
            popupWindow.dismiss()
        }


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            aberto=false
            popupWindow.dismiss()
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.layResumoVendas)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }

    //nota que o dono da loja da pro usuario
    fun queryUserNota(posicao: Int){

        ChamaDialog()
        var bdToQuery: String
        if (arrayClienteFiltrado.size==0){
            bdToQuery = arrayCliente.get(posicao)
        } else{
            bdToQuery = arrayClienteFiltrado.get(posicao)
        }

        val rootRef = databaseReference.child("usuarios").child(bdToQuery)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")
                var values: String
                values = p0.child("nota").getValue().toString()
                notaCliente = values.toDouble()
                values = p0.child("avaliacoes").getValue().toString()
                qntAvaliacoes = values.toDouble()

                EncerraDialog()
                AbreAvaliacao(bdToQuery)

            }
        })
    }

    /*
    fun openAvaliationSystem(bdCliente: String){

        FirebaseDatabase.getInstance().reference.child("compras").orderByChild("petshop").equalTo(petBD)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //não existem Vendas
                        } else {

                            var values: String
                            values = querySnapshot.child("hora_compra").value.toString()
                            arrayHora.add(values)

                        }
                    }

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })


    }
     */

    //busca informações detalhadas da venda
    fun QueryDetalhamentoDasVendas(bdDaCompra: String) {

        QueryProdutosVendidos(bdDaCompra)

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
    fun QueryProdutosVendidos(bdDaCompra: String) {

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

    fun AbreAvaliacao (bdCliente: String){

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
                var notafinal: Double = (notaCliente.toDouble()+votou.toDouble()).toDouble()
                notafinal= notafinal/avaliacoes

                databaseReference.child("usuarios").child(bdCliente).child("nota").setValue(notafinal)
                databaseReference.child("usuarios").child(bdCliente).child("avaliacoes").setValue(avaliacoes)
                Toast.makeText(this, "Obrigado pela avaliação", Toast.LENGTH_SHORT).show()
                //armazenar o voto no bd
                // Dismiss the popup window
                finish()
            } else {
                Toast.makeText(this, "Vote por favor", Toast.LENGTH_SHORT).show()
            }

        }


    }

    private fun GetPastDate (daysToAdd: Int) : String {

        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.format(Date())

        val c = Calendar.getInstance()
        c.time = sdf.parse(currentDate)
        c.add(Calendar.DATE, -daysToAdd) // number of days to add

        var tomorrow: String = sdf.format(Date())
        tomorrow = sdf.format(c.time) // dt is now the new date

        return tomorrow

    }

    //pega  a data
    private fun GetDate () : String {

        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.format(Date())

        return currentDate
    }

    //converte data para millis
    fun ConvertDateToMillis (dataArmazenada: String) :Long {

        //primeiro converte o string para Calendar
        var sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.parse(dataArmazenada)
        val calendarDate: Calendar = sdf.calendar

        return calendarDate.timeInMillis
    }

    //converte millis para Date em String
    fun ConvertMillistoDateInString (dataArmazenada: Long) :String {

        //primeiro converte o string para Calendar
        var sdf = SimpleDateFormat("dd/MM/yyyy")
        val currentDate = sdf.format(dataArmazenada)
        return currentDate

    }









    //METODOS DE PRESTACOES DE SERVICOS
    fun QueryMeusServicosPrestados(){

        val delimiter = "!?!%"

        ChamaDialog()

        val hoje = GetDate()
        val semanaPassada = GetPastDate(31)
        val hojeMillis = ConvertDateToMillis(hoje)
        val dataLimiteinMillis = ConvertDateToMillis(semanaPassada)

        //ChamaDialog()
        FirebaseDatabase.getInstance().reference.child("servicos").child(userBD).orderByChild("controle").equalTo("controle").limitToLast(150)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {

                        if (dataSnapshot == null) {
                            //não existem Vendas
                        } else {

                            /*
                            ******************************************
                            * ********ATENÇÃO**********************
                            * ***************************************
                            * PARA PODER USAR O MESMO LAYOUT E ADAPTER DA PARTE DE
                            * MINHASCOMPRASACTIVITY, AQUI O NOME DO PETSHOP NAO É IMPORTANTE, JÁ QUE SÓ
                            * MOSTRA AS COISAS DO SEU PRÓPRIO PETSHOP. SENDO ASSIM,
                            * O ARRAYFORMADEPAGTO USARA O ARRAYNOME DENTRO DO ADAPTER E O TVNOME NO LAYOUT
                            * *********************************************************
                             */

                            val data = querySnapshot.child("data").value.toString()
                            var longDate: Long = 0

                            if (data.contains("/")){  //se tiver / é pq por algum motivo o user estava usando versão antiga e salvou dd/mm/yyy ao invés de em millis.
                                longDate = ConvertDateToMillis(data)
                            } else {
                                longDate=data.toLong()
                            }

                            if (longDate.toLong()>dataLimiteinMillis) {


                                var value = "nao"
                                var textContainer = data+delimiter

                                //var values: String
                                value = querySnapshot.child("hora").value.toString()
                                textContainer = textContainer+value+delimiter

                                value = querySnapshot.child("status").value.toString()
                                textContainer = textContainer+value+delimiter

                                value = querySnapshot.child("apelido").value.toString()
                                textContainer = textContainer+value+delimiter

                                value = querySnapshot.child("servico").value.toString()
                                textContainer = textContainer+value+delimiter

                                value = querySnapshot.child("cliente").value.toString()
                                textContainer = textContainer+value+delimiter

                                value = querySnapshot.key.toString()
                                textContainer = textContainer+value+delimiter


                                arrayServicosPrestados.add(textContainer)

                                /*  arrayServicoPrestado
                                    pos 0 - data
                                    pos 1 - hora
                                    pos 2 - status
                                    pos 3 - apelido
                                    pos 4 - servico
                                    pos 5 - cliente bd
                                    pos 6 - bd do servico
                                 */

                            }


                        }
                    }

                    EncerraDialog()
                    MontaRecyclerViewServicosPrestados()

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    maketoast("Ocorreu um erro")
                    EncerraDialog()
                    finish()
                    // ...
                }
            })


        val cbServicos: CheckBox = findViewById(R.id.cBServicos)
        cbServicos.visibility=View.INVISIBLE

        val btnMes: Button = findViewById(R.id.btnMes)
        val btnSemana: Button = findViewById(R.id.btnSemana)
        val btnHoje: Button = findViewById(R.id.btnHoje)
        btnHoje.setOnClickListener {

            val tvExibe: TextView = findViewById(R.id.tvExibeServ)
            tvExibe.setText("resumo de hoje")

            btnMes.isEnabled = true
            btnSemana.isEnabled = true
            btnHoje.isEnabled = false

            arrayServicosPrestadosFiltrado.clear()

            var posicao =0
            var cont=0

            while (cont<arrayServicosPrestados.size){
                //previnir que datas em formato dd/mm/yyyy crashem o app;
                val tokens = StringTokenizer(arrayServicosPrestados.get(cont).toString(), delimiter)
                val data: String = tokens.nextToken() // this will contain "Fruit"
                //val bdDoUser = tokens.nextToken() // this will contain " they

                //val data = arrayData.get(cont)
                var longDate: Long = 0

                if (data.contains("/")){  //se tiver / é pq por algum motivo o user estava usando versão antiga e salvou dd/mm/yyy ao invés de em millis.
                    longDate = ConvertDateToMillis(data)
                } else {
                    longDate=data.toLong()
                }
                if(hojeMillis==longDate){
                    arrayServicosPrestadosFiltrado.add(arrayServicosPrestados.get(cont))
                }
                cont++

                /*  arrayServicoPrestado
                    pos 0 - data
                    pos 1 - hora
                    pos 2 - status
                    pos 3 - apelido
                    pos 4 - servico
                    pos 5 - cliente bd
                    pos 6 - bd do servico
                 */
            }
            MontaRecyclerViewFiltradaDoServicoPrestado()
        }


        btnSemana.setOnClickListener {

            btnMes.isEnabled = true
            btnSemana.isEnabled = false
            btnHoje.isEnabled = true

            arrayServicosPrestadosFiltrado.clear()

            val tvExibe: TextView = findViewById(R.id.tvExibeServ)
            tvExibe.setText("últimos 7 dias")

            var posicao =0
            var cont=0

                while (cont<arrayServicosPrestados.size){

                    val tokens = StringTokenizer(arrayServicosPrestados.get(cont).toString(), delimiter)
                    val data: String = tokens.nextToken() // this will contain "Fruit"

                    var longDate: Long = 0

                    if (data.contains("/")){  //se tiver / é pq por algum motivo o user estava usando versão antiga e salvou dd/mm/yyy ao invés de em millis.
                        longDate = ConvertDateToMillis(data)
                    } else {
                        longDate=data.toLong()
                    }

                    if(longDate > dataLimiteinMillis ){
                        arrayServicosPrestadosFiltrado.add(arrayServicosPrestados.get(cont))

                    }
                    cont++
            }
            MontaRecyclerViewFiltradaDoServicoPrestado()
        }


        btnMes.setOnClickListener {

            btnMes.isEnabled = false
            btnSemana.isEnabled = true
            btnHoje.isEnabled = true

            val tvExibe: TextView = findViewById(R.id.tvExibeServ)
            tvExibe.setText("vendas do mês")

            var posicao =0
            var cont=0

            arrayServicosPrestadosFiltrado.clear()

            MontaRecyclerViewServicosPrestados()

        }


    }

    fun MontaRecyclerViewServicosPrestados(){


        //vamos utilizar os mesmos layouts e recyclerviews do método padrão. Só trocamos aqui o adapter pois utilizamos só um array para fazer tudo.

        val recyclerFiltrada: RecyclerView = findViewById(R.id.RecyclerViewFiltrada)
        recyclerFiltrada.visibility = View.GONE

        val recylerInicial: RecyclerView = findViewById(R.id.resumoVendas_recyclerView)
        recylerInicial.visibility = View.VISIBLE


        val adapter2 = recyclerFiltrada.adapter
        recyclerFiltrada.swapAdapter(adapter2, true)


        //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
        var adapter: MeusServicosPrestadosRecyclerAdapter = MeusServicosPrestadosRecyclerAdapter(this, arrayServicosPrestados)

//chame a recyclerview
        var recyclerView: RecyclerView = findViewById(R.id.resumoVendas_recyclerView)

//define o tipo de layout (linerr, grid)
        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)

//coloca o adapter na recycleview
        recyclerView.adapter = adapter

        recyclerView.layoutManager = linearLayoutManager

// Notify the adapter for data change.
        adapter.notifyDataSetChanged()


        //click da recycleview
        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerView.addOnItemTouchListener(
            minhasComprasActivity.RecyclerTouchListener(
                this,
                recyclerView!!,
                object : minhasComprasActivity.ClickListener {

                    override fun onClick(view: View, position: Int) {
                        //Log.d("teste", aNome.get(position))
                        //Toast.makeText(this@MainActivity, !! aNome.get(position).toString(), Toast.LENGTH_SHORT).show()
                        //openPopUp("Finalização de compra", "Você confirma o recebimento do produto?", true, "Sim, confirmo", "Não", "rating", arrayBdDoPet.get(position), position, arrayBdDaCompra.get(position))

                        if (aberto==false){ //só abre se for false
                            aberto=true

                            /*  arrayServicoPrestado
                                pos 0 - data
                                pos 1 - hora
                                pos 2 - status
                                pos 3 - apelido
                                pos 4 - servico
                                pos 5 - cliente bd
                                pos 6 - bd do servico
                             */

                            val tokens = StringTokenizer(arrayServicosPrestados.get(position), "!?!%")
                            val data = tokens.nextToken() // this will contain "img link or nao"
                            val hora = tokens.nextToken()
                            val status = tokens.nextToken()
                            val apelido: String = tokens.nextToken()
                            val servico: String = tokens.nextToken()
                            val bdDoCliente = tokens.nextToken()  //bd do user
                            val bdDoServico = tokens.nextToken()  //bd do user



                            if (status.equals("encerrado")){
                                openPopUpServicoPrestado(userBD ,position, bdDoServico, status, false)
                            } else if (status.equals("Prestador confirma serviço feito")) {
                                openPopUpServicoPrestado(
                                    bdDoServico,
                                    position,
                                    userBD,
                                    status,
                                    false
                                )

                            } else if (status.equals("O serviço foi cancelado.")){
                                maketoast("Serviço foi cancelado.")
                            } else {
                                //falta o usuario opinar
                                openPopUpServicoPrestado(userBD ,position, bdDoServico, status, true)
                            }
                        }




                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                }))



    }

    fun MontaRecyclerViewFiltradaDoServicoPrestado (){

        //vamos utilizar os mesmos layouts e recyclerviews do método padrão. Só trocamos aqui o adapter pois utilizamos só um array para fazer tudo.

        val recylerInicial: RecyclerView = findViewById(R.id.resumoVendas_recyclerView)
        recylerInicial.visibility = View.GONE

        val recyclerFiltrada: RecyclerView = findViewById(R.id.RecyclerViewFiltrada)
        recyclerFiltrada.visibility = View.VISIBLE


        //chame aqui pelo adaptador que criamos, com o nome dado e o construtor
        var adapter2: MeusServicosPrestadosRecyclerAdapter = MeusServicosPrestadosRecyclerAdapter(this, arrayServicosPrestadosFiltrado)

        //trocando o adaptador para este novo
        recyclerFiltrada.swapAdapter(adapter2,true)

        //var recyclerView: RecyclerView = findViewById(R.id.resumoVendas_recyclerView)

        var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)

        recyclerFiltrada.adapter = adapter2

        recyclerFiltrada.layoutManager = linearLayoutManager

        // Notify the adapter for data change.
        adapter2.notifyDataSetChanged()


        //click da recycleview
        //constructor: context, nomedarecycleview, object:ClickListener
        recyclerFiltrada.addOnItemTouchListener(
            minhasComprasActivity.RecyclerTouchListener(
                this,
                recyclerFiltrada!!,
                object : minhasComprasActivity.ClickListener {

                    override fun onClick(view: View, position: Int) {

                        if (aberto==false){
                            aberto=true

                            /*  arrayServicoPrestado
                                pos 0 - data
                                pos 1 - hora
                                pos 2 - status
                                pos 3 - apelido
                                pos 4 - servico
                                pos 5 - cliente bd
                                pos 6 - bd do servico
                             */

                            val tokens = StringTokenizer(arrayServicosPrestadosFiltrado.get(position), "!?!%")
                            val data = tokens.nextToken() // this will contain "img link or nao"
                            val hora = tokens.nextToken()
                            val status = tokens.nextToken()
                            val apelido = tokens.nextToken()
                            val servico = tokens.nextToken()
                            val bdDoCliente = tokens.nextToken()  //bd do user
                            val bdDoServico = tokens.nextToken()  //bd do user

                            if (status.get(position).equals("encerrado")){
                                openPopUpServicoPrestado(userBD ,position, bdDoServico, status, false)
                            } else if (arrayStatus.get(position).equals("Prestador confirma serviço feito")){
                                openPopUpServicoPrestado(userBD ,position, bdDoServico, status, false)
                            } else {
                                openPopUpServicoPrestado(userBD ,position, bdDoServico, status, true)
                            }
                        }


                    }

                    override fun onLongClick(view: View?, position: Int) {

                    }
                }))

    }

    //especial para tratar das reclamações e recebimento
    //
    fun openPopUpServicoPrestado (bduser: String, position: Int, bddoServico:String, status: String, ExibeBtnRecebimento: Boolean) {
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
        val btnCancelarServ = view.findViewById<Button>(R.id.btnReclamacao) //btnReclamação vai ser o botão de cancelar o ticket pois pode ser que após o contato do eles não fecharem o serviço. Aí cancelam
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

            if (status.equals("cliente confirma serviço prestado")) { //nesta opção ele ja confirmouq ue recebeu e entrou aqui novamente

                //agora nesta situação vai abrir a tela de avaliação enquanto ocorre o salvamento no bd.
                //openAvaliationSystem()
                queryUserNotaParaServico(bduser)

                databaseReference.child("servicos").child(bduser).child(bddoServico).child("status").setValue("encerrado")
                Toast.makeText(this, "Tudo certo. Esta transação foi encerrada.", Toast.LENGTH_LONG).show()

                val delim="!?!%"
                val tokens = StringTokenizer(arrayServicosPrestados.get(position), "!?!%")
                val data = tokens.nextToken() // this will contain "img link or nao"
                val hora = tokens.nextToken()
                val status = tokens.nextToken()
                val apelido = tokens.nextToken()
                val servico = tokens.nextToken()
                val bdDoCliente = tokens.nextToken()  //bd do user
                val bdDoServico = tokens.nextToken()  //bd do user

                arrayServicosPrestados.set(position, data+delim+hora+delim+"Prestador confirma serviço feito"+delim+apelido+delim+servico+delim+bdDoCliente+delim+bddoServico)

            } else if (status.equals("Prestador confirma serviço feito")){ //neste caso o proprio vendedor ja deu como finalizada, mas o cliente ainda não. Ou seja, ele está clicando aqui novamente.
                Toast.makeText(this, "Estamos esperando o cliente confirmar o serviço para encerrar a transação.", Toast.LENGTH_LONG).show()

            } else {
                databaseReference.child("servicos").child(bduser).child(bddoServico).child("status").setValue("Prestador confirma serviço feito")
                Toast.makeText(this, "Serviço registrado. Aguardando o cliente confirmar a prestação do serviço para encerrar no negócio", Toast.LENGTH_LONG).show()

                val delim="!?!%"
                val tokens = StringTokenizer(arrayServicosPrestados.get(position), "!?!%")
                val data = tokens.nextToken() // this will contain "img link or nao"
                val hora = tokens.nextToken()
                val status = tokens.nextToken()
                val apelido = tokens.nextToken()
                val servico = tokens.nextToken()
                val bdDoCliente = tokens.nextToken()  //bd do user
                val bdDoServico = tokens.nextToken()  //bd do user

                arrayServicosPrestados.set(position, data+delim+hora+delim+"Prestador confirma serviço feito"+delim+apelido+delim+servico+delim+bdDoCliente+delim+bddoServico)
            }
            aberto=false
            popupWindow.dismiss()
        }

        btnCancelarServ.setText("Serviço cancelado")
        btnCancelarServ.setOnClickListener {
            databaseReference.child("servicos").child(bduser).child(bddoServico).child("status").setValue("O serviço foi cancelado.")
            Toast.makeText(this, "Serviço cancelado. Aguardando o cliente confirmar encerrar este negocio", Toast.LENGTH_LONG).show()

            val delim = "!?!%"
            val tokens = StringTokenizer(arrayServicosPrestados.get(position), delim)
            val data = tokens.nextToken() // this will contain "img link or nao"
            val hora = tokens.nextToken()
            val status = tokens.nextToken()
            val apelido = tokens.nextToken()
            val servico = tokens.nextToken()
            val bd = tokens.nextToken()

            arrayServicosPrestados.set(position, data+delim+hora+delim+"Você cancelou este serviço"+bd)

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
        val lay_root: ConstraintLayout = findViewById(R.id.layResumoVendas)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }

    fun queryUserNotaParaServico(bdDoUser: String){

        ChamaDialog()

        val rootRef = databaseReference.child("usuarios").child(bdDoUser)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")
                var values: String
                values = p0.child("nota").getValue().toString()
                notaCliente = values.toDouble()
                values = p0.child("avaliacoes").getValue().toString()
                qntAvaliacoes = values.toDouble()

                EncerraDialog()
                AbreAvaliacaoDoServico(bdDoUser)

            }
        })
    }

    fun AbreAvaliacaoDoServico (bdCliente: String){

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
                var notafinal: Double = (notaCliente.toDouble()+votou.toDouble()).toDouble()
                notafinal= notafinal/avaliacoes

                databaseReference.child("usuarios").child(bdCliente).child("nota").setValue(notafinal)
                databaseReference.child("usuarios").child(bdCliente).child("avaliacoes").setValue(avaliacoes)
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


    //este transforma numero comum em dinheiro
    fun currencyTranslation(valorOriginal: String): String{

        //passar o valor para string para poder ver o tamanho
        var valorString = valorOriginal.toString()
        valorString = valorString.trim()
        valorString = valorString.replace("R$", "")
        valorString = valorString.replace(".", "")
        valorString = valorString.replace(",", "")

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

    //este transforma numero comum em dinheiro
    fun SomeEstesDoisDinheiros(val1: String, val2: String): String{

        var str:String = val1.replace("R$", "")
        str = str.replace(",", "").trim()
        str = str.replace(".", "").trim()

        var str2:String = val2.replace("R$", "")
        str2 = str2.replace(",", "").trim()
        str2 = str2.replace(".", "").trim()

        val totalizando = (str).toInt()+(str2).toInt()
        //tvTotalCompra.text = "O valor total da sua compra é: "+currencyTranslation( totalizando.toString()
        return totalizando.toString()


    }

    override fun onBackPressed() {

        if (travaTela){
            //nao deixa sair da tela de votação
        }
    }

    fun maketoast(mensagem: String){
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

}

