// com/wizard/setup/Constants.kt

package com.wizard.setup

object Constants {
    // Notification
    const val NOTIFICATION_CHANNEL_ID = "SetupServiceChannel"
    const val NOTIFICATION_ID = 1

    // Actions
    const val ACTION_DEVICE_FOUND = "com.wizard.setup.DEVICE_FOUND"
    const val ACTION_SET_PAIRING_CODE = "com.wizard.setup.SET_PAIRING_CODE"

    // Remote Input Key
    const val KEY_PAIRING_CODE = "pairing_code"

    // Intent Extras
    const val EXTRA_IP_ADDRESS = "ip_address"
    const val EXTRA_PORT = "port"
    const val EXTRA_PAIRING_CODE_RECEIVED = "pairing_code_received"
}