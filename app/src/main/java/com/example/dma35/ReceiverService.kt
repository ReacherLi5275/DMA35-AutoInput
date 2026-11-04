package com.example.dma35

import android.app.*
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.concurrent.thread

class ReceiverService : Service() {
    private val lock = Any()
    @Volatile private var running = false
    @Volatile private var server: BluetoothServerSocket? = null
    @Volatile private var client: BluetoothSocket? = null
    @Volatile private var worker: Thread? = null
    private val spp = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = "dma35_receiver"
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(channel, "DMA35 Bluetooth receiver", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val note = Notification.Builder(this, channel).setContentTitle("DMA35 Bridge active")
            .setContentText("Waiting for DMA35 print data")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentIntent(open).build()
        startForeground(41, note)
        running = true
        worker = thread(name = "dma35-rfcomm") { receiveLoop() }
    }

    private fun prefs() = getSharedPreferences("dma35", MODE_PRIVATE)
    private fun state(s: String) { prefs().edit().putString("status", s).apply() }
    private fun detail(key: String, value: String) { prefs().edit().putString(key, value).apply() }
    private fun inc(key: String, amount: Long = 1) {
        synchronized(lock) { val p = prefs(); p.edit().putLong(key, p.getLong(key, 0) + amount).apply() }
    }
    private fun error(e: Exception) {
        val message = "${e.javaClass.simpleName}: ${e.message ?: "unknown"}"
        detail("lastError", message)
        state("Error: $message")
    }
    @Suppress("MissingPermission")
    private fun receiveLoop() {
        while (running) {
            try {
                val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                    ?: throw IllegalStateException("Bluetooth unavailable")
                if (!adapter.isEnabled) throw IllegalStateException("Bluetooth is off")
                val listener = adapter.listenUsingRfcommWithServiceRecord("Serial Port Profile (SPP)", spp)
                server = listener
                state("Listening for DMA35")
                while (running) {
                    val socket = listener.accept()
                    if (!running) { socket.close(); break }
                    client = socket
                    inc("connections")
                    detail("lastConnection", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
                    detail("lastError", "None")
                    state("Connected: ${socket.remoteDevice.name ?: socket.remoteDevice.address}")
                    try { readClient(socket) }
                    catch (e: Exception) { if (running) error(e) }
                    finally {
                        try { socket.close() } catch (_: Exception) { }
                        client = null
                    }
                    if (running) state("Listening for DMA35")
                }
            } catch (e: SecurityException) { error(e); break }
            catch (e: Exception) { if (running) error(e) }
            finally {
                try { server?.close() } catch (_: Exception) { }
                server = null
            }
            if (running) try { Thread.sleep(1500) } catch (_: InterruptedException) { break }
        }
    }
    private fun readClient(socket: BluetoothSocket) {
        val input = socket.inputStream
        val buffer = ByteArray(1024)
        val line = ByteArrayOutputStream()
        val record = StringBuilder()
        while (running) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            inc("receivedBytes", count.toLong())
            for (i in 0 until count) {
                val b = buffer[i].toInt() and 255
                if (b == 10 || b == 13) {
                    if (line.size() > 0) {
                        val sLine = line.toString("ISO-8859-1").trim()
                        line.reset()
                        if (sLine.startsWith("MeasNo:", true) && record.isNotEmpty()) record.setLength(0)
                        record.append(sLine).append('\n')
                        if (sLine.contains("Density@25", true)) {
                            val value = MeasurementStore.ingest(this, record.toString(), "DMA35")
                            detail("lastParse", if (value != null) "Accepted: $value" else "Rejected or duplicate: $sLine")
                            record.setLength(0)
                        }
                        if (record.length > 16384) record.setLength(0)
                    }
                } else if (line.size() < 8192) line.write(b) else line.reset()
            }
        }
        // Some printer implementations close without a trailing CR/LF.
        if (line.size() > 0) {
            val last = line.toString("ISO-8859-1").trim()
            if (last.contains("Density@25", true)) {
                record.append(last).append('\n')
                val value = MeasurementStore.ingest(this, record.toString(), "DMA35")
                detail("lastParse", if (value != null) "Accepted: $value" else "Rejected or duplicate: $last")
            }
        }
    }
    override fun onDestroy() {
        running = false
        try { client?.close() } catch (_: Exception) { }
        try { server?.close() } catch (_: Exception) { }
        worker?.interrupt()
        state("Stopped")
        super.onDestroy()
    }
}
