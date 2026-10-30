package com.candrive.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.candrive.app.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!
    private val main get() = requireActivity() as MainActivity

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        FragmentHomeBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        b.qaNav.setOnClickListener { main.openMap(null) }
        b.qaHist.setOnClickListener { main.openTab(R.id.nav_trips) }
        b.qaCar.setOnClickListener { startActivity(Intent(requireContext(), VehicleActivity::class.java)) }

        b.btnBell.setOnClickListener {
            b.dotBell.visibility = View.GONE
            Ui.notificationsDialog(requireContext())
        }

        b.btnMore.setOnClickListener {
            val isEn = Store.language == "English"
            val popup = PopupMenu(requireContext(), b.btnMore)
            popup.menu.add(0, 1, 0, if (isEn) "Profile & Settings" else "Profil & Pengaturan")
            popup.menu.add(0, 2, 1, if (isEn) "Help & FAQ" else "Bantuan & FAQ")
            popup.menu.add(0, 3, 2, if (isEn) "Share CanDrive" else "Bagikan CanDrive")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> main.openTab(R.id.nav_profile)
                    2 -> Ui.faq(requireContext())
                    3 -> startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Coba CanDrive — Better Routes, Smoother Journeys!")
                            },
                            if (isEn) "Share via" else "Bagikan lewat"
                        )
                    )
                }
                true
            }
            popup.show()
        }

        val dests = arrayOf("Bandung", "Bogor", "Puncak", "Cirebon", "Yogyakarta")
        b.tvSeeAll.setOnClickListener {
            val isEn = Store.language == "English"
            val title = if (isEn) "Route Recommendations" else "Rekomendasi Rute"
            Ui.listPicker(requireContext(), title, dests) { i ->
                main.openMap(dests[i])
            }
        }

        b.cardRoute.setOnClickListener { main.openMap("Bandung") }
        b.btnRouteGo.setOnClickListener { main.openMap("Bandung") }

        b.cardTips.setOnClickListener {
            val isEn = Store.language == "English"
            val title = if (isEn) "Driving Tips" else "Tips Berkendara"
            val msg = if (isEn) {
                "• Check tire pressure when tires are cold.\n• Ensure fuel, oil, and coolant levels are adequate.\n• Take a break every 2 hours of driving.\n• Maintain a safe distance from vehicles ahead."
            } else {
                "• Cek tekanan ban saat ban dingin.\n• Pastikan BBM, oli, dan air radiator cukup.\n• Istirahat tiap 2 jam perjalanan.\n• Jaga jarak aman dengan kendaraan di depan."
            }
            Ui.info(requireContext(), title, msg, iconRes = R.drawable.ic_tire)
        }

        bindLanguage()
    }

    override fun onResume() {
        super.onResume()
        bindLanguage()
    }

    private fun bindLanguage() {
        if (_b == null) return
        val isEn = Store.language == "English"
        b.tvGreetingTitle.text = if (isEn) "Hello, Driver!" else "Halo, Driver!"
        b.tvGreetingSub.text = if (isEn) "Roads today, new stories tomorrow." else "Jalan hari ini, cerita baru nanti."
        b.tvQaNav.text = if (isEn) "Navigation" else "Navigasi"
        b.tvQaHist.text = if (isEn) "History" else "Riwayat"
        b.tvQaCar.text = if (isEn) "Vehicle" else "Kendaraan"
        b.tvRecTitle.text = if (isEn) "Recommended Routes" else "Rekomendasi Rute"
        b.tvSeeAll.text = if (isEn) "See all" else "Lihat semua"
        b.tvRecRouteDetail.text = if (isEn) "± 2 hrs 30 mins · 136 km" else "± 2 jam 30 menit · 136 km"
        b.tvTipsTitle.text = if (isEn) "Driving Tips" else "Tips Berkendara"
        b.tvTipsSub.text = if (isEn) "Check tire pressure regularly for a safer journey." else "Cek tekanan ban secara rutin untuk perjalanan yang lebih aman."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
