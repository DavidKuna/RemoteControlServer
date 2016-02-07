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

    private static Context context;
	private final SensorManager mSensorManager;
    private Accelerometer mAccelerometer;
    private Gyroscope mGyroscope;
    private Compass mCompass;
    private SendClientMessageListener sendListener;

    private GPSTracker gpsTracker = null;
	
	public SensorController(Context context) {
		SensorController.context = context;	
		mSensorManager = (SensorManager)context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mGyroscope = new Gyroscope(mSensorManager);
        mAccelerometer = new Accelerometer(mSensorManager);
        gpsTracker = new GPSTracker(context);
        mCompass = new Compass(mSensorManager);
	}
    
    public void start() {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

    	if(preferences.getBoolean("accelerometr_enabled", true)) {
            mAccelerometer.start();
		}
    	
    	if(preferences.getBoolean("gyroscope_enabled", true)) {
    		mGyroscope.start();
    	}

        if(preferences.getBoolean("compass_enabled", true)) {
            mCompass.start();
        }
    }
    
    public void closeControl() {
        gpsTracker.stopUsingGPS();
        mGyroscope.stop();
        mAccelerometer.stop();
        mCompass.stop();
    }

	
	private void sendMessage(String message) {
		if(sendListener != null){
			//sendListener.onSendClientMessage(message);
		} else {
			Log.d(MainActivity.LOGTAG, "sendListener is not set");
		}
	}
	
	public void setSendClientMessageListener(SendClientMessageListener eventListener) {
		sendListener = eventListener;
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
            gpsTracker.showSettingsAlert();
            data.add(0.0);
            data.add(0.0);
        } else {
            data.add(gpsTracker.getLatitude());
            data.add(gpsTracker.getLongitude());
        }

        return new JSONArray(data).toString();
    }
}
