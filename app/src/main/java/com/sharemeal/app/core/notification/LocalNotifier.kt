package com.sharemeal.app.core.notification

import com.sharemeal.app.domain.model.AppNotification

interface LocalNotifier {
    fun show(notification: AppNotification)
}
