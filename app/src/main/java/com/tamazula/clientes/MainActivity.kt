
package com.tamazula.clientes
import android.os.Handler
import android.provider.Settings
import android.os.Looper
import androidx.media3.exoplayer.DefaultRenderersFactory
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
    val url: String,
    val lista: String = ""
)

class MainActivity : Activity() {

    private val servidor =
        "https://tv-tamazula-backend.onrender.com"

    private var token = ""
    private val canales = mutableListOf<Canal>()
    private val canalesMostrados = mutableListOf<Canal>()
    private lateinit var fondo: LinearLayout
    private lateinit var mensaje: TextView
    private lateinit var lista: ListView
    private lateinit var categorias: Spinner
    private lateinit var selectorListas: Spinner
    private var listaActual = ""
    private lateinit var estado: TextView
    private lateinit var botonPantalla: Button
    private lateinit var titulo: TextView

    private var reproductorVista: PlayerView? = null
    private var reproductor: ExoPlayer? = null
    private var pantallaCompleta = false
    private var ultimaCategoria = "Todas"
    private var canalActual: Canal? = null
    private val dispositivoId by lazy {
    Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "dispositivo-desconocido"
}
private val manejadorActividad = Handler(Looper.getMainLooper())

private val tareaActividad = object : Runnable {
    override fun run() {
        if (token.isNotEmpty()) {
            thread {
                try {
                    val conexion = URL("$servidor/api/my-presence")
                        .openConnection() as HttpURLConnection

                    conexion.requestMethod = "POST"
                    conexion.setRequestProperty(
                        "Authorization",
                        "Bearer $token"
                    )
                    conexion.connectTimeout = 10000
                    conexion.readTimeout = 10000

                    conexion.responseCode
                    conexion.disconnect()

                } catch (e: Exception) {
                    android.util.Log.e(
                        "TV_TAMAZULA",
                        "Error al registrar actividad",
                        e
                    )
                }
            }
        }

        manejadorActividad.postDelayed(this, 60000)
    }
}
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
    manejadorActividad.removeCallbacks(tareaActividad)
    manejadorActividad.post(tareaActividad)
    cargarPlaylist()
} else {
    mostrarLogin()
}
    }
override fun onResume() {
    super.onResume()

    if (token.isNotEmpty()) {
        manejadorActividad.removeCallbacks(tareaActividad)
        manejadorActividad.post(tareaActividad)
    }
}

