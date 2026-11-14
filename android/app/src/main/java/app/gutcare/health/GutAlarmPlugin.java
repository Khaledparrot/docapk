package app.gutcare.health;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

@CapacitorPlugin(name = "GutAlarm")
public class GutAlarmPlugin extends Plugin {

    @PluginMethod
    public void schedule(PluginCall call) {
        JSONArray items = call.getArray("items");
        if (items == null) {
            call.reject("items missing");
            return;
        }
        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        int count = 0;
        try {
            for (int i = 0; i < items.length(); i++) {
                JSONObject o = items.getJSONObject(i);
                scheduleOne(
                        am,
                        o.optInt("id", i + 1),
                        o.optInt("hour", 8),
                        o.optInt("minute", 0),
                        o.optInt("offsetSeconds", 0),
                        o.optString("title", "GutCare"),
                        o.optString("body", ""),
                        o.optString("mode", "ten")
                );
                count++;
            }
            getContext().getSharedPreferences("gutalarm", Context.MODE_PRIVATE)
                    .edit().putString("items", items.toString()).apply();
            JSObject ret = new JSObject();
            ret.put("scheduled", count);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void cancelAll(PluginCall call) {
        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        getContext().getSharedPreferences("gutalarm", Context.MODE_PRIVATE).edit().remove("items").apply();
        for (int id = 1; id <= 400; id++) {
            Intent intent = new Intent(getContext(), GutAlarmReceiver.class);
            intent.setAction(GutAlarmReceiver.ACTION_ALARM);
            PendingIntent pi = PendingIntent.getBroadcast(getContext(), id, intent, flags());
            am.cancel(pi);
            pi.cancel();
        }
        call.resolve();
    }

    @PluginMethod
    public void test(PluginCall call) {
        try {
            GutAlarmReceiver.showNow(
                    getContext(),
                    9999,
                    call.getString("title", "GutCare"),
                    call.getString("body", ""),
                    call.getString("mode", "loop")
            );
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        try {
            GutAlarmReceiver.cancel(getContext(), call.getInt("id", 0));
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }


    @PluginMethod
    public void requestPermission(PluginCall call) {
        try {
            if (Build.VERSION.SDK_INT >= 33 && getActivity() != null) {
                if (getActivity().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    getActivity().requestPermissions(
                            new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 4711);
                }
            }
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void openHtml(PluginCall call) {
        try {
            String html = call.getString("html", "");
            java.io.File f = new java.io.File(getContext().getCacheDir(), "idwazon-report.html");
            java.io.FileOutputStream out = new java.io.FileOutputStream(f);
            out.write(html.getBytes("UTF-8"));
            out.close();
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    getContext(), getContext().getPackageName() + ".fileprovider", f);
            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            i.setDataAndType(uri, "text/html");
            i.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void status(PluginCall call) {
        try {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            boolean enabled = true;
            if (Build.VERSION.SDK_INT >= 24) {
                enabled = nm.areNotificationsEnabled();
            }
            if (Build.VERSION.SDK_INT >= 33) {
                enabled = enabled && getContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED;
            }
            JSObject ret = new JSObject();
            ret.put("enabled", enabled);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    private int flags() {
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) f |= PendingIntent.FLAG_IMMUTABLE;
        return f;
    }

    private void scheduleOne(AlarmManager am, int id, int hour, int minute, int offsetSeconds,
                             String title, String body, String mode) {
        Intent intent = new Intent(getContext(), GutAlarmReceiver.class);
        intent.setAction(GutAlarmReceiver.ACTION_ALARM);
        intent.putExtra("id", id);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        intent.putExtra("mode", mode);
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), id, intent, flags());

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, offsetSeconds);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        }
    }
}
