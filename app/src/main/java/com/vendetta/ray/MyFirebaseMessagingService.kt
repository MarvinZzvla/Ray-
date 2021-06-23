package com.vendetta.ray

import android.os.Looper
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService:FirebaseMessagingService() {
    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)
        Looper.prepare()
        Toast.makeText(applicationContext,msg.notification?.title,Toast.LENGTH_SHORT).show()
    }
}