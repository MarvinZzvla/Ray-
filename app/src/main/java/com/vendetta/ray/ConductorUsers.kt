package com.vendetta.ray

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_conductor_users.*
import java.util.ArrayList
import com.google.firebase.database.ValueEventListener as ValueEventListener1


/*
ESTA ES UNA PRUEBA TEST QUE DEBERIA APARECER SOLO EN ESTA BRANCH
 */

//VARIABLES FOR CHECK TIME AND DETAILS OF CALLBACK FUNCTION
@SuppressLint("StaticFieldLeak")
private lateinit var fusedLocationClient : FusedLocationProviderClient
private lateinit var locationRequest : LocationRequest
private lateinit var locationCallback: LocationCallback

//VARIABLES FOR MAKE SENSE APP
private var myCoordenadas = Location("0")
private var myList: ArrayList<DataSnapshot> = arrayListOf<DataSnapshot>()
private var myName=""

private lateinit var mListener: ValueEventListener1
private lateinit var dBase:DatabaseReference

class ConductorUsers : AppCompatActivity() {

    /*
    ON CREATE
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conductor_users)

    }

    /*
    ON START

     */
    override fun onStart() {
        super.onStart()
        myList.clear()
        //Start request map every 5seg around 1KM
        startLocationUpdates()
        //When disconnect stop looking people and destroy info
        destroyInfo()

    }

    /*
    MAKE TOAST
    Create a toast
    Duration Long
     */
    private fun MakeToast(text:String){Toast.makeText(this,text,Toast.LENGTH_LONG).show()}

/*
REMOVEUSER
Quitar usuario de la lista a ser mostrada
 */
    fun removeUser(snap: DataSnapshot) {

    if(myList.isNotEmpty())
    {
    for(user in myList)
        {
            if(user.key.equals(snap.key))
            {
                println("BYE $user")
                myList.remove(user)
                addDriverListLayout.removeAllViews()
                myAdd()
                return
            }
        }
    }

    }

    /*
***********  READ USERS **********
Find users around 1km of us
and add it to a list then display it
 */

