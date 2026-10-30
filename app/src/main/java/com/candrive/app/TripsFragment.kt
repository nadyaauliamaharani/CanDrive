package com.candrive.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.candrive.app.databinding.FragmentTripsBinding
import com.candrive.app.databinding.ItemTripBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TripsFragment : Fragment() {
    private var _b: FragmentTripsBinding? = null
    private val b get() = _b!!
    private var filter = 0
    private var data: List<Trip> = emptyList()
    private val main get() = requireActivity() as MainActivity

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        FragmentTripsBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        b.rv.layoutManager = LinearLayoutManager(requireContext())
        b.rv.adapter = Adapter()
        b.btnBack.setOnClickListener { main.openTab(R.id.nav_home) }
        listOf(b.chip0, b.chip1, b.chip2, b.chip3).forEachIndexed { i, c ->
            c.setOnClickListener {
                filter = i
                refresh()
            }
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    fun refresh() {
        if (_b == null) return
        val isEn = Store.language == "English"
        b.tvHeaderTitle.text = if (isEn) "Trip History" else "Riwayat Perjalanan"
        b.chip0.text = if (isEn) "All" else "Semua"
        b.chip1.text = if (isEn) "Today" else "Hari Ini"
        b.chip2.text = if (isEn) "This Week" else "Minggu Ini"
        b.chip3.text = if (isEn) "This Month" else "Bulan Ini"
        b.tvEmpty.text = if (isEn) "No trips recorded in this period." else "Belum ada perjalanan pada periode ini."

        listOf(b.chip0, b.chip1, b.chip2, b.chip3).forEachIndexed { i, c ->
            c.setBackgroundResource(if (i == filter) R.drawable.bg_chip_on else R.drawable.bg_chip_off)
            c.setTextColor(requireContext().getColor(if (i == filter) R.color.white else R.color.text_primary))
        }
        val now = Calendar.getInstance()
        data = Store.trips().filter { t ->
            val c = Calendar.getInstance().apply { timeInMillis = t.time }
            val sameYear = c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            when (filter) {
                1 -> sameYear && c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                2 -> sameYear && c.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                3 -> sameYear && c.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                else -> true
            }
        }
        b.rv.adapter?.notifyDataSetChanged()
        b.tvEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun detail(t: Trip) {
        val intent = Intent(requireContext(), TripDetailActivity::class.java).apply {
            putExtra("trip_id", t.id)
        }
        startActivity(intent)
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {
        inner class VH(val v: ItemTripBinding) : RecyclerView.ViewHolder(v.root)
        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(ItemTripBinding.inflate(LayoutInflater.from(p.context), p, false))

        override fun getItemCount() = data.size

        override fun onBindViewHolder(h: VH, i: Int) {
            val t = data[i]
            val isEn = Store.language == "English"
            val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", if (isEn) Locale.US else Ui.ID)
            h.v.tvTitle.text = "${t.from} → ${t.to}"
            h.v.tvSub.text = "${Ui.fmtKm(t.km)} · ${Ui.fmtDurShort(t.min)}"
            h.v.tvDate.text = dateFmt.format(Date(t.time))
            h.v.root.setOnClickListener { detail(t) }

            h.v.root.setOnLongClickListener { v ->
                val popup = PopupMenu(v.context, v)
                val m = popup.menu
                m.add(0, 1, 0, if (isEn) "View Details" else "Lihat Detail")
                m.add(0, 2, 1, if (isEn) "Navigate Route" else "Navigasi Rute Ini")
                m.add(0, 3, 2, if (isEn) "Share Trip" else "Bagikan Rute")
                m.add(0, 4, 3, if (isEn) "Delete" else "Hapus Riwayat")

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> detail(t)
                        2 -> main.openMap(t.to)
                        3 -> {
                            val shareText = if (isEn)
                                "Trip CanDrive: ${t.from} to ${t.to} (${Ui.fmtKm(t.km)}, ${Ui.fmtDur(t.min)})"
                            else
                                "Perjalanan CanDrive: ${t.from} ke ${t.to} (${Ui.fmtKm(t.km)}, ${Ui.fmtDur(t.min)})"

                            startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    },
                                    if (isEn) "Share Trip" else "Bagikan Perjalanan"
                                )
                            )
                        }
                        4 -> {
                            val title = if (isEn) "Delete Trip" else "Hapus Riwayat"
                            val msg = if (isEn) "Are you sure you want to delete this trip?" else "Yakin ingin menghapus riwayat perjalanan ini?"
                            val pos = if (isEn) "Delete" else "Hapus"
                            val neg = if (isEn) "Cancel" else "Batal"

                            Ui.confirm(requireContext(), title, msg, pos, neg, isDanger = true) {
                                Store.deleteTrip(t.id)
                                refresh()
                            }
                        }
                    }
                    true
                }
                popup.show()
                true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