override fun onPause() {
    manejadorActividad.removeCallbacks(tareaActividad)
    super.onPause()
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

    val pantalla = FrameLayout(this)

    // Imagen de fondo
    val imagen = ImageView(this)
    imagen.setImageResource(R.drawable.tv_tamazula_login)
    imagen.scaleType = ImageView.ScaleType.FIT_XY
    pantalla.addView(
        imagen,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    // Campos reales de acceso
    val usuario = EditText(this).apply {
        hint = "Usuario"
        setTextColor(Color.WHITE)
        setHintTextColor(Color.LTGRAY)
        textSize = 18f
        gravity = Gravity.CENTER_VERTICAL
        setSingleLine(true)
        background = null
        setPadding(dp(12), 0, dp(12), 0)
    }

    val clave = EditText(this).apply {
        hint = "Contraseña"
        setTextColor(Color.WHITE)
        setHintTextColor(Color.LTGRAY)
        textSize = 18f
        gravity = Gravity.CENTER_VERTICAL
        inputType = 129
        setSingleLine(true)
        background = null
        setPadding(dp(12), 0, dp(12), 0)
    }

    val boton = Button(this).apply {
        text = "INICIAR SESIÓN"
        textSize = 18f
        setTextColor(Color.WHITE)
        setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(0, 119, 255)
            )
        )
    }

    mensaje = TextView(this).apply {
        setTextColor(Color.WHITE)
        textSize = 14f
        gravity = Gravity.CENTER
    }

    pantalla.addView(usuario)
    pantalla.addView(clave)
    pantalla.addView(boton)
    pantalla.addView(mensaje)

    pantalla.post {
        val ancho = pantalla.width
        val alto = pantalla.height

        fun colocar(
            vista: View,
            x: Float,
            y: Float,
            w: Float,
            h: Float
        ) {
            val params = FrameLayout.LayoutParams(
                (ancho * w).toInt(),
                (alto * h).toInt()
            )
            params.leftMargin = (ancho * x).toInt()
            params.topMargin = (alto * y).toInt()
            vista.layoutParams = params
        }

        colocar(usuario, 0.13f, 0.535f, 0.74f, 0.065f)
        colocar(clave, 0.13f, 0.625f, 0.74f, 0.065f)
        colocar(boton, 0.085f, 0.725f, 0.83f, 0.075f)
        colocar(mensaje, 0.10f, 0.81f, 0.80f, 0.06f)
    }

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
    manejadorActividad.removeCallbacks(tareaActividad)
    manejadorActividad.post(tareaActividad)

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

    setContentView(pantalla)
}


    
private fun cargarPlaylist() {
    liberarReproductor()
    fondo.removeAllViews()

    mensaje = TextView(this)
    mensaje.text = "Cargando tus listas..."
    mensaje.textSize = 18f
    mensaje.setTextColor(blanco)
    fondo.addView(mensaje)

    thread {
        try {
            val conexion = URL("$servidor/api/my-playlists")
                .openConnection() as HttpURLConnection

            conexion.requestMethod = "GET"
            conexion.connectTimeout = 20000
            conexion.readTimeout = 30000
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
                    error.optString(
                        "error",
                        "No se pudieron cargar tus listas"
                    )
                )
            }

            val objeto = JSONObject(respuesta)
            val listasJson = objeto.optJSONArray("playlists")
                ?: throw Exception("El servidor no devolvió las listas")

            if (listasJson.length() == 0) {
                throw Exception("No tienes listas asignadas")
            }

            val todosLosCanales = mutableListOf<Canal>()

            for (i in 0 until listasJson.length()) {
                val playlist = listasJson.getJSONObject(i)

                val nombreLista = playlist.optString(
                    "name",
                    "Lista ${i + 1}"
                )

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

                val canalesLista = leerM3U(textoM3u).map {
                    it.copy(lista = nombreLista)
                }

                todosLosCanales.addAll(canalesLista)
            }

            if (todosLosCanales.isEmpty()) {
                throw Exception("Las listas no contienen canales")
            }

            runOnUiThread {
                canales.clear()
                canales.addAll(todosLosCanales)

                listaActual = canales.first().lista
                ultimaCategoria = "Todas"

                mostrarCanales()
            }

        } catch (e: Exception) {
            runOnUiThread {
                fondo.removeAllViews()

                mensaje.text = e.message
                    ?: "Error al cargar programación"

                val salir = Button(this)
                salir.text = "VOLVER"

                salir.setOnClickListener {
                    cerrarSesion()
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
           val renderersFactory = DefaultRenderersFactory(this)
    .setExtensionRendererMode(
        DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
    )
    .setEnableDecoderFallback(true)

reproductor = ExoPlayer.Builder(this, renderersFactory)
    .setAudioAttributes(
        androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build(),
        true
    )
    .build()

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
        val botonLogout = Button(this)
botonLogout.text = "CERRAR SESIÓN"
botonLogout.textSize = 14f
botonLogout.setTextColor(Color.WHITE)
botonLogout.setBackgroundTintList(
    android.content.res.ColorStateList.valueOf(
        Color.rgb(190, 35, 35)
    )
)

botonLogout.setOnClickListener {
    confirmarCerrarSesion()
}

fondo.addView(botonLogout)
selectorListas = Spinner(this)

val nombresListas = canales
    .map { it.lista }
    .distinct()

selectorListas.adapter = ArrayAdapter(
    this,
    android.R.layout.simple_spinner_dropdown_item,
    nombresListas
)

selectorListas.setSelection(
    nombresListas.indexOf(listaActual).coerceAtLeast(0)
)

fondo.addView(selectorListas)

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
val buscador = EditText(this)
buscador.hint = "Buscar canal..."
buscador.textSize = 16f
buscador.setSingleLine(true)

fondo.addView(
    buscador,
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
)

buscador.addTextChangedListener(
    object : android.text.TextWatcher {

        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {}

        override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {
            actualizarCanales(
                categorias.selectedItem?.toString() ?: "Todas",
                s.toString()
            )
        }

        override fun afterTextChanged(
            s: android.text.Editable?
        ) {}
    }
)
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
        selectorListas.onItemSelectedListener =
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
                    listaActual = nombresListas[position]
                    actualizarCanales(ultimaCategoria)
                }
            }
       lista.setOnItemClickListener { _, _, position, _ ->

    if (position in canalesMostrados.indices) {
        reproducirCanal(canalesMostrados[position])
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

   
private fun actualizarCanales(
    categoria: String,
    busqueda: String = ""
) {
    canalesMostrados.clear()

    canalesMostrados.addAll(
        canales.filter {
            it.lista == listaActual &&
            (categoria == "Todas" || it.categoria == categoria) &&
            it.nombre.contains(
                busqueda.trim(),
                ignoreCase = true
            )
        }
    )

    lista.adapter = ArrayAdapter(
        this,
        android.R.layout.simple_list_item_1,
        canalesMostrados.map { it.nombre }
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
private fun confirmarCerrarSesion() {
    android.app.AlertDialog.Builder(this)
        .setTitle("Cerrar sesión")
        .setMessage("¿Deseas salir de tu cuenta?")
        .setPositiveButton("SÍ, CERRAR SESIÓN") { _, _ ->
            cerrarSesion()
        }
        .setNegativeButton("CANCELAR", null)
        .show()
}

private fun cerrarSesion() {
    manejadorActividad.removeCallbacks(tareaActividad)
    liberarReproductor()

    token = ""
    canales.clear()
    ultimaCategoria = "Todas"

    getSharedPreferences("TV_TAMAZULA", MODE_PRIVATE)
        .edit()
        .remove("token")
        .apply()

    requestedOrientation =
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_VISIBLE

    mostrarLogin()
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
