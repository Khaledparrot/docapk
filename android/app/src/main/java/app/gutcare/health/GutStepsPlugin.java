package app.gutcare.health;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@CapacitorPlugin(name = "GutSteps")
public class GutStepsPlugin extends Plugin implements SensorEventListener {

    private SensorManager sm;
    private Sensor sensor;

    @Override
    public void load() {
        sm = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        if (sm != null) sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    }

    @PluginMethod
    public void start(PluginCall call) {
        if (sensor == null) {
            call.reject("no step sensor on this phone");
            return;
        }
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        if (sm != null) sm.unregisterListener(this);
        call.resolve();
    }

    @PluginMethod
    public void enable(PluginCall call) {
        try {
            android.content.Intent i = new android.content.Intent(getContext(), StepService.class);
            if (android.os.Build.VERSION.SDK_INT >= 26) getContext().startForegroundService(i);
            else getContext().startService(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void disable(PluginCall call) {
        try {
            getContext().stopService(new android.content.Intent(getContext(), StepService.class));
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void today(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("steps", prefs().getInt("total", 0));
        ret.put("hasSensor", sensor != null);
        call.resolve(ret);
    }

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences("steps", Context.MODE_PRIVATE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            float rawF = event.values[0];
            int raw = (int) rawF;
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            SharedPreferences p = prefs();
            String day = p.getString("day", "");
            int last = p.getInt("raw", -1);
            int total = p.getInt("total", 0);

            if (!today.equals(day) || last < 0 || raw < last) {
                total = 0;
            } else {
                total += (raw - last);
            }
            p.edit().putString("day", today).putInt("raw", raw).putInt("total", total).apply();
        } catch (Exception e) {
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
