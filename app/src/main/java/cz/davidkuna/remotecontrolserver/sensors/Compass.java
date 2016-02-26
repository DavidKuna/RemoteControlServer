package cz.davidkuna.remotecontrolserver.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

/**
 * Created by David Kuna on 4.2.16.
 */
public class Compass extends AbstractSensor {

    public static int VERTICAL_MODE = 1;
    public static int HORIZONTAL_MODE = 2;

    private int calibration = VERTICAL_MODE;
    private float data;

    public Compass(SensorManager sensorManager) {
        super(sensorManager);
    }

    @Override
    protected void processEvent(SensorEvent event) {
        // get the angle around the z-axis rotated
        data = calibrate(Math.round(event.values[0]));
    }

    private float calibrate(float val) {
        if (calibration == VERTICAL_MODE) {
            val = val - 90;
            if (val < 0) {
                val = 360 + val;
            }
        }

        return val;
    }

    public void setCalibration(int mode) {
        calibration = mode;
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
