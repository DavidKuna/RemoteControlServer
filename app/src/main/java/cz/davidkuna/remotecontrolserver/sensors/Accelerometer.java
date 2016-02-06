package cz.davidkuna.remotecontrolserver.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import org.json.JSONArray;

import java.util.ArrayList;

/**
 * Created by David Kuna on 4.2.16.
 */
public class Accelerometer extends AbstractSensor {

    private long lastUpdate = 0;
    private float[] values = {0,0,0};

    public Accelerometer(SensorManager sensorManager) {
        super(sensorManager);
    }

    @Override
    protected void processEvent(SensorEvent event) {
        values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];
        //Log.d(MainActivity.LOGTAG, "Sensor data: \nx=" + x + " \ny=" + y + " \nz=" + z);
        String message = "Accelerometer data: \nx=" + x + " \ny=" + y + " \nz=" + z;
        //Log.d(MainActivity.LOGTAG, "Send message: " + message);
        //sendMessage(message);

        float accelationSquareRoot = (x * x + y * y + z * z)
                / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        long actualTime = event.timestamp;
        if (accelationSquareRoot >= 2) //
        {
            if (actualTime - lastUpdate < 200) {
                return;
            }
            lastUpdate = actualTime;
        }
    }

    @Override
    protected int getSensorType() {
        return Sensor.TYPE_ACCELEROMETER;
    }

    @Override
    public String getData() {
        ArrayList<String> strings = new ArrayList<String>();
        strings.add(String.valueOf(values[0]));
        strings.add(String.valueOf(values[1]));
        strings.add(String.valueOf(values[2]));

        return new JSONArray(strings).toString();
    }
}
