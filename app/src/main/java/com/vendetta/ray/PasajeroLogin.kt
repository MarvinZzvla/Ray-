package com.vendetta.ray

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_pasajero_login.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class PasajeroLogin : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pasajero_login)

        title = "Pasajero"

        showPasswordPasajero.setOnClickListener {
            var type = passwordPasajero.typeface
            if (passwordPasajero.inputType.equals(129)){passwordPasajero.inputType = 1}
            else{passwordPasajero.inputType = 129}
            passwordPasajero.typeface = type

        }

        //Boton Registrarse
       PasajeroRegistrar_btn.setOnClickListener {
            val registerIntent = Intent(this,RegistrarPasajero::class.java)
            startActivity(registerIntent)
        }
         //Iniciando FireBase
        auth = Firebase.auth
        loginPasajero.setOnClickListener { SignInUser() }
    }

    /*
    INICIAR SESION CON EL USUARIO
    Los campos deben estar llenos de email y password
    Debe siempre haber conexion a internet
     */

    fun SignInUser() {

        //Guardar variables de email y password
        var email = emailPasajero.text.toString()
        var password = passwordPasajero.text.toString()

            /*Si la contraseña y el email estan llenos iniciar codigo
              Si es un email valido y una contraseña valida de al menos 6 caracteres
              incluyendo numeros,mayusculas, y algunos simbolos como(!@#$)
             */
        if (email.isNotEmpty() && password.isNotEmpty() && email.isEmailValid() && password.isValidPassword())  {
            //Iniciar sesion en FIREBASE
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    //Si la conexion fue exitosa
                    if (it.isSuccessful) {
                        //Si la conexion no fue exitosa porque:
                        saveDataBase()
                    } else {
                        //Los datos no son correctos
                        if(isOnlineNet())
                        Toast.makeText(applicationContext, "La contraseña o email no estan correctos", Toast.LENGTH_LONG).show()
                        //Falta de internet
                        else{makeToast("Verifica tu conexion de internet")}
                    }
                }
        }//FIN DEL IF
        //Los datos estan vacios
        else {
            Toast.makeText(applicationContext, "Por favor ingresa tus datos correctamente", Toast.LENGTH_LONG).show()
            println("Estan vacios los campos")

        }//FIN DEL ELSE
    }//FIN DE LA FUNCION


    //Verificar si ya hay un usuario registrado y ingresar
    public override fun onStart() {
        super.onStart()
        var currentUser = auth.currentUser
        if(currentUser != null){
            saveDataBase()
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

    //DISPLAY A TOAST
    fun makeToast(text: String){

        Toast.makeText(this,text,Toast.LENGTH_LONG).show();
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


    //Call Activity
    fun saveDataBase(){
        //Date in format dd/mm/yy at: hr:min:sec
       val date = java.util.Calendar.getInstance().time.date.toString()+"/"+
               (java.util.Calendar.getInstance().time.month + 1).toString()+"/"+
               (java.util.Calendar.getInstance().time.year + 1900).toString()+" - at: "+
               java.util.Calendar.getInstance().time.hours.toString()+":"+java.util.Calendar.getInstance().time.minutes.toString()+":"+
               java.util.Calendar.getInstance().time.seconds.toString()


        //Inciando Firebase y registrando la conexion
        database = Firebase.database.reference
        database.child("MyUsers").child(auth.uid.toString()).child("Last_Login").setValue(date)

        //SAVE STATE OF APLICATION FOR DISPLAY PASAJERO HOME TO USER
        val prefs = getSharedPreferences("myPrefs", Context.MODE_PRIVATE).edit()
        prefs.putBoolean("Driver",false)
        prefs.putBoolean("Pasajero",true)
        //Apply preferences
        prefs.apply()

        //CALL ACTIVITY HOME PASAJERO
        val intent = Intent(this, PasajeroHome::class.java)
        startActivity(intent)

    }
}