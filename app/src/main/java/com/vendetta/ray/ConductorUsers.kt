package com.vendetta.ray

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_conductor_users.*


//VARIABLES FOR CHECK TIME AND DETAILS OF CALLBACK FUNCTION
@SuppressLint("StaticFieldLeak")
private lateinit var fusedLocationClient : FusedLocationProviderClient
private lateinit var locationRequest : LocationRequest
private lateinit var locationCallback: LocationCallback

//VARIABLES FOR MAKE SENSE APP
private var myCoordenadas = Location("0")
private var list = arrayListOf<DataSnapshot>()
private var myName=""

class ConductorUsers : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conductor_users)


    }


    override fun onStart() {
        super.onStart()
        //Stop updates if the user change activity
        //Start request map every 5seg around 1KM
        startLocationUpdates()
        //When disconnect stop looking people and destroy info
        destroyInfo()

    }

    private fun MakeToast(text:String){Toast.makeText(this,text,Toast.LENGTH_LONG).show()}

/*
LOAD USERS
Find users around 1km of us
and add it to a list then display it
 */
    fun loadUsers(){
    //Get Pasajeros Looking
        Firebase.database.getReference("PasajeroLooking").get().addOnSuccessListener {
    //Si existe
            if(it.exists()) {
                //Obtener de cada usuario su localizacion
                for (ds in it.children) {
                    var lat = ds.child("locationActual").child("latitude").getValue()
                    var long = ds.child("locationActual").child("longitude").getValue()

                    var location = Location("0").apply {
                        this.latitude = lat as Double
                        this.longitude = long as Double
                    }
                    //Obtener distancia
                    var distancia = myCoordenadas.distanceTo(location).toInt()

                    //Dectectar usuarios si estan a 1KM de distancia
                    if(distancia <= 1000) {
                        list.add(ds)
                    }
                }
                //Call myAdd
                myAdd(list)

            }
        }


    }

    /*
    MY ADD
    A cada usuario en la lista obtener su localizacion
    Y mostrarlo en pantalla
    Despues de haber mostrados todos borrar lista y obtener una actualizada
     */
    fun myAdd(user: ArrayList<DataSnapshot>){

        for (user in list){
            var lat = user.child("locationActual").child("latitude").getValue() as Double
            var long = user.child("locationActual").child("longitude").getValue() as Double
            var coordenadas = LatLng(lat,long)
            var thisLocation = Location("0").apply {this.latitude = lat; this.longitude = long}
            var distance = myCoordenadas.distanceTo(thisLocation).toInt()
            var name = user.child("name").getValue() as String + " " + user.child("apellido").getValue() as String
            var uI = user.key.toString()
            displayUsers(name,distance,uI)

        }
        list.clear()

    }

    /*
    DISPLAY USERS
    Crear Design de los usuarios que van a aparecer al usuario

     */

     fun displayUsers(name:String,distancia:Int,identificador:String) {

            //TODO CREATE Textview para el nombre
             var myName = TextView(this)
             myName.text = name
             addDriverListLayout.addView(myName)

            //TODO Create Textview for distance from user
             var myDistancia = TextView(this)
             var distanciaName = "Distancia: " + distancia.toString() + " Metros"
             myDistancia.text = distanciaName
             addDriverListLayout.addView(myDistancia)

            //TODO Create a horizontal layout for sort buttons
             var horizontalLayout = LinearLayout(this)
             horizontalLayout.orientation = LinearLayout.HORIZONTAL
             addDriverListLayout.addView(horizontalLayout)
            //TODO Create buttons for accept or deny user
             var btn1 = Button(this)
             var btn2 = Button(this)
             btn1.text = "Rechazar"
             btn2.text = "Aceptar"


         //TODO If user click accepts button
            btn2.setOnClickListener {
                aceptarFunction(name,distancia,identificador)

            }
         //Agregar botonos al horizontal layout
             horizontalLayout.addView(btn1)
             horizontalLayout.addView(btn2)
    }

    private fun aceptarFunction(name:String,distancia:Int,identificador:String) {

        Firebase.database.getReference("PasajeroLooking").child(identificador).child("Peticiones").child(Firebase.auth.currentUser?.uid.toString()).setValue(myName)
        Firebase.database.getReference("PasajeroLooking").child(identificador).child("Peticiones").child(Firebase.auth.currentUser?.uid.toString()).onDisconnect().removeValue()
        //GO TO NEXT ACTIVITY
        Intent(this,ConductorMaps::class.java).apply {
            //Mandar usuario, nombre y distancia
            this.putExtra("uI",identificador)
            this.putExtra("name",name)
            this.putExtra("distancia",distancia)
            destroyInfoNow()
            //Start activity
            startActivity(this)
        }
    }

    //DATA USER FORMAT TO SEND TO FIREBASE
    data class DataUser(var name:String, var apellido:String, var locationActual: LatLng)
