package vku.ltnhan.friendchat.messages

import android.R.id.message
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import vku.ltnhan.friendchat.NewMessageActivity
import vku.ltnhan.friendchat.R
import vku.ltnhan.friendchat.models.ChatMessage
import vku.ltnhan.friendchat.models.User
import vku.ltnhan.friendchat.utils.DateUtils.getFormattedTimeChatLog
import java.io.IOException
import java.util.*


class ChatLogActivity : AppCompatActivity() {
    companion object{
        val TAG = "ChatLog"
    }

    private var type = 1
    val adapter = GroupAdapter<ViewHolder>()
    var toUser: User? = null

    var filePath: Uri? = null

    private val PICK_IMAGE_REQUEST: Int = 2020

    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private var filename:String? = null
    private val apiNotification = API().getInstance().create(APINotification::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        recyclerview_chat_log.adapter = adapter
//       get data about chatlog user
        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser?.username
//        setupDummyData()

        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference
        listenForMessage()
        send_button_chat_log.setOnClickListener {
            Log.d(TAG, "Attempt to send message...")
            performSendMessage()
        }

        send_button_picture.setOnClickListener {
            chooseImage()
        }
    }

    private fun chooseImage() {
        val intent: Intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_PICK
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && data != null) {
            filePath = data.data

            Log.d("profile_file_path ", filePath?.path.toString())
            try {
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                img_selected.setImageBitmap(bitmap)
                type = 2
                img_selected.visibility = View.VISIBLE
                send_button_picture.visibility = View.GONE
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun listenForMessage(){
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)
                if (chatMessage != null) {
                    Log.d(TAG, chatMessage!!.text)

                    if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                        val currentUser = LatestMessagesActivity.currentUser ?: return
                        adapter.add(
                            ChatFromItem(
                                chatMessage.text,
                                currentUser,
                                chatMessage.timestamp,
                                chatMessage.type
                            )
                        )
                    } else {
                        adapter.add(
                            ChatToItem(
                                chatMessage.text,
                                toUser!!,
                                chatMessage.timestamp,
                                chatMessage.type
                            )
                        )
                    }
                }
                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
    private fun performSendMessage(){

        //how to we actually send a message to firebase...
        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = user!!.uid
        
        val fromReference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()
        if(type == 1){
            val text = edittext_chat_log.text.toString()

            if(fromId == null) return
//        val reference = FirebaseDatabase.getInstance().getReference("/messages").push()

            val chatMessage = ChatMessage(
                fromReference.key!!,
                text,
                fromId,
                toId,
                System.currentTimeMillis() / 1000,
                1
            )

            val payload = JsonObject()
            payload.addProperty("to", user.token_key)

            val data = JsonObject()
            data.addProperty("title", "Thông báo")
            data.addProperty("body", user.username+": "+text)

            payload.add("notification", data)
            sendNotification(payload)


            fromReference.setValue(chatMessage)
                .addOnCompleteListener {
                    Log.d(TAG, "Save our chat message: ${fromReference.key}")
                    edittext_chat_log.text.clear()
                    recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                }
            toReference.setValue(chatMessage)
                .addOnCompleteListener {
                    Log.d(TAG, "Save our chat message: ${toReference.key}")
                    edittext_chat_log.text.clear()
                    recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                }
            val latestMessageFromRef = FirebaseDatabase.getInstance().getReference("latest-messages/$fromId/$toId")
            latestMessageFromRef.setValue(chatMessage)

            val latestMessageToRef = FirebaseDatabase.getInstance().getReference("latest-messages/$toId/$fromId")
            latestMessageToRef.setValue(chatMessage)

        }else if (type == 2){
            var ref: StorageReference = storageRef.child(
                "image_message/" + UUID.randomUUID().toString()
            )
            ref.putFile(filePath!!)
                .addOnSuccessListener {

                    ref.downloadUrl.addOnSuccessListener {
                        filename = it.toString()
                        val text = it.toString()

                        if(fromId == null) return@addOnSuccessListener

                        val chatMessage = ChatMessage(
                            fromReference.key!!,
                            text,
                            fromId,
                            toId,
                            System.currentTimeMillis() / 1000,
                            2
                        )

                        val payload = JsonObject()
                        payload.addProperty("to", user.token_key)

                        val data = JsonObject()
                        data.addProperty("title", "Thông báo")
                        data.addProperty("body", user.username+": Đã gửi hình ảnh")

                        payload.add("notification", data)
                        sendNotification(payload)
                        fromReference.setValue(chatMessage)
                            .addOnCompleteListener {
                                Log.d(TAG, "Save our chat message: ${fromReference.key}")
                                edittext_chat_log.text.clear()
                                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                                img_selected.visibility = View.GONE
                                send_button_picture.visibility = View.VISIBLE
                                type = 1
                            }
                        toReference.setValue(chatMessage)
                            .addOnCompleteListener {
                                Log.d(TAG, "Save our chat message: ${toReference.key}")
                                edittext_chat_log.text.clear()
                                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                            }
                        val latestMessageFromRef = FirebaseDatabase.getInstance().getReference("latest-messages/$fromId/$toId")
                        latestMessageFromRef.setValue(chatMessage)

                        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("latest-messages/$toId/$fromId")
                        latestMessageToRef.setValue(chatMessage)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(applicationContext, "Failed" + it.message, Toast.LENGTH_SHORT)
                        .show()
                }
        }

    }

    private fun sendNotification(jsonObject: JsonObject){
        apiNotification.sendNotification(jsonObject).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {

            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId){
            R.id.menu_new_call -> {
                //
            }
            R.id.menu_new_callvideo -> {
                //
            }
            R.id.menu_feature -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu_chatlog, menu)
        return super.onCreateOptionsMenu(menu)
    }
}
class ChatFromItem(val text: String, val user: User, val timestamp: Long, val type: Int): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {

        if (type == 1){
            viewHolder.itemView.textview_from_row.visibility = View.VISIBLE
            viewHolder.itemView.img_chat_from.visibility = View.GONE
            viewHolder.itemView.textview_from_row.text = text
        }else if(type == 2){
            viewHolder.itemView.textview_from_row.visibility = View.GONE
            viewHolder.itemView.img_chat_from.visibility = View.VISIBLE
            Picasso.get().load(text).into(viewHolder.itemView.img_chat_from)
        }
        
        viewHolder.itemView.from_msg_time.text = getFormattedTimeChatLog(timestamp)

        //load our user image into the star
        val uri = user.profileImageUrl
        val targetImage = viewHolder.itemView.imageview_chat_from_row
        Picasso.get().load(uri).into(targetImage)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}
class ChatToItem(val text: String, val user: User, val timestamp: Long, val type: Int): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {

        if (type == 1){
            viewHolder.itemView.textview_to_row.visibility = View.VISIBLE
            viewHolder.itemView.img_chat_to.visibility = View.GONE
            viewHolder.itemView.textview_to_row.text = text
        }else if(type == 2){
            viewHolder.itemView.textview_to_row.visibility = View.GONE
            viewHolder.itemView.img_chat_to.visibility = View.VISIBLE
            Picasso.get().load(text).into(viewHolder.itemView.img_chat_to)
        }

        viewHolder.itemView.to_msg_time.text = getFormattedTimeChatLog(timestamp)

        //load our user image into the star
        val uri = user.profileImageUrl
        val targetImage = viewHolder.itemView.imageview_chat_to_row
        Picasso.get().load(uri).into(targetImage)
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}
