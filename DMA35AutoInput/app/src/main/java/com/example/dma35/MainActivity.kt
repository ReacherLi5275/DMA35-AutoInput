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
    private val exportRequest = 201
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 20) }
        fun addButton(title: String, action: () -> Unit) { layout.addView(Button(this).apply { text = title; setOnClickListener { action() } }) }
        layout.addView(TextView(this).apply { text = "DMA35 Auto Input — TEST v2.0"; textSize = 23f })
        status = TextView(this).apply { textSize = 16f; setPadding(0, 16, 0, 8) }
        latest = TextView(this).apply { textSize = 25f; setPadding(0, 8, 0, 16) }
        layout.addView(status); layout.addView(latest)
        addButton("1. Request Bluetooth permission") { requestBluetooth() }
        addButton("2. Start RFCOMM server") { startReceiver() }
        addButton("Stop receiver") { stopService(Intent(this, ReceiverService::class.java)); refresh() }
        addButton("3. Enable DMA35 keyboard") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        addButton("4. Choose DMA35 keyboard") { (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showInputMethodPicker() }
        layout.addView(TextView(this).apply { text = "5. Tap the field, select DMA35 keyboard and ARM before simulating or measuring:" })
        layout.addView(EditText(this).apply { hint = "Density @25°C"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL })
        addButton("SIMULATE MEASUREMENT (1.2452)") {
            val raw = "MeasNo: 747\nSample ID: 0747\nDensity@25°C: 1.2452 g/cm³\nDensity: 1.2462 g/cm³\n"
            val value = MeasurementStore.ingest(this, raw, "SIMULATION")
            Toast.makeText(this, if (value != null) "Saved simulated value: $value" else "Duplicate ignored", Toast.LENGTH_SHORT).show()
            refresh()
        }
        addButton("EXPORT CSV FOR EXCEL") {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "text/csv"; putExtra(Intent.EXTRA_TITLE, "DMA35_measurements.csv") }
            startActivityForResult(intent, exportRequest)
        }
        addButton("Refresh status") { refresh() }
        layout.addView(TextView(this).apply { text = "TEST ONLY. Simulated measurements are labeled SIMULATION. CSV exports must be reviewed before use. The keyboard only inserts into the currently focused field when manually armed; it does not submit forms."; gravity = Gravity.CENTER_HORIZONTAL })
        setContentView(ScrollView(this).apply { addView(layout) })
        refresh()
    }
    @Deprecated("Legacy result API supported for test app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == exportRequest && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try { contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(MeasurementStore.csv(this)) }
                ?: error("Cannot open output file")
                Toast.makeText(this, "CSV exported", Toast.LENGTH_LONG).show()
            } catch (e: Exception) { Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }
    private fun requestBluetooth() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 100)
        else Toast.makeText(this, "Bluetooth permission available", Toast.LENGTH_SHORT).show()
    }
    private fun startReceiver() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { requestBluetooth(); return }
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
