// com/wizard/setup/MainActivity.kt

package com.wizard.setup

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wizard.setup.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val deviceFoundReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Constants.ACTION_DEVICE_FOUND) {
                val ip = intent.getStringExtra(Constants.EXTRA_IP_ADDRESS)
                val port = intent.getIntExtra(Constants.EXTRA_PORT, 0)
                binding.tvIpAddress.text = "IP Address: $ip"
                binding.tvPort.text = "Port: $port"
            }
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startSetupService()
            } else {
                Toast.makeText(this, "Notification permission is required for setup.", Toast.LENGTH_LONG).show()
            }
        }

    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                copyApkToDownloads()
            } else {
                Toast.makeText(this, "Storage permission is required to copy APK.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            setupModernDeviceLayout()
        } else { // Android 10 or older
            setupLegacyDeviceLayout()
        }
    }

    private fun setupModernDeviceLayout() {
        binding.modernDeviceLayout.visibility = View.VISIBLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startSetupService()
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startSetupService()
        }
        val filter = IntentFilter(Constants.ACTION_DEVICE_FOUND)
        ContextCompat.registerReceiver(this, deviceFoundReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    
    private fun startSetupService() {
        val serviceIntent = Intent(this, SetupService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun setupLegacyDeviceLayout() {
        binding.legacyDeviceLayout.visibility = View.VISIBLE
        binding.btnCopyApk.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                copyApkToDownloads()
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun copyApkToDownloads() {
        try {
            val inputStream = assets.open("TargetApp.apk")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outputFile = File(downloadsDir, "TargetApp.apk")
            
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            Toast.makeText(this, "TargetApp.apk copied to Downloads folder.", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to copy APK: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister receiver only if it was for modern layout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            unregisterReceiver(deviceFoundReceiver)
        }
    }
}