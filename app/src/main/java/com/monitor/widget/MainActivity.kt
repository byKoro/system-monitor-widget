package com.monitor.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvPermissionStatus: TextView
    private lateinit var btnGrantPermission: Button
    private lateinit var btnPinWidget: Button
    private lateinit var tvMetricsSummary: TextView
    private lateinit var tvProcessesSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPermissionStatus = findViewById(R.id.tv_permission_status)
        btnGrantPermission = findViewById(R.id.btn_grant_permission)
        btnPinWidget = findViewById(R.id.btn_pin_widget)
        tvMetricsSummary = findViewById(R.id.tv_app_metrics_summary)
        tvProcessesSummary = findViewById(R.id.tv_app_processes_summary)

        btnGrantPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        btnPinWidget.setOnClickListener {
            pinWidgetToHomeScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val hasPermission = SystemMetricsHelper.hasUsageStatsPermission(this)

        if (hasPermission) {
            tvPermissionStatus.text = getString(R.string.permission_status_granted)
            tvPermissionStatus.setTextColor(getColor(R.color.status_granted))
            btnGrantPermission.text = "Permissão Ativa"
            btnGrantPermission.isEnabled = false
        } else {
            tvPermissionStatus.text = getString(R.string.permission_status_denied)
            tvPermissionStatus.setTextColor(getColor(R.color.status_denied))
            btnGrantPermission.text = getString(R.string.btn_grant_permission)
            btnGrantPermission.isEnabled = true
        }

        // Carregar Métricas
        val ram = SystemMetricsHelper.getRamInfo(this)
        val storage = SystemMetricsHelper.getStorageInfo()
        val battery = SystemMetricsHelper.getBatteryInfo(this)

        tvMetricsSummary.text = buildString {
            append("• Memória RAM: ${ram.percentUsed}% em uso\n")
            append("  (${String.format(Locale.US, "%.2f GB de %.2f GB", ram.usedGb, ram.totalGb)})\n\n")
            append("• Armazenamento: ${storage.percentUsed}% ocupado\n")
            append("  (${String.format(Locale.US, "%.1f GB livres de %.1f GB", storage.freeGb, storage.totalGb)})\n\n")
            append("• Bateria: ${battery.levelPercent}%\n")
            append("• Temperatura: ${String.format(Locale.US, "%.1f°C", battery.temperatureCelsius)}\n")
            append("• Estado: ${if (battery.isCharging) "Conectado ao carregador" else "Descarregando"}")
        }

        // Carregar Processos
        if (hasPermission) {
            val processes = SystemMetricsHelper.getActiveProcesses(this)
            if (processes.isEmpty()) {
                tvProcessesSummary.text = "Nenhum aplicativo recente registrado."
            } else {
                tvProcessesSummary.text = buildString {
                    processes.take(10).forEachIndexed { index, proc ->
                        append("${index + 1}. ${proc.appName} (${proc.packageName})\n")
                        append("   Status: ${proc.metricText}\n")
                    }
                }
            }
        } else {
            tvProcessesSummary.text = "⚠️ Permissão necessária para listar os processos ativos."
        }
    }

    private fun pinWidgetToHomeScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = getSystemService(AppWidgetManager::class.java)
            val myProvider = ComponentName(this, SystemMonitorWidgetProvider::class.java)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null)
            }
        }
    }
}
