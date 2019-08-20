package simon.kaelae.tvrecommendation


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.mediarouter.app.MediaRouteButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_exo.view.*
import kotlinx.android.synthetic.main.item_post.view.*
import org.json.JSONArray
import org.json.JSONObject


class PlaybackVideoExoFragment : Fragment() {

    private var adapter: CommentAdapter? = null
    private lateinit var commentsReference: DatabaseReference
    lateinit var recyclerPostComments: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {

        requireContext().theme.applyStyle(R.style.mytheme, true)

        val view: View = inflater.inflate(
            R.layout.activity_exo, container,
            false
        )

        val (id, title, _, _, videoUrl, func) = activity?.intent?.getSerializableExtra(DetailsActivity.MOVIE) as Movie
        setUpPlayer(view)
        setUpNetwork()
        prepareVideo(id, title, videoUrl, func)
        val sharedPreference = activity!!.getSharedPreferences("layout", Activity.MODE_PRIVATE)
        commentsReference = FirebaseDatabase.getInstance().reference
            .child("chat").child(title)
        recyclerPostComments = view.findViewById(R.id.recyclerPostComments) as RecyclerView
        var message = view.findViewById(R.id.message) as EditText
        var send = view.findViewById(R.id.send) as Button
        var linearlayout = view.findViewById(R.id.linearlayout) as LinearLayout
        val mLayoutManager = LinearLayoutManager(context)
        mLayoutManager.reverseLayout = true
        mLayoutManager.stackFromEnd = true
        recyclerPostComments.layoutManager = mLayoutManager


        if (title == "ViuTV" ||
            title == "now新聞台" ||
            title == "now直播台" ||
            title == "now 630台" ||
            title == "香港開電視" ||
            title == "有線新聞台" ||
            title == "有線直播台" ||
            title == "港台電視31" ||
            title == "港台電視32"
        ) {
            linearlayout.visibility = View.VISIBLE
            message.visibility = View.VISIBLE
            send.visibility = View.VISIBLE
        } else {
            linearlayout.visibility = View.GONE
            message.visibility = View.GONE
            send.visibility = View.GONE
        }

        if (sharedPreference.getString("chat_name", "") == "") {
            message.setHint("請先輸入聊天室花名才可發言")
            send.setText("設定名稱")
        }

        if (sharedPreference.getString("chatenabled", "true") == "true") {
            adapter = CommentAdapter(context!!, commentsReference)
            recyclerPostComments.adapter = adapter

        } else {
            linearlayout.visibility = View.GONE
            message.visibility = View.GONE
            send.visibility = View.GONE
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            linearlayout.visibility = View.GONE
            message.visibility = View.GONE
            send.visibility = View.GONE
        }
        val mMediaRouteButton = view.findViewById<MediaRouteButton>(R.id.media_route_button);
        CastButtonFactory.setUpMediaRouteButton(context, mMediaRouteButton);

        try {
            val mCastContext = CastContext.getSharedInstance(requireContext())
            if (mCastContext?.getCastState() != CastState.NO_DEVICES_AVAILABLE) {
                mMediaRouteButton.setVisibility(View.VISIBLE);
            }
            mCastContext?.addCastStateListener { state ->
                if (state == CastState.NO_DEVICES_AVAILABLE)
                    mMediaRouteButton.setVisibility(View.GONE);
                else {
                    if (mMediaRouteButton.getVisibility() == View.GONE)
                        mMediaRouteButton.setVisibility(View.VISIBLE);
                }
            }
        } catch (e: java.lang.Exception) {
        }

        send.setOnClickListener {
            if (send.text == "設定名稱") {
                sharedPreference.edit().putString("chat_name", message.text.toString()).apply()
                send.setText("發送")
                message.setText("")
                message.setHint("可以留言啦")
            } else {
                if (sharedPreference.getString("chat_name", "") != "" && sharedPreference.getString(
                        "chat_name",
                        ""
                    )!!.contains("admin") == false && message.text.toString() != ""
                ) {
                    //if (sharedPreference.getString("chat_name", "") != "" && message.text.toString() != "") {
                    send.visibility = View.GONE
                    val key = commentsReference.push().getKey()!!
                    val value = HashMap<String, Any?>();
                    value.put("name", sharedPreference.getString("chat_name", ""));
                    value.put("msg", message.text.toString());
                    value.put("time", ServerValue.TIMESTAMP);
                    commentsReference.child(key).setValue(value).addOnSuccessListener {

                        message.setText("")
                        send.visibility = View.VISIBLE
                    }

                } else {
                    message.setError("內容不能留空")
                }
            }
        }
        return view
    }

