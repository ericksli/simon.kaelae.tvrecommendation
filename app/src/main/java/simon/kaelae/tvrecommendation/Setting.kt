package simon.kaelae.tvrecommendation

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import simon.kaelae.tvrecommendation.recommendation.DefaultChannelRecommendationJobService
import java.io.File


class Setting : Activity() {
    var Local_ver = BuildConfig.VERSION_CODE
    var Cloud_ver = BuildConfig.VERSION_CODE
    var dllink = Uri.parse("https://www.facebook.com/androidtvhk")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settinglistview)
        ctx = this
        var switch = "切換界面"
        val sharedPreference = getSharedPreferences("layout", MODE_PRIVATE)
        var editor = sharedPreference.edit()

        val database = FirebaseDatabase.getInstance()
        database.getReference("version").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Cloud_ver = dataSnapshot.getValue(Int::class.java) as Int
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        database.getReference("dllink").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dllink = Uri.parse(dataSnapshot.getValue(String::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        if (sharedPreference.getString("layout", "") == "TV") {//TV
            switch = "切換成手機版界面"
        } else {//phone
            switch = "切換成TV版界面"
        }
        var newlist = mutableListOf<String>(switch, "自定頻道", "選擇播放器", "檢查更新","重置設定")


        val lvAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1, newlist
        )

        val lv = findViewById<ListView>(R.id.list)
        lv.adapter = lvAdapter

        lv.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            if (i == 0) {
                if (sharedPreference.getString("layout", "") == "TV") {//TV
                    editor.putString("layout", "phone")
                } else {//phone
                    editor.putString("layout", "TV")
                }
                editor.apply()
                val intent = Intent(this@Setting, MainActivity::class.java)
                startActivity(intent)
            }
            if (i == 1) {
                val dia = AlertDialog.Builder(this@Setting, R.style.Theme_AppCompat).apply {
                    val dialogView = this@Setting.layoutInflater.inflate(R.layout.dialog_new_category, null)
                    dialogView.findViewById<RadioGroup>(R.id.playerchoiceradiogroup).visibility = View.GONE
                     setView(dialogView)
                    setTitle("新增影片來源")
                    setMessage("輸入名稱及影片網址")
                    setPositiveButton("Save") { _, _ ->
                        val theURL = dialogView.findViewById<EditText>(R.id.url).text.toString().replace(" ", "")
                        val theName = dialogView.findViewById<EditText>(R.id.name).text.toString()
                        if (theURL == ""||theName == "") {
                            Toast.makeText(this@Setting, "儲存不成功:名稱或網址不可留空", Toast.LENGTH_SHORT).show()

                        } else if(theURL.split(",").size != theName.split(",").size){
                            Toast.makeText(this@Setting, "儲存不成功:名稱及網址數量不相稱", Toast.LENGTH_SHORT).show()

                        }else{
                            val sharedPreference = getSharedPreferences("layout", Context.MODE_PRIVATE)
                            var editor = sharedPreference.edit()
                            val original_name = sharedPreference.getString("name", "")
                            val original_url = sharedPreference.getString("url", "")
                            var new_name =
                                original_name + "," + dialogView.findViewById<EditText>(R.id.name).text.toString()
                            var new_url =
                                original_url + "," + dialogView.findViewById<EditText>(R.id.url).text.toString()
                            editor.putString("name", new_name)
                            editor.putString("url", new_url)
                            editor.apply()
                            try {
                                DefaultChannelRecommendationJobService.startJob(this@Setting)
                            } catch (e: Exception) {
                            }
                            val intent = Intent(this@Setting, MainActivity::class.java)
                            startActivity(intent)
                        }
                    }
                    setNegativeButton("Cancel") { _, _ ->
                    }
                    setNeutralButton("編輯自訂台") { _, _ ->
                        val intent = Intent(this@Setting, TVshowlist::class.java)
                        startActivity(intent)
                    }
                }.create().show()

            }
            if (i == 2) {
                AlertDialog.Builder(this@Setting, R.style.Theme_AppCompat).apply {
                    val dialogView = this@Setting.layoutInflater.inflate(R.layout.dialog_new_category, null)

                    dialogView.findViewById<RadioGroup>(R.id.playerchoiceradiogroup).visibility = View.VISIBLE
                    dialogView.findViewById<EditText>(R.id.name).visibility = View.GONE
                    dialogView.findViewById<EditText>(R.id.url).visibility = View.GONE
                    if (sharedPreference.getString("player", "originalplayer") == "originalplayer") {
                        dialogView.findViewById<RadioButton>(R.id.originalplayer).isChecked = true
                    }
                    if (sharedPreference.getString("player", "originalplayer") == "selfplayer") {
                        dialogView.findViewById<RadioButton>(R.id.selfplayer).isChecked = true
                    }
                    setView(dialogView)
                    setTitle("自定播放器")
                    setPositiveButton("Save") { _, _ ->
                        if (dialogView.findViewById<RadioButton>(R.id.originalplayer).isChecked) {
                            var editor = sharedPreference.edit()
                            editor.putString("player", "originalplayer")
                            editor.apply()
                        }
                        if (dialogView.findViewById<RadioButton>(R.id.selfplayer).isChecked) {
                            var editor = sharedPreference.edit()
                            editor.putString("player", "selfplayer")
                            editor.apply()
                        }
                        val intent = Intent(this@Setting, MainActivity::class.java)
                        startActivity(intent)
                    }

                    setNegativeButton("Cancel") { _, _ ->
                        //pass
                    }
                }.create().show()
            }
            if (i == 3) {
                if (Local_ver < Cloud_ver) {
                    //Toast.makeText(this@MainActivity,"UPDATE",Toast.LENGTH_SHORT).show()
                    AlertDialog.Builder(this@Setting, R.style.Theme_AppCompat).apply {
                        val dialogView = this@Setting.layoutInflater.inflate(R.layout.dialog_new_category, null)

                        setView(dialogView)
                        dialogView.findViewById<EditText>(R.id.name).visibility = View.GONE
                        dialogView.findViewById<EditText>(R.id.url).visibility = View.GONE
                        dialogView.findViewById<RadioGroup>(R.id.playerchoiceradiogroup).visibility = View.GONE
                        setTitle("有更新可用-UPDATE-")
                        setPositiveButton("下載更新") { _, _ ->

                            val openURL = Intent(android.content.Intent.ACTION_VIEW)
                            openURL.data = dllink
                            try {
                                startActivity(openURL)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@Setting,
                                    "Cannot find donwloader. Try to install file manager or web brower",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        setNegativeButton("Cancel") { _, _ ->
                            //pass
                        }
                    }.create().show()
                }
                if (Local_ver >= Cloud_ver) {
                    Toast.makeText(
                        this@Setting,
                        "已經是最新版",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
            if (i == 4) {
                AlertDialog.Builder(this@Setting, R.style.Theme_AppCompat).apply {
                    val dialogView = this@Setting.layoutInflater.inflate(R.layout.dialog_new_category, null)

                    setView(dialogView)
                    dialogView.findViewById<EditText>(R.id.name).visibility = View.GONE
                    dialogView.findViewById<EditText>(R.id.url).visibility = View.GONE
                    dialogView.findViewById<RadioGroup>(R.id.playerchoiceradiogroup).visibility = View.GONE
                    setTitle("要刪除所有設定?(會一並刪除所有自定頻道)")
                    setPositiveButton("確定") { _, _ ->
                        editor.clear().commit()
                        Toast.makeText(this@Setting, "已重置", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@Setting, MainActivity::class.java)
                        startActivity(intent)
                    }

                    setNegativeButton("取消") { _, _ ->
                        //pass
                    }
                }.create().show()

            }
        }
    }
    private fun downloadUpdate() {
        registerReceiver(onDownloadComplete(), IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        val request = DownloadManager
            .Request(Uri.parse("https://thematrix.dev/tvhk/app-release.apk"))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "tvhk.apk")

        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
    }

    private class onDownloadComplete: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val c = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            if(c != null){
                c.moveToFirst()
                val fileUri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                val mFile = File(Uri.parse(fileUri).path!!)
                val fileName = mFile.absolutePath

                context.unregisterReceiver(this)

                val intent = Intent(Intent.ACTION_VIEW)
                var contentUri: Uri
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", File(fileName))

                }else{
                    contentUri = Uri.fromFile(File(fileName))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
                startActivity(ctx, intent, null)
            }
        }
    }
    companion object {
        lateinit var ctx: Context
        private lateinit var downloadManager: DownloadManager
        private var downloadId: Long = -1
    }
}