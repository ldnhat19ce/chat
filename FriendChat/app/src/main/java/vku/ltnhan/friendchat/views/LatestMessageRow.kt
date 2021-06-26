package vku.ltnhan.friendchat.views

import android.app.Activity
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.latest_message_row.view.*
import vku.ltnhan.friendchat.R
import vku.ltnhan.friendchat.models.ChatMessage
import vku.ltnhan.friendchat.models.User
import vku.ltnhan.friendchat.utils.DateUtils
import vku.ltnhan.friendchat.utils.DateUtils.getFormattedTime
import java.util.*

fun getFormattedTime(timeInMilis: Long): String {
    val date = Date(timeInMilis * 1000L) // *1000 is to convert seconds to milliseconds

    return when {
        vku.ltnhan.friendchat.utils.DateUtils.isToday(date) -> vku.ltnhan.friendchat.utils.DateUtils.onlyTime.format(date)
        vku.ltnhan.friendchat.utils.DateUtils.isYesterday(date) -> "Yesterday"
        else -> vku.ltnhan.friendchat.utils.DateUtils.onlyDate.format(date)
    }

}
class LatestMessageRow(val chatMessage: ChatMessage, val context : Context): Item<ViewHolder>() {
    var chatPartnerUser: User? = null

    override fun bind(viewHolder: ViewHolder, position: Int) {

        val chatPartnerId: String
        if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
            chatPartnerId = chatMessage.toId

        } else {
            chatPartnerId = chatMessage.fromId

        }

        val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                chatPartnerUser = p0.getValue(User::class.java)

                viewHolder.itemView.username_textview_latest_message.text = chatPartnerUser?.username
                viewHolder.itemView.latest_msg_time.text = DateUtils.getFormattedTime(chatMessage.timestamp)

                val targetImageView = viewHolder.itemView.imageview_latest_message
                Picasso.get().load(chatPartnerUser?.profileImageUrl).into(targetImageView)

                if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                    if (chatMessage.type == 1){
                        viewHolder.itemView.message_textview_latest_message.text = chatMessage.text
                    }else{
                        viewHolder.itemView.message_textview_latest_message.text = "Bạn đã gửi hình ảnh"
                    }
                }else{
                    if (chatMessage.type == 1){
                        viewHolder.itemView.message_textview_latest_message.text = chatPartnerUser?.username+": "+chatMessage.text
                    }else{
                        viewHolder.itemView.message_textview_latest_message.text = chatPartnerUser?.username+": "+"Đã gửi hình ảnh"
                    }
                }
                viewHolder.itemView.imageview_latest_message.setOnClickListener{
                    BigImageDialog.newInstance(chatPartnerUser?.profileImageUrl!!).show((context as Activity).fragmentManager
                        , "")
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }
    override fun getLayout(): Int {
        return R.layout.latest_message_row
    }
}