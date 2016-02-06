package cz.davidkuna.remotecontrolserver.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

/**
 * Created by David Kuna on 4.2.16.
 */
public class Compass extends AbstractSensor {

    private float data;

    public Compass(SensorManager sensorManager) {
        super(sensorManager);
    }

    @Override
    protected void processEvent(SensorEvent event) {
        // get the angle around the z-axis rotated
        data = Math.round(event.values[0]);
    }

    @Override
    protected int getSensorType() {
        return Sensor.TYPE_ORIENTATION;
    }

    @Override
    public String getData() {
        return Float.toString(data);
    }
}
