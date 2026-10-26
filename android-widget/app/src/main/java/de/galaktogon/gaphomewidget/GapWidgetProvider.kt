package de.galaktogon.gaphomewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GapWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it, "Aufgaben werden geladen …", "Bitte kurz warten.") }
        Thread { refreshAll(context) }.start()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> Thread { refreshAll(context) }.start()
            ACTION_OPEN -> context.startActivity(Intent(context, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    companion object {
        const val ACTION_REFRESH = "de.galaktogon.gaphomewidget.REFRESH"
        const val ACTION_OPEN = "de.galaktogon.gaphomewidget.OPEN"
        private const val PREFS = "gap_widget_session"
        private const val BASE = "https://tkbopzdurmejcwawtswa.supabase.co"
        private const val API_KEY = "sb_publishable_NaSGbCf_A8HrrPV6O9D0ig_0tiFPA6D"

        fun render(context: Context, manager: AppWidgetManager, id: Int, summary: String, body: String) {
            val views = RemoteViews(context.packageName, R.layout.gap_widget)
            views.setTextViewText(R.id.widget_summary, summary)
            views.setTextViewText(R.id.widget_tasks, body)
            val refresh = PendingIntent.getBroadcast(context, id + 1000, Intent(context, GapWidgetProvider::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val open = PendingIntent.getBroadcast(context, id + 2000, Intent(context, GapWidgetProvider::class.java).setAction(ACTION_OPEN), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_refresh, refresh)
            views.setOnClickPendingIntent(R.id.widget_open, open)
            views.setOnClickPendingIntent(R.id.widget_tasks, open)
            manager.updateAppWidget(id, views)
        }

        fun runRefresh(context: Context) = refreshAll(context)

        private fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, GapWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val userId = prefs.getString("user_id", null)
            if (userId.isNullOrBlank() || prefs.getString("access_token", null).isNullOrBlank()) {
                ids.forEach { render(context, manager, it, "GAP-Konto verbinden", "Tippe hier und melde dich in der GAP-Widget-App an.") }
                return
            }
            try {
                val token = validAccessToken(context)
                val safeId = URLEncoder.encode(userId, "UTF-8")
                val response = request("$BASE/rest/v1/galaktogon_data?user_id=eq.$safeId&select=data,username", "GET", null, token)
                val rows = response.optJSONArray("rows")
                val row = if (rows != null && rows.length() > 0) rows.optJSONObject(0) else null
                val payload = row?.optJSONObject("data")
                if (payload == null) {
                    ids.forEach { render(context, manager, it, "Noch keine Aufgaben gefunden", "Öffne GAP und synchronisiere deine Daten.") }
                    return
                }
                val lines = ArrayList<String>()
                var total = 0
                listOf("tasks" to "SA", "exams" to "SP", "quests" to "Quest").forEach { pair ->
                    val items = payload.optJSONArray(pair.first) ?: return@forEach
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        if (isComplete(item)) continue
                        total++
                        if (lines.size < 7) {
                            val title = item.optString("title", pair.second).ifBlank { pair.second }
                            val due = item.optString("due", "")
                            lines.add("• [" + pair.second + "] " + title + if (due.isNotBlank()) " · " + due else "")
                        }
                    }
                }
                val body = if (total == 0) "Alle Aufgaben erledigt.\nSystemstatus: frei." else lines.joinToString("\n") + if (total > lines.size) "\n… und " + (total - lines.size) + " weitere" else ""
                val username = row?.optString("username", "Pilot") ?: "Pilot"
                val summary = username + " · " + total + " offene Aufgabe" + if (total == 1) "" else "n"
                ids.forEach { render(context, manager, it, summary, body) }
            } catch (e: Exception) {
                val message = if (e.message?.contains("401") == true || e.message?.contains("403") == true) "Sitzung abgelaufen. Öffne das Widget und melde dich erneut an." else "Keine Verbindung. Tippe auf ↻ zum erneuten Laden."
                ids.forEach { render(context, manager, it, "Synchronisierung nicht möglich", message) }
            }
        }

        private fun isComplete(item: JSONObject): Boolean =
            item.optBoolean("completed", false) || item.optBoolean("done", false) ||
            item.optBoolean("isDone", false) || item.optBoolean("complete", false) ||
            item.optString("status", "").equals("completed", true)

        private fun validAccessToken(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val access = prefs.getString("access_token", null) ?: throw IllegalStateException("Kein Login")
            return try {
                request("$BASE/rest/v1/galaktogon_data?select=user_id&limit=1", "GET", null, access)
                access
            } catch (e: Exception) {
                val refresh = prefs.getString("refresh_token", null) ?: throw e
                val response = request("$BASE/auth/v1/token?grant_type=refresh_token", "POST", JSONObject().put("refresh_token", refresh), null)
                val newAccess = response.optString("access_token")
                val newRefresh = response.optString("refresh_token")
                val userId = response.optJSONObject("user")?.optString("id")
                if (newAccess.isBlank() || userId.isNullOrBlank()) throw e
                prefs.edit().putString("access_token", newAccess).putString("refresh_token", newRefresh).putString("user_id", userId).apply()
                newAccess
            }
        }

        private fun request(address: String, method: String, payload: JSONObject?, token: String?): JSONObject {
            val connection = URL(address).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 10000
            connection.readTimeout = 12000
            connection.setRequestProperty("apikey", API_KEY)
            if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token)
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            connection.disconnect()
            if (code !in 200..299) throw IllegalStateException("HTTP " + code + ": " + text)
            val trimmed = text.trim()
            return if (trimmed.startsWith("[")) JSONObject().put("rows", org.json.JSONArray(trimmed)) else JSONObject(trimmed)
        }
    }
}
