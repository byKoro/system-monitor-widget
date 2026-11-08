package com.monitor.widget

import android.graphics.Bitmap

/**
 * Representa um processo ou aplicativo ativo no sistema.
 */
data class ProcessModel(
    val packageName: String,
    val appName: String,
    val icon: Bitmap?,
    val metricText: String,
    val lastTimeUsed: Long
)
