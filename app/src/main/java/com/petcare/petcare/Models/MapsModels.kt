package com.petcare.petcare.Models

import android.Manifest
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Location
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.GoogleMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.petcare.petcare.Controller.MapsController
import com.petcare.petcare.Utils.mySharedPrefs

object MapsModels {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    private lateinit var mMap: GoogleMap
    private var getLocal: Boolean = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    var raioBusca = 5.0 //var raioBusca  = 0.3 //marca o raio da busca dos pets  0.1 = 1km no mapa              obs: Mudamos para 10 km
    var raioUser = 7000 //obs: A busca está pegando endereços de um raio um pouco maior do que o desenhado. Vou aumentar o raio desenhado para nao apareceer o erro pro usuario               //var raioUser = 3000 //marca o circulo da distancia que foi buscada pelo user. 1000 = 1km no mapa    obs: Mudamos de 3000 para 10000 (10km)
    val dif = -0.07576889999999992 //diferença a ser adicona em startAtVal

    var userMail = "nao"
    var userBD = "nao"
    var alvara = "nao"
    var tipo = "usuario"
    var petBDseForEmpresario = "nao"
    var bdDoPet = "nao"
    var plano = "nao"
    var imgDoUser = "nao"

    var bdDoImpulsionamento = "nao"

    var tipoProdParaImpulso = "nao"

    var popupAberta = false

    var posicao: Int = 0 //usada no botão de carrinho que acompanha a tela na loja

    //anuncios
    var contadorAnuncio=0
    var checkAds = 0  //para controlar se já verificou o anuncio do local. Quando for pela localização isnerida ele vai verificar novamente, pois pode ser outro lugar

    var autonomoPlanoPremium: Boolean = false

    var liberaServico = false //vou verificar na query inicial. Se o user já tiver comprado algo alguma vez (saberemos isso pela avaliação) vailiberar o serviço.

    var petsNerbyWhereAlredyQueried = false

    fun setupInicial (activity: Activity){

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference

        val mySharedPrefs: mySharedPrefs = mySharedPrefs(activity)

        userBD = mySharedPrefs.getValue("userBdInicial")
        tipo = mySharedPrefs.getValue("tipo")
        val liberado = mySharedPrefs.getValue("liberaServicoInicial")
        if (liberado=="0"){
            liberaServico = true
        } else {
            liberaServico = false
        }
        imgDoUser = mySharedPrefs.getValue("imgInicial")


    }

    //coloca o lat, long e latlong em um petshop sem esta informação
    fun saveLatLongInPetshopWithouLocationYet(bd: String, location: Address){

        databaseReference.child("petshops").child(bd).child("lat").setValue(location.latitude)
        databaseReference.child("petshops").child(bd).child("long").setValue(location.longitude)
        databaseReference.child("petshops").child(bd).child("latlong").setValue(location.latitude + location.longitude)

    }

