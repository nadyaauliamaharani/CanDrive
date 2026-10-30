package com.candrive.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.candrive.app.databinding.ActivityActiveTripBinding
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.max

class ActiveTripActivity : AppCompatActivity() {
    private lateinit var b: ActivityActiveTripBinding
    private lateinit var r: RouteResult
    private lateinit var rem: DoubleArray          // sisa jarak (km) dari tiap titik ke tujuan
    private lateinit var me: Marker
    private var idx = 0
    private var gpsMode = false
    private var finished = false
    private val handler = Handler(Looper.getMainLooper())
    private val clock = SimpleDateFormat("HH:mm", Ui.ID)
    private lateinit var lm: LocationManager

    private val listener = object : LocationListener {
        override fun onLocationChanged(l: Location) {
            val p = GeoPoint(l.latitude, l.longitude)
            var best = 0; var bd = Double.MAX_VALUE
            for (i in r.points.indices) { val d = r.points[i].distanceToAsDouble(p); if (d < bd) { bd = d; best = i } }
            if (bd < 2000) { gpsMode = true; update(best); if (best >= r.points.size - 3 && !finished) arrived() }
        }
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    private val sim = object : Runnable {
        override fun run() {
            if (finished) return
            if (!gpsMode) {
                val step = max(1, r.points.size / 150)
                if (idx >= r.points.size - 1) { arrived(); return }
                update(minOf(idx + step, r.points.size - 1))
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = RouteApi.current
        if (route == null) { finish(); return }
        r = route
        b = ActivityActiveTripBinding.inflate(layoutInflater)
        setContentView(b.root)
        Ui.statusBar(this, ContextCompat.getColor(this, R.color.primary), false)

        val pts = r.points
        rem = DoubleArray(pts.size)
        for (i in pts.size - 2 downTo 0) rem[i] = rem[i + 1] + pts[i].distanceToAsDouble(pts[i + 1]) / 1000.0
        val scale = if (rem[0] > 0) r.km / rem[0] else 1.0
        for (i in rem.indices) rem[i] *= scale

        Ui.setupMap(b.map)
        Ui.drawRoute(this, b.map, r, withStart = false)
        me = Marker(b.map).apply {
            position = pts.first()
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setIcon(ContextCompat.getDrawable(this@ActiveTripActivity, R.drawable.ic_origin))
            setInfoWindow(null)
        }
        b.map.overlays.add(me)
        b.map.post { Ui.fit(b.map, r); update(0, follow = false) }

        b.tvRoute.text = "${r.from.ifEmpty { "Asal" }} → ${r.to.ifEmpty { "Tujuan" }}"
        b.tvRouteSub.text = "${Ui.fmtDur(r.min)} · ${Ui.fmtKm(r.km)}"
        b.btnBack.setOnClickListener { finish() }
        b.btnLocate.setOnClickListener { b.map.controller.setZoom(15.0); b.map.controller.animateTo(me.position) }
        b.btnLayers.setOnClickListener { Ui.pickTiles(this, b.map) }

        b.btnEnd.setOnClickListener {
            val isEn = Store.language == "English"
            Ui.confirm(
                this,
                if (isEn) "End Trip?" else "Akhiri Perjalanan?",
                if (isEn) "Trip will be saved to history." else "Perjalanan akan disimpan ke riwayat.",
                positiveText = if (isEn) "End Trip" else "Akhiri",
                negativeText = if (isEn) "Continue" else "Lanjutkan",
                isDanger = true
            ) {
                save()
                finish()
            }
        }

        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        startGps()
        handler.postDelayed(sim, 1500)
    }

    @SuppressLint("MissingPermission")
    private fun startGps() {
        val ok = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!ok) return
        try {
            lm.getProviders(true).forEach { lm.requestLocationUpdates(it, 2000L, 5f, listener) }
        } catch (_: Exception) {}
    }

    private fun update(i: Int, follow: Boolean = true) {
        idx = i
        me.position = r.points[i]
        val remKm = rem[i]
        b.tvDist.text = Ui.fmtKm(remKm)
        val remMin = (r.min * remKm / r.km).toLong()
        b.tvEta.text = clock.format(Date(System.currentTimeMillis() + remMin * 60_000))
        if (follow) b.map.controller.animateTo(me.position)
        b.map.invalidate()
    }

    private fun save() {
        if (finished) return
        finished = true
        val done = r.km - rem[idx]
        if (done < 0.1) { Toast.makeText(this, "Perjalanan dibatalkan (belum ada jarak tempuh)", Toast.LENGTH_SHORT).show(); return }
        val min = max(1, (r.min * done / r.km).toInt())
        Store.addTrip(Trip(System.currentTimeMillis(), r.from.ifEmpty { "Asal" }, r.to.ifEmpty { "Tujuan" }, Math.round(done * 10) / 10.0, min, System.currentTimeMillis()))
        Toast.makeText(this, "Perjalanan disimpan ke riwayat", Toast.LENGTH_SHORT).show()
    }

    private fun arrived() {
        val isEn = Store.language == "English"
        update(r.points.size - 1, follow = false)
        Ui.info(
            this,
            if (isEn) "You have arrived 🎉" else "Anda telah tiba 🎉",
            if (isEn) "Safely arrived at ${r.to.ifEmpty { "destination" }}." else "Selamat sampai di ${r.to.ifEmpty { "tujuan" }}."
        )
        save()
        finish()
    }

    override fun onResume() { super.onResume(); b.map.onResume() }
    override fun onPause() { b.map.onPause(); super.onPause() }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::lm.isInitialized) lm.removeUpdates(listener)
        if (::b.isInitialized) b.map.onDetach()
        super.onDestroy()
    }
}
