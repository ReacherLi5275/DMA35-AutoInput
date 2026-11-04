package com.example.dma35

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*

/** InputMethodService is intentionally used instead of Accessibility injection. */
class DensityIme : InputMethodService() {
    private var label: TextView? = null
    private var active = false
    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 10, 18, 10) }
        label = TextView(this).apply { textSize = 15f; text = "DMA35 Bridge · Ready" }
        root.addView(label)
        val row = LinearLayout(this)
        row.addView(Button(this).apply { text = "PAUSE / RESUME"; setOnClickListener {
            val p = getSharedPreferences("dma35", MODE_PRIVATE)
            p.edit().putBoolean("auto", !p.getBoolean("auto", true)).apply()
            updateLabel()
        } }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply { text = "NEXT"; setOnClickListener { advance() } }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        updateLabel()
        return root
    }
    private fun updateLabel() {
        val p = getSharedPreferences("dma35", MODE_PRIVATE)
        label?.text = "DMA35 · ${if (p.getBoolean("auto", true)) "AUTO" else "PAUSED"} · ${p.getString("direction", "DOWN")}" 
    }
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        active = true
        DensityBus.listener = { value ->
            val p = getSharedPreferences("dma35", MODE_PRIVATE)
            if (active && p.getBoolean("auto", true)) insert(value)
        }
    }
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateLabel()
    }
    private fun insert(value: String) {
        val ic = currentInputConnection ?: return
        // Select all existing field content, then replace. Do not submit the form.
        ic.beginBatchEdit()
        ic.performContextMenuAction(android.R.id.selectAll)
        ic.commitText(value, 1)
        ic.endBatchEdit()
        getSharedPreferences("dma35", MODE_PRIVATE).edit().putString("lastInserted", value).apply()
        if (getSharedPreferences("dma35", MODE_PRIVATE).getBoolean("advance", true)) advance()
        updateLabel()
    }
    private fun advance() {
        val ic = currentInputConnection ?: return
        when (getSharedPreferences("dma35", MODE_PRIVATE).getString("direction", "DOWN")) {
            "RIGHT" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB))
            }
            else -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
            }
        }
    }
    override fun onFinishInput() {
        active = false
        DensityBus.listener = null
        super.onFinishInput()
    }
    override fun onDestroy() {
        DensityBus.listener = null
        super.onDestroy()
    }
}
