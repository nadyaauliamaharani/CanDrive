package com.candrive.app

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

fun Int.dp(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

object Ui {
    val ID: Locale = Locale("id", "ID")
    val isEn get() = Store.language == "English"

    fun isNight(c: Context) =
        (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun statusBar(a: Activity, color: Int, lightIcons: Boolean) {
        a.window.statusBarColor = color
        WindowCompat.getInsetsController(a.window, a.window.decorView).isAppearanceLightStatusBars = lightIcons
    }

    fun applyTheme() = AppCompatDelegate.setDefaultNightMode(
        when (Store.theme) {
            "Gelap" -> AppCompatDelegate.MODE_NIGHT_YES
            "Sistem" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }
    )

    fun applyLanguage(c: Context) {
        val langTag = if (isEn) "en" else "id"
        val locales = LocaleListCompat.forLanguageTags(langTag)
        AppCompatDelegate.setApplicationLocales(locales)

        val locale = if (isEn) Locale.ENGLISH else Locale("id", "ID")
        Locale.setDefault(locale)
        val config = c.resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        c.resources.updateConfiguration(config, c.resources.displayMetrics)
    }

    fun fmtKm(km: Double) = if (km >= 10) "${km.toInt()} km" else String.format(if (isEn) Locale.US else ID, "%.1f km", km)

    fun fmtDur(min: Int): String {
        if (isEn) return if (min >= 60) "${min / 60} hour ${min % 60} mins" else "$min mins"
        return if (min >= 60) "${min / 60} jam ${min % 60} menit" else "$min menit"
    }

    fun fmtDurShort(min: Int) = if (min >= 60) "${min / 60}h ${min % 60}m" else "${min}m"

    fun loadAvatar(ctx: Context, iv: ImageView, uriStr: String) {
        if (uriStr.isNotEmpty()) {
            try {
                val uri = android.net.Uri.parse(uriStr)
                iv.setImageURI(uri)
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                iv.setPadding(0, 0, 0, 0)
                iv.imageTintList = null
                iv.clipToOutline = true
                return
            } catch (_: Exception) {}
        }
        iv.setImageResource(R.drawable.ic_person)
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        val p = 12.dp()
        iv.setPadding(p, p, p, p)
        iv.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(ctx, R.color.white)
        )
    }

    // ==========================================
    // ELEGANT CUSTOM POPUPS & BOTTOM SHEETS
    // ==========================================

    // ELEGANT CUSTOM MODAL POPUPS (REF: REFERENCE CARD STYLE)
    // ==========================================

    private fun createModalDialog(ctx: Context, title: String): Pair<android.app.Dialog, LinearLayout> {
        val d = android.app.Dialog(ctx)
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        d.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))

        val rootBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 24.dp().toFloat()
                setColor(ContextCompat.getColor(ctx, R.color.primary))
            }
            setPadding(20.dp(), 16.dp(), 20.dp(), 20.dp())
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // Top Row: Title + Close Button '✕'
        val topRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 14.dp()
            }
        }

        val tvTitle = TextView(ctx).apply {
            text = title
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(tvTitle)

        val closeBtn = TextView(ctx).apply {
            text = "✕"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8.dp(), 4.dp(), 8.dp(), 4.dp())
            isClickable = true
            isFocusable = true
            setOnClickListener { d.dismiss() }
        }
        topRow.addView(closeBtn)
        rootBox.addView(topRow)

        val contentBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        rootBox.addView(contentBox)

        d.setContentView(rootBox)
        d.window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        return Pair(d, contentBox)
    }

    /** Custom Info Popup (Driving Tips, Notifications, FAQ, About) */
    fun info(ctx: Context, title: String, msg: String, iconRes: Int = 0) {
        val (d, box) = createModalDialog(ctx, title)

        // Content Row: Left Icon + Right Message
        val contentRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 20.dp()
            }
        }

        if (iconRes != 0) {
            val iconContainer = ImageView(ctx).apply {
                setImageResource(iconRes)
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.WHITE)
                }
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.primary))
                setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
                layoutParams = LinearLayout.LayoutParams(52.dp(), 52.dp()).apply {
                    marginEnd = 16.dp()
                }
            }
            contentRow.addView(iconContainer)
        }

        val scroll = ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvMsg = TextView(ctx).apply {
            text = msg
            textSize = 14f
            setLineSpacing(4.dp().toFloat(), 1.2f)
            setTextColor(Color.parseColor("#EAF0F2"))
        }
        scroll.addView(tvMsg)
        contentRow.addView(scroll)
        box.addView(contentRow)

        // Close Button
        box.addView(TextView(ctx).apply {
            text = if (isEn) "Close" else "Tutup"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.primary))
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14.dp().toFloat()
                setColor(Color.WHITE)
            }
            setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 46.dp())
            setOnClickListener { d.dismiss() }
        })

        d.show()
    }

    /** Custom Notifications Dialog (Matching Reference Screen with Working Tabs & Filtered Items) */
    fun notificationsDialog(ctx: Context) {
        val title = if (isEn) "Notifications" else "Notifikasi"
        val (d, box) = createModalDialog(ctx, title)

        data class Notif(val icon: Int, val titleTime: String, val body: String, val type: String)

        val notifItems = listOf(
            Notif(
                R.drawable.ic_bell,
                if (isEn) "Welcome to CanDrive · 1d ago" else "Selamat Datang di CanDrive · 1 hari lalu",
                if (isEn) "Thank you for using CanDrive! Enjoy safer routes and smoother journeys for your daily commute." else "Terima kasih telah menggunakan CanDrive! Nikmati rute terbaik dan perjalanan yang lebih mulus.",
                "update"
            ),
            Notif(
                R.drawable.ic_tire,
                if (isEn) "Tire Pressure Tip · 3h ago" else "Tips Tekanan Ban · 3 jam lalu",
                if (isEn) "Check tire pressure regularly when tires are cold for better fuel efficiency and safety." else "Cek tekanan ban secara berkala saat ban dalam keadaan dingin untuk efisiensi bahan bakar.",
                "reminder"
            ),
            Notif(
                R.drawable.ic_car,
                if (isEn) "Vehicle Service Reminder · 1h ago" else "Pengingat Servis Kendaraan · 1 jam lalu",
                if (isEn) "Don't forget to set service reminders in the Vehicle menu to keep your engine in top condition." else "Jangan lupa atur pengingat servis di menu Kendaraan agar mesin tetap prima.",
                "reminder"
            )
        )

        // Scrollable Notification List Container
        val scroll = ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 340.dp()).apply {
                bottomMargin = 16.dp()
            }
            isFillViewport = true
        }

        val listContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(listContainer)

        // Function to populate list based on filter type ("all", "reminder", "update")
        fun populateList(filterType: String) {
            listContainer.removeAllViews()
            val filtered = if (filterType == "all") notifItems else notifItems.filter { it.type == filterType }

            if (filtered.isEmpty()) {
                listContainer.addView(TextView(ctx).apply {
                    text = if (isEn) "No notifications in this category." else "Tidak ada notifikasi di kategori ini."
                    textSize = 13f
                    setTextColor(Color.parseColor("#EAF0F2"))
                    gravity = Gravity.CENTER
                    setPadding(16.dp(), 32.dp(), 16.dp(), 32.dp())
                })
                return
            }

            filtered.forEach { notif ->
                val itemCard = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 14.dp().toFloat()
                        setColor(ContextCompat.getColor(ctx, R.color.surface))
                    }
                    setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = 10.dp()
                    }
                    gravity = Gravity.TOP
                }

                val iconIv = ImageView(ctx).apply {
                    setImageResource(notif.icon)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(ctx, R.color.chip_bg))
                    }
                    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.primary))
                    setPadding(10.dp(), 10.dp(), 10.dp(), 10.dp())
                    layoutParams = LinearLayout.LayoutParams(40.dp(), 40.dp()).apply {
                        marginEnd = 12.dp()
                    }
                }
                itemCard.addView(iconIv)

                val textCol = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                textCol.addView(TextView(ctx).apply {
                    text = notif.titleTime
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(ctx, R.color.primary))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = 4.dp()
                    }
                })

                textCol.addView(TextView(ctx).apply {
                    text = notif.body
                    textSize = 13f
                    setLineSpacing(3.dp().toFloat(), 1.1f)
                    setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                })

                itemCard.addView(textCol)
                listContainer.addView(itemCard)
            }
        }

        // Tabs Row ("Semua", "Pengingat", "Pembaruan")
        val tabsRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 14.dp()
            }
        }

        var tabAll: TextView? = null
        var tabRem: TextView? = null
        var tabInfo: TextView? = null

        fun updateTabStyles(selected: String) {
            fun applyTabStyle(tv: TextView, active: Boolean) {
                tv.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
                tv.setTextColor(if (active) Color.WHITE else Color.parseColor("#B0C4DE"))
                tv.background = if (active) {
                    android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 20.dp().toFloat()
                        setColor(Color.parseColor("#33FFFFFF"))
                    }
                } else null
            }
            applyTabStyle(tabAll!!, selected == "all")
            applyTabStyle(tabRem!!, selected == "reminder")
            applyTabStyle(tabInfo!!, selected == "update")
        }

        tabAll = createNotificationTab(ctx, if (isEn) "All" else "Semua", true).apply {
            setOnClickListener {
                updateTabStyles("all")
                populateList("all")
            }
        }
        tabRem = createNotificationTab(ctx, if (isEn) "Reminders" else "Pengingat", false).apply {
            setOnClickListener {
                updateTabStyles("reminder")
                populateList("reminder")
            }
        }
        tabInfo = createNotificationTab(ctx, if (isEn) "Updates" else "Pembaruan", false).apply {
            setOnClickListener {
                updateTabStyles("update")
                populateList("update")
            }
        }

        tabsRow.addView(tabAll)
        tabsRow.addView(tabRem)
        tabsRow.addView(tabInfo)
        box.addView(tabsRow)
        box.addView(scroll)

        // Initial populate
        populateList("all")

        // Close Button
        box.addView(TextView(ctx).apply {
            text = if (isEn) "Close" else "Tutup"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.primary))
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14.dp().toFloat()
                setColor(Color.WHITE)
            }
            setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 46.dp())
            setOnClickListener { d.dismiss() }
        })

        d.show()
    }

    private fun createNotificationTab(ctx: Context, text: String, isActive: Boolean): TextView {
        return TextView(ctx).apply {
            this.text = text
            textSize = 13f
            setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(if (isActive) Color.WHITE else Color.parseColor("#B0C4DE"))
            gravity = Gravity.CENTER
            setPadding(16.dp(), 8.dp(), 16.dp(), 8.dp())
            background = if (isActive) {
                android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 20.dp().toFloat()
                    setColor(Color.parseColor("#33FFFFFF"))
                }
            } else null
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 8.dp()
            }
        }
    }

    /** Custom List Picker Popup ("More dll") */
    fun listPicker(
        ctx: Context, title: String, items: Array<String>,
        selectedIndex: Int = -1, onSelect: (Int) -> Unit
    ) {
        val (d, box) = createModalDialog(ctx, title)

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 16.dp().toFloat()
                setColor(ContextCompat.getColor(ctx, R.color.surface))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            clipToOutline = true
        }

        items.forEachIndexed { i, itemText ->
            if (i > 0) {
                card.addView(View(ctx).apply {
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.stroke))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1.dp())
                })
            }

            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
                val typedValue = TypedValue()
                ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                setBackgroundResource(typedValue.resourceId)
                isClickable = true
                isFocusable = true
            }

            val tv = TextView(ctx).apply {
                text = itemText
                textSize = 14f
                if (i == selectedIndex) setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, if (i == selectedIndex) R.color.primary else R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tv)

            if (i == selectedIndex) {
                val check = ImageView(ctx).apply {
                    setImageResource(R.drawable.ic_check_circle)
                    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.primary))
                    layoutParams = LinearLayout.LayoutParams(20.dp(), 20.dp())
                }
                row.addView(check)
            } else {
                val chev = ImageView(ctx).apply {
                    setImageResource(R.drawable.ic_chevron)
                    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.text_secondary))
                    layoutParams = LinearLayout.LayoutParams(18.dp(), 18.dp())
                }
                row.addView(chev)
            }

            row.setOnClickListener {
                d.dismiss()
                onSelect(i)
            }
            card.addView(row)
        }

        box.addView(card)
        d.show()
    }

    /** Custom Form Popup */
    fun form(
        ctx: Context, title: String, fields: List<Pair<String, String>>,
        password: Set<Int> = emptySet(), onSave: (List<String>) -> Unit
    ) {
        val (d, box) = createModalDialog(ctx, title)

        val edits = fields.mapIndexed { i, (hintStr, initVal) ->
            val fieldBox = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 12.dp()
                }
            }
            fieldBox.addView(TextView(ctx).apply {
                text = hintStr
                textSize = 12f
                setTextColor(Color.parseColor("#EAF0F2"))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 4.dp()
                }
            })

            val et = EditText(ctx).apply {
                hint = hintStr
                setText(initVal)
                setSingleLine()
                textSize = 14f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                setHintTextColor(ContextCompat.getColor(ctx, R.color.icon_gray))
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_input)
                setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
                if (i in password) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            fieldBox.addView(et)
            box.addView(fieldBox)
            et
        }

        // Buttons Row
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8.dp()
            }
        }

        val btnCancel = TextView(ctx).apply {
            text = if (isEn) "Cancel" else "Batal"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14.dp().toFloat()
                setColor(Color.WHITE)
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, 46.dp(), 1f).apply { marginEnd = 8.dp() }
            setOnClickListener { d.dismiss() }
        }

        val btnSave = TextView(ctx).apply {
            text = if (isEn) "Save" else "Simpan"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14.dp().toFloat()
                setColor(ContextCompat.getColor(ctx, R.color.accent))
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, 46.dp(), 1f)
            setOnClickListener {
                d.dismiss()
                onSave(edits.map { it.text.toString().trim() })
            }
        }

        btnRow.addView(btnCancel)
        btnRow.addView(btnSave)
        box.addView(btnRow)

        d.show()
    }

    /** Custom Confirm Popup */
    fun confirm(
        ctx: Context, title: String, msg: String,
        positiveText: String, negativeText: String,
        isDanger: Boolean = false, onConfirm: () -> Unit
    ) {
        val (d, box) = createModalDialog(ctx, title)

        // Message
        box.addView(TextView(ctx).apply {
            text = msg
            textSize = 14f
            setLineSpacing(3.dp().toFloat(), 1.2f)
            setTextColor(Color.parseColor("#EAF0F2"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 20.dp()
            }
        })

        // Buttons Row
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val btnNeg = TextView(ctx).apply {
            text = negativeText
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14.dp().toFloat()
                setColor(Color.WHITE)
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, 46.dp(), 1f).apply { marginEnd = 8.dp() }
            setOnClickListener { d.dismiss() }
        }

        val btnPos = TextView(ctx).apply {
            text = positiveText
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (isDanger) ContextCompat.getColor(ctx, R.color.logout_text) else Color.WHITE)
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14.dp().toFloat()
                setColor(if (isDanger) ContextCompat.getColor(ctx, R.color.logout_bg) else Color.WHITE)
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, 46.dp(), 1f)
            setOnClickListener {
                d.dismiss()
                onConfirm()
            }
        }

        btnRow.addView(btnNeg)
        btnRow.addView(btnPos)
        box.addView(btnRow)

        d.show()
    }

    /** Custom Multi-Choice Switch Picker Popup */
    fun multiChoicePicker(
        ctx: Context, title: String, items: Array<String>,
        checkedState: BooleanArray, onSave: (BooleanArray) -> Unit
    ) {
        val (d, box) = createModalDialog(ctx, title)

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 16.dp().toFloat()
                setColor(ContextCompat.getColor(ctx, R.color.surface))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            clipToOutline = true
        }

        val currentChecked = checkedState.clone()

        items.forEachIndexed { i, itemText ->
            if (i > 0) {
                card.addView(View(ctx).apply {
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.stroke))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1.dp())
                })
            }

            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
                isClickable = true
                isFocusable = true
            }

            val tv = TextView(ctx).apply {
                text = itemText
                textSize = 14f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val switchView = SwitchCompat(ctx).apply {
                isChecked = currentChecked[i]
                setOnCheckedChangeListener { _, isChecked -> currentChecked[i] = isChecked }
            }

            row.addView(tv)
            row.addView(switchView)
            row.setOnClickListener { switchView.toggle() }
            card.addView(row)
        }

        box.addView(card)

        // Buttons Row
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 16.dp()
            }
        }

        val btnCancel = TextView(ctx).apply {
            text = if (isEn) "Cancel" else "Batal"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14.dp().toFloat()
                setColor(Color.WHITE)
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, 46.dp(), 1f).apply { marginEnd = 8.dp() }
            setOnClickListener { d.dismiss() }
        }

        val btnSave = TextView(ctx).apply {
            text = if (isEn) "Save" else "Simpan"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.primary))
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14.dp().toFloat()
                setColor(Color.WHITE)
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, 46.dp(), 1f)
            setOnClickListener {
                d.dismiss()
                onSave(currentChecked)
            }
        }

        btnRow.addView(btnCancel)
        btnRow.addView(btnSave)
        box.addView(btnRow)

        d.show()
    }

    /** Custom Maintenance Notes Dialog */
    fun notesDialog(
        ctx: Context, list: MutableList<String>,
        onAdd: () -> Unit, onDeleteAll: () -> Unit
    ) {
        val (d, box) = createModalDialog(ctx, if (isEn) "Maintenance Notes" else "Catatan Perawatan")

        val scroll = ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 16.dp()
            }
            isFillViewport = true
        }

        val notesCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }

        if (list.isEmpty()) {
            notesCard.addView(TextView(ctx).apply {
                text = if (isEn) "No maintenance notes recorded yet." else "Belum ada catatan perawatan."
                textSize = 13f
                setTextColor(Color.parseColor("#EAF0F2"))
                gravity = Gravity.CENTER
                setPadding(16.dp(), 24.dp(), 16.dp(), 24.dp())
            })
        } else {
            list.forEachIndexed { i, item ->
                notesCard.addView(LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 12.dp().toFloat()
                        setColor(ContextCompat.getColor(ctx, R.color.surface))
                    }
                    setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        if (i > 0) topMargin = 8.dp()
                    }
                    addView(TextView(ctx).apply {
                        text = item
                        textSize = 13f
                        setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                    })
                })
            }
        }

        scroll.addView(notesCard)
        box.addView(scroll)

        // Action Buttons Row
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val btnAdd = TextView(ctx).apply {
            text = if (isEn) "Add Note" else "Tambah Catatan"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.primary))
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14.dp().toFloat()
                setColor(Color.WHITE)
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, 46.dp(), 1f).apply { marginEnd = 8.dp() }
            setOnClickListener {
                d.dismiss()
                onAdd()
            }
        }
        btnRow.addView(btnAdd)

        if (list.isNotEmpty()) {
            val btnDel = TextView(ctx).apply {
                text = if (isEn) "Delete All" else "Hapus Semua"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, R.color.logout_text))
                gravity = Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 14.dp().toFloat()
                    setColor(ContextCompat.getColor(ctx, R.color.logout_bg))
                }
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(0, 46.dp(), 1f).apply { marginEnd = 8.dp() }
                setOnClickListener {
                    d.dismiss()
                    onDeleteAll()
                }
            }
            btnRow.addView(btnDel)
        }

        val btnClose = TextView(ctx).apply {
            text = if (isEn) "Close" else "Tutup"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14.dp().toFloat()
                setColor(Color.WHITE)
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, 46.dp(), 1f)
            setOnClickListener { d.dismiss() }
        }

        btnRow.addView(btnClose)
        box.addView(btnRow)

        d.show()
    }

    fun faq(ctx: Context) {
        val title = if (isEn) "Help & FAQ" else "Bantuan & FAQ"
        val msg = if (isEn) {
            "• How to start navigation?\nOpen Map tab, enter destination, then press \"Start Navigation\".\n\n" +
            "• Where does map data come from?\nOpenStreetMap (map), OSRM (route) and Nominatim (search).\n\n" +
            "• Where is trip history?\nTrips tab. Trips are saved when you press \"End Trip\".\n\n" +
            "• How to edit vehicle data?\nHome → Vehicle → pencil icon or Vehicle Settings."
        } else {
            "• Bagaimana memulai navigasi?\nBuka tab Peta, isi tujuan, lalu tekan \"Mulai Navigasi\".\n\n" +
            "• Dari mana data peta berasal?\nOpenStreetMap (peta), OSRM (rute) dan Nominatim (pencarian lokasi).\n\n" +
            "• Di mana riwayat perjalanan?\nTab Perjalanan. Perjalanan tersimpan saat Anda menekan \"Akhiri Perjalanan\".\n\n" +
            "• Mengubah data kendaraan?\nBeranda → Kendaraan → ikon pensil atau Pengaturan Kendaraan."
        }
        info(ctx, title, msg, iconRes = R.drawable.ic_help)
    }

    fun about(ctx: Context) {
        val title = if (isEn) "About CanDrive" else "Tentang CanDrive"
        val msg = if (isEn) {
            "CanDrive v1.0\nBetter Routes, Smoother Journeys.\n\nMap © OpenStreetMap contributors."
        } else {
            "CanDrive v1.0\nBetter Routes, Smoother Journeys.\n\nPeta © OpenStreetMap contributors."
        }
        info(ctx, title, msg, iconRes = R.drawable.ic_info)
    }

    // ---------- helper peta (osmdroid) ----------
    fun setupMap(m: MapView) {
        m.setTileSource(TileSourceFactory.MAPNIK)
        m.setMultiTouchControls(true)
        m.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        m.isTilesScaledToDpi = true
        m.controller.setZoom(9.0)
        m.controller.setCenter(GeoPoint(-6.5, 107.3))
    }

    fun drawRoute(c: Context, m: MapView, r: RouteResult, withStart: Boolean = true): List<Overlay> {
        val line = Polyline().apply {
            setPoints(r.points)
            outlinePaint.color = Color.parseColor("#2F80ED")
            outlinePaint.strokeWidth = 14f
            outlinePaint.strokeCap = Paint.Cap.ROUND
        }
        val list = mutableListOf<Overlay>(line)
        if (withStart) list.add(Marker(m).apply {
            position = r.points.first()
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setIcon(ContextCompat.getDrawable(c, R.drawable.ic_origin))
            setInfoWindow(null)
        })
        list.add(Marker(m).apply {
            position = r.points.last()
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setIcon(ContextCompat.getDrawable(c, R.drawable.ic_pin_red))
            setInfoWindow(null)
        })
        m.overlays.addAll(list)
        m.invalidate()
        return list
    }

    fun fit(m: MapView, r: RouteResult) {
        if (m.width == 0) return
        m.zoomToBoundingBox(BoundingBox.fromGeoPoints(r.points), true, 140)
    }

    fun pickTiles(c: Context, m: MapView) {
        val title = if (isEn) "Map Layer" else "Lapisan peta"
        val options = if (isEn) {
            arrayOf("Standard (OpenStreetMap)", "Topographic (OpenTopoMap)")
        } else {
            arrayOf("Standar (OpenStreetMap)", "Topografi (OpenTopoMap)")
        }
        listPicker(c, title, options) { i ->
            m.setTileSource(if (i == 0) TileSourceFactory.MAPNIK else TileSourceFactory.OpenTopo)
        }
    }
}
