package vku.ltnhan.friendchat.registerlogin

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.button_login
import kotlinx.android.synthetic.main.activity_login.button_register
import kotlinx.android.synthetic.main.activity_login.loading_view
import kotlinx.android.synthetic.main.activity_register.*
import vku.ltnhan.friendchat.R
import vku.ltnhan.friendchat.messages.LatestMessagesActivity
import vku.ltnhan.friendchat.models.TokenNotification

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Glide.with(this).asGif()
                .load("https://media1.tenor.com/images/1d550cc7494b9ac5a85fbe4f6bc184c8/tenor.gif?itemid=11525834")
                .apply(RequestOptions.circleCropTransform())
                .into(kotlinImageView)


        button_login.setOnClickListener {
            performLogin()
        }

        button_register.setOnClickListener{
            finish()
        }
    }

    private fun performLogin() {
        val email = email_edittext_login.text.toString()
        val password = password_edittext_login.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out email or password.", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener

                Log.d("Login", "Successfully logged in: ${it.result!!.user.uid}")

                val tokenKey = getSharedPreferences("token", Context.MODE_PRIVATE)
                    .getString("token_key", null)
                val userUid = FirebaseAuth.getInstance().uid

                val tokenNotification = TokenNotification()
                tokenNotification.key = tokenKey
                tokenNotification.userUid = userUid

                var firebaseUser: FirebaseUser = FirebaseAuth.getInstance().currentUser!!

                var databaseReference: DatabaseReference =
                    FirebaseDatabase.getInstance().getReference("/users").child(firebaseUser.uid)
                val hashMap:HashMap<String,String> = HashMap()
                hashMap.put("token_key", tokenKey.toString())
                databaseReference.updateChildren(hashMap as Map<String, Any>)
                FirebaseDatabase.getInstance().getReference("/token/$tokenKey")
                    .setValue(tokenNotification)



                val intent = Intent(this, LatestMessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to log in: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        loading_view.visibility = View.VISIBLE
    }


}