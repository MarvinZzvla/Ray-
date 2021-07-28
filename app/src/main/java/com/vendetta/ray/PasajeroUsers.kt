@file:Suppress("SpellCheckingInspection")

package com.vendetta.ray

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_conductor_users.addPasajeroListLayout
import kotlinx.android.synthetic.main.activity_pasajero_users.*

//VARIABLES FOR CHECK TIME AND DETAILS OF CALLBACK FUNCTION
private lateinit var fusedLocationClient : FusedLocationProviderClient
private lateinit var locationRequest : LocationRequest
private lateinit var locationCallback: LocationCallback

//VARIABLES FOR MAKE SENSE APP
private var myCoordenadas = Location("0")
private var list = arrayListOf<DataSnapshot>()
private var isPedir = false
private var startApp = false
private var fisrtTime = true
private var msgUsuarioUbicacion = ""


class PasajeroUsers : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pasajero_users)

        destroyInfo()
    }


    private fun requestBtnFun() {
        if(!startApp){
            startLocationUpdates()
            startApp = true
        }

        if (!isPedir){
            fisrtTime = true
            isPedir = true
            requestBtn.text = "Cancelar"
            startLocationUpdates()

        }else
        {

            isPedir = false
            requestBtn.text = "Pedir"
            stopLocationUpdates()
            destroyInfoNow()
            addPasajeroListLayout.removeAllViewsInLayout()
            list.clear()
        }
    }

    override fun onStart() {
        super.onStart()
        readUsers()

        requestBtn.setOnClickListener {
            if(msgUbicacion.text.isNotEmpty()) {
                msgUbicacion.visibility= View.INVISIBLE
                msgUsuarioUbicacion = msgUbicacion.text.toString()
                requestBtnFun()

            }else{MakeToast("Por favor ingrese detalles de su ubicacion")}
        }

        getLocationUpdates()

        fisrtTime = true
        isPedir = false
        startApp = false


        //Stop updates if the user change activity

    }

    fun removeUser(snap: DataSnapshot){
        for(user in list)
        {
            if(user.key == snap.key)
            {
                list.remove(user)
                myAdd()
                return
            }
        }
    }

    fun readUsers(){
        var auth = Firebase.auth.currentUser?.uid
        Firebase.database.getReference("PasajeroLooking").child(auth.toString()).child("Peticiones").addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(snap: DataSnapshot, previousChildName: String?) {
                 Firebase.database.getReference("MyUsers").child(snap.key.toString()).get().addOnSuccessListener {
                     list.add(it)
                     myAdd()
                 }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
               // TODO("Not yet implemented")
            }

            override fun onChildRemoved(snap: DataSnapshot) {
               removeUser(snap)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
               // TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
               // TODO("Not yet implemented")
            }

        })
    }

    /*
LOAD USERS
Find users around 1km of us
and add it to a list then display it
 */
    fun loadUsers(){
var auth = Firebase.auth.currentUser

        //Get Pasajeros Looking
        Firebase.database.getReference("PasajeroLooking").child(auth?.uid.toString()).child("Peticiones").get().addOnSuccessListener {
            //Si existe
            if (it.exists()) {
                //Obtener de cada usuario su localizacion
                for (user in it.children) {
                    Firebase.database.getReference("ConductorLooking").child(user.key.toString())
                        .get().addOnSuccessListener {
                            var lat = it.child("locationActual").child("latitude").getValue()
                            var long =
                                it.child("locationActual").child("longitude").getValue()

                            var location = Location("0").apply {
                                this.latitude = lat as Double
                                this.longitude = long as Double
                            }
                            //Obtener distancia
                            var distancia = myCoordenadas.distanceTo(location).toInt()

                            //Dectectar usuarios si estan a 1KM de distancia
                            if (distancia <= 1000) {
                              list.add(it)
                            }
                        }
                }
                //Call myAdd
                myAdd()
            }
        }



    }

    /*
    MY ADD
    A cada usuario en la lista obtener su localizacion
    Y mostrarlo en pantalla
    Despues de haber mostrados todos borrar lista y obtener una actualizada
     */
    fun myAdd(){
        addPasajeroListLayout.removeAllViews()
        for (user in list){
            var lat = user.child("Coordenadas").child("latitude").getValue() as Double
            var long = user.child("Coordenadas").child("longitude").getValue() as Double
            var thisLocation = Location("0").apply {this.latitude = lat; this.longitude = long}
            var distance = myCoordenadas.distanceTo(thisLocation).toInt()
            var name = user.child("name").getValue() as String + " " + user.child("apellido").getValue() as String
            var uI = user.key.toString()
            displayUsers(name,distance,uI)


        }
      //  list.clear()

    }

    /*
    DISPLAY USERS
    Crear Design de los usuarios que van a aparecer al usuario

     */

    fun displayUsers(name:String,distancia:Int,identificador:String) {

        println(name + " - " + identificador)
        println(distancia.toString() +" Metros")

        println("Tienes  " + list.size + " Usuario esperando")

        //TODO CREATE Textview para el nombre
        var myName = TextView(this)
        myName.text = name
        addPasajeroListLayout.addView(myName)

        //TODO Create Textview for distance from user
        var myDistancia = TextView(this)
        var distanciaName = "Distancia: " + distancia.toString() + " Metros"
        myDistancia.text = distanciaName
        addPasajeroListLayout.addView(myDistancia)

        //TODO Create a horizontal layout for sort buttons
        var horizontalLayout = LinearLayout(this)
        horizontalLayout.orientation = LinearLayout.HORIZONTAL
        addPasajeroListLayout.addView(horizontalLayout)
        //TODO Create buttons for accept or deny user
        var btn1 = Button(this)
        var btn2 = Button(this)
        btn1.text = "Rechazar"
        btn2.text = "Aceptar"


        btn1.setOnClickListener {
            rechazarFunction(identificador)
        }

        //TODO If user click accepts button
        btn2.setOnClickListener {
                    Firebase.database.getReference("PasajeroLooking").child(Firebase.auth.currentUser?.uid.toString()).child("Peticiones").child(identificador).child("acepto").setValue(true)
                    //STOP REQUEST MAPS UPDATE
                    stopLocationUpdates()
                    //GO TO NEXT ACTIVITY
                    Intent(this, PasajeroMaps::class.java).apply {
                        //Mandar usuario, nombre y distancia
                        this.putExtra("uI", identificador)
                        this.putExtra("name", name)
                        this.putExtra("distancia", distancia)
                        destroyInfoNow()
                        //Start activity
                        startActivity(this)
                    }


        }
        //Agregar botonos al horizontal layout
        horizontalLayout.addView(btn1)
        horizontalLayout.addView(btn2)
    }

    private fun rechazarFunction(identificador: String){
        for(user in list)
        {
            if(user.key.toString().equals(identificador))
            {
                list.remove(user)
                myAdd()
                return
            }
        }
    }

    //DATA USER FORMAT TO SEND TO FIREBASE
    data class DataUser(var name:String, var apellido:String, var locationActual: LatLng,var msg: String)
    /*
    LOAD DATA
    Update localizacion del usuario
    y mandarlo a la lista de ConductorLooking
     */
    fun loadData(lat : Double, long :Double)
    {
        var coordenadas = LatLng(lat,long)
        myCoordenadas.latitude = lat
        myCoordenadas.longitude= long
        val auth = Firebase.auth.currentUser
        Firebase.database.getReference("MyUsers").child(auth?.uid.toString()).child("Coordenadas").setValue(coordenadas)


        Firebase.database.getReference("MyUsers").child(auth?.uid.toString()).get().addOnSuccessListener {
            var name = it.child("name").value.toString()
            var apellido = it.child("apellido").value.toString()
            var lat = it.child("Coordenadas").child("latitude").value as Double
            var long = it.child("Coordenadas").child("longitude").value as Double
            var locationActual = LatLng(lat,long)

            var database = Firebase.database.getReference("PasajeroLooking").child(auth?.uid.toString())

            if(fisrtTime){
                database.setValue(DataUser(name, apellido, locationActual, msgUsuarioUbicacion))
                fisrtTime = false
            } else{
                database.child("locationActual").setValue(locationActual)
            }

        }
    }

    /*
    GET LOCATION UPDATES
    Get location of user and save it on a variable
     */

    fun getLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 3000
        locationRequest.fastestInterval = 3000
        // locationRequest.smallestDisplacement = 170f
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationCallback = object : LocationCallback() {

            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                p0 ?: return

                if (p0.locations.isNotEmpty()) {
                    var location = p0.lastLocation
                   // loadUsers()
                    loadData(location.latitude,location.longitude)

                }
            }
        }
    }

    /*
    START LOCATION UPDATES
    If user allows request maps every 5seg
     */

    fun startLocationUpdates(){


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),44)
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null)


    }
    fun destroyInfo(){ Firebase.database.getReference("PasajeroLooking").child(Firebase.auth?.uid.toString()).apply {this.onDisconnect().removeValue()}}
    fun destroyInfoNow(){ Firebase.database.getReference("PasajeroLooking").child(Firebase.auth?.uid.toString()).apply {this.removeValue()} }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 44 && grantResults.size>0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED){
            startLocationUpdates()
        }else{
            Toast.makeText(this,"Porfavor da permisos a la aplicacion", Toast.LENGTH_LONG).show()}
    }

    fun stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback)

    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        //destroyInfoNow()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        stopLocationUpdates()
        //destroyInfoNow()
    }

    private fun MakeToast(text:String){
        Toast.makeText(this,text, Toast.LENGTH_LONG).show()}


}