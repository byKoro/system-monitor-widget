package com.monitor.widget

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import java.util.Locale

data class RamData(
    val totalGb: Double,
    val usedGb: Double,
    val freeGb: Double,
    val percentUsed: Int
)

data class StorageData(
    val totalGb: Double,
    val usedGb: Double,
    val freeGb: Double,
    val percentUsed: Int
)

data class BatteryData(
    val levelPercent: Int,
    val temperatureCelsius: Double,
    val isCharging: Boolean
)

object SystemMetricsHelper {

    /**
     * Obtém informações de memória RAM do dispositivo.
     */
    fun getRamInfo(context: Context): RamData {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val totalGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val freeGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        val usedGb = totalGb - freeGb
        val percent = if (totalGb > 0) ((usedGb / totalGb) * 100).toInt() else 0

        return RamData(
            totalGb = totalGb,
            usedGb = usedGb,
            freeGb = freeGb,
            percentUsed = percent
        )
    }

    /**
     * Obtém informações de armazenamento interno.
     */
    fun getStorageInfo(): StorageData {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)

        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - freeBytes

        val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
        val usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
        val freeGb = freeBytes / (1024.0 * 1024.0 * 1024.0)
        val percent = if (totalGb > 0) ((usedGb / totalGb) * 100).toInt() else 0

        return StorageData(
            totalGb = totalGb,
            usedGb = usedGb,
            freeGb = freeGb,
            percentUsed = percent
        )
    }

    /**
     * Obtém temperatura e nível da bateria através de sticky intent nativo do Android.
     */
    fun getBatteryInfo(context: Context): BatteryData {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, filter)

        var level = 0
        var tempC = 0.0
        var isCharging = false

        if (batteryIntent != null) {
            val rawLevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (rawLevel >= 0 && scale > 0) {
                level = (rawLevel * 100) / scale
            }

            val rawTemp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            tempC = rawTemp / 10.0

            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }

        return BatteryData(
            levelPercent = level,
            temperatureCelsius = tempC,
            isCharging = isCharging
        )
    }

    /**
     * Verifica se o usuário concedeu a permissão PACKAGE_USAGE_STATS.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Lê a lista de processos e aplicativos ativos / recentes usando o UsageStatsManager.
     */
    fun getActiveProcesses(context: Context): List<ProcessModel> {
        if (!hasUsageStatsPermission(context)) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        val now = System.currentTimeMillis()
        val startTime = now - (1000 * 60 * 60 * 4) // Últimas 4 horas

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            now
        ) ?: return emptyList()

        // Agrupar por pacote e ordenar por último uso
        val activeApps = stats
            .filter { it.lastTimeUsed > 0 && it.totalTimeInForeground > 0 }
            .sortedByDescending { it.lastTimeUsed }
            .distinctBy { it.packageName }
            .take(25)

        val resultList = mutableListOf<ProcessModel>()

        for (item in activeApps) {
            try {
                val appInfo = pm.getApplicationInfo(item.packageName, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                val iconDrawable = pm.getApplicationIcon(appInfo)
                val iconBitmap = drawableToBitmap(iconDrawable)

                val diffMinutes = (now - item.lastTimeUsed) / (1000 * 60)
                val metricText = when {
                    diffMinutes <= 1 -> "Ativo agora"
                    diffMinutes < 60 -> "Há ${diffMinutes}m"
                    else -> "Há ${diffMinutes / 60}h"
                }

                resultList.add(
                    ProcessModel(
                        packageName = item.packageName,
                        appName = label,
                        icon = iconBitmap,
                        metricText = metricText,
                        lastTimeUsed = item.lastTimeUsed
                    )
                )
            } catch (ignored: PackageManager.NameNotFoundException) {
                // Pacote removido ou do sistema não listável
            }
        }

        return resultList
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 64
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 64
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
