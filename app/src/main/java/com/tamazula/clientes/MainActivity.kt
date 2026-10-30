
package com.tamazula.clientes

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout

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
    private lateinit var estado: TextView
    private lateinit var botonPantalla: Button
    private lateinit var titulo: TextView

    private var reproductorVista: PlayerView? = null
    private var reproductor: ExoPlayer? = null
    private var pantallaCompleta = false
    private var ultimaCategoria = "Todas"
    private var canalActual: Canal? = null

    private val blanco = Color.WHITE
    private val oscuro = Color.rgb(15, 15, 20)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        prepararPantalla()
        setContentView(fondo)

        token = getSharedPreferences("TV_TAMAZULA", MODE_PRIVATE)
            .getString("token", "") ?: ""

        if (token.isNotEmpty()) {
            cargarPlaylist()
        } else {
            mostrarLogin()
        }
    }

    private fun prepararPantalla(): LinearLayout {
        fondo = LinearLayout(this)
        fondo.orientation = LinearLayout.VERTICAL
        fondo.setPadding(24, 20, 24, 20)
        fondo.setBackgroundColor(oscuro)
        return fondo
    }

    private fun mostrarLogin() {
        liberarReproductor()
        prepararPantalla()

        val tituloLogin = TextView(this)
        tituloLogin.text = "TV TAMAZULA CLIENTES"
        tituloLogin.textSize = 26f
        tituloLogin.setTextColor(blanco)
        tituloLogin.gravity = Gravity.CENTER
        fondo.addView(tituloLogin)

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

                    conexion.disconnect()

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

                    getSharedPreferences("TV_TAMAZULA", MODE_PRIVATE)
                        .edit()
                        .putString("token", token)
                        .apply()

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
        liberarReproductor()
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

                conexion.disconnect()

                if (codigo !in 200..299) {
                    val error = JSONObject(respuesta)
                    throw Exception(
                        error.optString("error", "No se pudo cargar la lista")
                    )
                }

                val playlist = JSONObject(respuesta)
                val tipo = playlist.getString("source_type")
                val contenido = playlist.getString("content")

                val textoM3u = if (tipo == "url") {
                    val descarga = URL(contenido)
                        .openConnection() as HttpURLConnection

                    descarga.connectTimeout = 20000
                    descarga.readTimeout = 30000

                    try {
                        descarga.inputStream.bufferedReader().use {
                            it.readText()
                        }
                    } finally {
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
                        getSharedPreferences("TV_TAMAZULA", MODE_PRIVATE)
                            .edit()
                            .remove("token")
                            .apply()
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
                val grupo = Regex("group-title=\"([^\"]*)\"")
                    .find(linea)?.groupValues?.get(1)

                categoria = grupo?.takeIf {
                    it.isNotBlank()
                } ?: "Otros"

                nombre = linea.substringAfterLast(",")
                    .trim()
                    .ifBlank { "Canal sin nombre" }

            } else if (
                linea.isNotBlank() &&
                !linea.startsWith("#") &&
                nombre.isNotBlank()
            ) {
                resultado.add(Canal(nombre, categoria, linea))
                nombre = ""
            }
        }

        return resultado
    }

    private fun mostrarCanales() {
        if (reproductor == null) {
            reproductor = ExoPlayer.Builder(this).build()

            reproductor?.addListener(
                object : Player.Listener {
                    override fun onPlayerError(
                        error: androidx.media3.common.PlaybackException
                    ) {
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${error.errorCodeName}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        prepararPantalla()

        titulo = TextView(this)
        titulo.text = "TV TAMAZULA"
        titulo.textSize = 24f
        titulo.setTextColor(blanco)
        fondo.addView(titulo)

        val vista = PlayerView(this)
        vista.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(220)
        )
        vista.setBackgroundColor(Color.BLACK)
        vista.useController = true
        vista.controllerAutoShow = true
        vista.controllerShowTimeoutMs = 5000
        vista.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        vista.player = reproductor

        reproductorVista = vista
        fondo.addView(vista)

        botonPantalla = Button(this)
        botonPantalla.text = "⛶ PANTALLA COMPLETA"
        botonPantalla.setOnClickListener {
            cambiarPantallaCompleta()
        }
        fondo.addView(botonPantalla)

        estado = TextView(this)
        estado.text = canalActual?.nombre
            ?: "Selecciona un canal para reproducir"
        estado.textSize = 14f
        estado.setTextColor(blanco)
        estado.setPadding(0, 10, 0, 10)
        fondo.addView(estado)

        val nombres = listOf("Todas") +
            canales.map { it.categoria }.distinct().sorted()

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
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    ultimaCategoria = nombres[position]
                    actualizarCanales(ultimaCategoria)
                }
            }

        lista.setOnItemClickListener { _, _, position, _ ->
            val categoria = categorias.selectedItem?.toString() ?: "Todas"

            val filtrados = if (categoria == "Todas") {
                canales
            } else {
                canales.filter { it.categoria == categoria }
            }

            if (position in filtrados.indices) {
                reproducirCanal(filtrados[position])
            }
        }

        val posicion = nombres.indexOf(ultimaCategoria)
            .coerceAtLeast(0)

        categorias.setSelection(posicion)
        actualizarCanales(ultimaCategoria)

        setContentView(fondo)
    }

    private fun cambiarPantallaCompleta() {
        val vista = reproductorVista ?: return

        if (!pantallaCompleta) {
            val pantalla = FrameLayout(this)
            pantalla.setBackgroundColor(Color.BLACK)

            (vista.parent as? ViewGroup)?.removeView(vista)

            vista.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            pantalla.addView(vista)
            pantallaCompleta = true
            setContentView(pantalla)

            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        } else {
            (vista.parent as? ViewGroup)?.removeView(vista)

            vista.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(220)
            )

            fondo.addView(vista, 1)

            pantallaCompleta = false
            setContentView(fondo)

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE

            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
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

    private fun reproducirCanal(canal: Canal) {
        try {
            val reproductorActual = reproductor ?: return

            canalActual = canal
            estado.text = "Reproduciendo: ${canal.nombre}"

            reproductorActual.setMediaItem(
                MediaItem.fromUri(canal.url)
            )
            reproductorActual.prepare()
            reproductorActual.play()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun liberarReproductor() {
        reproductorVista?.player = null
        reproductorVista = null

        reproductor?.release()
        reproductor = null

        pantallaCompleta = false
        canalActual = null
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (pantallaCompleta) {
            cambiarPantallaCompleta()
        } else {
            super.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        reproductor?.pause()
    }

    override fun onDestroy() {
        liberarReproductor()
        super.onDestroy()
    }
}
