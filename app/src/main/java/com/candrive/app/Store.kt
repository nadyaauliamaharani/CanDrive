package com.candrive.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

data class Trip(val id: Long, val from: String, val to: String, val km: Double, val min: Int, val time: Long)

/** Penyimpanan lokal sederhana (SharedPreferences). */
object Store {
    private lateinit var sp: SharedPreferences
    fun init(c: Context) { sp = c.getSharedPreferences("candrive", Context.MODE_PRIVATE) }

    private fun str(k: String, d: String) = object : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String = sp.getString(k, d) ?: d
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            sp.edit().putString(k, value).apply()
        }
    }

    var loggedIn by str("logged_in", "0")
    var userName by str("user_name", "Deswinta Windia Rahma")
    var userEmail by str("user_email", "deswinta@gmail.com")
    var userPhone by str("user_phone", "081234567890")
    var userBio by str("user_bio", "Better Routes, Smoother Journeys")
    var userHandle by str("user_handle", "deswinta_wr")
    var userPhotoUri by str("user_photo_uri", "")
    var theme by str("theme", "Terang")
    var language by str("language", "Indonesia")
    var notifs by str("notifs", "1,1,1")
    var vName by str("v_name", "Toyota Fortuner")
    var vPlate by str("v_plate", "B 1234 KZ")
    var vYear by str("v_year", "2021")
    var vFuel by str("v_fuel", "Diesel")
    var vKm by str("v_km", "48.250")
    var vStatus by str("v_status", "Baik")
    var serviceDate by str("service_date", "")
    var notes by str("notes", "[]")
    private var tripsJson by str("trips", "")

    fun noteList(): MutableList<String> {
        val a = JSONArray(notes)
        return MutableList(a.length()) { a.getString(it) }
    }
    fun saveNotes(l: List<String>) { notes = JSONArray(l).toString() }

    private fun at(day: Int, h: Int, m: Int): Long =
        Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, day, h, m, 0) }.timeInMillis

    private fun seed() = listOf(
        Trip(1, "Jakarta", "Bandung", 136.0, 148, at(25, 8, 12)),
        Trip(2, "Jakarta", "Depok", 32.0, 58, at(24, 16, 20)),
        Trip(3, "Bandung", "Cirebon", 119.0, 135, at(22, 9, 40)),
        Trip(4, "Jakarta", "Bekasi", 25.0, 45, at(20, 17, 5)),
        Trip(5, "Depok", "Jakarta", 28.0, 52, at(18, 14, 32))
    )

    fun trips(): List<Trip> {
        if (tripsJson.isEmpty()) saveTrips(seed())
        val a = JSONArray(tripsJson)
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Trip(o.getLong("id"), o.getString("from"), o.getString("to"), o.getDouble("km"), o.getInt("min"), o.getLong("time"))
        }.sortedByDescending { it.time }
    }

    private fun saveTrips(l: List<Trip>) {
        val arr = JSONArray()
        l.forEach { t ->
            arr.put(JSONObject().put("id", t.id).put("from", t.from).put("to", t.to)
                .put("km", t.km).put("min", t.min).put("time", t.time))
        }
        tripsJson = arr.toString()
    }
    fun addTrip(t: Trip) = saveTrips(trips() + t)
    fun deleteTrip(id: Long) = saveTrips(trips().filter { it.id != id })
}
