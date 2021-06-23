package com.vendetta.ray

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_pasajero_home.*

class PasajeroHome : AppCompatActivity() {
    private  lateinit var database: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pasajero_home)
        //Set Tittle to activity
        title="Home"


        //BOTON CERRAR SESION
        logOutButton.setOnClickListener {
            //SignOut de Firebase
            FirebaseAuth.getInstance().signOut()
            //Delete preferences of activity
            val prefs = getSharedPreferences("myPrefs", Context.MODE_PRIVATE).edit()
            prefs.clear()
            //Aplicar cambios
            prefs.apply()
            //Call MAIN
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)

        }

        //Boton guardar
        saveBtn.setOnClickListener{
            val i = Intent(this, PasajeroUsers::class.java)
            startActivity(i)}

    }
    override fun onStart() {
        super.onStart()
        //Save Data
        saveData()

    }
    //Boton atras
    override fun onBackPressed() {
        super.onBackPressed()
        val i = Intent(applicationContext,MainActivity::class.java)
        startActivity(i)
    }

/*
        SAVE DATA AND LAST LOGIN
 */
    fun saveData(){
        //Date in format dd/mm/yy at: hr:min:sec
        val date = java.util.Calendar.getInstance().time.date.toString()+"/"+
                (java.util.Calendar.getInstance().time.month + 1).toString()+"/"+
                (java.util.Calendar.getInstance().time.year + 1900).toString()+" - at: "+
                java.util.Calendar.getInstance().time.hours.toString()+":"+java.util.Calendar.getInstance().time.minutes.toString()+":"+
                java.util.Calendar.getInstance().time.seconds.toString()

        val auth = Firebase.auth
        val database = Firebase.database.reference
        database.child("MyUsers").child(auth.uid.toString()).child("Last_Login").setValue(date)
    }


}