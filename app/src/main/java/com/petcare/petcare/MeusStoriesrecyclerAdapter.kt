    package com.petcare.petcare

    import android.content.Context
    import android.graphics.Color
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageView
    import android.widget.TextView
    import androidx.constraintlayout.widget.ConstraintLayout
    import androidx.core.content.ContextCompat
    import androidx.recyclerview.widget.RecyclerView
    import com.bumptech.glide.Glide
    import com.bumptech.glide.load.resource.bitmap.RoundedCorners
    import java.util.*

    class MeusStoriesrecyclerAdapter(private var context: Context, private var arrayStories:MutableList<String>, private var arrayImg:MutableList<String>): RecyclerView.Adapter<MeusStoriesrecyclerAdapter.ViewHolder>() {

        val delim:String = "!?!%&**!"

        override fun getItemCount(): Int {
            return arrayStories.size;
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.meusstories_itemrow, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            //ordem
            //aqui   imgLink!?!%message!?!%textColor!?!%bgColor!?!%messagePosition!?!%BD

            /*
            //array: DIVISOR: !?!%
            0 - img
            1 - message
            2 - textColor
            3 - bgColor
            4 - messagePosition
            7 - BD
             */
            val img = arrayImg.get(position)

            val tokens = StringTokenizer(arrayStories.get(position), delim)
            //val img = tokens.nextToken() // this will contain "img link or nao"
            val message = tokens.nextToken()
            val textColor = tokens.nextToken()
            val bgColor = tokens.nextToken()
            val messagePosition = tokens.nextToken()
            val BD = tokens.nextToken()
            //arrayImgCarrinho.add(img)


            if (img=="nao"){
                //nao tem imagem, exibir uma imagem vazia
                holder.img.visibility = View.GONE
                holder.background.visibility = View.VISIBLE

            } else {
                holder.background.visibility = View.GONE
                Glide.with(context).load(bgColor).transform(RoundedCorners(15)).into(holder.img)
            }

            //definir a cor do fundo
            if (bgColor.equals("#FFFFFF")){
                //holder.img.setColorFilter(ContextCompat.getColor(context,  R.color.branco), android.graphics.PorterDuff.Mode.MULTIPLY)
                holder.background.setBackgroundColor(Color.parseColor("#FFFFFF"))
            } else if (bgColor.equals("#000000")){
                //holder.img.setColorFilter(ContextCompat.getColor(context,  R.color.preto), android.graphics.PorterDuff.Mode.MULTIPLY)
                holder.background.setBackgroundColor(Color.parseColor("#000000"))
            } else if (bgColor.equals("#2196F3")) {
                //holder.img.setColorFilter(ContextCompat.getColor(context,  R.color.azul), android.graphics.PorterDuff.Mode.MULTIPLY)
                holder.background.setBackgroundColor(Color.parseColor("#2196F3"))
            } else if (bgColor.equals("#FFEB3B")) {
                holder.background.setBackgroundColor(Color.parseColor("#FFEB3B"))
                //holder.img.setColorFilter(ContextCompat.getColor(context,  R.color.amarelo), android.graphics.PorterDuff.Mode.MULTIPLY)
            } else {
                holder.background.setBackgroundColor(Color.parseColor("#F44336"))
                //holder.img.setColorFilter(ContextCompat.getColor(context,  R.color.vermelho), android.graphics.PorterDuff.Mode.MULTIPLY)
            }



            //aqui você define os valores em cada elemento
            if (message.equals("nao")){
                holder.textViewMessage.visibility = View.GONE
            } else {
                holder.textViewMessage.visibility = View.VISIBLE
                holder.textViewMessage.setText(message)

                //set the position
                if (messagePosition.equals("top")){
                    val params = holder.textViewMessage.layoutParams as ConstraintLayout.LayoutParams
                    params.topToTop = holder.img.id
                    holder.textViewMessage.requestLayout()
                } else if (messagePosition.equals("middle")){
                    val params = holder.textViewMessage.layoutParams as ConstraintLayout.LayoutParams
                    params.topToTop = holder.img.id
                    params.bottomToBottom = holder.img.id
                    holder.textViewMessage.requestLayout()

                } else {
                    val params = holder.textViewMessage.layoutParams as ConstraintLayout.LayoutParams
                    params.bottomToBottom = holder.img.id
                    holder.textViewMessage.requestLayout()

                }

                //setar a cor do texto
                holder.textViewMessage.setTextColor(Color.parseColor(textColor))

            }

        }


        class ViewHolder(itemView: View?) :
            RecyclerView.ViewHolder(itemView!!) {
            //aqui você associa cada elemento a um nome para invocar nesta classe
            var textViewMessage: TextView = itemView!!.findViewById(R.id.meusStories_message)
            var img: ImageView = itemView!!.findViewById(R.id.meusStories_iv)
            var background : ConstraintLayout = itemView!!.findViewById(R.id.meusStories_background)

        }

    }