    fun updateAutonomosStatus(state: String, servico:String, apelido: String, lastLocation: Location){

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

    fun updateUserStatus(state: String, img: String, location: Location, activity: Activity){


        if (location != null) {

            lastLocation = location

            val lat = lastLocation.latitude
            val long = lastLocation.longitude

            if (state.equals("online")) {
                            databaseReference.child("onlineUsers").child(MapsModels.userBD).child("latlong")
                                .setValue(lat + long)
                            databaseReference.child("onlineUsers").child(MapsModels.userBD).child("state")
                                .setValue(state)
                            databaseReference.child("onlineUsers").child(MapsModels.userBD).child("img")
                                .setValue(img)
                            databaseReference.child("onlineUsers").child(MapsModels.userBD).child("lat")
                                .setValue(lat)
                            databaseReference.child("onlineUsers").child(MapsModels.userBD).child("long")
                                .setValue(long)
                        } else {
                            databaseReference.child("onlineUsers").child(MapsModels.userBD).removeValue()
                        }

        }



    }

    fun saveVendaNoBd(activity: Activity, buscaOuEntrega: String, formaPagamento: String, bandeira: String, endereco: String, numero: String, complemento: String, whatsApp: String, precoFinal: String, nomePet: String, bairro: String, cidade: String, estado: String, bdDoPet: String, arrayTipoCarrinho: MutableList<String>, arrayNomesCarrinho: MutableList<String>, arrayPrecoCarrinho: MutableList<String>, arrayDescCarrinho: MutableList<String>) :String {

        val mySharedPrefs: mySharedPrefs = mySharedPrefs(activity)

        mySharedPrefs.setValue("liberaServicoInicial", "1")

        val newCad: String = databaseReference.child("compras").push().key.toString()

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

        //vamos registrar agora o alerta caso seja compra de ração
        cont=0
        while (cont<arrayTipoCarrinho.size){
            if (arrayTipoCarrinho.get(cont).equals("racao")){ //se tiver ração ele chama o metodo
                ativarLembrete(bdDoPet, arrayNomesCarrinho.get(cont), activity)
                cont=arrayTipoCarrinho.size //se achou uma ração, ele acaba com esse while pois nao preciso verificar mais de uma vez. Mesmo se o user comprou varias rações, nao importa pois nao vou avisar varias vezes. Vai ser somente um aviso.
            }
            cont++
        }

        return produtos

    }

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

    //verifica se este produto já está registrado no bd. Se não, ele vai criar uma entrada para depois verificar quantos produtos deste topo ja foram vendidos na região
    fun ExisteEsteProduto (cidade: String, nomeProduto: String, preco: String) {

        //FirebaseDatabase.getInstance().reference.child("produtos").child(cidade).child(nomeProduto).orderByChild("controle").equalTo("controle")
        val rootRef = databaseReference.child("produtos").child(cidade).child(nomeProduto)
        //val rootRef = databaseReference.child("compras").child(bdDaCompra)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //TODO("Not yet implemented")
                // EncerraDialog()
            }

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()){

                    var values: String
                    var qnt = 0
                    values = p0.child("quantidade").value.toString()
                    qnt = (values.toInt() + 1)

                    var precoMedio : String
                    precoMedio = p0.child("precoTotal").value.toString()
                    precoMedio = MapsController.CalculeEstesValoresEmDinheiro(precoMedio, preco, qnt)

                    //salva a quantidade de vendas
                    databaseReference.child("produtos").child(cidade).child(nomeProduto).child("quantidade")
                        .setValue(qnt)
                    //salva o preço total. Depois para pegar o preço médio basta dividir pela quantidade
                    databaseReference.child("produtos").child(cidade).child(nomeProduto).child("precoTotal").setValue(precoMedio)

                    //verificando preços para definir o preço mais alto deste produto
                    values = p0.child("precoMax").value.toString()
                    var precoMax = MapsController.CalculeSeEsteEoMenorPreco(values, preco)

                    //se for igual, não precisa mexer no bd
                    if (!precoMax.equals(values)){
                        databaseReference.child("produtos").child(cidade).child(nomeProduto).child("precoMax").setValue(precoMax)
                    }

                    //vamos agora verificar o menor preco. Vamos usar as mesmas variaveis pra não precisar criar outras a toa. Mas na verdade estamso falando aqui do menor valor
                    values = p0.child("precoMin").value.toString()
                    precoMax = MapsController.CalculeSeEsteEoMenorPreco(values, preco)


                    //se for igual, não precisa mexer no bd
                    if (!precoMax.equals(values)){
                        databaseReference.child("produtos").child(cidade).child(nomeProduto).child("precoMin").setValue(precoMax)
                    }

                } else {

                    //se nao existe, criar o campo
                    databaseReference.child("produtos").child(cidade).child(nomeProduto).child("quantidade").setValue(1)
                    databaseReference.child("produtos").child(cidade).child(nomeProduto).child("precoTotal").setValue(preco)
                    databaseReference.child("produtos").child(cidade).child(nomeProduto).child("controle").setValue("controle")
                    databaseReference.child("produtos").child(cidade).child(nomeProduto).child("precoMax").setValue(preco)
                    databaseReference.child("produtos").child(cidade).child(nomeProduto).child("precoMin").setValue(preco)

                }

            }

        })


    }

    //Metodo de lembrar ao user que passou um tempo desde que comprou um item e eprguntar se quer repetir a compra
    //este método pega a data de hoje e salva um lembrete daqui a dez dias.
    fun ativarLembrete(bdDoPet: String, nomeProduto: String, activity: Activity){

        val dataRemember = MapsController.GetfutureDate(10)
        val mySharedPrefs: mySharedPrefs = mySharedPrefs(activity)

        mySharedPrefs.setValue("remeberNomeProduto", nomeProduto)
        mySharedPrefs.setValue("rememberBdDoPEt", bdDoPet)
        mySharedPrefs.setValue("rememberDate", dataRemember)

    }

    //cria e apaga o campo do usuario que está online.
    fun updateUserStatus(state: String, img: String, location: Location){



        lastLocation = location

        val lat = lastLocation.latitude
        val long = lastLocation.longitude

        if (state.equals("online")) {
            databaseReference.child("onlineUsers").child(MapsModels.userBD).child("latlong")
                .setValue(lat + long)
            databaseReference.child("onlineUsers").child(MapsModels.userBD).child("state")
                .setValue(state)
            databaseReference.child("onlineUsers").child(MapsModels.userBD).child("img")
                .setValue(img)
            databaseReference.child("onlineUsers").child(MapsModels.userBD).child("lat")
                .setValue(lat)
            databaseReference.child("onlineUsers").child(MapsModels.userBD).child("long")
                .setValue(long)
        } else {
            databaseReference.child("onlineUsers").child(MapsModels.userBD).removeValue()
        }

    }




}