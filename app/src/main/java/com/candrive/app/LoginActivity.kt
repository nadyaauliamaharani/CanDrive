package com.candrive.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.candrive.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private var showPass = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        Ui.statusBar(this, ContextCompat.getColor(this, R.color.bg), !Ui.isNight(this))

        b.btnEye.setOnClickListener {
            showPass = !showPass
            b.etPass.inputType = InputType.TYPE_CLASS_TEXT or
                (if (showPass) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD)
            b.etPass.setSelection(b.etPass.text.length)
            b.btnEye.setImageResource(if (showPass) R.drawable.ic_eye else R.drawable.ic_eye_off)
        }
        b.btnLogin.setOnClickListener { login() }
        b.tvForgot.setOnClickListener {
            Ui.form(this, "Lupa password", listOf("Email" to b.etEmail.text.toString())) { v ->
                if (Patterns.EMAIL_ADDRESS.matcher(v[0]).matches())
                    Toast.makeText(this, "Tautan reset password dikirim ke ${v[0]} (simulasi)", Toast.LENGTH_LONG).show()
                else Toast.makeText(this, "Email tidak valid", Toast.LENGTH_SHORT).show()
            }
        }
        val oauth = { name: String ->
            Toast.makeText(this, "Login $name butuh konfigurasi OAuth — lihat README. Mode demo: masuk langsung.", Toast.LENGTH_LONG).show()
            enter()
        }
        b.btnGoogle.setOnClickListener { oauth("Google") }
        b.btnApple.setOnClickListener { oauth("Apple") }
        b.tvRegister.setOnClickListener {
            Ui.form(this, "Daftar akun", listOf("Nama lengkap" to "", "Email" to "", "Password (min. 6 karakter)" to ""), setOf(2)) { v ->
                when {
                    v[0].isEmpty() -> toast("Nama wajib diisi")
                    !Patterns.EMAIL_ADDRESS.matcher(v[1]).matches() -> toast("Email tidak valid")
                    v[2].length < 6 -> toast("Password minimal 6 karakter")
                    else -> { Store.userName = v[0]; Store.userEmail = v[1]; toast("Akun dibuat"); enter() }
                }
            }
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun login() {
        val id = b.etEmail.text.toString().trim()
        val pw = b.etPass.text.toString()
        val ok = Patterns.EMAIL_ADDRESS.matcher(id).matches() ||
            (id.all { it.isDigit() || it == '+' } && id.count { it.isDigit() } >= 9)
        if (!ok) { b.etEmail.error = "Masukkan email atau nomor HP yang valid"; return }
        if (pw.length < 6) { b.etPass.error = "Password minimal 6 karakter"; return }
        if (!b.cbTerms.isChecked) {
            toast("Harap setujui Syarat & Ketentuan terlebih dahulu")
            return
        }
        if (id.contains("@")) Store.userEmail = id
        enter()
    }

    private fun enter() {
        Store.loggedIn = "1"
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
