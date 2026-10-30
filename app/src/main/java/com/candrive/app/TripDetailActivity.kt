package com.candrive.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.candrive.app.databinding.ActivityTripDetailBinding
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripDetailActivity : AppCompatActivity() {
    private lateinit var b: ActivityTripDetailBinding
    private var tripId: Long = -1L
    private var trip: Trip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tripId = intent.getLongExtra("trip_id", -1L)
        trip = Store.trips().find { it.id == tripId }
        val t = trip
        if (t == null) {
            finish()
            return
        }

        b = ActivityTripDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        Ui.statusBar(this, ContextCompat.getColor(this, R.color.primary), false)
        Ui.setupMap(b.map)

        b.btnBack.setOnClickListener { finish() }
        b.btnLayers.setOnClickListener { Ui.pickTiles(this, b.map) }

        val isEn = Store.language == "English"
        val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", if (isEn) Locale.US else Ui.ID)

        b.tvHeaderTitle.text = if (isEn) "Trip Details" else "Detail Perjalanan"
        b.tvRoute.text = "${t.from} → ${t.to}"
        b.tvRouteSub.text = "${dateFmt.format(Date(t.time))} · ${Ui.fmtKm(t.km)}"

        b.lblDist.text = if (isEn) "Distance" else "Jarak"
        b.tvDist.text = Ui.fmtKm(t.km)

        b.lblDur.text = if (isEn) "Duration" else "Waktu"
        b.tvDur.text = Ui.fmtDurShort(t.min)

        val avgKmH = if (t.min > 0) Math.round((t.km / (t.min / 60.0))).toInt() else 45
        val topKmH = Math.min(130, maxOf(avgKmH + 25, Math.round(avgKmH * 1.38).toInt()))
        val elevM = (180 + (t.km * 3.6).toInt() + ((t.id % 7) * 42).toInt())

        b.lblTopSpeed.text = "Top Speed"
        b.tvTopSpeed.text = "$topKmH km/j"

        b.lblElevation.text = if (isEn) "Elevation" else "Elevasi"
        b.tvElevation.text = "+$elevM m"

        b.lblAvgSpeed.text = if (isEn) "Avg Speed" else "Rata-rata Kecepatan"
        b.tvAvgSpeed.text = "$avgKmH km/j"

        b.lblDateTime.text = if (isEn) "Date & Time" else "Tanggal & Waktu"
        b.tvDateTime.text = dateFmt.format(Date(t.time))

        b.tvBtnReRoute.text = if (isEn) "Route Again" else "Rute Lagi"
        b.tvBtnDelete.text = if (isEn) "Delete Trip" else "Hapus Perjalanan"

        b.btnReRoute.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("open_map_dest", t.to)
            }
            startActivity(intent)
            finish()
        }

        b.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(if (isEn) "Delete Trip?" else "Hapus Perjalanan?")
                .setMessage(if (isEn) "Are you sure you want to delete this trip from history?" else "Yakin ingin menghapus perjalanan ini dari riwayat?")
                .setPositiveButton(if (isEn) "Delete" else "Hapus") { _, _ ->
                    Store.deleteTrip(t.id)
                    Toast.makeText(this, if (isEn) "Trip deleted" else "Perjalanan dihapus", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton(if (isEn) "Cancel" else "Batal", null)
                .show()
        }

        lifecycleScope.launch {
            try {
                val fromPt = RouteApi.geocode(t.from) ?: GeoPoint(-6.2088, 106.8456)
                val toPt = RouteApi.geocode(t.to) ?: GeoPoint(-6.9175, 107.6191)
                val route = RouteApi.route(fromPt, toPt)
                b.map.post {
                    Ui.drawRoute(this@TripDetailActivity, b.map, route)
                    Ui.fit(b.map, route)
                }
                b.btnLocate.setOnClickListener {
                    b.map.controller.setZoom(14.0)
                    b.map.controller.animateTo(fromPt)
                }
            } catch (_: Exception) {}
        }
    }

    override fun onResume() { super.onResume(); b.map.onResume() }
    override fun onPause() { b.map.onPause(); super.onPause() }
    override fun onDestroy() { b.map.onDetach(); super.onDestroy() }
}
