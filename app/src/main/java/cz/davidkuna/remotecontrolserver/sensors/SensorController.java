package cz.davidkuna.remotecontrolserver.sensors;

import cz.davidkuna.remotecontrolserver.activity.MainActivity;
import cz.davidkuna.remotecontrolserver.socket.SendClientMessageListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class SensorController implements SensorEventListener {

	private final SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private static Context context;
    private long lastUpdate;
    private SendClientMessageListener sendListener;
	
	public SensorController(Context context) {
		SensorController.context = context;	
		mSensorManager = (SensorManager)context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);		            
	}
    
    public void start() {
    	Log.d(MainActivity.LOGTAG, "Sensor controller started");
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    	if(preferences.getBoolean("accelerometr_enabled", true)) {
			mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		}

    	if(preferences.getBoolean("gyroscope_enabled", true)) {
    		Log.d(MainActivity.LOGTAG, "gyroscope_enabled");
    	} else {
    		Log.d(MainActivity.LOGTAG, "gyroscope_disabled");
    	}
    	
    	if(preferences.getBoolean("gyroscope_enabled", true)) {
    		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);  
    		mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    	}
    	    	
    	lastUpdate = System.currentTimeMillis();
    }
    
    public void closeControl() {
    	mSensorManager.unregisterListener(this);
    }
	
	@Override
	public void onAccuracyChanged(Sensor event, int accuracy) {		
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
	      getAccelerometer(event);
	    } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
	    	getGryroscope(event);
	    }
	}
	
	private void getAccelerometer(SensorEvent event) {
	    float[] values = event.values;
	    // Movement
	    float x = values[0];
	    float y = values[1];
	    float z = values[2];
	    //Log.d(MainActivity.LOGTAG, "Sensor data: \nx=" + x + " \ny=" + y + " \nz=" + z);
	    String message = "Accelerometer data: \nx=" + x + " \ny=" + y + " \nz=" + z;
	    //Log.d(MainActivity.LOGTAG, "Send message: " + message);
	    sendMessage(message);
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
	
	private void getGryroscope(SensorEvent event) {
		float[] values = event.values;
	    // Movement
	    float x = values[0];
	    float y = values[1];
	    float z = values[2];
	    String message = "Gyroscope data: \nx=" + x + " \ny=" + y + " \nz=" + z;
	    //Log.d(MainActivity.LOGTAG, "Send message: " + message);
		sendMessage(message);
	}
	
	private void sendMessage(String message) {
		if(sendListener != null){
			sendListener.onSendClientMessage(message);
		} else {
			Log.d(MainActivity.LOGTAG, "sendListener is not set");
		}
	}
	
	public void setSendClientMessageListener(SendClientMessageListener eventListener) {
		sendListener = eventListener;
	}
}
