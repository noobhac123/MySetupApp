package com.wizard.setup

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

// This helper class runs code on the main UI thread.
class MainExecutor : Executor {
    private val handler = Handler(Looper.getMainLooper())

    override fun execute(command: Runnable) {
        handler.post(command)
    }
}
