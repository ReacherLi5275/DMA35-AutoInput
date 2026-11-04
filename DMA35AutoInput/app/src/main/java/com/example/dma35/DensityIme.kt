package com.example.dma35

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class DensityIme : InputMethodService() {
    private var enabled = false
    private var status: TextView? = null
    override fun onCreateInputView(): View {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 12, 20, 12) }
        status = TextView(this).apply { text = "DMA35 Keyboard: Auto input OFF"; textSize = 16f }
        val toggle = Button(this).apply {
            text = "Enable auto input (TEST ONLY)"
            setOnClickListener {
                enabled = !enabled
                text = if (enabled) "Disable auto input" else "Enable auto input (TEST ONLY)"
                status?.text = if (enabled) "ARMED: focused field receives next density" else "Auto input OFF"
            }
        }
        layout.addView(status)
        layout.addView(toggle)
        return layout
    }
    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        enabled = false // Require explicit arming for each field to prevent accidental entries.
        status?.text = "Auto input OFF; arm for current field"
        DensityBus.listener = { value ->
            if (enabled && currentInputConnection != null) {
                currentInputConnection?.beginBatchEdit()
                currentInputConnection?.deleteSurroundingText(100, 100)
                currentInputConnection?.commitText(value, 1)
                currentInputConnection?.endBatchEdit()
                status?.text = "Inserted: $value (auto input OFF)"
                enabled = false
            }
        }
    }
    override fun onFinishInputView(finishingInput: Boolean) {
        enabled = false
        DensityBus.listener = null
        super.onFinishInputView(finishingInput)
    }
}