    fun readUsers(){
        Firebase.database.getReference("PasajeroLooking").addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snap: DataSnapshot, previousChildName: String?) {


                var lat = snap.child("locationActual").child("latitude").value as Double
                var long = snap.child("locationActual").child("longitude").value as Double
                var location = Location("0").apply {
                    this.latitude = lat
                    this.longitude = long
                }
                var distancia = myCoordenadas.distanceTo(location).toInt()

                if(distancia<1000)
                {
                  myList.add(snap)
                    myAdd()
                }

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snap: DataSnapshot) {

                removeUser(snap)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
//                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
//                TODO("Not yet implemented")
            }

        })
    }


    /*
    MY ADD
    A cada usuario en la lista obtener su localizacion
    Y mostrarlo en pantalla
    Despues de haber mostrados todos borrar lista y obtener una actualizada
     */
    fun myAdd() {

        addDriverListLayout.removeAllViews()
        for (user in myList){
            var status = true
//            println("Usuario $user y el tamaño ${list.size}")
            if(user.hasChild("Peticiones"))
            {
               status = false
            }

            var msg = user.child("msg").value as String
            var lat = user.child("locationActual").child("latitude").value as Double
            var long = user.child("locationActual").child("longitude").value as Double
            var thisLocation = Location("0").apply {this.latitude = lat; this.longitude = long}
            var distance = myCoordenadas.distanceTo(thisLocation).toInt()
            var name = user.child("name").value as String + " " + user.child("apellido").value as String
            var uI = user.key.toString()
            displayUsers(name,distance,uI,status,msg)

        }

    }

    /*
    DISPLAY USERS
    Crear Design de los usuarios que van a aparecer al usuario
     */
     fun displayUsers(name: String, distancia: Int, identificador: String, status: Boolean,msg: String) {
            //TODO CREATE Textview para el nombre
             var myName = TextView(this)
             myName.text = name
             addDriverListLayout.addView(myName)

            //TODO Create Textview para la ubicacion
            var myMsg = TextView(this)
            myMsg.text = "Localizacion: " + msg
            addDriverListLayout.addView(myMsg)

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
             if(!status){
                 btn2.text = "...Esperando"
             }


        btn1.setOnClickListener { rechazarFunction(identificador) }


         //TODO If user click accepts button
            btn2.setOnClickListener {
                if(status && btn2.text == "Aceptar")
                {
                    btn2.text = "...Esperando"
                    aceptarFunction(name,distancia,identificador)
                }else{
                    MakeToast("Aguarde porfavor")
                }
            }

         //Agregar botonos al horizontal layout
             horizontalLayout.addView(btn1)
             horizontalLayout.addView(btn2)

    }

    private fun rechazarFunction(identificador: String) {

            Firebase.database.getReference("PasajeroLooking").child(identificador)
                .child("Peticiones").child(Firebase.auth.currentUser?.uid.toString())
                .removeValue()

        for(user in myList)
        {

            if(user.key.equals(identificador))
            {
                myList.remove(user)
                myAdd()
                return
            }

        }

    }

    private fun aceptarFunction(name:String,distancia:Int,identificador:String) {

        dBase = Firebase.database.getReference("PasajeroLooking").child(identificador).child("Peticiones").child(Firebase.auth.currentUser?.uid.toString()).child("acepto")

        Firebase.database.getReference("PasajeroLooking").child(identificador).child("Peticiones").child(Firebase.auth.currentUser?.uid.toString()).child("acepto").setValue(false)
        Firebase.database.getReference("PasajeroLooking").child(identificador).child("Peticiones").child(Firebase.auth.currentUser?.uid.toString()).onDisconnect().removeValue()
        //GO TO NEXT ACTIVITY

    mListener = dBase.addValueEventListener(object :
         ValueEventListener1 {
            override fun onDataChange(aceptar: DataSnapshot) {
                if(aceptar.value == true)
                {
                        Intent(applicationContext,ConductorMaps::class.java).apply {
                        //Mandar usuario, nombre y distancia
                        this.putExtra("uI",identificador)
                        this.putExtra("name",name)
                        this.putExtra("distancia",distancia)
                            destroyInfoNow()

                            //Start activity
                            startActivity(this)
                            finish()
                }
                    }
            }

            override fun onCancelled(error: DatabaseError) {
//                TODO("Not yet implemented")
            }

        })

    }

    //DATA USER FORMAT TO SEND TO FIREBASE
    data class DataUser(var name:String, var apellido:String, var locationActual: LatLng)
/*
LOAD DATA
Update localizacion del usuario
y mandarlo a la lista de ConductorLooking
 */
    fun loadData(lat: Double, long: Double, firstTime: Boolean)
    {
            var coordenadas = LatLng(lat, long)
            myCoordenadas.latitude = lat
            myCoordenadas.longitude = long
        if(!firstTime)
        {
            readUsers()
        }
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
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 1000
        // locationRequest.smallestDisplacement = 170f
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /*
    GET LOCATION UPDATES
    Get location of user and save it on a variable
     */

    fun getLocationUpdates() {
        var firstTime = false
            locationCallback = object : LocationCallback() {

                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    if (p0.locations.isNotEmpty()) {
                        var location = p0.lastLocation
                        isGpsOff()
                       // loadUsers()
                        loadData(location.latitude, location.longitude,firstTime)
                        firstTime = true
                       // addDriverListLayout.removeAllViews()
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
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),44)
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null)


    }
    fun destroyInfo(){ Firebase.database.getReference("ConductorLooking").child(Firebase.auth.uid.toString()).apply {this.onDisconnect().removeValue()}}
    fun destroyInfoNow(){ Firebase.database.getReference("ConductorLooking").child(Firebase.auth.uid.toString()).apply {this.removeValue()} }

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
//      Intent(this,ConductorHome::class.java).apply { startActivity(this) }
        stopLocationUpdates()
        //destroyInfoNow()

    }

    override fun onDestroy() {
        super.onDestroy()
        addDriverListLayout.removeAllViews()
        myList.clear()

       if(::mListener.isInitialized) {
           println("IM HERE")
           dBase.removeEventListener(mListener)
       }

    }


    override fun onBackPressed() {
        super.onBackPressed()
        stopLocationUpdates()
        destroyInfoNow()
    }

}
