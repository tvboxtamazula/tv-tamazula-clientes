
package com.tamazula.clientes

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(15, 15, 20)
        window.navigationBarColor = Color.BLACK

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(Color.rgb(15, 15, 20))

        val title = TextView(this)
        title.text = "TV TAMAZULA CLIENTES"
        title.textSize = 25f
        title.setTextColor(Color.WHITE)
        title.setPadding(24, 35, 24, 25)
        layout.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "TV EN VIVO  •  PELÍCULAS  •  SERIES"
        subtitle.textSize = 14f
        subtitle.setTextColor(Color.LTGRAY)
        subtitle.setPadding(24, 10, 24, 30)
        layout.addView(subtitle)

        val message = TextView(this)
        message.text = "Bienvenido a TV TAMAZULA"
        message.textSize = 20f
        message.setTextColor(Color.WHITE)
        message.setPadding(24, 35, 24, 20)
        layout.addView(message)

        val info = TextView(this)
        info.text = "Inicia sesión para disfrutar de tu programación."
        info.textSize = 16f
        info.setTextColor(Color.LTGRAY)
        info.setPadding(24, 10, 24, 20)
        layout.addView(info)

        setContentView(layout)
    }
}
