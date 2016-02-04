package cz.davidkuna.remotecontrolserver.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

/**
 * Created by David Kuna on 4.2.16.
 */
public class Gyroscope extends AbstractSensor {

    String gyroscopeData;

    public Gyroscope(SensorManager sensorManager) {
        super(sensorManager);
    }

    @Override
    protected void processEvent(SensorEvent event) {
        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];
        String message = "Gyroscope data: \nx=" + x + " \ny=" + y + " \nz=" + z;
        //Log.d(MainActivity.LOGTAG, "Send message: " + message);
        //sendMessage(message);
        gyroscopeData = "x=" + x + " y=" + y + " z=" + z;
    }

    @Override
    protected int getSensorType() {
        return Sensor.TYPE_GYROSCOPE;
    }

    @Override
    public String getData() {
        return gyroscopeData;
    }
}
