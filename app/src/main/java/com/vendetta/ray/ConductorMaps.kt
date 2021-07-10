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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.vendetta.ray.databinding.ActivityConductorMapsBinding
import kotlinx.android.synthetic.main.activity_pasajero_maps.*

class ConductorMaps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityConductorMapsBinding

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationRequest : LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var myCoordenadas = Location("0")
    private var list = arrayListOf<DataSnapshot>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConductorMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.Cmap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        title = "Conductor"
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        var name = intent.getStringExtra("name")?:""
        var identificador = intent.getStringExtra("uI")?:""
       // var distancia = intent.getIntExtra("distancia")

        getLocationUpdates()
        loadData()
        startLocationUpdates()
        destroyInfo()

    }

    override fun onStart() {
        super.onStart()
        cancelBtn.setOnClickListener {
            onBackPressed()
        }
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

            var database = Firebase.database.getReference("Viajes").child(auth?.uid.toString())
            database.setValue(DataUser(name, apellido, locationActual))
        }
    }

    fun loadUsers(myLat:Double,myLong:Double){
        var name = intent.getStringExtra("name")?:""
        var identificador = intent.getStringExtra("uI")?:""
        var thisCoordenadas = LatLng(myLat,myLong)

        Firebase.database.getReference("MyUsers").child(identificador).get().addOnSuccessListener {


                    var lat = it.child("Coordenadas").child("latitude").getValue()
                    var long = it.child("Coordenadas").child("longitude").getValue()
                    var coordenadas = LatLng(lat as Double, long as Double)

                    var location = Location("0").apply {
                        this.latitude = lat as Double
                        this.longitude = long as Double
                    }
                    var distancia = myCoordenadas.distanceTo(location).toInt()
                     mMap.clear()
                    //Pasajero Marca
                mMap.addMarker(MarkerOptions().position(coordenadas).title(" $name $distancia Metros").icon(
                BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_foreground)))
                    //Nosotros marca
                mMap.addMarker(MarkerOptions().position(thisCoordenadas).title("Zavala Aqui").icon(
                BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_foreground)))

                var zoom = 16.5F
                if(mMap.cameraPosition.zoom >= 16.5F){
                zoom = mMap.cameraPosition.zoom}
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(thisCoordenadas,zoom))

        }

    }



    fun destroyInfo(){ Firebase.database.getReference("ConductorLooking").child(Firebase.auth?.uid.toString()).apply {this.onDisconnect().removeValue()}}
    fun destroyInfoNow(){
        var identificador = intent.getStringExtra("uI")?:""
        Firebase.database.getReference("PasajeroLooking").child(identificador).child("Peticiones").child(Firebase.auth?.uid.toString()).apply {this.removeValue()}
    }


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
                    loadUsers(location.latitude,location.longitude)
                    loadData()
                    agregarMarcador(location.latitude,location.longitude)

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

//        mMap.addMarker(MarkerOptions().position(coordenadas).title("Zavala Aqui").icon(
//            BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_foreground)))

//        var zoom = 16.5F
//        if(mMap.cameraPosition.zoom >= 16.5F){
//         zoom = mMap.cameraPosition.zoom}
//        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coordenadas,zoom))


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
        }else{Toast.makeText(this,"Porfavor da permisos a la aplicacion",Toast.LENGTH_LONG).show()}
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
        //stopLocationUpdates()

    }

    override fun onBackPressed() {
        super.onBackPressed()
        //stopLocationUpdates()
        destroyInfoNow()
        Intent(this,ConductorHome::class.java).apply { startActivity(this) }
    }

    override fun onMapReady(google: GoogleMap) {
        mMap = google
        var thisCoordenadas = LatLng(11.915555,-86.143940)
        google.animateCamera(CameraUpdateFactory.newLatLngZoom(thisCoordenadas,8F))

    }


}