package com.example.localbase.view

import android.app.ActivityOptions
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.localbase.R
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import com.example.localbase.databinding.ActivitySplashBinding
import com.example.localbase.helper.ToolBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Splash : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var outerCircle: View
    private lateinit var innerCircle: View
    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())

    @Inject
    lateinit var toolBar: ToolBar

    private lateinit var binding: ActivitySplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize views
        progressBar = findViewById(R.id.progressBar)
        loadingText = findViewById(R.id.loadingText)
        outerCircle = findViewById(R.id.outerCircle)
        innerCircle = findViewById(R.id.innerCircle)

        // Start animations
        startAnimations()

        // Start loading progress
        simulateLoading()
    }

    private fun startAnimations() {
        // Pulse animation for outer circle
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)
        outerCircle.startAnimation(pulseAnim)

        // Rotate animation for inner circle
        val rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_animation)
        innerCircle.startAnimation(rotateAnim)
    }

    private fun simulateLoading() {
        Thread {
            while (progressStatus < 100) {
                progressStatus += 2

                // Update UI on main thread
                handler.post {
                    progressBar.progress = progressStatus
                    updateLoadingText(progressStatus)
                }

                try {
                    // Simulate loading time
                    Thread.sleep(30)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }

            // Navigate to main activity after completion
            handler.postDelayed({
                navigateToMainActivity()
            }, 2000)
        }.start()
    }

    private fun updateLoadingText(progress: Int) {
        loadingText.text = when {
            progress < 30 -> getString(R.string.loading_init)
            progress < 60 -> getString(R.string.loading_ai)
            progress < 90 -> getString(R.string.loading_prep)
            else -> getString(R.string.loading_ready)
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, Dashboard::class.java)
        val options = toolBar.custAni()
        startActivity(intent, options.toBundle())
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}