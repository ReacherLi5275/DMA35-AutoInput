package com.example.dma35

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*

class DensityIme : InputMethodService() {
    private var label: TextView? = null
    private var active = false
    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 10, 18, 10) }
        label = TextView(this).apply { textSize = 15f }
        root.addView(label)
        val row = LinearLayout(this)
        row.addView(Button(this).apply { text = "PAUSE / RESUME"; setOnClickListener {
            val p = getSharedPreferences("dma35", MODE_PRIVATE)
            p.edit().putBoolean("auto", !p.getBoolean("auto", true)).apply()
            updateLabel()
        } }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply { text = "INSERT PENDING"; setOnClickListener { tryInsert() } }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        updateLabel()
        return root
    }
    private fun updateLabel() {
        val p = getSharedPreferences("dma35", MODE_PRIVATE)
        label?.text = "DMA35 · ${if (p.getBoolean("auto", true)) "AUTO" else "PAUSED"} · ${p.getString("direction", "DOWN")} · ${if (PendingInput.peek(this) != null) "PENDING" else "READY"}"
    }
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        active = true
        DensityBus.listener = { if (getSharedPreferences("dma35", MODE_PRIVATE).getBoolean("auto", true)) tryInsert() }
        updateLabel()
        // Do not auto-insert a stale measurement when a different field gains focus.
    }
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateLabel()
    }
    private fun tryInsert() {
        val value = PendingInput.peek(this) ?: return
        val ic = currentInputConnection ?: return
        if (!active) return
        try {
            ic.beginBatchEdit()
            val selected = ic.performContextMenuAction(android.R.id.selectAll)
            val committed = ic.commitText(value, 1)
            ic.endBatchEdit()
            if (!committed) {
                getSharedPreferences("dma35", MODE_PRIVATE).edit().putString("inputStatus", "WAITING FOR FIELD: commit rejected").apply()
                return
            }
            PendingInput.acknowledge(this, value)
            if (getSharedPreferences("dma35", MODE_PRIVATE).getBoolean("advance", true)) advance()
        } catch (e: Exception) {
            getSharedPreferences("dma35", MODE_PRIVATE).edit().putString("inputStatus", "WAITING FOR FIELD: ${e.javaClass.simpleName}").apply()
        }
        updateLabel()
    }
    private fun advance() {
        val ic = currentInputConnection ?: return
        val p = getSharedPreferences("dma35", MODE_PRIVATE)
        val direction = p.getString("direction", "DOWN")
        val action = if (direction == "RIGHT") EditorInfo.IME_ACTION_NEXT else EditorInfo.IME_ACTION_DONE
        // Host applications vary: performEditorAction may commit, not move the selection.
        if (direction == "RIGHT") {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB))
        } else {
            ic.performEditorAction(action)
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
        }
    }
    override fun onFinishInput() {
        active = false
        DensityBus.listener = null
        super.onFinishInput()
    }
    override fun onDestroy() { DensityBus.listener = null; super.onDestroy() }
}
