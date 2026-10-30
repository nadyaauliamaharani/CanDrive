package com.candrive.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

data class RouteResult(
    val points: List<GeoPoint>, val km: Double, val min: Int, val via: String,
    val from: String = "", val to: String = ""
)

/** Geocoding (Nominatim) + routing (OSRM) — keduanya berbasis OpenStreetMap, tanpa API key. */
object RouteApi {
    /** Rute yang sedang dipakai untuk Perjalanan Aktif. */
    var current: RouteResult? = null

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "CanDrive/1.0 (Android)")
        c.connectTimeout = 15000
        c.readTimeout = 25000
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    suspend fun geocode(q: String): GeoPoint? = withContext(Dispatchers.IO) {
        val a = JSONArray(get("https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + URLEncoder.encode(q, "UTF-8")))
        if (a.length() == 0) null
        else a.getJSONObject(0).let { GeoPoint(it.getString("lat").toDouble(), it.getString("lon").toDouble()) }
    }

    suspend fun cityName(p: GeoPoint): String = withContext(Dispatchers.IO) {
        try {
            val j = JSONObject(get("https://nominatim.openstreetmap.org/reverse?format=json&zoom=10&lat=${p.latitude}&lon=${p.longitude}"))
            val ad = j.getJSONObject("address")
            val n = listOf("city", "town", "municipality", "county", "state").map { ad.optString(it) }.firstOrNull { it.isNotBlank() }
            (n ?: "Lokasi Saya").removePrefix("Kota ").removePrefix("Kabupaten ")
        } catch (e: Exception) { "Lokasi Saya" }
    }

    suspend fun route(a: GeoPoint, b: GeoPoint): RouteResult = withContext(Dispatchers.IO) {
        val url = "https://router.project-osrm.org/route/v1/driving/${a.longitude},${a.latitude};${b.longitude},${b.latitude}" +
            "?overview=full&geometries=geojson&steps=true"
        val r = JSONObject(get(url)).getJSONArray("routes").getJSONObject(0)
        val coords = r.getJSONObject("geometry").getJSONArray("coordinates")
        val pts = ArrayList<GeoPoint>(coords.length())
        for (i in 0 until coords.length()) {
            val c = coords.getJSONArray(i)
            pts.add(GeoPoint(c.getDouble(1), c.getDouble(0)))
        }
        var via = ""
        var best = 0.0
        val steps = r.getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
        for (i in 0 until steps.length()) {
            val s = steps.getJSONObject(i)
            val n = s.optString("name")
            val d = s.optDouble("distance")
            if (n.isNotBlank() && d > best) { best = d; via = n }
        }
        RouteResult(pts, r.getDouble("distance") / 1000.0, (r.getDouble("duration") / 60).roundToInt(), via)
    }
}
