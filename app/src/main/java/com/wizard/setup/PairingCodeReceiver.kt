// com/wizard/setup/PairingCodeReceiver.kt

package com.wizard.setup

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class PairingCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.ACTION_SET_PAIRING_CODE) {
            val remoteInput = RemoteInput.getResultsFromIntent(intent)
            remoteInput?.getCharSequence(Constants.KEY_PAIRING_CODE)?.let { code ->
                val serviceIntent = Intent(context, SetupService::class.java).apply {
                    putExtra(Constants.EXTRA_PAIRING_CODE_RECEIVED, code.toString())
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}