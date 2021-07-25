package com.vendetta.ray

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.vendetta.ray.databinding.ActivityConductorMapsBinding
import com.vendetta.ray.myFolder.AdpaterMensajes
import com.vendetta.ray.myFolder.Mensaje
import kotlinx.android.synthetic.main.activity_conductor_maps.*
import kotlinx.android.synthetic.main.activity_pasajero_latitude.*
import kotlinx.android.synthetic.main.activity_pasajero_maps.*
import kotlinx.android.synthetic.main.card_view_mensaje.*

class ConductorMaps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityConductorMapsBinding

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationRequest : LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var isFirstTime = true
    private var FirstMsg = true
    var myName = ""
    private var IcancelRide=false;

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

        val prefs = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        myName = prefs.getString("name","Conductor").toString()



        var name = intent.getStringExtra("name")?:"Pasajero"
        nameDriver.text = name
        var identificador = intent.getStringExtra("uI")?:""
       // var distancia = intent.getIntExtra("distancia")

        enableChat() //Enable chat
        setComponents()
        defaultMsgChat()
        getLocationUpdates()
        //loadData()
        startLocationUpdates()
        destroyInfo()



    }

    private fun defaultMsgChat() {
        var database = Firebase.database.getReference("Viajes").child(Firebase.auth.currentUser?.uid.toString()).child("Chat")
        database.push().setValue(Mensaje("Este es el chat de Ray!","Ray!","","1",":D"))
    }

    private fun enableChat() {
        chatBtnDriver.setOnClickListener {
            if(chatLayoutDriver.visibility == View.GONE)
            {
                chatLayoutDriver.visibility = View.VISIBLE
            }
            else if (chatLayoutDriver.visibility == View.VISIBLE){
                chatLayoutDriver.visibility = View.GONE
            }
        }

        closeBtnDriverChat.setOnClickListener {
            if(chatLayoutDriver.visibility == View.GONE)
            {
                chatLayoutDriver.visibility = View.VISIBLE
            }
            else if (chatLayoutDriver.visibility == View.VISIBLE){
                chatLayoutDriver.visibility = View.GONE
            }
        }
    }

    private fun setComponents() {
        var database = Firebase.database.getReference("Viajes").child(Firebase.auth.currentUser?.uid.toString()).child("Chat")
        var fotoPerfil = userImage
        var txtMensajes =textSendDriver
        var nombre = nameDriver
        var rvMensajes = rvChatDriver
        var btnEnviar = sendBtnDriverChat
        var adapter = AdpaterMensajes(this)
        var l = LinearLayoutManager(this)
        rvMensajes.layoutManager = l
        rvMensajes.adapter = adapter

        btnEnviar.setOnClickListener {

            if(txtMensajes.text.isNotEmpty())
            {
                var time = java.util.Calendar.getInstance().time.hours.toString()+":"+java.util.Calendar.getInstance().time.minutes.toString()
            database.push().setValue(Mensaje(txtMensajes.text.toString(),myName,"","1",time))
            txtMensajes.setText("")
            }
            else{
                MakeToast("Escribe algo antes de enviar")
            }
        }
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                rvMensajes.scrollToPosition(adapter.itemCount - 1)
            }
        })

        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                //var m = snapshot.value as Mensaje
                adapter.addMensaje(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

                if(FirstMsg) {
                    FirstMsg = false
                    stopLocationUpdates()
                    if(IcancelRide){
                        MakeToast("Has cancelado el viaje")
                    }
                    else{
                    MakeToast("El usuario ha cancelado el viaje :c")}
                    Intent(
                        applicationContext,
                        ConductorHome::class.java
                    ).apply { startActivity(this) }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })


    }


    override fun onStart() {
        super.onStart()
        cancelBtnDriver.setOnClickListener {
           cancelarRide()
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
            if(isFirstTime){
                isFirstTime = false
                database.setValue(DataUser(name, apellido, locationActual))
            }
            else{

                database.child("locationActual").setValue(locationActual)
            }

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



    fun destroyInfo(){Firebase.database.getReference("Viajes").child(Firebase.auth.currentUser?.uid.toString()).onDisconnect().removeValue()}
    fun destroyInfoNow(){
        var identificador = intent.getStringExtra("uI")?:""
        Firebase.database.getReference("Viajes").child(Firebase.auth.currentUser?.uid.toString()).removeValue()
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
                    //loadData()
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

    fun cancelarRide(){

        AlertDialog.Builder(this).apply {
            this.setTitle("Cancelar Viaje?")
            this.setMessage("Se te aplicara un pequeña tarifa por esta accion.\nEstas seguro?")

            this.setPositiveButton("Si,Cancelar",DialogInterface.OnClickListener { dialog, which ->
               IcancelRide=true;
                stopLocationUpdates()
                destroyInfoNow()
                Intent(applicationContext,ConductorHome::class.java).apply { startActivity(this) }

            })

            this.setNegativeButton("No, Continuar viaje", DialogInterface.OnClickListener { dialog, which ->
                //TODO NO OCURRE NADA
            })
            this.show()
        }
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
        //super.onBackPressed()
if(chatLayoutDriver.visibility == View.VISIBLE)
{
    chatLayoutDriver.visibility = View.GONE
}
        else if(chatLayoutDriver.visibility == View.GONE)
{
            cancelarRide()
}

    }

    private fun MakeToast(text:String){
        Toast.makeText(this,text, Toast.LENGTH_LONG).show()}

    override fun onMapReady(google: GoogleMap) {
        mMap = google
        var thisCoordenadas = LatLng(11.915555,-86.143940)
        google.animateCamera(CameraUpdateFactory.newLatLngZoom(thisCoordenadas,8F))

    }


}