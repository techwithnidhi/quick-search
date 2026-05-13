package com.tk.quicksearch.widgetsPanel

import android.content.ComponentName
import android.content.Context
import com.tk.quicksearch.search.data.preferences.BasePreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class PanelWidgetInfo(
    val appWidgetId: Int,
    val providerPackage: String,
    val providerClassName: String,
    val heightDp: Float? = null,
)

class WidgetsPanelPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getWidgets(): List<PanelWidgetInfo> {
        val stored = prefs.getString(KEY_WIDGETS_PANEL_ITEMS, null).orEmpty()
        if (stored.isBlank()) return emptyList()
        return try {
            val array = JSONArray(stored)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val appWidgetId = item.optInt(FIELD_APP_WIDGET_ID, -1)
                    val providerPackage = item.optString(FIELD_PROVIDER_PACKAGE)
                    val providerClassName = item.optString(FIELD_PROVIDER_CLASS)
                    if (
                        appWidgetId != -1 &&
                        providerPackage.isNotBlank() &&
                        providerClassName.isNotBlank()
                    ) {
                        add(
                            PanelWidgetInfo(
                                appWidgetId = appWidgetId,
                                providerPackage = providerPackage,
                                providerClassName = providerClassName,
                                heightDp =
                                    item
                                        .optDouble(FIELD_HEIGHT_DP, Double.NaN)
                                        .takeIf { !it.isNaN() }
                                        ?.toFloat(),
                            ),
                        )
                    }
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun setWidgets(widgets: List<PanelWidgetInfo>) {
        val array = JSONArray()
        widgets.forEach { widget ->
            array.put(
                JSONObject()
                    .put(FIELD_APP_WIDGET_ID, widget.appWidgetId)
                    .put(FIELD_PROVIDER_PACKAGE, widget.providerPackage)
                    .put(FIELD_PROVIDER_CLASS, widget.providerClassName)
                    .apply {
                        widget.heightDp?.let { put(FIELD_HEIGHT_DP, it) }
                    },
            )
        }
        prefs.edit().putString(KEY_WIDGETS_PANEL_ITEMS, array.toString()).apply()
    }

    fun addWidget(
        appWidgetId: Int,
        provider: ComponentName,
    ): List<PanelWidgetInfo> {
        val next =
            getWidgets() +
                PanelWidgetInfo(
                    appWidgetId = appWidgetId,
                    providerPackage = provider.packageName,
                    providerClassName = provider.className,
                )
        setWidgets(next)
        return next
    }

    fun removeWidget(appWidgetId: Int): List<PanelWidgetInfo> {
        val next = getWidgets().filterNot { it.appWidgetId == appWidgetId }
        setWidgets(next)
        return next
    }

    fun updateWidgetHeight(
        appWidgetId: Int,
        heightDp: Float,
    ): List<PanelWidgetInfo> {
        val next =
            getWidgets().map { widget ->
                if (widget.appWidgetId == appWidgetId) {
                    widget.copy(heightDp = heightDp)
                } else {
                    widget
                }
            }
        setWidgets(next)
        return next
    }

    private companion object {
        const val KEY_WIDGETS_PANEL_ITEMS = "widgets_panel_items"
        const val FIELD_APP_WIDGET_ID = "appWidgetId"
        const val FIELD_PROVIDER_PACKAGE = "providerPackage"
        const val FIELD_PROVIDER_CLASS = "providerClassName"
        const val FIELD_HEIGHT_DP = "heightDp"
    }
}
