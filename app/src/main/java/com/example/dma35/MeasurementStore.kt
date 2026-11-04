package com.example.dma35

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MeasurementStore {
    private val densityPattern = Regex("Density\\s*@\\s*25[^:]*:\\s*([0-9]+(?:[.,][0-9]+)?)", RegexOption.IGNORE_CASE)
    private val numberPattern = Regex("MeasNo\\s*:\\s*([A-Za-z0-9_-]+)", RegexOption.IGNORE_CASE)
    private val samplePattern = Regex("Sample\\s*ID\\s*:\\s*([A-Za-z0-9_-]+)", RegexOption.IGNORE_CASE)
    private const val FILE = "dma35_measurements.csv"

    @Synchronized fun ingest(context: Context, raw: String, source: String): String? {
        val match = densityPattern.find(raw) ?: return null
        val value = match.groupValues[1].replace(',', '.')
        val number = value.toDoubleOrNull() ?: return null
        if (number !in 0.5..2.0) return null // Broad test sanity limit; not product acceptance criteria.
        val prefs = context.getSharedPreferences("dma35", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val measNo = numberPattern.find(raw)?.groupValues?.get(1) ?: ""
        val previousNo = prefs.getString("lastMeasNo", "") ?: ""
        if (source == "DMA35" && measNo.isNotEmpty() && previousNo == measNo) return null
        if (prefs.getString("latest", "") == value && now - prefs.getLong("receivedAt", 0L) < 2000L) return null
        val sample = samplePattern.find(raw)?.groupValues?.get(1) ?: ""
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(now))
        val file = context.getFileStreamPath(FILE)
        if (!file.exists() || file.length() == 0L) context.openFileOutput(FILE, Context.MODE_APPEND).bufferedWriter().use {
            it.appendLine("received_at,source,measurement_no,sample_id,density_at_25c_g_cm3")
        }
        context.openFileOutput(FILE, Context.MODE_APPEND).bufferedWriter().use {
            it.appendLine(listOf(stamp, source, measNo, sample, value).joinToString(",") { field -> "\"${field.replace("\"", "\"\"")}\"" })
        }
        prefs.edit().putString("latest", value).putLong("receivedAt", now).putString("status", "Received ($source)").putString("lastMeasNo", if (source == "DMA35") measNo else previousNo).apply()
        DensityBus.publish(value)
        return value
    }
    fun csv(context: Context): String = context.getFileStreamPath(FILE).takeIf { it.exists() }?.readText() ?: "received_at,source,measurement_no,sample_id,density_at_25c_g_cm3\n"
}
