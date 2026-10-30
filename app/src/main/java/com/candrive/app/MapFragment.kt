package com.candrive.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.candrive.app.databinding.FragmentMapBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Overlay

class MapFragment : Fragment() {
    private var _b: FragmentMapBinding? = null
    private val b get() = _b!!
    private val JAKARTA = GeoPoint(-6.2088, 106.8456)
    private val ME = "Lokasi saya"

    var pendingDest: String? = null
    private var started = false
    private var needsFit = false
    private var lastRoute: RouteResult? = null
    private var job: Job? = null
    private val overlays = mutableListOf<Overlay>()
    private val cache = HashMap<String, GeoPoint>()

    private val perm = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { calc() }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        FragmentMapBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        Ui.setupMap(b.map)
        b.map.setOnTouchListener { _, _ -> b.compass.rotation = -b.map.mapOrientation; false }
        b.compass.setOnClickListener { b.map.mapOrientation = 0f; b.compass.rotation = 0f }

        val search = TextViewAction { calc() }
        b.etFrom.setOnEditorActionListener(search)
        b.etTo.setOnEditorActionListener(search)
        b.btnSwap.setOnClickListener {
            val f = b.etFrom.text.toString(); val t = b.etTo.text.toString()
            b.etFrom.setText(t); b.etTo.setText(f); calc()
        }
        b.btnLocate.setOnClickListener {
            val p = deviceLocation()
            if (p != null) { b.map.controller.setZoom(15.0); b.map.controller.animateTo(p) }
            else { toast("Lokasi belum tersedia. Izinkan akses lokasi & aktifkan GPS."); askPerm() }
        }
        b.btnLayers.setOnClickListener { Ui.pickTiles(requireContext(), b.map) }
        b.btnStart.setOnClickListener {
            val r = lastRoute
            if (r == null) { toast("Rute belum tersedia"); return@setOnClickListener }
            RouteApi.current = r
            startActivity(Intent(requireContext(), ActiveTripActivity::class.java))
        }
    }

    /** Dipanggil MainActivity setiap tab Peta ditampilkan. */
    fun onShown() {
        if (_b == null) return
        if (!started) {
            started = true
            b.etFrom.setText(ME)
            b.etTo.setText(pendingDest ?: "Bandung")
            pendingDest = null
            if (!hasPerm()) askPerm() else calc()
        } else if (pendingDest != null) {
            b.etTo.setText(pendingDest); pendingDest = null; calc()
        } else if (needsFit) {
            b.map.post { lastRoute?.let { Ui.fit(b.map, it) }; needsFit = false }
        }
    }

    private fun hasPerm() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }

    private fun askPerm() = perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))

    @SuppressLint("MissingPermission")
    private fun deviceLocation(): GeoPoint? {
        if (!hasPerm()) return null
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val l = lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        return l?.let { GeoPoint(it.latitude, it.longitude) }
    }

    private fun toast(s: String) = Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show()

    private suspend fun resolve(t: String): GeoPoint? {
        if (t.isEmpty() || t.equals(ME, true)) return deviceLocation() ?: JAKARTA
        return cache[t.lowercase()] ?: RouteApi.geocode(t)?.also { cache[t.lowercase()] = it }
    }

    private fun calc() {
        if (_b == null) return
        val from = b.etFrom.text.toString().trim()
        val to = b.etTo.text.toString().trim()
        if (to.isEmpty()) { toast("Masukkan tujuan"); return }
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(b.etTo.windowToken, 0)
        b.tvDuration.text = "Menghitung rute…"
        b.tvSub.text = ""
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val a = resolve(from)
                val d = resolve(to)
                if (a == null || d == null) {
                    b.tvDuration.text = "Lokasi tidak ditemukan"; toast("Lokasi tidak ditemukan"); return@launch
                }
                val fromLabel = if (from.isEmpty() || from.equals(ME, true)) RouteApi.cityName(a) else from.replaceFirstChar { it.uppercase() }
                val r = RouteApi.route(a, d).copy(from = fromLabel, to = to.replaceFirstChar { it.uppercase() })
                showRoute(r)
            } catch (e: Exception) {
                if (_b != null) { b.tvDuration.text = "Rute tidak tersedia"; toast("Gagal memuat rute. Periksa koneksi internet / tujuan.") }
            }
        }
    }

    private fun showRoute(r: RouteResult) {
        lastRoute = r
        overlays.forEach { b.map.overlays.remove(it) }
        overlays.clear()
        overlays.addAll(Ui.drawRoute(requireContext(), b.map, r))
        b.tvDuration.text = Ui.fmtDur(r.min)
        b.tvSub.text = Ui.fmtKm(r.km) + " · " + (if (r.via.isNotBlank()) "Lewat ${r.via}" else "Rute tercepat")
        if (b.map.width > 0) b.map.post { Ui.fit(b.map, r) } else needsFit = true
    }

    override fun onResume() { super.onResume(); _b?.map?.onResume() }
    override fun onPause() { _b?.map?.onPause(); super.onPause() }
    override fun onDestroyView() { job?.cancel(); b.map.onDetach(); _b = null; super.onDestroyView() }

    private class TextViewAction(val run: () -> Unit) : android.widget.TextView.OnEditorActionListener {
        override fun onEditorAction(v: android.widget.TextView?, id: Int, e: android.view.KeyEvent?): Boolean {
            if (id == EditorInfo.IME_ACTION_SEARCH || id == EditorInfo.IME_ACTION_DONE) { run(); return true }
            return false
        }
    }
}
