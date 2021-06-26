package vku.ltnhan.friendchat.messages

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.user_row_new_message.view.*
import vku.ltnhan.friendchat.R
import vku.ltnhan.friendchat.models.User
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

class ProfileActivity : AppCompatActivity() {

    private lateinit var firebaseUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference

    var filePath: Uri? = null
    var fileCoverPath:Uri? = null

    private val PICK_IMAGE_REQUEST: Int = 2020
    private val PICK_IMAGE_COVER:Int = 9324

    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.title = "Profile User"

        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        databaseReference =
            FirebaseDatabase.getInstance().getReference("/users").child(firebaseUser.uid)

        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                etUserName.setText(user!!.username)
//                et_email.setText(user!!.uid)

                if (user.profileImageUrl == "") {
                    userImage.setImageResource(R.drawable.no_image2)
                } else {
                    Glide.with(this@ProfileActivity).load(user.profileImageUrl).into(userImage)
                }

                if (user.profileCoverUrl.isBlank()){
                    cover_image.setImageResource(R.drawable.ic_lock)
                }else{
                    Glide.with(this@ProfileActivity).load(user.profileCoverUrl).into(cover_image)
                }


            }
        })
        userImage.setOnClickListener {
            chooseImage(PICK_IMAGE_REQUEST)
        }
        cover_image.setOnClickListener{
            chooseImage(PICK_IMAGE_COVER)
        }

        btnSave.setOnClickListener {
            uploadImage()
            progressBar.visibility = View.VISIBLE
        }
    }
    private fun chooseImage(requestCode: Int) {
        val intent: Intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_PICK
        startActivityForResult(Intent.createChooser(intent, "Select Image"), requestCode)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && data != null) {
            filePath = data.data

            Log.d("profile_file_path ", filePath?.path.toString())
            try {
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                userImage.setImageBitmap(bitmap)

                btnSave.visibility = View.VISIBLE
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }else if (requestCode == PICK_IMAGE_COVER && data != null){
            fileCoverPath = data.data
            try {
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, fileCoverPath)
                cover_image.setImageBitmap(bitmap)

                btnSave.visibility = View.VISIBLE
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    private fun uploadImage() {
        var ref: StorageReference = storageRef.child("images/" + UUID.randomUUID().toString())
        var refProfileCover:StorageReference = storageRef.child("images_profile_cover/"+UUID.randomUUID().toString())
        val hashMap:HashMap<String,String> = HashMap()

        if (filePath != null && fileCoverPath == null){
            ref.putFile(filePath!!)
                .addOnSuccessListener {
                    Log.d("file path ", filePath.toString())
                    hashMap.put("username",etUserName.text.toString())
                    ref.downloadUrl.addOnSuccessListener { it1 ->
                        hashMap.put("profileImageUrl",it1.toString())
                        databaseReference.updateChildren(hashMap as Map<String, Any>)
                        progressBar.visibility = View.GONE
                        Toast.makeText(applicationContext, "Uploaded", Toast.LENGTH_SHORT).show()
                        btnSave.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(applicationContext, "Failed" + it.message, Toast.LENGTH_SHORT)
                        .show()
                }
        }else if (filePath == null && fileCoverPath != null){

            refProfileCover.putFile(fileCoverPath!!)
                .addOnSuccessListener {
                    hashMap.put("username",etUserName.text.toString())
                    refProfileCover.downloadUrl.addOnSuccessListener {
                        hashMap.put("profileCoverUrl", it.toString())
                        databaseReference.updateChildren(hashMap as Map<String, Any>)
                        progressBar.visibility = View.GONE
                        Toast.makeText(applicationContext, "Uploaded", Toast.LENGTH_SHORT).show()
                        btnSave.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(applicationContext, "Failed" + it.message, Toast.LENGTH_SHORT)
                        .show()
                }
        }else{
            ref.putFile(filePath!!)
                .addOnSuccessListener {
                    Log.d("file path ", filePath.toString())
                    hashMap.put("username",etUserName.text.toString())
                    ref.downloadUrl.addOnSuccessListener { it1 ->
                        hashMap.put("profileImageUrl",it1.toString())
                        refProfileCover.putFile(fileCoverPath!!)
                            .addOnSuccessListener {
                                refProfileCover.downloadUrl.addOnSuccessListener {
                                    hashMap.put("profileCoverUrl", it.toString())
                                    databaseReference.updateChildren(hashMap as Map<String, Any>)
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(applicationContext, "Uploaded", Toast.LENGTH_SHORT).show()
                                    btnSave.visibility = View.GONE
                                }
                            }
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(applicationContext, "Failed" + it.message, Toast.LENGTH_SHORT)
                        .show()
                }
        }

    }

}