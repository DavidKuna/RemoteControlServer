package cz.davidkuna.remotecontrolserver.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import org.json.JSONArray;

import java.util.ArrayList;

/**
 * Created by David Kuna on 4.2.16.
 */
public class Gyroscope extends AbstractSensor {

    private float[] values = {0,0,0};

    public Gyroscope(SensorManager sensorManager) {
        super(sensorManager);
    }

    @Override
    protected void processEvent(SensorEvent event) {
        values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];
        String message = "Gyroscope data: \nx=" + x + " \ny=" + y + " \nz=" + z;
        //Log.d(MainActivity.LOGTAG, "Send message: " + message);
    }

    @Override
    protected int getSensorType() {
        return Sensor.TYPE_GYROSCOPE;
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
