package de.galaktogon.gaphomewidget

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : Activity() {
    private val base = "https://tkbopzdurmejcwawtswa.supabase.co"
    private val apiKey = "sb_publishable_NaSGbCf_A8HrrPV6O9D0ig_0tiFPA6D"
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xFF172432.toInt())
        }
        fun label(value: String, size: Float = 16f) = TextView(this).apply {
            text = value; textSize = size; setTextColor(0xFFF2F7FB.toInt())
        }
        layout.addView(label("GALAKTOGON // HOME WIDGET", 21f))
        layout.addView(label("Melde dich mit demselben GAP-Konto an, das deine Aufgaben enthält."))
        email = EditText(this).apply { hint = "E-Mail"; setSingleLine(); inputType = 33 }
        password = EditText(this).apply { hint = "Passwort"; setSingleLine(); inputType = 129 }
        layout.addView(email, ViewGroup.LayoutParams(-1, -2))
        layout.addView(password, ViewGroup.LayoutParams(-1, -2))
        val login = Button(this).apply { text = "Anmelden und Widget verbinden" }
        layout.addView(login)
        val refresh = Button(this).apply { text = "Widget aktualisieren" }
        layout.addView(refresh)
        status = label("Dein Passwort wird nicht gespeichert.", 13f)
        layout.addView(status)
        setContentView(layout)
        login.setOnClickListener { signIn() }
        refresh.setOnClickListener {
            Thread { GapWidgetProvider.runRefresh(this) }.start()
            status.text = "Aktualisierung angefordert."
        }
    }

    private fun signIn() {
        val mail = email.text.toString().trim()
        val pass = password.text.toString()
        if (mail.isBlank() || pass.isBlank()) {
            status.text = "Bitte E-Mail und Passwort eingeben."
            return
        }
        status.text = "Anmeldung läuft …"
        Thread {
            try {
                val connection = URL("$base/auth/v1/token?grant_type=password").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 12000
                connection.doOutput = true
                connection.setRequestProperty("apikey", apiKey)
                connection.setRequestProperty("Content-Type", "application/json")
                val body = JSONObject().put("email", mail).put("password", pass).toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
                connection.disconnect()
                if (code !in 200..299) throw IllegalStateException("Anmeldung fehlgeschlagen (HTTP " + code + "). E-Mail oder Passwort prüfen.")
                val response = JSONObject(responseText)
                val userId = response.optJSONObject("user")?.optString("id").orEmpty()
                val access = response.optString("access_token")
                val refresh = response.optString("refresh_token")
                if (userId.isBlank() || access.isBlank()) throw IllegalStateException("Die Anmeldung lieferte keine gültige Sitzung.")
                getSharedPreferences("gap_widget_session", Context.MODE_PRIVATE).edit()
                    .putString("user_id", userId)
                    .putString("access_token", access)
                    .putString("refresh_token", refresh)
                    .apply()
                runOnUiThread {
                    status.text = "Konto verbunden. Jetzt das GAP-Widget auf dem Startbildschirm hinzufügen."
                    password.text.clear()
                }
                GapWidgetProvider.runRefresh(this)
            } catch (e: Exception) {
                runOnUiThread { status.text = e.message ?: "Anmeldung nicht möglich. Verbindung prüfen." }
            }
        }.start()
    }
}
