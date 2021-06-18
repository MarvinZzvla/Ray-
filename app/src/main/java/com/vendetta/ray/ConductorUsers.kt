package com.vendetta.ray

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.widget.*
import androidx.constraintlayout.helper.widget.Layer
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_conductor_users.*
import kotlinx.android.synthetic.main.activity_pasajero_latitude.*
import org.w3c.dom.Text

private lateinit var fusedLocationClient : FusedLocationProviderClient
private lateinit var locationRequest : LocationRequest
private lateinit var locationCallback: LocationCallback

private var myCoordenadas = Location("0")
private var list = arrayListOf<DataSnapshot>()

class ConductorUsers : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conductor_users)


        getLocationUpdates()
        startLocationUpdates()
        destroyInfo()
    }


    fun loadUsers(){
        Firebase.database.getReference("PasajeroLooking").get().addOnSuccessListener {

            if(it.exists()) {
                for (ds in it.children) {
                    var lat = ds.child("locationActual").child("latitude").getValue()
                    var long = ds.child("locationActual").child("longitude").getValue()

                    var location = Location("0").apply {
                        this.latitude = lat as Double
                        this.longitude = long as Double
                    }
                    var distancia = myCoordenadas.distanceTo(location).toInt()

                    //Dectectar usuarios si estan a 1KM de distancia
                    if(distancia <= 8000) {
                        list.add(ds)
                    }
                }
                myAdd(list)

            }
        }


    }

    fun myAdd(user: ArrayList<DataSnapshot>){

        for (user in list){
            var lat = user.child("locationActual").child("latitude").getValue() as Double
            var long = user.child("locationActual").child("longitude").getValue() as Double
            var coordenadas = LatLng(lat,long)
            var thisLocation = Location("0").apply {this.latitude = lat; this.longitude = long}
            var distance = myCoordenadas.distanceTo(thisLocation).toInt()
            var name = user.child("name").getValue() as String + " " + user.child("apellido").getValue() as String
            displayUsers(name,distance)


        }

        list.clear()




    }

     fun displayUsers(name:String,distancia:Int) {

         println(name)
         println(distancia)

         println("ESTA VEZ ES: " + list.size)


             var myName = TextView(this)
             myName.text = name
             addUserListLayout.addView(myName)

             var myDistancia = TextView(this)
             var distanciaName = "Distancia: " + distancia.toString() + " Metros"
             myDistancia.text = distanciaName
             addUserListLayout.addView(myDistancia)


             var horizontalLayout = LinearLayout(this)
             horizontalLayout.orientation = LinearLayout.HORIZONTAL
             addUserListLayout.addView(horizontalLayout)

             var btn1 = Button(this)
             var btn2 = Button(this)
             btn1.text = "Rechazar"
             btn2.text = "Aceptar"
             horizontalLayout.addView(btn1)
             horizontalLayout.addView(btn2)










    }


    data class DataUser(var name:String, var apellido:String, var locationActual: LatLng)

    fun loadData(lat : Double, long :Double)
    {
        var coordenadas = LatLng(lat,long)
        myCoordenadas.latitude = lat
        myCoordenadas.longitude= long
        val auth = Firebase.auth.currentUser
        var database = Firebase.database.getReference("MyUsers").child(auth?.uid.toString()).child("Coordenadas")
        database.setValue(coordenadas)


        Firebase.database.getReference("MyUsers").child(auth?.uid.toString()).get().addOnSuccessListener {
            var name = it.child("name").value.toString()
            var apellido = it.child("apellido").value.toString()
            var lat = it.child("Coordenadas").child("latitude").value as Double
            var long = it.child("Coordenadas").child("longitude").value as Double
            var locationActual = LatLng(lat,long)

            var database = Firebase.database.getReference("ConductorLooking").child(auth?.uid.toString())
            database.setValue(DataUser(name, apellido, locationActual))
        }
    }

    fun getLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 5000
        // locationRequest.smallestDisplacement = 170f
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationCallback = object : LocationCallback() {

            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                p0 ?: return

                if (p0.locations.isNotEmpty()) {
                    var location = p0.lastLocation

                    getLocationUpdates()

                    loadUsers()

                    loadData(location.latitude,location.longitude)
                    addUserListLayout.removeAllViews()
                }
            }
        }
    }

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
            Toast.makeText(this,"Porfavor da permisos a la aplicacion", Toast.LENGTH_LONG).show()}
    }

    fun stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback)

    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        destroyInfoNow()
        Intent(this,ConductorHome::class.java).apply { startActivity(this) }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        stopLocationUpdates()
        destroyInfoNow()
    }


}


