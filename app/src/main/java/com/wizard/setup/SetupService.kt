package com.wizard.setup

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import dev.mobile.dadb.Dadb
import kotlinx.coroutines.*

class SetupService : Service() {

    private lateinit var nsdManager: NsdManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        var deviceIp: String? = null
        var devicePort: Int? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(Constants.NOTIFICATION_ID, createInitialNotification())

        val receivedCode = intent?.getStringExtra(Constants.EXTRA_PAIRING_CODE_RECEIVED)
        if (receivedCode != null) {
            handlePairing(receivedCode)
        } else {
            startDeviceDiscovery()
        }
        return START_NOT_STICKY
    }

    private fun startDeviceDiscovery() {
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdManager.discoverServices("_adb-tls-pairing._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    // YEH CHANGE HUA HAI: Variable ka type explicitly define kiya hai
    private val discoveryListener: NsdManager.DiscoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {}
        override fun onServiceFound(service: NsdServiceInfo) {
            nsdManager.resolveService(service, resolveListener)
        }
        override fun onServiceLost(service: NsdServiceInfo) {}
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { try { nsdManager.stopServiceDiscovery(this) } catch (e: Exception) {} }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { try { nsdManager.stopServiceDiscovery(this) } catch (e: Exception) {} }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            deviceIp = serviceInfo.host.hostAddress
            devicePort = serviceInfo.port
            try { nsdManager.stopServiceDiscovery(discoveryListener) } catch (e: Exception) {}

            val broadcastIntent = Intent(Constants.ACTION_DEVICE_FOUND).apply {
                putExtra(Constants.EXTRA_IP_ADDRESS, deviceIp)
                putExtra(Constants.EXTRA_PORT, devicePort)
            }
            sendBroadcast(broadcastIntent)
            updateNotificationForPairing()
        }
    }

    private fun handlePairing(code: String) {
        val ip = deviceIp ?: return
        val port = devicePort ?: return
        
        serviceScope.launch {
            try {
                updateNotificationStatus("Pairing...")
                Dadb.pair(ip, port, code).use { dadb ->
                    updateNotificationStatus("Pairing Successful. Running commands...")
                    runSetupCommands(dadb)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateNotificationStatus("Pairing Failed: ${e.message}")
            } finally {
                delay(5000)
                stopSelf()
            }
        }
    }

    private suspend fun runSetupCommands(dadb: Dadb) {
        withContext(Dispatchers.Main) {
            updateNotificationStatus("Setup Complete!")
        }
    }

    private fun updateNotificationStatus(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Setup Progress")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(false)
            .build()
        notificationManager.notify(Constants.NOTIFICATION_ID, notification)
    }

    private fun updateNotificationForPairing() {
        val remoteInput = RemoteInput.Builder(Constants.KEY_PAIRING_CODE).run {
            setLabel("Enter Pairing Code")
            build()
        }
        val replyIntent = Intent(this, PairingCodeReceiver::class.java).apply {
            action = Constants.ACTION_SET_PAIRING_CODE
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            this, 0, replyIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val action = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send, "ENTER CODE", replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Device Found!")
            .setContentText("Ready to pair. Enter code from target device.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(action)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.NOTIFICATION_ID, notification)
    }
    
    private fun createInitialNotification() = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
        .setContentTitle("Setup Service")
        .setContentText("Searching for wireless debugging devices...")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setOngoing(true)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                "Setup Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (this::nsdManager.isInitialized) {
            try { nsdManager.stopServiceDiscovery(discoveryListener) } catch (e: Exception) {}
        }
    }
}
