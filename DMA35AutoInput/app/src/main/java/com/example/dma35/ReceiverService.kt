package com.example.dma35

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.concurrent.thread

class ReceiverService : Service() {
    private var server: BluetoothServerSocket? = null
    @Volatile private var running = false
    private val spp = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        val channel = "dma35_receiver"
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(channel, "DMA35 receiver", NotificationManager.IMPORTANCE_LOW))
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val note = Notification.Builder(this, channel).setContentTitle("DMA35 receiver active")
            .setContentText("Waiting for DMA35 Bluetooth SPP").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(open).build()
        startForeground(41, note)
        running = true
        thread(name = "dma35-rfcomm") { receiveLoop() }
    }
    private fun state(message: String) { getSharedPreferences("dma35", MODE_PRIVATE).edit().putString("status", message).apply() }
    @Suppress("MissingPermission")
    private fun receiveLoop() {
        try {
            val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter: BluetoothAdapter = manager.adapter ?: error("Bluetooth unavailable")
            server = adapter.listenUsingRfcommWithServiceRecord("Serial Port Profile (SPP)", spp)
            state("Listening for DMA35")
            while (running) {
                val socket = server?.accept() ?: break
                state("Connected: ${socket.remoteDevice.name ?: "DMA35"}")
                try {
                    socket.use { s ->
                        val input = s.inputStream
                        val bytes = ByteArray(512)
                        val record = ByteArrayOutputStream()
                        val fullRecord = StringBuilder()
                        while (running) {
                            val n = input.read(bytes)
                            if (n < 0) break
                            for (i in 0 until n) {
                                val b = bytes[i].toInt() and 255
                                if (b == 10 || b == 13) {
                                    if (record.size() > 0) {
                                        val line = record.toString("ISO-8859-1")
                                        record.reset()
                                        fullRecord.append(line).append("\n")
                                        if (line.contains("Density@25", ignoreCase = true)) {
                                            MeasurementStore.ingest(this@ReceiverService, fullRecord.toString(), "DMA35")
                                            fullRecord.setLength(0)
                                        }
                                    }
                                } else if (record.size() < 8192) record.write(b)
                                else record.reset()
                            }
                        }
                    }
                } catch (_: Exception) { }
                state("Disconnected; listening again")
            }
        } catch (e: SecurityException) { state("Bluetooth permission missing: ${e.message}") }
          catch (e: Exception) { if (running) state("Receiver error: ${e.message}") }
    }
    override fun onDestroy() { running = false; try { server?.close() } catch (_: Exception) {}; super.onDestroy() }
}
