package cz.davidkuna.remotecontrolserver.sensors;

import cz.davidkuna.remotecontrolserver.activity.MainActivity;
import cz.davidkuna.remotecontrolserver.location.GPSTracker;
import cz.davidkuna.remotecontrolserver.socket.SendClientMessageListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class SensorController {

	private final SensorManager mSensorManager;
    private Accelerometer mAccelerometer;
    private Gyroscope mGyroscope;
    private Compass mCompass;

    private GPSTracker gpsTracker = null;
	
	public SensorController(Context context) {
		mSensorManager = (SensorManager)context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mGyroscope = new Gyroscope(mSensorManager);
        mAccelerometer = new Accelerometer(mSensorManager);
        gpsTracker = new GPSTracker(context);
        mCompass = new Compass(mSensorManager);
	}
    
    public void start() {
        mAccelerometer.start();
        mGyroscope.start();
        mCompass.start();
    }
    
    public void closeControl() {
        gpsTracker.stopUsingGPS();
        mGyroscope.stop();
        mAccelerometer.stop();
        mCompass.stop();
    }

	public DataMessage getData() {
		return new DataMessage()
                .addData(DataMessage.TYPE_ACCELEROMETER + "", mAccelerometer.getData())
                .addData(DataMessage.TYPE_GYROSCOPE + "", mGyroscope.getData())
                .addData(DataMessage.TYPE_GPS, getLocation())
                .addData(DataMessage.TYPE_COMPASS + "", mCompass.getData());
	}

    private String getLocation() {
        ArrayList<Double> data = new ArrayList<Double>();
        if(!gpsTracker.canGetLocation()){
            //gpsTracker.showSettingsAlert();
            data.add(0.0);
            data.add(0.0);
        } else {
            data.add(gpsTracker.getLatitude());
            data.add(gpsTracker.getLongitude());
        }

        return new JSONArray(data).toString();
    }
}
