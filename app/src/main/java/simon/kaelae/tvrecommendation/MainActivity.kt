package simon.kaelae.tvrecommendation

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import android.widget.AdapterView.OnItemLongClickListener
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import simon.kaelae.tvrecommendation.recommendation.DefaultChannelRecommendationJobService
import simon.kaelae.tvrecommendation.recommendation.PROGRAM_QUERY
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext


class MainActivity : Activity() {

    var Local_ver = BuildConfig.VERSION_CODE
    var Cloud_ver = BuildConfig.VERSION_CODE

    var fblink = Uri.parse("https://www.facebook.com/androidtvhk")
    var fbtitle = "AndroidTV 教室\n "

    var title = mutableListOf(
        "ViuTV",
        "now新聞台",
        "now直播台",
        "now 630台",
        "香港開電視",
        "有線新聞台",
        "有線直播台",
        "港台電視31",
        "港台電視32",
        "AndroidTV 教室"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this@MainActivity)
        val database = FirebaseDatabase.getInstance()
        val sharedPreference = getSharedPreferences("layout", Context.MODE_PRIVATE)
        sharedPreference.getString("layout", "")
        setUpSSL()
        if (isTV() || sharedPreference.getString("layout", "") == "TV") {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            setTheme(R.style.AppTheme)
            setContentView(R.layout.activity_main)
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            setTheme(R.style.mytheme)
            setContentView(R.layout.phone_layout)
            val gridview = findViewById<GridView>(R.id.gridview)

            val sharedPreference = getSharedPreferences("layout", Context.MODE_PRIVATE)

            if (sharedPreference.getString("name", "") == "") {
            } else {
                //.makeText(this@MainActivity,sharedPreference.getString("name", "")!!.split(",")[1],Toast.LENGTH_SHORT).show()
                //Toast.makeText(this@MainActivity,sharedPreference.getString("name", "")!!.split(",").size.toString(),Toast.LENGTH_SHORT).show()

                for (i in 0 until sharedPreference.getString("name", "")!!.split(",").size) {
                    title.add(sharedPreference.getString("name", "")!!.split(",")[i])
                }
                title.removeAt(10)


            }

            val adapter = ImageListAdapter(this, R.layout.grid_cell, title)
            gridview.adapter = adapter


            gridview.onItemClickListener = AdapterView.OnItemClickListener { parent, v, position, id ->

                if (position == 9) {
                    val openURL = Intent(android.content.Intent.ACTION_VIEW)
                    openURL.data = fblink
                    startActivity(openURL)
                } else if (position > 9) {

                    val intent = Intent(this, PlaybackActivity::class.java)
                    val movie = Movie(
                        id.toInt(),
                        title = sharedPreference.getString("name", "")!!.split(",")[position - 9],
                        description = "",
                        cardImageUrl = "",
                        videoUrl = sharedPreference.getString("url", "")!!.split(",")[position - 9],
                        func = ""
                    )
                    intent.putExtra(DetailsActivity.MOVIE, movie)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, PlaybackActivity::class.java)
                    val movie = MovieList.list.find { it.id == id.toInt() }
                    intent.putExtra(DetailsActivity.MOVIE, movie)
                    startActivity(intent)
                }
            }

