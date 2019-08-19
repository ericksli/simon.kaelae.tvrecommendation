package simon.kaelae.tvrecommendation

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

internal class ImageListAdapter internal constructor(
    context: Context,
    private val resource: Int,
    private val itemList: MutableList<String>?
) : ArrayAdapter<ImageListAdapter.ItemHolder>(context, resource) {
    internal var cardImageUrl = arrayOf(
        Uri.parse("http://i.imgur.com/xTtwwzS.jpg"),
        Uri.parse("http://i.imgur.com/ucm7n4h.jpg"),
        Uri.parse("http://i.imgur.com/dydKNhm.jpg"),

        Uri.parse("https://i.imgur.com/HfISddg.jpg"),
        Uri.parse("http://i.imgur.com/a2yFl08.jpg"),
        Uri.parse("http://i.imgur.com/a7rFbnX.png"),
        Uri.parse("http://i.imgur.com/ManpvmP.png"),
        Uri.parse("http://i.imgur.com/wnl8bSg.jpg"),
        Uri.parse("http://i.imgur.com/1pvaI8X.jpg"),
        Uri.parse("https://i.imgur.com/oKNRcsb.jpg")


    )

    override fun getCount(): Int {
        return if (this.itemList != null) this.itemList.size else 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val database = FirebaseDatabase.getInstance()
        val holder: ItemHolder
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(resource, null)
            holder = ItemHolder()
            holder.name = convertView!!.findViewById(R.id.textView)
            holder.icon = convertView.findViewById(R.id.icon)


            try {
                Picasso.with(context).load(cardImageUrl[position]).into(holder.icon);
            } catch (e: Exception) {

            }
            convertView.tag = holder
        } else {
            holder = convertView.tag as ItemHolder
        }
        if (position == 9) {

            database.getReference("fbtitle").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    holder.name!!.text = dataSnapshot.getValue(String::class.java) ?: "Facebook"
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
            database.getReference("fbicon").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    try {
                        Picasso.with(context).load(dataSnapshot.getValue(String::class.java)).into(holder.icon);

                    } catch (e: Exception) {
                        Picasso.with(context).load("https://i.imgur.com/oKNRcsb.jpg").into(holder.icon);
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
            database.getReference("fbbg").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dataSnapshot.getValue(String::class.java)?.let {
                        convertView.setBackgroundColor(Color.parseColor(it))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

            holder.name!!.text = this.itemList!![position]
            val sharedPreference = context.getSharedPreferences("layout", Activity.MODE_PRIVATE)
            try {
                Picasso.with(context).load(sharedPreference.getString("logo", "")).into(holder.icon);
            } catch (e: Exception) {
                Picasso.with(context).load("https://i.imgur.com/XQnIwzp.png").into(holder.icon);
            }
        }
        if (position > 9) {


            holder.name!!.text = this.itemList!![position]
            val sharedPreference = context.getSharedPreferences("layout", Activity.MODE_PRIVATE)
            try {
                Picasso.with(context).load(sharedPreference.getString("logo", "")).into(holder.icon);
            } catch (e: Exception) {
                Picasso.with(context).load("https://i.imgur.com/XQnIwzp.png").into(holder.icon);
            }


        } else {
            val title = arrayOf(
                "ViuTV",
                "now新聞台",
                "now直播台",
                "now 630台",
                "香港開電視",
                "有線新聞台",
                "有線直播台",
                "港台電視31",
                "港台電視32"
            )


            holder.name!!.text = this.itemList!![position]
            try {
                Picasso.with(context).load(cardImageUrl[position]).into(holder.icon);
            } catch (e: Exception) {

            }
        }
        return convertView


    }

    internal class ItemHolder {
        var name: TextView? = null
        var icon: ImageView? = null
    }
}
