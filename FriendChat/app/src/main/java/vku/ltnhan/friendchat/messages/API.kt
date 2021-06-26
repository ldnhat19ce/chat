package vku.ltnhan.friendchat.messages

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class API {

    lateinit var retrofit: Retrofit

    fun getInstance() : Retrofit{

        retrofit = Retrofit.Builder().baseUrl("https://fcm.googleapis.com/fcm/")
            .addConverterFactory(GsonConverterFactory.create()).build()

        return retrofit
    }
}