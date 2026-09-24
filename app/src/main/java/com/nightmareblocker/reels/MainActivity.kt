package com.nightmareblocker.reels

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.nightmareblocker.reels.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.enableToggle.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setEnabled(this, isChecked)
            updateStatusLabel(isChecked)
        }

        binding.resetButton.setOnClickListener {
            Prefs.resetScareCount(this)
            binding.scareCountValue.text = "0"
        }

        binding.grantAccessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        val enabled = Prefs.isEnabled(this)
        binding.enableToggle.isChecked = enabled
        updateStatusLabel(enabled)
        binding.scareCountValue.text = Prefs.getScareCount(this).toString()

        val serviceRunning = isAccessibilityServiceEnabled()
        binding.serviceStatusValue.text = if (serviceRunning) "GRANTED" else "NOT GRANTED"
        binding.grantAccessibilityButton.text =
            if (serviceRunning) "Open Accessibility Settings" else "Grant Accessibility Access"
    }

    private fun updateStatusLabel(enabled: Boolean) {
        binding.statusLabel.text = if (enabled) "ACTIVE" else "SLEEPING"
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = "$packageName/${ReelsAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }
}