/*
LOAD DATA
Update localizacion del usuario
y mandarlo a la lista de ConductorLooking
 */
    fun loadData(lat : Double, long :Double)
    {
            var coordenadas = LatLng(lat, long)
            myCoordenadas.latitude = lat
            myCoordenadas.longitude = long
            val auth = Firebase.auth.currentUser
            var database = Firebase.database.getReference("MyUsers").child(auth?.uid.toString())
                .child("Coordenadas")
            database.setValue(coordenadas)


            Firebase.database.getReference("MyUsers").child(auth?.uid.toString()).get()
                .addOnSuccessListener {
                    var name = it.child("name").value.toString()
                    myName = name
                    var apellido = it.child("apellido").value.toString()
                    var lat = it.child("Coordenadas").child("latitude").value as Double
                    var long = it.child("Coordenadas").child("longitude").value as Double
                    var locationActual = LatLng(lat, long)

                    var database = Firebase.database.getReference("ConductorLooking")
                        .child(auth?.uid.toString())
                    database.setValue(DataUser(name, apellido, locationActual))

        }
    }

    private fun isGpsOff(){
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Intent(this,ConductorHome::class.java).apply {
                MakeToast("Enciende tu GPS e intenta nuevamente")
                startActivity(this)

            }
        }
    }


    private fun requestSetting(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 5000
        // locationRequest.smallestDisplacement = 170f
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /*
    GET LOCATION UPDATES
    Get location of user and save it on a variable
     */

    fun getLocationUpdates() {

            locationCallback = object : LocationCallback() {

                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    p0 ?: return
                    if (p0.locations.isNotEmpty()) {
                        var location = p0.lastLocation
                        isGpsOff()
                        loadUsers()
                        loadData(location.latitude, location.longitude)
                        addDriverListLayout.removeAllViews()
                    }

                }

            }
    }

    /*
    START LOCATION UPDATES
    If user allows request maps every 5seg
     */

    fun startLocationUpdates(){
        requestSetting()
        getLocationUpdates()

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
    fun destroyInfo(){ Firebase.database.getReference("ConductorLooking").child(Firebase.auth?.uid.toString()).apply {this.onDisconnect().removeValue()}}
    fun destroyInfoNow(){ Firebase.database.getReference("ConductorLooking").child(Firebase.auth?.uid.toString()).apply {this.removeValue()} }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 44 && grantResults.size>0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED){
            startLocationUpdates()
        }else{
            Toast.makeText(this,"Porfavor activa tu GPS", Toast.LENGTH_SHORT).show()}
    }

    fun stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onPause() {
        super.onPause()
//        Intent(this,ConductorHome::class.java).apply { startActivity(this) }
        stopLocationUpdates()
        destroyInfoNow()

    }


    override fun onBackPressed() {
        super.onBackPressed()
        stopLocationUpdates()
        destroyInfoNow()
    }

}


