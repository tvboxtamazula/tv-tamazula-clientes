
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

data class Canal(
    val nombre: String,
    val categoria: String,
    val url: String
)

class MainActivity : Activity() {

    private val servidor =
        "https://tv-tamazula-backend.onrender.com"

    private var token = ""
    private val canales = mutableListOf<Canal>()

    private lateinit var fondo: LinearLayout
    private lateinit var mensaje: TextView
    private lateinit var lista: ListView
    private lateinit var categorias: Spinner

    private val blanco = Color.WHITE
    private val oscuro = Color.rgb(15, 15, 20)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mostrarLogin()
    }

    private fun prepararPantalla(): LinearLayout {
        fondo = LinearLayout(this)
        fondo.orientation = LinearLayout.VERTICAL
        fondo.setPadding(24, 25, 24, 25)
        fondo.setBackgroundColor(oscuro)
        return fondo
    }

    private fun mostrarLogin() {
        prepararPantalla()

        val titulo = TextView(this)
        titulo.text = "TV TAMAZULA CLIENTES"
        titulo.textSize = 26f
        titulo.setTextColor(blanco)
        titulo.gravity = Gravity.CENTER
        fondo.addView(titulo)

        val usuario = EditText(this)
        usuario.hint = "Usuario"
        usuario.setSingleLine(true)
        usuario.setTextColor(blanco)
        usuario.setHintTextColor(Color.LTGRAY)
        fondo.addView(usuario)

        val clave = EditText(this)
        clave.hint = "Contraseña"
        clave.inputType = 129
        clave.setSingleLine(true)
        clave.setTextColor(blanco)
        clave.setHintTextColor(Color.LTGRAY)
        fondo.addView(clave)

        val boton = Button(this)
        boton.text = "INICIAR SESIÓN"
        fondo.addView(boton)

        mensaje = TextView(this)
        mensaje.setTextColor(blanco)
        mensaje.gravity = Gravity.CENTER
        fondo.addView(mensaje)

        boton.setOnClickListener {
            val user = usuario.text.toString().trim()
            val pass = clave.text.toString()

            if (user.isEmpty() || pass.isEmpty()) {
                mensaje.text = "Escribe usuario y contraseña"
                return@setOnClickListener
            }

            boton.isEnabled = false
            mensaje.text = "Verificando cuenta..."

            thread {
                try {
                    val conexion = URL("$servidor/api/login")
                        .openConnection() as HttpURLConnection

                    conexion.requestMethod = "POST"
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
                    val nuevoToken = resultado.optString("token", "")

                    if (codigo !in 200..299 || nuevoToken.isEmpty()) {
                        throw Exception(
                            resultado.optString(
                                "error",
                                "No se pudo iniciar sesión"
                            )
                        )
                    }

                    token = nuevoToken
                    conexion.disconnect()

                    runOnUiThread {
                        cargarPlaylist()
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        boton.isEnabled = true
                        mensaje.text = e.message ?: "Error de conexión"
                    }
                }
            }
        }

        setContentView(fondo)
    }

    private fun cargarPlaylist() {
        fondo.removeAllViews()

        mensaje = TextView(this)
        mensaje.text = "Cargando tu programación..."
        mensaje.textSize = 18f
        mensaje.setTextColor(blanco)
        fondo.addView(mensaje)

        thread {
            try {
                val conexion = URL("$servidor/api/my-playlist")
                    .openConnection() as HttpURLConnection

                conexion.requestMethod = "GET"
                conexion.setRequestProperty(
                    "Authorization", "Bearer $token"
                )

                val codigo = conexion.responseCode
                val flujo = if (codigo in 200..299) {
                    conexion.inputStream
                } else {
                    conexion.errorStream
                }

                val respuesta = flujo.bufferedReader().use {
                    it.readText()
                }

                if (codigo !in 200..299) {
                    val error = JSONObject(respuesta)
                    throw Exception(
                        error.optString("error", "No se pudo cargar la lista")
                    )
                }

                val playlist = JSONObject(respuesta)
                val tipo = playlist.getString("source_type")
                val contenido = playlist.getString("content")

                conexion.disconnect()

                val textoM3u = if (tipo == "url") {
                    val descarga = URL(contenido)
                        .openConnection() as HttpURLConnection

                    descarga.connectTimeout = 20000
                    descarga.readTimeout = 30000

                    descarga.inputStream.bufferedReader().use {
                        it.readText()
                    }.also {
                        descarga.disconnect()
                    }
                } else {
                    contenido
                }

                val resultado = leerM3U(textoM3u)

                runOnUiThread {
                    canales.clear()
                    canales.addAll(resultado)
                    mostrarCanales()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    fondo.removeAllViews()
                    mensaje.text = e.message ?: "Error al cargar programación"
                    val salir = Button(this)
                    salir.text = "VOLVER"
                    salir.setOnClickListener {
                        token = ""
                        mostrarLogin()
                    }
                    fondo.addView(mensaje)
                    fondo.addView(salir)
                }
            }
        }
    }

    private fun leerM3U(texto: String): List<Canal> {
        val resultado = mutableListOf<Canal>()
        val lineas = texto.lines()

        var nombre = ""
        var categoria = "Otros"

        for (lineaOriginal in lineas) {
            val linea = lineaOriginal.trim()

            if (linea.startsWith("#EXTINF", ignoreCase = true)) {
                val grupo = Regex("""group-title="([^"]*)"""")
                    .find(linea)?.groupValues?.get(1)

                categoria = grupo?.takeIf { it.isNotBlank() } ?: "Otros"

                nombre = linea.substringAfterLast(",")
                    .trim()
                    .ifBlank { "Canal sin nombre" }

            } else if (
                linea.isNotBlank() &&
                !linea.startsWith("#") &&
                nombre.isNotBlank()
            ) {
                resultado.add(
                    Canal(nombre, categoria, linea)
                )
                nombre = ""
            }
        }

        return resultado
    }

    private fun mostrarCanales() {
        fondo.removeAllViews()

        val titulo = TextView(this)
        titulo.text = "TV TAMAZULA"
        titulo.textSize = 24f
        titulo.setTextColor(blanco)
        fondo.addView(titulo)

        val nombres = (listOf("Todas") +
            canales.map { it.categoria }.distinct().sorted())

        categorias = Spinner(this)
        categorias.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            nombres
        )
        fondo.addView(categorias)

        lista = ListView(this)
        fondo.addView(
            lista,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        categorias.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    actualizarCanales(nombres[position])
                }
            }

        actualizarCanales("Todas")
    }

    private fun actualizarCanales(categoria: String) {
        val filtrados = if (categoria == "Todas") {
            canales
        } else {
            canales.filter { it.categoria == categoria }
        }

        lista.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            filtrados.map { it.nombre }
        )
    }
}
