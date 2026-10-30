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

            // Colocar el reproductor otra vez debajo del título.
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
