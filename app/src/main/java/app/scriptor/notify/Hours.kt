package app.scriptor.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.scriptor.MainActivity
import app.scriptor.R
import app.scriptor.container
import app.scriptor.domain.Rules
import app.scriptor.localized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The two canonical hours the scriptorium keeps: Matins in the morning, to recall what was written
 * yesterday, and Compline before sleep, to restore a few fading pages. Each rings only when there is
 * something to do, and never more than once a day.
 */
object Hours {
    private const val CHANNEL = "hours_v1"
    private const val ACTION = "app.scriptor.notify.HOUR"
    private const val EXTRA = "hour"

    /** Opens the app straight into that hour's restoration. */
    const val OPEN = "app.scriptor.HOUR"
    const val MATINS = "matins"
    const val COMPLINE = "compline"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val l = context.localized()
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, l.getString(R.string.channel_hours), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = l.getString(R.string.channel_hours_desc)
            }
        )
    }

    private fun pi(context: Context, hour: String) = PendingIntent.getBroadcast(
        context, if (hour == MATINS) 1 else 2,
        Intent(context, HourReceiver::class.java).setAction(ACTION).putExtra(EXTRA, hour),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** The next moment a daily bell at [minutes] after midnight rings, strictly after [now]. */
    fun next(now: Long, minutes: Int, zone: ZoneId): Long {
        val n = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone)
        var at = n.toLocalDate().atTime(minutes / 60, minutes % 60).atZone(zone)
        if (!at.isAfter(n)) at = at.plusDays(1)
        return at.toInstant().toEpochMilli()
    }

    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        for ((hour, on, at) in listOf(Triple(MATINS, s.matins, s.matinsAt), Triple(COMPLINE, s.compline, s.complineAt))) {
            val p = pi(context, hour)
            // A few minutes either way is fine for a bell; this avoids the exact-alarm permission.
            if (!on || !s.onboarded) am.cancel(p) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next(now, at, zone), p)
        }
    }

    suspend fun ring(context: Context, hour: String) {
        val c = context.container
        val s = c.settings.now()
        if (!canNotify(context) || !s.onboarded) return
        val now = System.currentTimeMillis()
        val pages = c.db.dao().pages()
        val l = context.localized()
        val (title, text) = when (hour) {
            MATINS -> {
                val n = Rules.yesterday(pages, LocalDate.now(), ZoneId.systemDefault()).size
                if (n == 0) return
                l.getString(R.string.matins) to l.resources.getQuantityString(R.plurals.matins_text, n, n)
            }
            else -> {
                val n = Rules.fading(pages, now, s.retention.toDouble()).size
                if (n == 0) return
                l.getString(R.string.compline) to l.resources.getQuantityString(R.plurals.compline_text, n, n, minOf(n, s.complineSize))
            }
        }
        val open = Intent(context, MainActivity::class.java)
            .setAction(OPEN).putExtra(EXTRA, hour)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(PendingIntent.getActivity(context, if (hour == MATINS) 11 else 12, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(if (hour == MATINS) 1 else 2, n)
        } catch (_: SecurityException) {
        }
    }

    fun hourOf(intent: Intent?): String? = if (intent?.action == OPEN) intent.getStringExtra(EXTRA) else null

    fun clear(context: Context) = NotificationManagerCompat.from(context).cancelAll()

    internal const val RING = ACTION
    internal const val HOUR = EXTRA
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class HourReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Hours.RING) return
        val hour = intent.getStringExtra(Hours.HOUR) ?: return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Hours.ring(context, hour)
                Hours.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Hours.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
