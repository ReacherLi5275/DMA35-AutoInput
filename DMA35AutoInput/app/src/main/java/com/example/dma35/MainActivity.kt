package com.example.dma35

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 40, 32, 20) }
        fun addButton(title: String, action: () -> Unit) {
            layout.addView(Button(this).apply { text = title; setOnClickListener { action() } })
        }
        layout.addView(TextView(this).apply { text = "DMA35 Auto Input — TEST v1.0"; textSize = 23f })
        status = TextView(this).apply { textSize = 16f; setPadding(0, 22, 0, 8) }
        latest = TextView(this).apply { textSize = 25f; setPadding(0, 8, 0, 20) }
        layout.addView(status); layout.addView(latest)
        addButton("1. Request Bluetooth permission") { requestBluetooth() }
        addButton("2. Start RFCOMM server") { startReceiver() }
        addButton("Stop receiver") { stopService(Intent(this, ReceiverService::class.java)); refresh() }
        addButton("3. Enable DMA35 keyboard") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        addButton("4. Choose DMA35 keyboard") {
            (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showInputMethodPicker()
        }
        layout.addView(TextView(this).apply { text = "5. Test input below (tap field; arm keyboard before each measurement)" })
        layout.addView(EditText(this).apply { hint = "Density @25°C"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL })
        addButton("Refresh status") { refresh() }
        layout.addView(TextView(this).apply { text = "Test only. Do not use for production records. Keep the phone discoverable when DMA35 searches for a printer."; gravity = Gravity.CENTER_HORIZONTAL })
        setContentView(ScrollView(this).apply { addView(layout) })
        refresh()
    }
    private fun requestBluetooth() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 100)
        else Toast.makeText(this, "Bluetooth permission available", Toast.LENGTH_SHORT).show()
    }
    private fun startReceiver() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBluetooth(); return
        }
        try { startForegroundService(Intent(this, ReceiverService::class.java)) }
        catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_LONG).show() }
        refresh()
    }
    private fun refresh() {
        val prefs = getSharedPreferences("dma35", MODE_PRIVATE)
        status.text = "Bluetooth: ${prefs.getString("status", "Not started")}" 
        latest.text = "Latest Density@25°C: ${prefs.getString("latest", "—")}" 
    }
    override fun onResume() { super.onResume(); if (::status.isInitialized) refresh() }
}
