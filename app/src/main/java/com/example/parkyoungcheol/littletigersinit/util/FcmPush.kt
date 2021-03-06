package com.example.parkyoungcheol.littletigersinit.util

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import com.example.parkyoungcheol.littletigersinit.Model.*

class FcmPush() {
    val JSON = MediaType.parse("application/json; charset=utf-8")
    val url = "https://fcm.googleapis.com/fcm/send"
    val serverKey = "AAAAqxb--C4:APA91bETqeGKfGxircpXufVj14Tu53q6AbeNaSb6x8akiFgVJ0dVAkHd73q3PTE7c3W7yunJ5-ro_ExyrYeDXkzEaxT8mZiF-CKA2NvFCn-FVkLOlDyTvIaZK2g9_Bp4pDh4XmoRm8CC"

    var okHttpClient: OkHttpClient? = null
    var gson: Gson? = null
    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }
/*    test 제대로된거 올림 */

    fun sendMessage(destinationUid: String, title: String, message: String) {
            FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var token = task.result!!["pushtoken"].toString()
                    println(token)
                    var pushDTO = PushDTO()
                    pushDTO.to = token
                    pushDTO.notification?.title = title
                    pushDTO.notification?.body = message

                    var body = RequestBody.create(JSON, gson?.toJson(pushDTO))
                    var request = Request
                            .Builder()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Authorization", "key=" + serverKey)
                            .url(url)
                            .post(body)
                            .build()
                    okHttpClient?.newCall(request)?.enqueue(object : Callback {
                        override fun onFailure(call: Call?, e: IOException?) {
                        }
                        override fun onResponse(call: Call?, response: Response?) {
                            println(response?.body()?.string())
                        }
                    })
                }
            }
    }
}