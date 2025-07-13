package com.khrlanamm.sitirta.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.khrlanamm.sitirta.R

@Suppress("DEPRECATION")
class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Implement full screen immersive mode using WindowManager flags
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_onboarding)
        playVideo()
    }

    private fun playVideo() {
        val videoView = findViewById<VideoView>(R.id.splash_video_view)
        val videoPath = "android.resource://" + packageName + "/" + R.raw.splash_video
        val uri = Uri.parse(videoPath)

        videoView.setVideoURI(uri)

        // Use setOnPreparedListener for better control when the video is ready
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.start()
        }

        videoView.setOnCompletionListener {
            navigateToAuth()
        }

        videoView.setOnErrorListener { _, _, _ ->
            navigateToAuth()
            true
        }
    }

    private fun navigateToAuth() {
        if (isFinishing) {
            return
        }
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }
}