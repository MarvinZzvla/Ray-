package com.vendetta.ray

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.vendetta.ray.databinding.ActivityConductorMapsBinding

class ConductorMaps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityConductorMapsBinding

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationRequest : LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var myCoordenadas = Location("0")



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConductorMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.Cmap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        title = "Conductor"
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLocationUpdates()
        loadData()
        startLocationUpdates()
        destroyInfo()

    }

    data class DataUser(var name:String, var apellido:String, var locationActual:LatLng)

    fun loadData()
    {
        val auth = Firebase.auth.currentUser
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

    fun loadUsers(){
        var list = arrayListOf<LatLng>()
        var n = 0
        Firebase.database.getReference("PasajeroLooking").get().addOnSuccessListener {
            if(it.exists()) {
                for (ds in it.children) {
                    var lat = ds.child("locationActual").child("latitude").getValue()
                    var long = ds.child("locationActual").child("longitude").getValue()

                    var location = Location("0").apply {
                        this.latitude = lat as Double
                        this.longitude = long as Double
                    }
                    var coordenadas = LatLng(lat as Double, long as Double)
                    var distancia = myCoordenadas.distanceTo(location).toString()
                    addUsuario(coordenadas,distancia)

                    println("La distancia es: " + myCoordenadas.distanceTo(location).toInt())


                }
            } else{Toast.makeText(this,"No hay pasajeros disponibles en este momento intente mas tarde",Toast.LENGTH_SHORT).show()}
        }
    }

    fun addUsuario(coordenadas:LatLng, name: String){
        mMap.addMarker(MarkerOptions().position(coordenadas).title(name + " Metros").icon(
            BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_foreground)))

    }

    fun destroyInfo(){ Firebase.database.getReference("ConductorLooking").child(Firebase.auth?.uid.toString()).apply {this.onDisconnect().removeValue()}}
    fun destroyInfoNow(){ Firebase.database.getReference("ConductorLooking").child(Firebase.auth?.uid.toString()).apply {this.removeValue()} }


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
                    loadUsers()

                }
            }
        }
    }


    fun agregarMarcador(lat:Double, long:Double){


        var coordenadas = LatLng(lat,long)
        myCoordenadas.latitude = lat
        myCoordenadas.longitude= long

        var auth = Firebase.auth.currentUser
        var database = Firebase.database.getReference("MyUsers").child(auth?.uid.toString()).child("Coordenadas")
        database.setValue(coordenadas)

        mMap.addMarker(MarkerOptions().position(coordenadas).title("Zavala Aqui").icon(
            BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_foreground)))
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
        }else{Toast.makeText(this,"Porfavor da permisos a la aplicacin",Toast.LENGTH_LONG).show()}
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
}