    override fun onStop() {
        super.onStop()
        player.release()

    }


    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    private fun setUpPlayer(view: View) {
        // setup track selector
        val bandwithMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwithMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        // create player
        player = ExoPlayerFactory.newSimpleInstance(activity, trackSelector)
        player.playWhenReady = true
        playerView = view.player_view
        playerView.useController = true
        playerView.requestFocus()
        playerView.player = player
        playerView.hideController()

        dataSourceFactory = DefaultDataSourceFactory(activity, "exoplayer", bandwithMeter)
        hlsMediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)

        toast = Toast.makeText(context, "", Toast.LENGTH_LONG)
    }

    private fun setUpNetwork() {
        requestQueue = RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply {
            start()
        }
    }

    fun channelSwitch(direction: String, showMessage: Boolean) {
        lastDirection = direction

        val list = MovieList.list
        //val sharedPreference = activity?.getSharedPreferences("layout", Activity.MODE_PRIVATE)

        var videoId = currentVideoID

        if (direction.equals("PREVIOUS")) {
            videoId--
        } else if (direction.equals("NEXT")) {
            videoId++
        }

        val channelCount = list.count()
        if (videoId < 0) {
            videoId = channelCount - 1
        } else if (videoId >= channelCount) {
            videoId = 0
        }

        val item = list[videoId]

        if (showMessage) {
            toast.setText("正在轉台到 " + item.title)
            toast.show()
        }

        prepareVideo(item.id, item.title, item.videoUrl, item.func)
    }

    fun prepareVideo(id: Int, title: String, videoUrl: String, func: String) {

        currentVideoID = id

        if (videoUrl.equals("")) {
            getVideoUrl(title, func)
        } else {
            playVideo(title, videoUrl)
            cast(title, videoUrl)
        }

    }

    fun playVideo(title: String, videoUrl: String) {

        val sharedPreference = activity?.getSharedPreferences("layout", Context.MODE_PRIVATE)
        if (sharedPreference?.getString("player", "exoplayer") == "exoplayer") {
            val mediaUri = Uri.parse(videoUrl)
            val mediaSource = hlsMediaSourceFactory.createMediaSource(mediaUri)
            player.prepare(mediaSource)
        } else {


            try {
                val playIntent: Intent = Uri.parse(videoUrl).let { uri ->
                    Intent(Intent.ACTION_VIEW, uri)
                }
                startActivity(playIntent)
            } catch (e: java.lang.Exception) {
                //Toast.makeText(context?.applicationContext,"沒有播放器，建議安裝Mx Player，改用內置播放器",Toast.LENGTH_SHORT).show()
                val mediaUri = Uri.parse(videoUrl)
                val mediaSource = hlsMediaSourceFactory.createMediaSource(mediaUri)
                player.prepare(mediaSource)
            }
        }
    }

    private fun getVideoUrl(title: String, ch: String) {
        requestQueue.cancelAll(this)

        lateinit var url: String

        if (ch.equals("viutv99") || ch.equals("nowtv332") || ch.equals("nowtv331")) {
            val params = JSONObject()

            if (ch.equals("viutv99")) {
                url = "https://api.viu.now.com/p8/2/getLiveURL"

                params.put("channelno", "099")

                params.put("deviceId", "AndroidTV")
                params.put("deviceType", "5")
            } else {
                url = "https://hkt-mobile-api.nowtv.now.com/09/1/getLiveURL"

                if (ch.equals("nowtv332")) {
                    params.put("channelno", "332")
                } else if (ch.equals("nowtv331")) {
                    params.put("channelno", "331")
                }

                params.put("audioCode", "")
            }

            params.put("callerReferenceNo", "")
            params.put("format", "HLS")
            params.put("mode", "prod")

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                Response.Listener { response ->
                    try {
                        url = JSONArray(
                            JSONObject(JSONObject(response.get("asset").toString()).get("hls").toString()).get("adaptive").toString()
                        ).get(0).toString()
//                        try {
//
//                            var myClipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                            var myClip: ClipData = ClipData.newPlainText("note_copy", url)
//                            myClipboard.setPrimaryClip(myClip)
//                            //Toast.makeText(context, "已複製播放網址到剪貼簿", Toast.LENGTH_SHORT).show()
//                        } catch (e: java.lang.Exception) {
//                        }
                        playVideo(title, url)
                        cast(title, url)

                    } catch (exception: Exception) {
                        //showPlaybackErrorMessage(title)
                    }
                },
                Response.ErrorListener { error ->
                    //showPlaybackErrorMessage(title)
                }
            )

            jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 5,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )

            requestQueue.add(jsonObjectRequest)
        } else if (ch.equals("cabletv109") || ch.equals("cabletv110")) {
            url = "https://mobileapp.i-cable.com/iCableMobile/API/api.php"

            val stringRequest = object : StringRequest(
                Method.POST,
                url,
                Response.Listener { response ->
                    try {
                        playVideo(
                            title,
                            JSONObject(JSONObject(response).get("result").toString()).get("stream").toString()
                        )
                        cast(title, JSONObject(JSONObject(response).get("result").toString()).get("stream").toString())
                    } catch (exception: Exception) {
                        showPlaybackErrorMessage(title)
                    }
                },
                Response.ErrorListener { error ->
                    showPlaybackErrorMessage(title)
                }
            ) {
                override fun getRetryPolicy(): RetryPolicy {
                    return DefaultRetryPolicy(
                        DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 5,
                        0,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val params = mutableMapOf<String, String>()

                    params.put("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 6.0.1; AndroidTV Build/35.0.A.1.282)")

                    return params
                }

                override fun getParams(): MutableMap<String, String> {
                    val params = mutableMapOf<String, String>()

                    if (ch.equals("cabletv109")) {
                        params.put("channel_no", "_9")
                        params.put("vlink", "_9")
                    } else if (ch.equals("cabletv110")) {
                        params.put("channel_no", "_10")
                        params.put("vlink", "_10")
                    }

                    params.put("device", "aos_mobile")
                    params.put("method", "streamingGenerator2")
                    params.put("quality", "h")
                    params.put("uuid", "")
                    params.put("is_premium", "0")
                    params.put("network", "wifi")
                    params.put("platform", "1")
                    params.put("deviceToken", "")
                    params.put("appVersion", "6.3.4")
                    params.put("market", "G")
                    params.put("lang", "zh_TW")
                    params.put("version", "6.3.4")
                    params.put("osVersion", "23")
                    params.put("channel_id", "106")
                    params.put("deviceModel", "AndroidTV")
                    params.put("type", "live")

                    return params
                }
            }

            requestQueue.add(stringRequest)
        } else if (ch.equals("nowtv630")) {
            url = "https://sports.now.com/VideoCheckOut/?pid=webch630_4&service=NOW360&type=channel"
            val queue = Volley.newRequestQueue(context)
            val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->

                    playVideo(
                        title,
                        response.substringBeforeLast("html5streamurl").substringAfterLast("html5streamurl").substringBeforeLast(
                            "]"
                        ).substringBeforeLast("]").substringAfterLast("[")
                    )
                    cast(
                        title,
                        response.substringBeforeLast("html5streamurl").substringAfterLast("html5streamurl").substringBeforeLast(
                            "]"
                        ).substringBeforeLast("]").substringAfterLast("[")
                    )
                },
                Response.ErrorListener { })
            queue.add(stringRequest)

        }
    }


    private fun showPlaybackErrorMessage(title: String) {
        toast.setText(title + " 暫時未能播放，請稍候再試。")
        toast.show()
        channelSwitch(lastDirection, false)
    }


    private class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(comment: Comment) {
            itemView.name.text = comment.name
            itemView.msg.text = comment.msg
            itemView.time.text = DateUtils.getRelativeTimeSpanString(comment.time!!)
        }
    }

    private class CommentAdapter(
        private val context: Context,
        private val databaseReference: DatabaseReference
    ) : RecyclerView.Adapter<CommentViewHolder>() {

        private val childEventListener: ChildEventListener?

        private val commentIds = ArrayList<String>()
        private val comments = ArrayList<Comment>()

        init {

            // Create child event listener
            // [START child_event_listener_recycler]
            val childEventListener = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                    //Log.d(TAG, "onChildAdded:" + dataSnapshot.key!!)

                    // A new comment has been added, add it to the displayed list
                    val comment = dataSnapshot.getValue(Comment::class.java)

                    // [START_EXCLUDE]
                    // Update RecyclerView
                    commentIds.add(dataSnapshot.key!!)
                    comments.add(comment!!)
                    notifyItemInserted(comments.size - 1)
                    notifyDataSetChanged()
                    // [END_EXCLUDE]
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                    //Log.d(TAG, "onChildChanged: ${dataSnapshot.key}")

                    // A comment has changed, use the key to determine if we are displaying this
                    // comment and if so displayed the changed comment.
                    val newComment = dataSnapshot.getValue(Comment::class.java)
                    val commentKey = dataSnapshot.key

                    // [START_EXCLUDE]
                    val commentIndex = commentIds.indexOf(commentKey)
                    if (commentIndex > -1 && newComment != null) {
                        // Replace with the new data
                        comments[commentIndex] = newComment

                        // Update the RecyclerView
                        notifyItemChanged(commentIndex)

                    } else {
                        // Log.w(TAG, "onChildChanged:unknown_child: $commentKey")
                    }
                    // [END_EXCLUDE]
                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                    // Log.d(TAG, "onChildRemoved:" + dataSnapshot.key!!)

                    // A comment has changed, use the key to determine if we are displaying this
                    // comment and if so remove it.
                    val commentKey = dataSnapshot.key

                    // [START_EXCLUDE]
                    val commentIndex = commentIds.indexOf(commentKey)
                    if (commentIndex > -1) {
                        // Remove data from the list
                        commentIds.removeAt(commentIndex)
                        comments.removeAt(commentIndex)

                        // Update the RecyclerView
                        notifyItemRemoved(commentIndex)
                    } else {
                        // Log.w(TAG, "onChildRemoved:unknown_child:" + commentKey!!)
                    }
                    // [END_EXCLUDE]
                }

                override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                    //  Log.d(TAG, "onChildMoved:" + dataSnapshot.key!!)

                    // A comment has changed position, use the key to determine if we are
                    // displaying this comment and if so move it.
                    val movedComment = dataSnapshot.getValue(Comment::class.java)
                    val commentKey = dataSnapshot.key

                    // ...
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    //Log.w(TAG, "postComments:onCancelled", databaseError.toException())
                    Toast.makeText(
                        context, "Failed to load comments.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            databaseReference.orderByChild("time").limitToLast(25).addChildEventListener(childEventListener)
            // [END child_event_listener_recycler]

            // Store reference to listener so it can be removed on app stop
            this.childEventListener = childEventListener
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.item_post, parent, false)
            return CommentViewHolder(view)
        }

        override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
            holder.bind(comments[position])
            if (position % 2 == 1) {
                holder.itemView.setBackgroundColor(Color.parseColor("#455a64"))
            } else {
                holder.itemView.setBackgroundColor(Color.parseColor("#263238"))
            }
        }

        override fun getItemCount(): Int = comments.size

        fun cleanupListener() {
            childEventListener?.let {
                databaseReference.removeEventListener(it)
            }
        }
    }

    fun cast(title: String, url: String) {
        try {
            val mCastContext = CastContext.getSharedInstance(requireContext())
            val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW)
            movieMetadata.putString(MediaMetadata.KEY_TITLE, title)
            val mediaInfo = MediaInfo.Builder(url)
                .setMetadata(movieMetadata).build()
            val castPlayer = CastPlayer(mCastContext)
            castPlayer.loadItem(MediaQueueItem.Builder(mediaInfo).build(), 0)
        } catch (e: Exception) {
        }
    }

    companion object {
        private lateinit var player: SimpleExoPlayer
        private lateinit var playerView: SimpleExoPlayerView
        private lateinit var dataSourceFactory: DefaultDataSourceFactory
        private lateinit var hlsMediaSourceFactory: HlsMediaSource.Factory

        var currentVideoID = -1
        private lateinit var requestQueue: RequestQueue
        private var lastDirection = "NEXT"
        private lateinit var toast: Toast


    }
}
