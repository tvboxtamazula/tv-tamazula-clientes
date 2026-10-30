
package com.tamazula.clientes

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val servidor =
        "https://tv-tamazula-backend.onrender.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(15, 15, 20)
        window.navigationBarColor = Color.BLACK

        val fondo = LinearLayout(this)
        fondo.orientation = LinearLayout.VERTICAL
        fondo.gravity = Gravity.CENTER
        fondo.setPadding(30, 30, 30, 30)
        fondo.setBackgroundColor(Color.rgb(15, 15, 20))

        val titulo = TextView(this)
        titulo.text = "TV TAMAZULA"
        titulo.textSize = 30f
        titulo.setTextColor(Color.WHITE)
        titulo.gravity = Gravity.CENTER
        fondo.addView(titulo)

        val subtitulo = TextView(this)
        subtitulo.text = "CLIENTES"
        subtitulo.textSize = 18f
        subtitulo.setTextColor(Color.LTGRAY)
        subtitulo.gravity = Gravity.CENTER
        fondo.addView(subtitulo)

        val usuario = EditText(this)
        usuario.hint = "Usuario"
        usuario.setSingleLine(true)
        usuario.setTextColor(Color.WHITE)
        usuario.setHintTextColor(Color.LTGRAY)
        fondo.addView(usuario)

        val clave = EditText(this)
        clave.hint = "Contraseña"
        clave.setSingleLine(true)
        clave.inputType = 129
        clave.setTextColor(Color.WHITE)
        clave.setHintTextColor(Color.LTGRAY)
        fondo.addView(clave)

        val boton = Button(this)
        boton.text = "INICIAR SESIÓN"
        fondo.addView(boton)

        val mensaje = TextView(this)
        mensaje.text = ""
        mensaje.textSize = 15f
        mensaje.setTextColor(Color.WHITE)
        mensaje.gravity = Gravity.CENTER
        fondo.addView(mensaje)

        boton.setOnClickListener {
            val user = usuario.text.toString().trim()
            val pass = clave.text.toString()

            if (user.isEmpty() || pass.isEmpty()) {
                mensaje.text = "Escribe tu usuario y contraseña"
                return@setOnClickListener
            }

            boton.isEnabled = false
            mensaje.text = "Verificando cuenta..."

            thread {
                try {
                    val conexion = URL("$servidor/api/login")
                        .openConnection() as HttpURLConnection

                    conexion.requestMethod = "POST"
                    conexion.connectTimeout = 15000
                    conexion.readTimeout = 15000
                    conexion.doOutput = true
                    conexion.setRequestProperty(
                        "Content-Type", "application/json"
                    )

                    val datos = JSONObject()
                    datos.put("username", user)
                    datos.put("password", pass)

                    conexion.outputStream.use {
                        it.write(datos.toString().toByteArray(Charsets.UTF_8))
                    }

                    val codigo = conexion.responseCode
                    val flujo = if (codigo in 200..299) {
                        conexion.inputStream
                    } else {
                        conexion.errorStream
                    }

                    val respuesta = flujo.bufferedReader().use {
                        it.readText()
                    }

                    val resultado = JSONObject(respuesta)
                    val token = resultado.optString("token", "")

                    runOnUiThread {
                        boton.isEnabled = true

                        if (codigo in 200..299 && token.isNotEmpty()) {
                            mensaje.text = "¡Acceso autorizado!"
                        } else {
                            mensaje.text = resultado.optString(
                                "error",
                                "Usuario o contraseña incorrectos"
                            )
                        }
                    }

                    conexion.disconnect()

                } catch (e: Exception) {
                    runOnUiThread {
                        boton.isEnabled = true
                        mensaje.text = "No se pudo conectar al servidor"
                    }
                }
            }
        }

        setContentView(fondo)
    }
}
