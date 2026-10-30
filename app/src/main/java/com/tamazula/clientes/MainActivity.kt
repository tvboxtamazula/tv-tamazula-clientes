
private fun cambiarPantallaCompleta() {
    val vista = reproductorVista ?: return

    if (!pantallaCompleta) {
        val pantalla = FrameLayout(this)
        pantalla.setBackgroundColor(Color.BLACK)

        // Retirar la vista de su contenedor actual
        (vista.parent as? ViewGroup)?.removeView(vista)

        vista.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        pantalla.addView(vista)

        pantallaCompleta = true
        setContentView(pantalla)

        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

    } else {
        pantallaCompleta = false

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_VISIBLE

        // Regresar la misma vista sin crear otro reproductor
        (vista.parent as? ViewGroup)?.removeView(vista)

        vista.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(220)
        )

        fondo.addView(vista, 1)
        setContentView(fondo)
    }
}
