package com.monitor.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemMonitorWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.monitor.widget.ACTION_REFRESH"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SystemMonitorWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in allWidgetIds) {
                updateWidget(context, appWidgetManager, id)
                appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_process_list)
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_system_monitor)

        // 1. Métricas de RAM
        val ram = SystemMetricsHelper.getRamInfo(context)
        views.setTextViewText(R.id.tv_ram_val, "${ram.percentUsed}%")
        views.setTextViewText(
            R.id.tv_ram_detail,
            String.format(Locale.US, "%.1f / %.1f GB", ram.usedGb, ram.totalGb)
        )

        // 2. Métricas de Armazenamento
        val storage = SystemMetricsHelper.getStorageInfo()
        views.setTextViewText(R.id.tv_storage_val, "${storage.percentUsed}%")
        views.setTextViewText(
            R.id.tv_storage_detail,
            String.format(Locale.US, "%.0f GB liv.", storage.freeGb)
        )

        // 3. Temperatura e Bateria
        val battery = SystemMetricsHelper.getBatteryInfo(context)
        views.setTextViewText(
            R.id.tv_temp_val,
            String.format(Locale.US, "%.1f°C", battery.temperatureCelsius)
        )
        views.setTextViewText(R.id.tv_battery_val, "${battery.levelPercent}%")
        views.setTextViewText(
            R.id.tv_battery_state,
            if (battery.isCharging) "Carregando" else "Bateria"
        )

        // 4. Horário da última atualização
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        views.setTextViewText(R.id.tv_last_update, timeFormat.format(Date()))

        // 5. Configurar Botão de Atualizar Instantâneo
        val refreshIntent = Intent(context, SystemMonitorWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

        // 6. Configurar Serviço da Lista de Processos
        val serviceIntent = Intent(context, ProcessWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_process_list, serviceIntent)
        views.setEmptyView(R.id.widget_process_list, R.id.widget_empty_view)

        // Notificar gerenciador
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
