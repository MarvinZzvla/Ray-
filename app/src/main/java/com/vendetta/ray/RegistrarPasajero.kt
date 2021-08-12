package com.vendetta.ray

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_registrar_pasajero.*
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class RegistrarPasajero : AppCompatActivity(){
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var registrarOk = true
    private var password: String = ""
    private var email=""
    private var choise = ""
    private var nameReg = ""
    private var apellido=""
    private var telefone= 0
    private var isConductor = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_pasajero)
        title = "Registrar"
        //iniciando
        auth = Firebase.auth

        //crear variable todo ok

      registrarButton.setOnClickListener { registrarUser() }

    }


    override fun onStart() {
        super.onStart()
        readData()
    }

fun readData(){
     val database = Firebase.database
        val list = arrayListOf<String>()

    database.getReference("Cities").addValueEventListener(object :ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            list.clear()
            snapshot.let {
               for (city in it.children){
                   list.add(city.getValue().toString())
               }
           }
            updateAdapter(list)
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }

    })


}


    fun updateAdapter(list: ArrayList<String>){

        val adapter = ArrayAdapter(this,R.layout.spinner_style,list)
        listCiudad.adapter = adapter

        listCiudad.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
               choise = list[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

    }

    fun registrarUser() {


           registrarOk = true
            //save email
            email = emailRegister.text.toString()
            nameReg = nameRegister.text.toString()
            apellido = apellidoRegister.text.toString()
            var temp = phoneRegister.text.toString()
            if(temp.isNotEmpty()){telefone = temp.toInt()}

            isConductor = isDriver.isChecked

            //Si el email y las contraseñas estan correctas y no vacias crear un usuario
            if (CheckCamps()) {
                password = passwordRegister.text.toString()
            } else {
                displayAlert("Error en un campo","Porfavor verifique todos los campos e intente nuevamente")
            }
            if (registrarOk) {
                CrearUsuario()
            }//Fin boton Registrar

    }


    fun CheckCamps() : Boolean{
        return (passwordRegister.text.toString() == confirmarPasswordRegister.text.toString()
                && email.isNotEmpty() && passwordRegister.text.isNotEmpty()
                && email.isEmailValid()&&passwordRegister.text.toString().isValidPassword()
                && choise != ""&& nameRegister.text.isNotEmpty() && apellidoRegister.text.isNotEmpty()
                &&phoneRegister.text.isNotEmpty())

    }

/*
     CREATE ALERT TO VERFY INPUT FIELDS AGAIN
 */
        fun displayAlert(titulo : String, msg:String) {
            //Cada vez que pasa por aqui, no va a ser posible registrarse
            registrarOk = false;
            //Buton Ok y muestra un pequeño toast
            val positiveButtonClick = { dialog: DialogInterface, which: Int ->
                Toast.makeText(applicationContext, "Intenta nuevamente", Toast.LENGTH_LONG).show()
            }

            //Desplega una alerta para el usuario revisar nuevamente
            val alertDialogBuilder = AlertDialog.Builder(this)
            with(alertDialogBuilder) {
                //QUE COLOCAR AQUI DENTRO
                setTitle(titulo)
                setMessage(msg)
                setPositiveButton(
                    "Ok", DialogInterface.OnClickListener(function = positiveButtonClick)
                )
                alertDialogBuilder.show()
            }

        }

    fun CrearUsuario(){
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{
            if (it.isSuccessful){
            val user = auth.currentUser
                CrearBaseDeDatos()
            updateUI(user)
            }else {
                if(!isOnlineNet()){
                Toast.makeText(applicationContext,"Verifica la conexion de Internet",Toast.LENGTH_LONG).show()}
                else{makeToast("El correo ya ha sido registrado")}

            }
        }
    }

    data class userInfo(var email:String,var password:String,var phone:Int,var name:String,var apellido:String,var isDriver:Boolean,var lastLogin:String)


    fun CrearBaseDeDatos(){
        //Date in format dd/mm/yy at: hr:min:sec
        val date = java.util.Calendar.getInstance().time.date.toString()+"/"+
                (java.util.Calendar.getInstance().time.month + 1).toString()+"/"+
                (java.util.Calendar.getInstance().time.year + 1900).toString()+" - at: "+
                java.util.Calendar.getInstance().time.hours.toString()+":"+java.util.Calendar.getInstance().time.minutes.toString()+":"+
                java.util.Calendar.getInstance().time.seconds.toString()

        //Set User DataBase in FireBase
        database = Firebase.database.reference
        database.child("MyUsers").child(auth.uid.toString()).setValue(userInfo(email,password,telefone,nameReg,apellido,isConductor,date))

    }

    private fun updateUI(user: FirebaseUser?){
        println("Usuario Añadido")
        //sendEmailVerification(user)
        val callBack = Intent(this,MainActivity::class.java)
        startActivity(callBack)
    }

    /* Enviar Email para verificar usuario
        Aplicar si necesesario, desactivado por default
     */
    private fun sendEmailVerification(user: FirebaseUser?){

        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener {
                //Email Verification Sent
            }
        }
    }

    /*
   VERIFICAR LA CONEXION A INTERNET
   Esta funcion verifica si existe acesso a internet
   haciendo un ping para un servidor y regresando una respuesta
    */
    fun isOnlineNet(): Boolean {
        try {
            //Ping para google.com
            val p =
                Runtime.getRuntime().exec("ping -c 1 www.google.com")
            //Depende la version de construccion esperar mas o menos tiempo
            val m = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                p.waitFor(500, TimeUnit.MILLISECONDS)
            } else {
                val m = p.waitFor()
            }
            //Para codigo aqui y regresar respuesta si el valor es true
            return m == true

        } catch (e: Exception) {
            // TODO Auto-generated catch block
            return false
            //e.printStackTrace()
        }

        //Si el valor es falso
        return false
    }


    //CHECK IF EMAIL WAS WROTE CORRECT
    fun String.isEmailValid(): Boolean {
        return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

    }

    //CHECK IF PASSWORD AT LEAST 6 CHARACTER INCLUDES LETTERS,NUMBERS,CAPS AND SOME SYMBOLS
    fun String.isValidPassword():Boolean{
        val password = Pattern.compile("[a-zA-Z0-9\\!\\@\\#\\$]{6,24}")
        return !TextUtils.isEmpty(this)&& password.matcher(this).matches()
    }

    fun makeToast(text: String){

        Toast.makeText(this,text,Toast.LENGTH_LONG).show();
    }

}


