package com.vendetta.ray

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.vendetta.ray.databinding.ActivityPasajeroMapsBinding
import kotlinx.android.synthetic.main.activity_pasajero_latitude.*
import kotlinx.android.synthetic.main.activity_pasajero_maps.*

class PasajeroMaps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityPasajeroMapsBinding

   private lateinit var fusedLocationClient : FusedLocationProviderClient
   private lateinit var locationRequest : LocationRequest
   private lateinit var locationCallback: LocationCallback




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPasajeroMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        title = "Mapa"
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLocationUpdates()
        loadData()
        startLocationUpdates()
        destroyInfo()
        var change:String

    }

data class dataUser(var name:String, var apellido:String, var locationActual:LatLng)

    fun loadData()
    {
        var auth = Firebase.auth.currentUser
    Firebase.database.getReference("MyUsers").child(auth?.uid.toString()).get().addOnSuccessListener {
        var name = it.child("name").value.toString()
        var apellido = it.child("apellido").value.toString()
        var lat = it.child("Coordenadas").child("latitude").value as Double
        var long = it.child("Coordenadas").child("longitude").value as Double
        var locationActual = LatLng(lat,long)

        var database = Firebase.database.getReference("PasajeroLooking").child(auth?.uid.toString())
        database.setValue(dataUser(name, apellido, locationActual))
    }
    }


    fun destroyInfo(){ Firebase.database.getReference("PasajeroLooking").child(Firebase.auth?.uid.toString()).apply {this.onDisconnect().removeValue()}}
    fun destroyInfoNow(){ Firebase.database.getReference("PasajeroLooking").child(Firebase.auth?.uid.toString()).apply {this.removeValue()} }


    @Suppress("DEPRECATION")
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
                    mMap.clear()
                    loadData()
                    agregarMarcador(location.latitude,location.longitude)


                }
            }
        }
    }


    fun agregarMarcador(lat:Double, long:Double){

        println("SE REPITIO")
        var coordenadas = LatLng(lat,long)
        var auth = Firebase.auth.currentUser
        var database = Firebase.database.getReference("MyUsers").child(auth?.uid.toString()).child("Coordenadas")
        database.setValue(coordenadas)

        mMap.addMarker(MarkerOptions().position(coordenadas).title("Zavala Aqui").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_foreground)))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coordenadas,16F))
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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 44 && grantResults.size>0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED){
            startLocationUpdates()
        }
    }


    fun stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback)

    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        destroyInfoNow()
        Intent(this,PasajeroHome::class.java).apply { startActivity(this) }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        stopLocationUpdates()
        destroyInfoNow()
    }

    override fun onMapReady(google: GoogleMap) {
        mMap = google
    }

    fun getAllUsers(){
        var auth = Firebase.auth.currentUser
        var database = Firebase.database.getReference("MyUsers").get().addOnSuccessListener {

        }
    }

}