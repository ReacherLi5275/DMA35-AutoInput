package com.example.dma35

import android.os.Handler
import android.os.Looper

/** Main-thread event for live IME; durable pending values are stored separately. */
object DensityBus {
    private val main = Handler(Looper.getMainLooper())
    var listener: (() -> Unit)? = null
    fun notifyMeasurement() { main.post { listener?.invoke() } }
}
