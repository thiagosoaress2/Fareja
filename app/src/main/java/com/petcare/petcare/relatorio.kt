package com.petcare.petcare

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToLong


class relatorio : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var mFireBaseStorage: FirebaseStorage

    var userBD: String = "nao"
    var petBD:String = "nao"
    var latLong: Double = 0.0

    lateinit var cidade:String
    var arrayProdutoPreco: MutableList<String> = ArrayList()
    var arrayProdutoQuantidade: MutableList<String> = ArrayList()
    var arrayProdutoNome: MutableList<String> = ArrayList()
    var arrayProdutoPrecoMax: MutableList<String> = ArrayList()
    var arrayProdutoPrecoMin: MutableList<String> = ArrayList()

    var arrayPetsNerby: MutableList<String> = ArrayList()
    var visitas: Int = 0
    var visitasVizinhos: Int = 0

    var VendasVizinhos: String = "00.00"
    var MinhasVendas: String = "00.00"

    var work= 0 // ao final de cada função vai receber +1. Quando atingir o total fecha a tela de loading. Vão ser muitas ações por isto este controle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_relatorio)

        //recupera o email do usuário
        userBD = intent.getStringExtra("userBD")
        petBD = intent.getStringExtra("petBD")

        databaseReference = FirebaseDatabase.getInstance().reference

        val btnFechar: Button = findViewById(R.id.btnFecharActivityRelatorio)
        btnFechar.setOnClickListener {
            finish()
        }

        val btnGerarRelatorio: Button = findViewById(R.id.btnGerarRelatorio)
        btnGerarRelatorio.setOnClickListener {
            ChamaDialog()
            //abreRelatorio()
            gerandoRelatorio()
        }
    }

    fun gerandoRelatorio(){

        //orgem dos acontecimentos:
        //PegarDadosUser() --> ProdutoMaisVendido() --> VendaTotalDoMesAnterioreAtual()
        PegarDadosUser()
        //ProdutoMaisVendido()


    }

    fun PegarDadosUser(){

        val rootRef = databaseReference.child("petshops").child(petBD)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {
                //TODO("Not yet implemented")
                var values: String
                cidade = p0.child("cidade").getValue().toString()

                values = p0.child("latlong").getValue().toString()
                latLong = values.toDouble()

                values = p0.child("visitas").getValue().toString()
                visitas = values.toInt()

                //codigo

                work++
                if (work==4){
                    EncerraDialog()
                }

                ProdutoMaisVendido()
                //VendaTotalDoMesAnterioreAtual()
                getPetsNerby()
                //MinhaVendaDesteMes()
            }

        })



    }


    //PRODUTOS MAIS VENDIDOS, TODOS METODOS AQUI
    //******************************************
    fun ProdutoMaisVendido(){

        var cont=0
        FirebaseDatabase.getInstance().reference.child("produtos").child(cidade).orderByChild("quantidade").limitToFirst(3)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (querySnapshot in dataSnapshot.children) {
                        if (dataSnapshot == null) {
                            //EncerraDialog()
                            //criar usuário
                            //createUser()
                        } else {

                            if (cont<3) {

                                //carregar infos
                                var values: String
                                //pega o endereço de cada iamgem de cada item para apagar a imagem

                                values = querySnapshot.child("quantidade").getValue().toString()
                                val qnt = values.toInt()
                                arrayProdutoQuantidade.add(values)

                                values = querySnapshot.child("precoTotal").getValue().toString()

                                //coloca o preço médio
                                arrayProdutoPreco.add(CalculeEretorneString(values, qnt))

                                values = querySnapshot.child("precoMax").getValue().toString()
                                arrayProdutoPrecoMax.add(values)

                                values = querySnapshot.child("precoMax").getValue().toString()
                                arrayProdutoPrecoMin.add(values)

                                values = querySnapshot.key.toString()
                                arrayProdutoNome.add(values)

                                cont++
                            }

                        }

                        /*
                        work++
                        if (work==4){
                            EncerraDialog()
                        }

                         */
                    }


                    //colocar valores nos textviews
                    var txtNome: TextView = findViewById(R.id.maisvendidosTxtNome)
                    var txtprecoMed: TextView = findViewById(R.id.maisvendidosTxtPrecoMedio)
                    var txtprecoMax: TextView = findViewById(R.id.maisvendidosTxtPrecoMax)
                    var txtprecoMin: TextView = findViewById(R.id.maisvendidosTxtPrecoMin)
                    var txtqnt: TextView = findViewById(R.id.maisvendidosTxtQnt)

                    if (arrayProdutoNome.size!=0){
                        txtNome.setText(arrayProdutoNome.get(0))
                        txtprecoMax.setText("Maior preço"+arrayProdutoPrecoMax.get(0))
                        txtprecoMin.setText("Menor preço"+arrayProdutoPrecoMin.get(0))
                        txtprecoMed.setText("Preço médio"+arrayProdutoPreco.get(0))  //preco é o preço médio
                        txtqnt.setText(arrayProdutoQuantidade.get(0))
                    }

                    if (arrayProdutoNome.size>=2){

                        txtNome = findViewById(R.id.maisvendidosTxtNome2)
                        txtprecoMed  = findViewById(R.id.maisvendidosTxtPrecoMedio2)
                        txtprecoMax  = findViewById(R.id.maisvendidosTxtPrecoMax2)
                        txtprecoMin  = findViewById(R.id.maisvendidosTxtPrecoMin2)
                        var txtqnt: TextView = findViewById(R.id.maisvendidosTxtQnt2)

                        txtNome.setText(arrayProdutoNome.get(1))
                        txtprecoMax.setText("Maior preço"+arrayProdutoPrecoMax.get(1))
                        txtprecoMin.setText("Menor preço"+arrayProdutoPrecoMin.get(1))
                        txtprecoMed.setText("Preço médio"+arrayProdutoPreco.get(1))  //preco é o preço médio
                        txtqnt.setText(arrayProdutoQuantidade.get(1))
                    }

                    if (arrayProdutoNome.size==3){

                        txtNome = findViewById(R.id.maisvendidosTxtNome3)
                        txtprecoMed  = findViewById(R.id.maisvendidosTxtPrecoMedio3)
                        txtprecoMax  = findViewById(R.id.maisvendidosTxtPrecoMax3)
                        txtprecoMin  = findViewById(R.id.maisvendidosTxtPrecoMin3)
                        var txtqnt: TextView = findViewById(R.id.maisvendidosTxtQnt3)

                        txtNome.setText(arrayProdutoNome.get(2))
                        txtprecoMax.setText("Maior preço"+arrayProdutoPrecoMax.get(2))
                        txtprecoMin.setText("Menor preço"+arrayProdutoPrecoMin.get(2))
                        txtprecoMed.setText("Preço médio"+arrayProdutoPreco.get(2))  //preco é o preço médio
                        txtqnt.setText(arrayProdutoQuantidade.get(2))

                    }

                    val layGrafico: ConstraintLayout = findViewById(R.id.layGrafico)
                    //vamos desenhar um gráfico
                    if (arrayProdutoNome.size==0){
                        layGrafico.visibility=View.GONE
                    } else if (arrayProdutoNome.size==1){
                        montaGrafico(arrayProdutoQuantidade.get(0).toInt(), arrayProdutoNome.get(0),0, "nao",  0, "nao", "Produtos mais vendidos")
                    } else if (arrayProdutoNome.size==2){
                        montaGrafico(arrayProdutoQuantidade.get(0).toInt(), arrayProdutoNome.get(0),arrayProdutoQuantidade.get(1).toInt(), arrayProdutoNome.get(1),  2, "nao", "Produtos mais vendidos")
                    } else {
                        montaGrafico(arrayProdutoQuantidade.get(0).toInt(), arrayProdutoNome.get(0),arrayProdutoQuantidade.get(1).toInt(), arrayProdutoNome.get(1),arrayProdutoQuantidade.get(2).toInt(), arrayProdutoNome.get(2), "Produtos mais vendidos")
                    }




                    work++
                    if (work==4){
                        EncerraDialog()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    EncerraDialog()
                    // ...
                }
            })



    }

    fun montaGrafico(val1: Int, label1:String, val2: Int, label2:String, val3: Int, label3: String, titulo: String){

        val bar1: ConstraintLayout = findViewById(R.id.customgraphBar1)
        val bar2: ConstraintLayout = findViewById(R.id.customgraphBar2)
        val bar3: ConstraintLayout = findViewById(R.id.customgraphBar3)

        val label1tv: TextView = findViewById(R.id.label1)
        val label2tv: TextView = findViewById(R.id.label2)
        val label3tv: TextView = findViewById(R.id.label3)

        findViewById<TextView>(R.id.customgraphTitle).setText(titulo)

        if (label1.equals("nao")){
            label1tv.visibility = View.GONE
        } else {
            label1tv.setText(label1)
        }
        if (label2.equals("nao")){
            label2tv.visibility = View.GONE
        } else {
            label2tv.setText(label2)
        }
        if (label3.equals("nao")){
            label3tv.visibility = View.GONE
        } else {
            label3tv.setText(label3)
        }

        //primeiro passo: Descobrir qual é o maior número.
        if (val1 >= val2 && val1 >= val3) {
            //val 1 é o maior. e agora que sabemos isso, vamos colocar ele com 100 dp.

            adjustBarSize(100, bar1) //AJUSTA BARRA 1

            var x: Long
            //calculo da segunda barra a partir da primeira
            if (val2==0){
                adjustBarSize(0, bar2)
            } else {
                x = (val1/val2).toLong() //aqui descobrimos a razão entre os valores
                x = 100/x  //aplicamos a razão nos tamanhos das barras
                adjustBarSize(x, bar2) //ajusta a barra
            }

            //calculo da terceira barra
            if (val3==0){
                adjustBarSize(0, bar3)
            } else {
                x = (val1/val3).toLong()
                x = 100/x
                adjustBarSize(x, bar3)
            }

            //valores 10 e 5
            //10/5 =2
            //tamanho da barra 100/2 = resultado é o tamanho que tem que ficar

        } else if (val2 >= val1 && val2 >= val3) {
            //val 2 é o maior numero
            adjustBarSize(100, bar2) //AJUSTA BARRA 2, que agora esta é a mais alta

            var x: Long

            if (val1==0){
                adjustBarSize(0, bar1)
            } else {
                x = (val2/val1).toLong() //aqui descobrimos a razão entre os valores
                x = 100/x  //aplicamos a razão nos tamanhos das barras
                adjustBarSize(x, bar1) //ajusta a barra
            }

            //calculo da terceira barra
            if (val3==0){
                adjustBarSize(0, bar3)
            } else {
                x = (val2/val1).toLong()
                x = 100/x
                adjustBarSize(x, bar3)
            }

        } else {
            //val 3 é o maiork numero
            adjustBarSize(100, bar3) //AJUSTA BARRA 2, que agora esta é a mais alta

            var x: Long

            if (val1==0){
                adjustBarSize(0, bar1)
            } else {
                x = (val3/val1).toLong() //aqui descobrimos a razão entre os valores
                x = 100/x  //aplicamos a razão nos tamanhos das barras
                adjustBarSize(x, bar1) //ajusta a barra
            }

            //calculo da terceira barra
            if (val2==0){
                adjustBarSize(0, bar2)
            } else {
                x = (val3/val2).toLong()
                x = 100/x
                adjustBarSize(x, bar2)
            }

        }

    }

    fun adjustBarSize (dpsSize: Long, barra: ConstraintLayout){

        if (dpsSize.toInt()==0){

            val lp = barra.getLayoutParams()
            lp.height = 0
            barra.setLayoutParams(lp)

        } else {

            //vamos descobrir quantos pixels tem 100dp neste telefone. Pra isso, a primeira barra está por padrão setada para 100dp.
            //val dps = 100
            val scale: Float = this.getResources().getDisplayMetrics().density
            val pixels = (dpsSize * scale + 0.5f)

            //agora passamos os parametros para a barra 1 ficar com 100dp.
            val lp = barra.getLayoutParams()
            lp.height = pixels.toInt()
            barra.setLayoutParams(lp)
        }

    }

    fun CalculeEretorneString(precoTotal: String, quantidade: Int) :String {

        var precoTotal = precoTotal.toString().replace("R$", "")
        precoTotal = precoTotal.replace(",", ".").trim()
        var precoBigDecimal : BigDecimal = precoTotal.toBigDecimal()
        val quantBigDecimal: BigDecimal = quantidade.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
        precoBigDecimal = precoBigDecimal/quantBigDecimal

        //volta pra string
        precoTotal = precoBigDecimal.toString()
        precoTotal = precoTotal.replace(".", ",")
        precoTotal = "R$"+precoTotal

        return precoTotal
    }
    //PRODUTOS MAIS VENDIDOS, TODOS METODOS AQUI
    //******************************************



    //VENDAS, TODOS METODOS AQUI
    //******************************************
    fun getPetsNerby(){

        var startAtval = latLong-(0.01f*3.0)  //0.4 representa 4km
        var endAtval = latLong+(0.01f*3.0) //3.0 representa 30km

        //getInstance().reference.child("petshops").orderByChild("latlong").startAt(latlong - (0.01f * raio)).endAt(latlong + (0.01f * raio))
        FirebaseDatabase.getInstance().reference.child("petshops").orderByChild("latlong").startAt(startAtval).endAt(endAtval).limitToFirst(5)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.exists()) {
                        for (querySnapshot in dataSnapshot.children) {

                            var values: String
                            //values = querySnapshot.child("nome").value.toString()  //pos0
                            values = querySnapshot.key.toString()

                            if (!values.equals(petBD)){  //se for o próprio pet ele tem que sair da conta entao nao pegamos nada

                                arrayPetsNerby.add(values)

                                if (querySnapshot.child("visitas").exists()){
                                    values = querySnapshot.child("visitas").value.toString()
                                    visitasVizinhos = visitasVizinhos+values.toInt()
                                } else {
                                    visitasVizinhos = visitasVizinhos+0
                                }

                            }

                        }

                        var cont =0
                        while (cont<arrayPetsNerby.size){
                            VendaTotalDoMesAnterioreAtual(cont)
                            cont++
                        }

                    } else {
                        //nao existem petshops proximos. Avisar ao usuario.
                        //esconder dados de comparação
                    }

                    //chama metodos aqui
                    //chamar o método que vai somar e exibir tudo
                    //MontaDadosDasVisitasEvalores()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })

    }

    fun VendaTotalDoMesAnterioreAtual(posicao: Int){

        val data = GetMonthWithYear()
        //vamos pegar a venda deste mês primeiro
        val rootRef = databaseReference.child("vendaCadaPet").child(arrayPetsNerby.get(posicao)).child(data)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {

                for (querySnapshot in p0.children) {

                    //TODO("Not yet implemented")
                    var values: String
                    values = p0.child("valor").getValue().toString()
                    VendasVizinhos = SomeEstesDinheiros(VendasVizinhos,values)


                }

                MinhaVendaDesteMes()
            }

        })

        //VendasVizinhos.toDouble()/arrayPetsNerby.size
        //MinhaVendaDesteMes()

    }

    fun MinhaVendaDesteMes(){

        val data = GetMonthWithYear()
        //vamos pegar a venda deste mês primeiro
        val rootRef = databaseReference.child("vendaCadaPet").child(petBD).child(data)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (querySnapshot in p0.children) {

                    //TODO("Not yet implemented")
                    val txMinhasVendas: TextView = findViewById(R.id.layValoresTxVendaDoMeuPet)

                    var values: String
                    values = p0.child("valor").getValue().toString()

                    MinhasVendas = values

                    //txMinhasVendas.setText("Minhas vendas deste mês: "+values)

                }

                MontaDadosDeEvalores()

            }

        })


    }

    fun MontaDadosDeEvalores(){

        val txMinhasVendas: TextView = findViewById(R.id.layValoresTxVendaDoMeuPet)
        val txVendasMedias: TextView = findViewById(R.id.layValoresTxVendasPetsNerby)


        val mediaVizinhos: String = CalculeAmediaDosVizinhosPls(VendasVizinhos.toString(), arrayPetsNerby.size)
        txMinhasVendas.setText("Minhas vendas este mês: R$"+MinhasVendas)
        txVendasMedias.setText("Média das vendas de \nestabelecimentos próximos: "+mediaVizinhos)

        //montaGraficoValores(MinhasVendas, VendasVizinhos)
        montaGraficoValores(MinhasVendas, mediaVizinhos)
        ExibeDadosDasVisitas()
        abreRelatorio()

        EncerraDialog()
    }

    fun CalculeAmediaDosVizinhosPls(valorVendas: String, quantidade: Int): String{

        var valorvendasProvi = valorVendas.replace("R$", "")
        valorvendasProvi = valorvendasProvi.replace(",", ".")
        var valorVendasBigDecimal : BigDecimal = valorvendasProvi.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
        valorVendasBigDecimal = valorVendasBigDecimal/quantidade.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)

        var str: String = valorVendasBigDecimal.toString()
        str = str.replace(".", ",")
        str = "R$"+str

        return str

    }

    fun montaGraficoValores(val1: String, val2: String){

        val bar1: ConstraintLayout = findViewById(R.id.customgraphBar12)
        val bar2: ConstraintLayout = findViewById(R.id.customgraphBar22)

        val txLabel1: TextView = findViewById(R.id.label12)
        val txLabel2: TextView = findViewById(R.id.label22)
        txLabel1.setText("Minhas vendas")
        txLabel2.setText("Média vizinhança")


        //primeiro passo: Descobrir qual é o maior número.
        if (val1 >= val2) {
            //val 1 é o maior. e agora que sabemos isso, vamos colocar ele com 100 dp.

            adjustBarSize(100, bar1) //AJUSTA BARRA 1

            var x: Long
            val valor2 = passeOvalorDeDinheiroParaNumero(val2)
            val valor1 = passeOvalorDeDinheiroParaNumero(val1)
            //calculo da segunda barra a partir da primeira
            if (valor2<1){
                adjustBarSize(0, bar2)
            } else {
                x = (valor1/valor2).toLong() //aqui descobrimos a razão entre os valores
                x = 100/x  //aplicamos a razão nos tamanhos das barras
                adjustBarSize(x, bar2) //ajusta a barra
            }

        } else {

            adjustBarSize(100, bar2) //AJUSTA BARRA 1

            var x: Long
            val valor2 = passeOvalorDeDinheiroParaNumero(val2)
            val valor1 = passeOvalorDeDinheiroParaNumero(val1)
            //calculo da segunda barra a partir da primeira
            if (valor1 <1){
                adjustBarSize(0, bar1)
            } else {
                x = (valor2/valor1).toDouble().roundToLong() //aqui descobrimos a razão entre os valores

                x = 100/x  //aplicamos a razão nos tamanhos das barras
                adjustBarSize(x, bar1) //ajusta a barra
            }

        }

    }

    //VISITAS DOS PETS E VENDAS, TODOS METODOS AQUI
    //******************************************





    //VISITAS DOS PETS
    //******************************************
    /*
    fun ExibeDadosDasVisitas(){
        val txVisitas: TextView = findViewById(R.id.layVisitastxVisitas)
        val txNumeroVisita: TextView = findViewById(R.id.layVisitastxVisitasNumero)
        //colocar a mensagem

        var vendasVizinhosString = VendasVizinhos.replace("R$", "")
        vendasVizinhosString = vendasVizinhosString.replace(",", ".")
        var vendasVizinhosDouble = vendasVizinhosString.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)

        var VendaVizinhosParaExibir: String
        val sizeBigDecimal = arrayPetsNerby.size.toBigDecimal()
        //VendasVizinhos = (vendasVizinhosDouble/sizeBigDecimal).toString()
        VendaVizinhosParaExibir = (vendasVizinhosDouble/sizeBigDecimal).toString()
        Log.d("teste", "ExibeDadosDasVisitas() vendasVizinhoFinal "+VendasVizinhos)
        VendaVizinhosParaExibir = VendaVizinhosParaExibir.substring(0, 4)
        Log.d("teste", "ExibeDadosDasVisitas() vendasVizinhoFinal apos substring "+VendasVizinhos)


        Log.d("teste", "ultimo passo: O valor final de visitas é "+visitas)
        Log.d("teste", "ultimo passo: O valor final de visitasVizinho é "+visitasVizinhos)
        if (visitas==visitasVizinhos){
            txVisitas.setText("Você está dentro da média de visitas dos estabelecimentos da região")
            txNumeroVisita.visibility = View.GONE
        } else if (visitas> visitasVizinhos){
            val percent = CalculePorcentagemDesteNumero(visitas.toString(), VendaVizinhosParaExibir)
            txVisitas.setText("Você têm "+percent+"% mais visitas do que a média da região.")
            txNumeroVisita.setText(percent.toString()+"%")
        } else {
            val percent = CalculePorcentagemDesteNumero(VendaVizinhosParaExibir, visitas.toString())
            txVisitas.setText("Voce têm "+percent+"% menos visitas do que a média da região.")
            txNumeroVisita.setText(percent.toString()+"%")
        }
    }
     */
    fun ExibeDadosDasVisitas(){
        val txVisitas: TextView = findViewById(R.id.layVisitastxVisitas)
        val txNumeroVisita: TextView = findViewById(R.id.layVisitastxVisitasNumero)
        //colocar a mensagem
        var mediaVisitas = visitasVizinhos/arrayPetsNerby.size

        if (visitas==mediaVisitas){
            txVisitas.setText("Você está dentro da média de visitas dos estabelecimentos da região")
            txNumeroVisita.visibility = View.GONE
        } else if (visitas> mediaVisitas){
            val percent = CalculePorcentagemDesteNumero(visitas, mediaVisitas)
            var percentString = percent.toString()
            percentString = percentString.replace("-", "")
            txVisitas.setText("Você têm "+percentString+"% mais visitas do que a média da região. Parabéns")
            txNumeroVisita.setText(percentString+"%")
        } else {
            //val percent = CalculePorcentagemDesteNumero(visitasVizinhos, visitas)
            val percent = CalculePorcentagemDesteNumero(visitas, mediaVisitas)
            txVisitas.setText("Voce têm "+percent+"% menos visitas do que a média da região.")
            txNumeroVisita.setText(percent.toString()+"%")
        }
    }

    //pega a porcentagem
    private fun CalculePorcentagemDesteNumero(valor: Int, total: Int): Int {

        //val x = valor / total * 100
        val x= ((valor*100)/total).toFloat()
        //descobri a porcentagem das visitas em relação ao total. Agora preciso calcular

        val final = 100-x.toInt()

        //return x.toInt()
        return final
    }


    fun abreRelatorio(){
        findViewById<ConstraintLayout>(R.id.gerarRelat_layRelatorio).visibility = View.VISIBLE
        findViewById<ConstraintLayout>(R.id.gerarRelat_layInicial).visibility = View.GONE
    }

    fun passeOvalorDeDinheiroParaNumero(valorEmString : String): Long{

        var precoEmString = valorEmString.replace("R$", "")
        precoEmString = precoEmString.replace(",", "")
        precoEmString = precoEmString.substring(0, 1)
        var precoBigDec = precoEmString.toLong()
        return precoBigDec

    }

    fun CalculeEstesValoresEmDinheiro(val1:String, val2:String, quantidade:Int) :String{

        var precoMedio = val1.replace("R$", "")
        precoMedio = precoMedio.replace("R$", "")
        var precoaqui = val2.replace("R$", "")
        precoMedio = precoMedio.replace(",", ".")
        var precoMedioBigDec = precoMedio.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
        precoaqui = precoaqui.replace(",", ".")
        var precoAquiBigDec = precoaqui.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
        precoMedioBigDec = precoMedioBigDec+precoAquiBigDec
        precoMedio = precoMedioBigDec.toString()
        precoMedio = precoMedio.replace(".", ",")
        precoMedio = "R$"+precoMedio

        return precoMedio

    }

    fun SomeEstesDinheiros(val1:String, val2:String) :String{

        var dinheiro1 = val1.replace("R$", "")
        dinheiro1 = dinheiro1.replace(",", ".")

        var dinheiro2 = val2.replace("R$", "")
        dinheiro2 = dinheiro2.replace(",", ".")
        var dinheiro1BigDec = dinheiro1.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
        dinheiro2 = dinheiro2.replace(",", ".")
        var dinheiro2BigDec = dinheiro2.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
        dinheiro1BigDec = dinheiro1BigDec+dinheiro2BigDec
        dinheiro1 = dinheiro1BigDec.toString()
        dinheiro1 = dinheiro1.replace(".", ",")
        dinheiro1 = "R$"+dinheiro1

        return dinheiro1

    }

    fun GetMonthWithYear(): String{
        val c: Calendar = GregorianCalendar()
        c.time = Date()
        val sdf = SimpleDateFormat("MMMM YYYY")
        var datafinal: String = sdf.format(c.time)
        datafinal = datafinal.trim()
        return  datafinal
        //fazer a query para ver se ja existe um node com o nome do produto
    }

    fun ChamaDialog() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        ) //este serve para bloquear cliques que pdoeriam dar erros
        val layout = findViewById(R.id.LayoutProgressBar) as ConstraintLayout
        layout.visibility = View.VISIBLE
        val spinner = findViewById(R.id.progressBar1) as ProgressBar
        spinner.visibility = View.VISIBLE
    }

    //este método torna invisivel um layout e encerra o dialogbar spinner.
    fun EncerraDialog() {
        val layout = findViewById(R.id.LayoutProgressBar) as ConstraintLayout
        val spinner = findViewById(R.id.progressBar1) as ProgressBar
        layout.visibility = View.GONE
        spinner.visibility = View.GONE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) //libera os clicks
    }

}
