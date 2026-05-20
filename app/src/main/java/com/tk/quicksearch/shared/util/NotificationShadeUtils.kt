package com.tk.quicksearch.shared.util

import android.app.StatusBarManager
import android.content.Context

fun Context.openNotificationShade(): Boolean =
    runCatching {
        val statusBarManager = getSystemService(StatusBarManager::class.java) ?: return false
        StatusBarManager::class.java
            .getMethod("expandNotificationsPanel")
            .invoke(statusBarManager)
        true
    }.getOrDefault(false)
