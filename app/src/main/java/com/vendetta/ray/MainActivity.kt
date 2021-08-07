package com.vendetta.ray

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


       //Intent(this, ActivityTest::class.java).apply { startActivity(this) }

//        FirebaseApp.initializeApp(/*context=*/ this)
//        val firebaseAppCheck = FirebaseAppCheck.getInstance()
//        firebaseAppCheck.installAppCheckProviderFactory(
//            SafetyNetAppCheckProviderFactory.getInstance())

     //   Cuando click en pasajero por primera vez
        pasajero_btn.setOnClickListener {
            val i = Intent(this, PasajeroLogin::class.java)
            startActivity(i)

        }
        //Click en conductor por primera vez
        conductor_btn.setOnClickListener {
            val i = Intent(this, ConductorLogin::class.java)
            startActivity(i)
        }
    }

    /*
    CHECKEAR
    Verificar la ultima activity de sesion
    *PASAJERO = Home del Pasajero
    *CONDUCTOR = Home del Condunctor
     */
    fun checkSession(){
        //Call prefs for read activity prefers
        val prefs = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        //Save prefs in variables
        var isPasajero = prefs.getBoolean("Pasajero", null == false)
        var isDriver = prefs.getBoolean("Driver", null == false)
        //Check what is the option to display to user
        if (isPasajero){Intent(this,PasajeroHome::class.java).apply {startActivity(this)}}
        else if (isDriver){Intent(this,ConductorHome::class.java).apply { startActivity(this) }}
    }


    override fun onStart() {
        super.onStart()
        //Checkear sesion
        checkSession()
// 1248 520
        val displayMetrics = resources.displayMetrics
        //println("Heigth: ${displayMetrics.heightPixels} Width: ${displayMetrics.widthPixels}")




    }

}