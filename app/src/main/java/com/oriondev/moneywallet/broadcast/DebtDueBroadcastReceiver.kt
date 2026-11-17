/*
 * Copyright (c) 2026.
 *
 * This file is part of Tallybook.
 *
 * Tallybook is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.oriondev.moneywallet.broadcast

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.oriondev.moneywallet.R
import com.oriondev.moneywallet.storage.database.Contract
import com.oriondev.moneywallet.storage.database.DataContentProvider
import com.oriondev.moneywallet.ui.activity.MainActivity
import com.oriondev.moneywallet.ui.notification.NotificationContract
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Posts local reminders for active debts/credits due today or tomorrow.
 *
 * The reminder uses the existing reminder notification channel and database fields;
 * it does not persist notification state or change the database schema.
 */
class DebtDueBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val REQUEST_CODE = 23462
        private const val REMINDER_HOUR = 9

        @JvmStatic
        fun scheduleDailyReminder(context: Context) {
            val intent = Intent(context, DebtDueBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Service.ALARM_SERVICE) as AlarmManager?
                ?: return
            val firstRun = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, firstRun, AlarmManager.INTERVAL_DAY, pendingIntent
            )
        }

        private fun dateString(calendar: Calendar): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val today = Calendar.getInstance()
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val todayString = dateString(today)
        val tomorrowString = dateString(tomorrow)
        val projection = arrayOf(
            Contract.Debt.ID,
            Contract.Debt.TYPE,
            Contract.Debt.DESCRIPTION,
            Contract.Debt.EXPIRATION_DATE
        )
        val selection = Contract.Debt.ARCHIVED + " = 0 AND " +
            Contract.Debt.EXPIRATION_DATE + " BETWEEN ? AND ?"
        val cursor: Cursor? = context.contentResolver.query(
            DataContentProvider.CONTENT_DEBTS, projection, selection,
            arrayOf(todayString, tomorrowString), Contract.Debt.EXPIRATION_DATE + " ASC"
        )
        if (cursor != null) {
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(Contract.Debt.ID)
                val typeIndex = it.getColumnIndexOrThrow(Contract.Debt.TYPE)
                val descriptionIndex = it.getColumnIndexOrThrow(Contract.Debt.DESCRIPTION)
                val expirationIndex = it.getColumnIndexOrThrow(Contract.Debt.EXPIRATION_DATE)
                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val type = it.getInt(typeIndex)
                    val description = it.getString(descriptionIndex).orEmpty()
                    val dueDate = it.getString(expirationIndex)
                    val dueToday = dueDate == todayString
                    val typeLabel = if (type == Contract.DebtType.CREDIT.value) {
                        context.getString(R.string.notification_debt_due_credit_type)
                    } else {
                        context.getString(R.string.notification_debt_due_debt_type)
                    }
                    val message = context.getString(
                        if (dueToday) R.string.notification_debt_due_today
                        else R.string.notification_debt_due_tomorrow,
                        description
                    )
                    val openApp = PendingIntent.getActivity(
                        context, id.toInt(),
                        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val notification = NotificationCompat.Builder(
                        context, NotificationContract.NOTIFICATION_CHANNEL_REMINDER
                    )
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(context.getString(R.string.notification_debt_due_title))
                        .setContentText("$typeLabel · $message")
                        .setStyle(NotificationCompat.BigTextStyle().bigText("$typeLabel · $message"))
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setAutoCancel(true)
                        .setContentIntent(openApp)
                        .build()
                    try {
                        NotificationManagerCompat.from(context).notify(id.toInt(), notification)
                    } catch (_: SecurityException) {
                        // Notifications may be disabled by the user or denied on Android 13+.
                    }
                }
            }
        }
        scheduleDailyReminder(context)
    }
}
