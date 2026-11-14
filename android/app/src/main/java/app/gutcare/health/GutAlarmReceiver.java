package app.gutcare.health;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class GutAlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_ALARM = "app.gutcare.health.ALARM";
    public static final String ACTION_STOP = "app.gutcare.health.STOP";
    private static final String CH_LOUD = "gutcare-alarm-v3";
    private static final String CH_QUIET = "gutcare-quiet-v3";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            rescheduleAll(ctx);
            return;
        }

        int id = intent.getIntExtra("id", 1);

        if (ACTION_STOP.equals(action)) {
            cancel(ctx, id);
            return;
        }

        showNow(ctx, id,
                intent.getStringExtra("title"),
                intent.getStringExtra("body"),
                intent.getStringExtra("mode"));

        rearmTomorrow(ctx, intent, id);
    }

    public static void rescheduleAll(Context ctx) {
        try {
            String raw = ctx.getSharedPreferences("gutalarm", Context.MODE_PRIVATE).getString("items", null);
            if (raw == null) return;
            org.json.JSONArray items = new org.json.JSONArray(raw);
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            for (int i = 0; i < items.length(); i++) {
                org.json.JSONObject o = items.getJSONObject(i);
                Intent intent = new Intent(ctx, GutAlarmReceiver.class);
                intent.setAction(ACTION_ALARM);
                intent.putExtra("id", o.optInt("id", i + 1));
                intent.putExtra("title", o.optString("title", "GutCare"));
                intent.putExtra("body", o.optString("body", ""));
                intent.putExtra("mode", o.optString("mode", "ten"));
                PendingIntent pi = PendingIntent.getBroadcast(ctx, o.optInt("id", i + 1), intent, pflags());

                java.util.Calendar c = java.util.Calendar.getInstance();
                c.set(java.util.Calendar.HOUR_OF_DAY, o.optInt("hour", 8));
                c.set(java.util.Calendar.MINUTE, o.optInt("minute", 0));
                c.set(java.util.Calendar.SECOND, o.optInt("offsetSeconds", 0));
                c.set(java.util.Calendar.MILLISECOND, 0);
                if (c.getTimeInMillis() <= System.currentTimeMillis()) c.add(java.util.Calendar.DAY_OF_YEAR, 1);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
                }
            }
        } catch (Exception e) {
        }
    }

    private void rearmTomorrow(Context ctx, Intent intent, int id) {
        try {
            Intent again = new Intent(ctx, GutAlarmReceiver.class);
            again.setAction(ACTION_ALARM);
            again.putExtra("id", id);
            again.putExtra("title", intent.getStringExtra("title"));
            again.putExtra("body", intent.getStringExtra("body"));
            again.putExtra("mode", intent.getStringExtra("mode"));
            PendingIntent pi = PendingIntent.getBroadcast(ctx, id, again, pflags());
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            long next = System.currentTimeMillis() + 24L * 60L * 60L * 1000L;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, next, pi);
            }
        } catch (Exception e) {
        }
    }

    public static void showNow(Context ctx, int id, String title, String body, String mode) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        ensureChannels(ctx);
        boolean loud = !"silent".equals(mode);
        String channel = loud ? CH_LOUD : CH_QUIET;

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(ctx, id, open, pflags());

        Intent stopIntent = new Intent(ctx, GutAlarmReceiver.class);
        stopIntent.setAction(ACTION_STOP);
        stopIntent.putExtra("id", id);
        PendingIntent stopPending = PendingIntent.getBroadcast(ctx, id + 10000, stopIntent, pflags());

        String text = body == null ? "" : body;

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_stat_idwazon)
                .setContentTitle(title == null ? "GutCare" : title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(loud ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(content)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_stat_idwazon, "Stop", stopPending);

        Notification n = b.build();
        if (loud && "loop".equals(mode)) {
            n.flags |= Notification.FLAG_INSISTENT;
        }
        nm.notify(id, n);
    }

    public static void cancel(Context ctx, int id) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(id);
    }

    private static void ensureChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CH_LOUD) == null) {
            NotificationChannel c = new NotificationChannel(CH_LOUD, "GutCare alarms", NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("Reminders that ring until you stop them");
            c.enableVibration(true);
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            c.setSound(sound, attrs);
            nm.createNotificationChannel(c);
        }
        if (nm.getNotificationChannel(CH_QUIET) == null) {
            NotificationChannel q = new NotificationChannel(CH_QUIET, "GutCare quiet", NotificationManager.IMPORTANCE_LOW);
            q.setDescription("Reminders without sound");
            q.setSound(null, null);
            q.enableVibration(false);
            nm.createNotificationChannel(q);
        }
    }

    private static int pflags() {
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) f |= PendingIntent.FLAG_IMMUTABLE;
        return f;
    }
}
