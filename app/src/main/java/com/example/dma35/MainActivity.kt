package com.example.dma35

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var latest: TextView
    private lateinit var mode: Switch
    private lateinit var advance: Switch
    private lateinit var direction: Spinner
    private val exportRequest = 201
    private val p by lazy { getSharedPreferences("dma35", MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 30, 28, 30) }
        fun text(t: String, size: Float = 15f) = TextView(this).apply { text = t; textSize = size; setPadding(0, 7, 0, 7) }
        fun button(t: String, f: () -> Unit) { root.addView(Button(this).apply { text = t; isAllCaps = false; setOnClickListener { f() } }) }
        root.addView(text("DMA35 Bridge", 28f))
        root.addView(text("Bluetooth density capture · v3.2 field trial", 14f))
        status = text("", 16f); latest = text("", 31f)
        root.addView(status); root.addView(latest)
        mode = Switch(this).apply { text = "Automatic input"; isChecked = p.getBoolean("auto", true); setOnCheckedChangeListener { _, b -> p.edit().putBoolean("auto", b).apply() } }
        root.addView(mode)
        advance = Switch(this).apply { text = "Advance after input"; isChecked = p.getBoolean("advance", true); setOnCheckedChangeListener { _, b -> p.edit().putBoolean("advance", b).apply() } }
        root.addView(advance)
        root.addView(text("Next field direction"))
        direction = Spinner(this)
        direction.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("DOWN", "RIGHT"))
        direction.setSelection(if (p.getString("direction", "DOWN") == "RIGHT") 1 else 0)
        direction.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { p.edit().putString("direction", if (position == 0) "DOWN" else "RIGHT").apply() }
        }
        root.addView(direction)
        button("Start Bluetooth receiver") { startReceiver() }
        button("Stop receiver") { stopService(Intent(this, ReceiverService::class.java)); p.edit().putBoolean("startOnce", false).putString("status", "Stopped").apply(); refresh() }
        button("Enable DMA35 keyboard") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        button("Select DMA35 keyboard") { (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showInputMethodPicker() }
        button("Export CSV for Excel") {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "text/csv"; putExtra(Intent.EXTRA_TITLE, "DMA35_measurements.csv") }
            startActivityForResult(intent, exportRequest)
        }
        button("Refresh status") { refresh() }
        button("Clear pending input") { PendingInput.clear(this); refresh() }
        root.addView(text("Receives only data transmitted by DMA35. Save-to-send must be enabled on the instrument, if supported. Input requires the DMA35 keyboard and a focused editable field. Verify field navigation in each target app. Do not use for controlled production records until site validation is complete.", 12f).apply { gravity = Gravity.CENTER_HORIZONTAL })
        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
        if (p.getBoolean("startOnce", false)) startReceiver() // user previously opted in
    }
    @Deprecated("Legacy result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == exportRequest && resultCode == RESULT_OK) {
            try {
                val uri = data?.data ?: return
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(MeasurementStore.csv(this)) } ?: error("Cannot open file")
                Toast.makeText(this, "CSV exported", Toast.LENGTH_LONG).show()
            } catch (e: Exception) { Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }
    private fun startReceiver() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 100); return
        }
        try {
            p.edit().putBoolean("startOnce", true).apply()
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(Intent(this, ReceiverService::class.java)) else startService(Intent(this, ReceiverService::class.java))
        } catch (e: Exception) { Toast.makeText(this, "Start failed: ${e.message}", Toast.LENGTH_LONG).show() }
        refresh()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startReceiver()
    }
    private fun refresh() {
        status.text = "Bluetooth · ${p.getString("status", "Not started")}" 
        latest.text = p.getString("latest", "—") ?: "—"
        status.append("\nInput: ${p.getString("inputStatus", "Waiting for measurement")}\nPending: ${if (PendingInput.peek(this) != null) "1" else "0"}")
    }
    override fun onResume() { super.onResume(); if (::status.isInitialized) refresh() }
}
