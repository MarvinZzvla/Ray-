package com.vendetta.ray

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_pasajero_latitude.*

class ActivityTest : AppCompatActivity() {
    lateinit var myRequest : LocationRequest
    lateinit var myCall : LocationCallback
    lateinit var client : FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pasajero_latitude)

        configRequest()


        stopUpdates.setOnClickListener {
            stopRequestUpdates()

        }

    }

    fun stopRequestUpdates()
    {
        client.removeLocationUpdates(myCall)
    }
    fun requestUpdates(){
        myCallBack()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

        }
        client.requestLocationUpdates(myRequest, myCall, null)
    }

    fun myCallBack(){

         myCall = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                p0 ?: return
                if (p0.locations.isNotEmpty()) {
                    println("Aqui o zavala ")
                }
            }
        }
    }

    fun configRequest(){
        client = FusedLocationProviderClient(this)
        myRequest = LocationRequest().apply {
            interval = 5000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        requestUpdates()

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        stopRequestUpdates()
    }
}