            gridview.setOnItemLongClickListener(OnItemLongClickListener { arg0, arg1, position, arg3 ->

                if (position > 9) {
                    //Toast.makeText(this@MainActivity,sharedPreference.getString("url", "")!!.split(",")[position-8],Toast.LENGTH_SHORT).show()

                    lateinit var dialog: AlertDialog
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("刪除此自定項目?")

                    val dialogClickListener = DialogInterface.OnClickListener { _, which ->
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> removeSharePreference(position - 9)
                        }
                    }
                    builder.setPositiveButton("YES", dialogClickListener)
                    builder.setNegativeButton("CANCEL", dialogClickListener)
                    dialog = builder.create()
                    dialog.show()
                }
                true
            })

            val setting = findViewById<Button>(R.id.setting)
            setting.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                }
            })

            val notice = findViewById<TextView>(R.id.notice)
            database.getReference("notice").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.getValue(String::class.java) == "") {
                        notice.visibility = GONE
                    } else {
                        notice.text = dataSnapshot.getValue(String::class.java)
                        notice.visibility = VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

            database.getReference("link").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    fblink = Uri.parse(dataSnapshot.getValue(String::class.java) ?: "about:blank")
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
        database.getReference("version").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Cloud_ver = dataSnapshot.getValue(Int::class.java) ?: Local_ver
                if (Local_ver < Cloud_ver) {
                    Toast.makeText(this@MainActivity, "發現更新，請到設定下載", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

//        database.getReference("fbtitle").addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                fbtitle = dataSnapshot.getValue(String::class.java)!!
//                title[8]=fbtitle
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//            }
//        })
        val setting = findViewById<Button>(R.id.setting)
        setting.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@MainActivity, Setting::class.java)
                startActivity(intent)

            }
        })
        try {
            DefaultChannelRecommendationJobService.startJob(this)
        } catch (e: Exception) {
        } catch (e: Error) {
            //handle carefully
        }
        if (intent != null && intent.data != null) {
            showMovie()

        }
    }


    private fun showMovie() {
        val sharedPreference = getSharedPreferences("layout", Context.MODE_PRIVATE)
        val id: String? = intent.data?.getQueryParameter(PROGRAM_QUERY)
        id ?: return
        if (id.toInt() < 9) {
            //Toast.makeText(this@MainActivity,id.toInt().toString(),Toast.LENGTH_SHORT).show()
            val movie = MovieList.list.find { it.id == id.toInt() }
            val intent = Intent(this, PlaybackActivity::class.java)
            intent.putExtra(DetailsActivity.MOVIE, movie)
            startActivity(intent)
        } else {

            //Toast.makeText(this@MainActivity,id.toInt().toString(),Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PlaybackActivity::class.java)
            val movie = Movie(
                id.toInt(),
                title = sharedPreference.getString("name", "")!!.split(",")[id.toInt() - 8],
                description = "",
                cardImageUrl = "",
                videoUrl = sharedPreference.getString("url", "")!!.split(",")[id.toInt() - 8],
                func = ""
            )
            intent.putExtra(DetailsActivity.MOVIE, movie)
            startActivity(intent)
        }
    }

    private fun isTV(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    fun removeSharePreference(i: Int) {
        val sharedPreference = getSharedPreferences("layout", Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        val original_name: MutableList<String> = sharedPreference.getString("name", "")?.split(",")!!.toMutableList()
        original_name.removeAt(i)
        val original_name_string = original_name.joinToString(separator = ",")
        val original_url: MutableList<String> = sharedPreference.getString("url", "")?.split(",")!!.toMutableList()
        original_url.removeAt(i)
        val original_url_string = original_url.joinToString(separator = ",")
        editor.putString("name", original_name_string)
        editor.putString("url", original_url_string)
        editor.apply()
        recreate();
    }

    private fun setUpSSL() {
        try {
            ProviderInstaller.installIfNeeded(applicationContext)
            var sslContext: SSLContext? = null
            sslContext = SSLContext.getInstance("TLSv1.2")
            try {
                sslContext!!.init(null, null, null)
                val engine = sslContext.createSSLEngine()
                engine.enabledCipherSuites

                //Toast.makeText(this, "強制使用 TLSv1.2", Toast.LENGTH_SHORT).show()
            } catch (e: KeyManagementException) {
                //Toast.makeText(this, "強制使用 TLSv1.2 失敗", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NoSuchAlgorithmException) {
            //Toast.makeText(this, "系統沒有 TLSv1.2", Toast.LENGTH_SHORT).show()
        } catch (e: GooglePlayServicesNotAvailableException) {
            //Toast.makeText(this, "系統沒有 Google Play Service", Toast.LENGTH_SHORT).show()
        } catch (e: GooglePlayServicesRepairableException) {
            //Toast.makeText(this, "Google Play Service 錯誤", Toast.LENGTH_SHORT).show()
        }
    }

}
