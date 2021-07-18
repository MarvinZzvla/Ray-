package com.vendetta.ray.myFolder

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.vendetta.ray.R
import kotlinx.android.synthetic.main.activity_pasajero_latitude.*

class ActivityTest : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pasajero_latitude)

        setComponents()
        //chat()

        //myChat.setOnClickListener {
           
       // }


    }

    private fun setComponents() {
        var database = Firebase.database.getReference("Chat")
        var fotoPerfil = userImage
        var txtMensajes =textSend
        var nombre = nameUser
        var rvMensajes = rvMsg
        var btnEnviar = enviarBtn
        var adapter = AdpaterMensajes(this)
        var l = LinearLayoutManager(this)
        rvMensajes.layoutManager = l
        rvMensajes.adapter = adapter

        btnEnviar.setOnClickListener {
            database.push().setValue(Mensaje(textSend.text.toString(),nombre.text.toString(),"","1","00:00"))
            textSend.setText("")
        }
adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        super.onItemRangeInserted(positionStart, itemCount)
        rvMensajes.scrollToPosition(adapter.itemCount - 1)
    }
})

        database.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                //var m = snapshot.value as Mensaje
               adapter.addMensaje(snapshot)
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


}

