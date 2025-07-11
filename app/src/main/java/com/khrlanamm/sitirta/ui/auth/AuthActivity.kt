package com.khrlanamm.sitirta.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.common.SignInButton
import com.khrlanamm.sitirta.MainActivity
import com.khrlanamm.sitirta.R

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- TAMBAHKAN KODE INI ---

        // 1. Temukan tombol dari layout menggunakan ID-nya
        val googleSignInButton = findViewById<SignInButton>(R.id.button_google_sign_in)

        // 2. Atur OnClickListener pada tombol
        googleSignInButton.setOnClickListener {
            // 3. Buat Intent untuk berpindah ke MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // 4. (Opsional) Selesaikan AuthActivity agar pengguna tidak bisa kembali
            //    ke halaman ini dengan menekan tombol kembali.
            finish()
        }
        // -------------------------
    }
}