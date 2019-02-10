package org.walleth.ethpos

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.supplement_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.ligi.kaxtui.alert
import org.walleth.ethpos.model.Supplement

var currentSelectedSupplement: Supplement? = null

class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(supplement: Supplement) {
        itemView.supplement_name.text = supplement.name
        itemView.supplement_price.text = supplement.price + " DAI"

        if (supplement == currentSelectedSupplement) {
            itemView.supplement_card.setBackgroundColor(Color.argb(0xff, 0, 0xcc, 0))
        } else {
            itemView.supplement_card.setBackgroundColor(Color.WHITE)
        }

        UrlImageViewHelper.setUrlDrawable(itemView.supplement_image, BASE_IMAGE_URL + supplement.image)

        itemView.setOnClickListener {
            currentSelectedSupplement = supplement
            itemView.rootView.recycler_view.adapter?.notifyDataSetChanged()

            (itemView.context as Activity).createInvoice()
        }
    }
}

class ItemAdapter(private val supplements: List<Supplement>) : RecyclerView.Adapter<ItemViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): ItemViewHolder {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        val layout = layoutInflater.inflate(R.layout.supplement_item, viewGroup, false)
        return ItemViewHolder(layout)
    }

    override fun getItemCount() = supplements.size

    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        viewHolder.bind(supplements[position])
    }

}

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_main)

        supportActionBar?.title = "ΞTH POS"

        recycler_view.layoutManager = LinearLayoutManager(this)


        GlobalScope.launch(Dispatchers.IO) {
            val response =
                OkHttpClient.Builder().build().newCall(Request.Builder().url(BASE_URL + "data.json").build()).execute()

            val listMyData = Types.newParameterizedType(List::class.java, Supplement::class.java)
            val moshi = Moshi.Builder().build()


            val list = moshi.adapter<List<Supplement>>(listMyData).fromJson(response.body()?.source())

            launch(Dispatchers.Main) {
                if (list == null) {
                    alert("could not load data")
                } else {
                    recycler_view.adapter = ItemAdapter(list)
                    currentSelectedSupplement = list.first()
                    createInvoice()
                }

            }
        }


    }

}


private fun Activity.createInvoice() {

    currentSelectedSupplement?.let { supplement ->

        val functionParams = mutableListOf("address" to RECEIVE_ADDRESS)

        try {
            functionParams.add("uint256" to supplement.price.replace(".", "") + "0".repeat(17))
        } catch (e: NumberFormatException) {
        }

        val currentERC67String = ERC681(
            address = "0x89d24a6b4ccb1b6faa2625fe562bdd9a23260359", function = "transfer",
            functionParams = functionParams
        ).generateURL()
        qr_code.setQRCode(currentERC67String)

    }
}
