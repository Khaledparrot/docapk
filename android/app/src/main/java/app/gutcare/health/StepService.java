package app.gutcare.health;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepService extends Service implements SensorEventListener {

    private static final String CH = "idwazon-steps";
    private SensorManager sm;
    private Sensor sensor;
    private int lastNotified = -100;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(7001, build(prefs().getInt("total", 0)));
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sm != null) {
            sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (sensor != null) sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try { if (sm != null) sm.unregisterListener(this); } catch (Exception e) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private SharedPreferences prefs() {
        return getSharedPreferences("steps", Context.MODE_PRIVATE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            int raw = (int) event.values[0];
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            SharedPreferences p = prefs();
            String day = p.getString("day", "");
            int last = p.getInt("raw", -1);
            int total = p.getInt("total", 0);

            if (!today.equals(day) || last < 0 || raw < last) total = 0;
            else total += (raw - last);

            p.edit().putString("day", today).putInt("raw", raw).putInt("total", total).apply();

            if (Math.abs(total - lastNotified) >= 50) {
                lastNotified = total;
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.notify(7001, build(total));
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private Notification build(int steps) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel(CH) == null) {
                NotificationChannel c = new NotificationChannel(CH, "Step counter", NotificationManager.IMPORTANCE_MIN);
                c.setDescription("Keeps counting your steps");
                c.setSound(null, null);
                nm.createNotificationChannel(c);
            }
        }
        return new NotificationCompat.Builder(this, CH)
                .setSmallIcon(R.drawable.ic_stat_idwazon)
                .setContentTitle("idwaZon")
                .setContentText(steps + " steps today — counting")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }
}
