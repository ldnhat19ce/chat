package vku.ltnhan.friendchat.messages

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface APINotification {


    @Headers("Authorization: key=AAAANMxfUYU:APA91bEljZoQqUQfhKiBaClktKi0u8-oXbrRam3j40Z92zkIYwosV4QdUYSsCTtJl-Zw0vD1vrfOfnrkDB4EIDRUBYJiybRMdMozyUvql5v6NPo4Xz3VnW2nDW-nE8csJGyb6Yqex5yF",
        "Content-type: application/json"
        )
    @POST("send")
    fun sendNotification(@Body jsonObject: JsonObject) : Call<Void>
}