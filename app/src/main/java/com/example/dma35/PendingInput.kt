package com.example.dma35

import android.content.Context

/** One durable, explicitly acknowledged pending value; never silently overwrite an uninserted value. */
object PendingInput {
    private const val PREF = "dma35"
    @Synchronized fun offer(context: Context, value: String): Boolean {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!p.getString("pendingValue", "").isNullOrEmpty()) {
            p.edit().putString("inputStatus", "WAITING FOR FIELD: previous value pending").apply()
            return false
        }
        return p.edit().putString("pendingValue", value)
            .putString("inputStatus", "WAITING FOR FIELD").commit()
    }
    @Synchronized fun peek(context: Context): String? = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        .getString("pendingValue", null)?.takeIf { it.isNotEmpty() }
    @Synchronized fun acknowledge(context: Context, value: String) {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (p.getString("pendingValue", null) == value) {
            p.edit().remove("pendingValue").putString("lastInserted", value)
                .putString("inputStatus", "INSERTED (sent to target field)").commit()
        }
    }
    @Synchronized fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .remove("pendingValue").putString("inputStatus", "Pending input cleared").commit()
    }
}
