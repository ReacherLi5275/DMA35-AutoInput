package com.example.dma35

import android.app.*
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
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channel, "DMA35 Bluetooth receiver", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val note = Notification.Builder(this, channel).setContentTitle("DMA35 Bridge active")
            .setContentText("Listening for density measurements").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentIntent(open).build()
        startForeground(41, note)
        running = true
        thread(name = "dma35-rfcomm") { receiveLoop() }
    }
    private fun state(s: String) { getSharedPreferences("dma35", MODE_PRIVATE).edit().putString("status", s).apply() }
    @Suppress("MissingPermission")
    private fun receiveLoop() {
        while (running) {
            try {
                val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter ?: error("Bluetooth unavailable")
                server = adapter.listenUsingRfcommWithServiceRecord("Serial Port Profile (SPP)", spp)
                state("Listening for DMA35")
                while (running) {
                    val socket = server?.accept() ?: break
                    state("Connected: ${socket.remoteDevice.name ?: "DMA35"}")
                    try {
                        socket.use { s ->
                            val input = s.inputStream
                            val bytes = ByteArray(512)
                            val line = ByteArrayOutputStream()
                            val record = StringBuilder()
                            while (running) {
                                val count = input.read(bytes)
                                if (count < 0) break
                                for (i in 0 until count) {
                                    val b = bytes[i].toInt() and 255
                                    if (b == 10 || b == 13) {
                                        if (line.size() > 0) {
                                            val sLine = line.toString("ISO-8859-1")
                                            line.reset()
                                            // New measurement number signals a new record.
                                            if (sLine.startsWith("MeasNo:", true) && record.isNotEmpty()) record.setLength(0)
                                            record.append(sLine).append('\n')
                                            if (sLine.contains("Density@25", true)) {
                                                MeasurementStore.ingest(this@ReceiverService, record.toString(), "DMA35")
                                                record.setLength(0)
                                            }
                                            if (record.length > 16384) record.setLength(0)
                                        }
                                    } else if (line.size() < 8192) line.write(b) else line.reset()
                                }
                            }
                        }
                    } catch (_: Exception) { }
                    if (running) state("Listening for DMA35")
                }
            } catch (e: SecurityException) { state("Bluetooth permission required"); break }
              catch (e: Exception) { if (running) state("Receiver error: ${e.message}") }
            if (running) try { Thread.sleep(1500) } catch (_: InterruptedException) { break }
        }
    }
    override fun onDestroy() { running = false; try { server?.close() } catch (_: Exception) {}; super.onDestroy() }
}
