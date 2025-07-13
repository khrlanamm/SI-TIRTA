package com.khrlanamm.sitirta.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.khrlanamm.sitirta.MainActivity
import com.khrlanamm.sitirta.R
import com.khrlanamm.sitirta.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        if (auth.currentUser != null) {
            navigateToMain(auth.currentUser)
            return
        }

        configureGoogleSignIn()

        binding.buttonGoogleSignIn.setOnClickListener {
            signIn()
        }
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Ambil dari file google-services.json
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        updateUI(true) // Tampilkan loading
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("AuthActivity", "Google sign in failed", e)
                Toast.makeText(this, "Gagal masuk dengan Google: ${e.message}", Toast.LENGTH_SHORT).show()
                updateUI(false) // Sembunyikan loading
            }
        } else {
            Log.w("AuthActivity", "Google sign in flow cancelled. Result code: ${result.resultCode}")
            Toast.makeText(this, "Login dengan Google dibatalkan", Toast.LENGTH_SHORT).show()
            updateUI(false) // Sembunyikan loading
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("AuthActivity", "signInWithCredential_firebase:success")
                    val user = task.result.user

                    // PENAMBAHAN TOAST DI SINI
                    Toast.makeText(this, "Berhasil login sebagai ${user?.displayName}", Toast.LENGTH_SHORT).show()

                    navigateToMain(user)
                } else {
                    Log.w("AuthActivity", "signInWithCredential_firebase:failure", task.exception)
                    Toast.makeText(this, "Autentikasi Firebase gagal.", Toast.LENGTH_SHORT).show()
                    updateUI(false)
                }
            }
    }

    private fun navigateToMain(user: FirebaseUser?) {
        if (user == null) return
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER_NAME", user.displayName)
            putExtra("USER_PHOTO_URL", user.photoUrl?.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun updateUI(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonGoogleSignIn.isEnabled = !isLoading
    }
}