package com.candrive.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.candrive.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val tags = listOf("home", "map", "trips", "profile")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        updateNavTitles()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, HomeFragment(), "home")
                .add(R.id.container, MapFragment(), "map")
                .add(R.id.container, TripsFragment(), "trips")
                .add(R.id.container, ProfileFragment(), "profile")
                .commitNow()
        }
        b.nav.setOnItemSelectedListener {
            show(when (it.itemId) {
                R.id.nav_map -> "map"
                R.id.nav_trips -> "trips"
                R.id.nav_profile -> "profile"
                else -> "home"
            })
            true
        }
        show(tags[when (b.nav.selectedItemId) { R.id.nav_map -> 1; R.id.nav_trips -> 2; R.id.nav_profile -> 3; else -> 0 }])

        handleIntent(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (b.nav.selectedItemId != R.id.nav_home) b.nav.selectedItemId = R.id.nav_home
                else finish()
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val dest = intent?.getStringExtra("open_map_dest")
        if (!dest.isNullOrEmpty()) {
            openMap(dest)
        }
    }

    override fun onResume() {
        super.onResume()
        updateNavTitles()
    }

    private fun updateNavTitles() {
        val isEn = Store.language == "English"
        b.nav.menu.findItem(R.id.nav_home).title = if (isEn) "Home" else "Beranda"
        b.nav.menu.findItem(R.id.nav_map).title = if (isEn) "Map" else "Peta"
        b.nav.menu.findItem(R.id.nav_trips).title = if (isEn) "Trips" else "Perjalanan"
        b.nav.menu.findItem(R.id.nav_profile).title = if (isEn) "Profile" else "Profil"
    }

    private fun frag(tag: String): Fragment = supportFragmentManager.findFragmentByTag(tag)!!

    private fun show(tag: String) {
        val tr = supportFragmentManager.beginTransaction()
        tags.forEach { if (it == tag) tr.show(frag(it)) else tr.hide(frag(it)) }
        tr.commitNow()
        if (tag == "map") {
            Ui.statusBar(this, ContextCompat.getColor(this, R.color.surface), !Ui.isNight(this))
            (frag("map") as MapFragment).onShown()
        } else {
            Ui.statusBar(this, ContextCompat.getColor(this, R.color.primary), false)
        }
        if (tag == "trips") (frag("trips") as TripsFragment).refresh()
    }

    fun openTab(id: Int) { b.nav.selectedItemId = id }

    /** Buka tab Peta dan hitung rute ke [dest] (null = tujuan bawaan). */
    fun openMap(dest: String?) {
        val m = frag("map") as MapFragment
        m.pendingDest = dest
        if (b.nav.selectedItemId == R.id.nav_map) m.onShown() else b.nav.selectedItemId = R.id.nav_map
    }
}
