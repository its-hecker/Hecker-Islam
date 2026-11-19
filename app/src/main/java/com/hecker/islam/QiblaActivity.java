package com.hecker.islam;

import android.app.Activity;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.hecker.azaan.AppSettings;
import java.util.Locale;

public final class QiblaActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetic;
    private final float[] gravity = new float[3], field = new float[3];
    private boolean haveGravity, haveMagnetic;
    private double bearing, declination;
    private boolean configured;
    private TextView arrow, detail, status;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        AppSettings settings = new AppSettings(this);
        configured = settings.configured();
        if (configured) {
            bearing = calculateBearing(settings.latitude(), settings.longitude());
            declination = new GeomagneticField((float) settings.latitude(),
                    (float) settings.longitude(), 0f, System.currentTimeMillis()).getDeclination();
        }
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BACKGROUND);
        Ui.rootInsets(root);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = Ui.column(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 28), Ui.dp(this, 20), Ui.dp(this, 28));
        scroll.addView(content);
        TextView back = Ui.text(this, "‹  Hecker Islam", 15, Ui.GOLD, true);
        back.setOnClickListener(v -> finish()); content.addView(back);
        Ui.space(this, content, 25);
        content.addView(Ui.text(this, "Qibla", 32, Ui.WHITE, true));
        Ui.space(this, content, 4);
        content.addView(Ui.text(this, configured ? settings.city() : "Location required", 14, Ui.MUTED, false));
        Ui.space(this, content, 25);
        LinearLayout panel = Ui.panel(this);
        arrow = Ui.text(this, "↑", 140, Ui.GOLD, true);
        arrow.setGravity(Gravity.CENTER);
        arrow.setMinimumHeight(Ui.dp(this, 245));
        panel.addView(arrow);
        detail = Ui.text(this, "", 19, Ui.WHITE, true);
        detail.setGravity(Gravity.CENTER); panel.addView(detail);
        Ui.space(this, panel, 12);
        status = Ui.text(this, "", 14, Ui.MUTED, false);
        status.setGravity(Gravity.CENTER); panel.addView(status);
        content.addView(panel);
        Ui.space(this, content, 20);
        content.addView(Ui.text(this,
                "Hold your device flat. Move it in a figure-eight to calibrate the compass. Magnetic interference can affect accuracy.",
                14, Ui.MUTED, false));
        Ui.space(this, content, 18);
        if (!configured) {
            LinearLayout action = Ui.panel(this);
            action.addView(Ui.text(this, "Set location in Azaan   ›", 17, Ui.GOLD, true));
            action.setOnClickListener(v ->
                    startActivity(new Intent(this, com.hecker.azaan.MainActivity.class)));
            content.addView(action);
        }
        setContentView(root);
        if (!configured) {
            arrow.setText("—");
            detail.setText("Location not configured");
            status.setText("Select a city or GPS location in Azaan first.");
        } else if (accelerometer == null || magnetic == null) {
            arrow.setText("—");
            detail.setText(String.format(Locale.ENGLISH, "Qibla bearing %.0f° from true north", bearing));
            status.setText("This device does not have a usable compass sensor.");
        } else {
            detail.setText(String.format(Locale.ENGLISH, "Qibla bearing %.0f° from true north", bearing));
            status.setText("Finding direction…");
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if (configured && accelerometer != null && magnetic != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_UI);
        }
    }
    @Override protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }
    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, 3); haveGravity = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, field, 0, 3); haveMagnetic = true;
        }
        if (!haveGravity || !haveMagnetic) return;
        float[] rotation = new float[9], orientation = new float[3];
        if (!SensorManager.getRotationMatrix(rotation, null, gravity, field)) return;
        SensorManager.getOrientation(rotation, orientation);
        double north = (Math.toDegrees(orientation[0]) + declination + 360) % 360;
        double delta = (bearing - north + 360) % 360;
        arrow.setRotation((float) delta);
        status.setText(String.format(Locale.ENGLISH,
                "Turn towards the arrow • %.0f° clockwise from the top of the screen", delta));
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    private static double calculateBearing(double lat, double lon) {
        double p1 = Math.toRadians(lat), p2 = Math.toRadians(21.4225);
        double dl = Math.toRadians(39.8262 - lon);
        double y = Math.sin(dl);
        double x = Math.cos(p1) * Math.tan(p2) - Math.sin(p1) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }
}
