package com.vendetta.ray

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_pasajero_latitude.*

class ActivityTest : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pasajero_latitude)

        //TODO VAMOS A DARLE

        var list = arrayListOf<String>()
        list.add("Marvin is here")
        list.add("Haziel is here")
        list.add("Zavala is here")
        list.add("Sanchez is here")

        for (myList in list) {
            var dynamicTextview = TextView(this)
            dynamicTextview.text = myList.toString()
            addUsersLayout.addView(dynamicTextview)
        }


// add TextView to LinearLayout


    }
}

