package com.vendetta.ray

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_conductor_home.*

var isDriver = false
class ConductorHome : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conductor_home)
        //Set title to activity
        title = "Home"

        //CERRAR SESION BOTON
        signOut_conductor.setOnClickListener {
            //Cerrar sesion firebase
            FirebaseAuth.getInstance().signOut()
            //DELETE PREFERENCES
            val prefs = getSharedPreferences("myPrefs",Context.MODE_PRIVATE).edit()
            prefs.clear()
            //Apply Changes
            prefs.apply()
            //Call Activity
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)

        }

        mapaBtn.setOnClickListener {
            Intent(this,ConductorUsers::class.java).apply { startActivity(this) }
        }


    }
    override fun onStart() {
        super.onStart()
        //Checkear si es un conductor y guardar last login
        isADriver()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val i = Intent(applicationContext,MainActivity::class.java)
        startActivity(i)
    }
/*
LAST LOGIN SAVE
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
    /*
    CHECKEAR SI ES UN CONDUCTOR LA CUENTA ACTUAL
     */

    fun isADriver() {
        //Set firebase variables
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        //Si el usuario actual existe
        if(currentUser != null){
           //Verficar con firebase si es conductor
            FirebaseDatabase.getInstance().reference.child("MyUsers").child(auth?.uid.toString()).child("driver").get().addOnSuccessListener{

                try {
                    //Save si es condcutor
                    isDriver = it.value as Boolean
                    //Guardar informacion si es conductor
                    if (isDriver){saveData()
                        //Si no es conductor llevar a la pantalla de registrarse
                    }else{Intent(this,RegistrarPasajero::class.java).apply { startActivity(this)}}
                }catch (error:Error){
                    Log.w("Fail",error.cause.toString())}
                //Si falla verificar conexion a internet
            }.addOnFailureListener { makeToast("Verifica tu conexion a internet") }
        }
    }
/*
CREAR UN TOAST PARA MOSTRAR EN PANTALLA DE DURACION GRANDE
 */
    fun makeToast(text: String){

        Toast.makeText(this,text, Toast.LENGTH_LONG).show();
    }


}