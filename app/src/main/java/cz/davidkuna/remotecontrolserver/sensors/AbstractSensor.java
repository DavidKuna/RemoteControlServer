package cz.davidkuna.remotecontrolserver.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by David Kuna on 4.2.16.
 */
public abstract class AbstractSensor implements SensorEventListener {

    private Sensor sensor;
    private final SensorManager mSensorManager;

    public AbstractSensor(SensorManager sensorManager) {
        mSensorManager = sensorManager;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == getSensorType()) {
            processEvent(event);
        }
    }

    public void start() {
        sensor = mSensorManager.getDefaultSensor(getSensorType());
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    protected abstract void processEvent(SensorEvent event);

    /**
     * Return number of sensor in Sensor class
     * @return int
     */
    protected abstract int getSensorType();

    public abstract String getData();

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
