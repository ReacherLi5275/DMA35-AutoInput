package com.example.dma35

import android.os.Handler
import android.os.Looper

object DensityBus {
    private val main = Handler(Looper.getMainLooper())
    var listener: ((String) -> Unit)? = null
    fun publish(value: String) { main.post { listener?.invoke(value) } }
}
