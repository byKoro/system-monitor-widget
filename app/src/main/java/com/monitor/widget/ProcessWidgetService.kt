package com.monitor.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class ProcessWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ProcessRemoteViewsFactory(applicationContext)
    }
}

class ProcessRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val processList = mutableListOf<ProcessModel>()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        processList.clear()
        processList.addAll(SystemMetricsHelper.getActiveProcesses(context))
    }

    override fun onDestroy() {
        processList.clear()
    }

    override fun getCount(): Int = processList.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= processList.size) {
            return null
        }

        val item = processList[position]
        val views = RemoteViews(context.packageName, R.layout.widget_process_item)

        views.setTextViewText(R.id.tv_process_name, item.appName)
        views.setTextViewText(R.id.tv_process_package, item.packageName)
        views.setTextViewText(R.id.tv_process_metric, item.metricText)

        if (item.icon != null) {
            views.setImageViewBitmap(R.id.iv_process_icon, item.icon)
        } else {
            views.setImageViewResource(R.id.iv_process_icon, android.R.drawable.sym_def_app_icon)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
