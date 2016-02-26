package cz.davidkuna.remotecontrolserver.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by David Kuna on 4.2.16.
 */
public class Accelerometer extends AbstractSensor {

    public static int VERTICAL_MODE = 1;
    public static int HORIZONTAL_MODE = 2;

    private long lastUpdate = 0;
    private float[] values = {0,0,0};
    private int calibration = VERTICAL_MODE;

    public Accelerometer(SensorManager sensorManager) {
        super(sensorManager);
    }

    @Override
    protected void processEvent(SensorEvent event) {
        this.values = calibrate(event.values);

        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

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

    public void setCalibration(int mode) {
        this.calibration = mode;
    }

    private float[] calibrate(float[] val) {
        if (calibration == VERTICAL_MODE) {
            float [] tmp = Arrays.copyOf(val, val.length);
            val = new float[]{-tmp[2], -tmp[1], tmp[0]};
        }

        return val;
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
