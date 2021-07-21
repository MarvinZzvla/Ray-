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
import com.vendetta.ray.databinding.ActivityPasajeroMapsBinding
import com.vendetta.ray.myFolder.AdpaterMensajes
import com.vendetta.ray.myFolder.Mensaje
import kotlinx.android.synthetic.main.activity_conductor_maps.*
import kotlinx.android.synthetic.main.activity_pasajero_latitude.*
import kotlinx.android.synthetic.main.activity_pasajero_maps.*

class PasajeroMaps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityPasajeroMapsBinding

   private lateinit var fusedLocationClient : FusedLocationProviderClient
   private lateinit var locationRequest : LocationRequest
   private lateinit var locationCallback: LocationCallback
    private var myName= ""
    private var identificador =""
    private var isFirstMsg = true

    var myCoordenadas = Location("0")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPasajeroMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.Cmap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        title = "Mapa"
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val prefs = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        myName = prefs.getString("name","Pasajero").toString()

        var name = intent.getStringExtra("name")?:"Condcutor"
        namePasajero.text = name
        identificador = intent.getStringExtra("uI")?:""
        // var distancia = intent.getIntExtra("distancia")


        enableChat()//Activar chat
        setComponents()//Establece las cardview y las obtine del server
        getLocationUpdates()
       // loadData()
        startLocationUpdates()
        destroyInfo()


    }


    /*
    *******ENABLE CHAT******
    Activa el layout del chat y lo desactiva
     */
    private fun enableChat() {
        chatBtnPasajero.setOnClickListener {
            if(chatLayoutPasajero.visibility == View.GONE)
            {
                chatLayoutPasajero.visibility = View.VISIBLE
            }
            else if (chatLayoutPasajero.visibility == View.VISIBLE){
                chatLayoutPasajero.visibility = View.GONE
            }
        }

    closeBtnPasajeroChat.setOnClickListener {
        if(chatLayoutPasajero.visibility == View.GONE)
        {
            chatLayoutPasajero.visibility = View.VISIBLE
        }
        else if (chatLayoutPasajero.visibility == View.VISIBLE){
            chatLayoutPasajero.visibility = View.GONE
        }
    }

    }

    /*
    ON START
     */
    override fun onStart() {
        super.onStart()
        cancelBtnPasajero.setOnClickListener {
            cancelarRide()
        }
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

//        var database = Firebase.database.getReference("PasajeroLooking").child(auth?.uid.toString())
//        database.setValue(dataUser(name, apellido, locationActual))
    }
    }

    fun loadUsers(myLat: Double, myLong: Double) {
        var name = intent.getStringExtra("name") ?: ""
        var identificador = intent.getStringExtra("uI") ?: ""
        var thisCoordenadas = LatLng(myLat,myLong)

        Firebase.database.getReference("MyUsers").child(identificador).child("Coordenadas").get().addOnSuccessListener {

            if (it.exists()) {
                var lat = it.child("latitude").value
                var long = it.child("longitude").value
                var coordenadas = LatLng(lat as Double, long as Double)

                var location = Location("0").apply {
                    this.latitude = lat as Double
                    this.longitude = long as Double
                }
                var distancia = myCoordenadas.distanceTo(location).toInt()
                mMap.clear()

                //Conductor Marcador en el mapa
                mMap.addMarker(MarkerOptions().position(coordenadas).title(" $name $distancia Metros").icon(
                    BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_foreground)))

                //Nosotros marcador en el mapa
                mMap.addMarker(MarkerOptions().position(thisCoordenadas).title("Zavala Aqui").icon(
                    BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_foreground)))

                var zoom = 16.5F
                if(mMap.cameraPosition.zoom >= 16.5F){
                    zoom = mMap.cameraPosition.zoom}
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(thisCoordenadas,zoom))

            }else{MakeToast("Ha ocurrido un error vuelve abrir la aplicacion")}
        }
    }


    fun destroyInfo(){ Firebase.database.getReference("PasajeroLooking").child(Firebase.auth?.uid.toString()).apply {this.onDisconnect().removeValue()}}
    fun destroyInfoNow(){
        Firebase.database.getReference("Viajes").child(identificador).apply {this.removeValue()}
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

//        mMap.addMarker(MarkerOptions().position(coordenadas).title("Zavala Aqui").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_foreground)))
//        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coordenadas,16F))
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

                }

                fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null)


            }

    fun cancelarRide(){
        AlertDialog.Builder(this).apply {
            this.setTitle("Cancelar Viaje?")
            this.setMessage("Se te aplicara un pequeña tarifa por esta accion.\nEstas seguro?")

            this.setPositiveButton("Si,Cancelar", DialogInterface.OnClickListener { dialog, which ->
                //TODO Aqui cuando el usuario acepta cancelar viaje
               stopLocationUpdates()
                destroyInfoNow()
                Intent(applicationContext, PasajeroHome::class.java).apply { startActivity(this) }
            })

            this.setNegativeButton("No, Continuar viaje", DialogInterface.OnClickListener { dialog, which ->
                //TODO NO OCURRE NADA
            })
            this.show()
        }

    }

    private fun setComponents() {
        var database = Firebase.database.getReference("Viajes").child(identificador).child("Chat")
        var fotoPerfil = userImage
        var txtMensajes =textSendPasajero
        var nombre = namePasajero
        var rvMensajes = rvChatPasajero
        var btnEnviar = sendBtnPasajeroChat
        var adapter = AdpaterMensajes(this)
        var l = LinearLayoutManager(this)
        rvMensajes.layoutManager = l
        rvMensajes.adapter = adapter

        btnEnviar.setOnClickListener {
            var time = java.util.Calendar.getInstance().time.hours.toString()+":"+java.util.Calendar.getInstance().time.minutes.toString()
            database.push().setValue(Mensaje(txtMensajes.text.toString(),myName,"","1",time))
            txtMensajes.setText("")
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
                if(isFirstMsg) {
                    stopLocationUpdates()
                    MakeToast("El conductor ha cancelado el viaje :c")
                    Intent(
                        applicationContext,
                        PasajeroHome::class.java
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
        //stopLocationUpdates()
       // destroyInfoNow()


    }

    override fun onBackPressed() {
        //super.onBackPressed()
       if(chatLayoutPasajero.visibility == View.VISIBLE)
       {
           chatLayoutPasajero.visibility = View.GONE
       }
        else if(chatLayoutPasajero.visibility == View.GONE){
            cancelarRide()
       }
    }

    override fun onMapReady(google: GoogleMap) {
        mMap = google
        var thisCoordenadas = LatLng(11.915555,-86.143940)
        google.animateCamera(CameraUpdateFactory.newLatLngZoom(thisCoordenadas,8F))
    }

    private fun MakeToast(text:String){
        Toast.makeText(this,text, Toast.LENGTH_LONG).show()}

}
