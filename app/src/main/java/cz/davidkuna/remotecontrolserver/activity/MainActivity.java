package cz.davidkuna.remotecontrolserver.activity;

import cz.davidkuna.remotecontrolserver.R;
import cz.davidkuna.remotecontrolserver.helpers.Network;
import cz.davidkuna.remotecontrolserver.helpers.Settings;
import cz.davidkuna.remotecontrolserver.sensors.SensorController;
import cz.davidkuna.remotecontrolserver.sensors.SensorDataStream;
import cz.davidkuna.remotecontrolserver.socket.SendClientMessageListener;
import cz.davidkuna.remotecontrolserver.socket.SocketServer;
import cz.davidkuna.remotecontrolserver.socket.SocketServerEventListener;
import cz.davidkuna.remotecontrolserver.socket.UDPServer;
import cz.davidkuna.remotecontrolserver.video.CameraStream;

import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SendClientMessageListener, SocketServerEventListener, OnClickListener {
	
	public static final String LOGTAG = "RC_SERVER";

    public final int DEFAULT_COMMAND_LISTENER_PORT = 8000;
	public final int DEFAULT_SENSOR_STREAM_PORT = 8001;
    public final int DEFAULT_CAMERA_STREAM_PORT = 8080;

	private UDPServer udpServer = null;
	private SocketServer socketServer;
	private SensorController sensorController;
	private SensorDataStream sensorDataStream;

    private CameraStream cameraStream = null;
	private ToggleButton toggleSocketServer;
    private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
        prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

		TextView tvLocalIP = (TextView) findViewById(R.id.tvLocalIP);
		tvLocalIP.setText(Network.getLocalIpAddress());

        initCameraSizes(0);

		toggleSocketServer = (ToggleButton)findViewById(R.id.toggleSocketServer);
		toggleSocketServer.setOnClickListener(this);

        SurfaceHolder mPreviewDisplay = ((SurfaceView) findViewById(R.id.camera)).getHolder();
        cameraStream = new CameraStream(prefs, mPreviewDisplay);

        Settings settings = new Settings();
        settings.setServerAddress(Network.getLocalIpAddress())
                .setCameraUDPPort(DEFAULT_CAMERA_STREAM_PORT)
				.setSensorUDPPort(DEFAULT_SENSOR_STREAM_PORT)
				.setControlUDPPort(DEFAULT_COMMAND_LISTENER_PORT);
        Gson gson = new Gson();

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int qrDimensionSize = (int)(metrics.heightPixels * 0.01 * 40);

		qrCodeInit(gson.toJson(settings).toString(), qrDimensionSize, qrDimensionSize);
	}

	private void qrCodeInit(String content, int width, int height) {
		try {
            ImageView imageView = (ImageView) findViewById(R.id.qrCode);
			BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height);
			BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

			imageView.setImageBitmap(barcodeEncoder.createBitmap(bitMatrix));
		} catch (WriterException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(socketServer != null) {
			socketServer.close();
		}		
	}
    
    public void enableSensorController() {
		sensorController = new SensorController(getApplicationContext());
		sensorController.setSendClientMessageListener(this);
		sensorController.start();
    }
    
    public void disableSensorController() {
    	sensorController.closeControl();
    }
    
    public void startSocketServer() {
    	if (socketServer == null) {
    		socketServer = new SocketServer();
    		onSocketServerEvent(SocketServer.EVENT_START);
    		socketServer.setSocketServerEventListener(this);
		} else if (socketServer.isClosed()) {
			socketServer.start();
		}
    }

	public void startUDPServer() {
		udpServer = new UDPServer();
		udpServer.runUdpServer(DEFAULT_COMMAND_LISTENER_PORT, sensorController);

		try {
			sensorDataStream = new SensorDataStream(DEFAULT_SENSOR_STREAM_PORT, sensorController);
			sensorDataStream.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    public boolean isSocketServerRunning() {
    	if (socketServer == null || socketServer.isClosed()) {
    		return false;
		} else {
			return true;
		}
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent();
	        intent.setClassName(MainActivity.this, "cz.davidkuna.remotecontrolserver.activity.Preferences");
	        startActivity(intent);	       
	        return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.toggleSocketServer) {
			//toggleSocketServer();
			toggleUDPServer();
		}
	}

	private void toggleSocketServer() {
		if (!isSocketServerRunning()) {
			startSocketServer();
		} else {
			socketServer.close();
		}
	}

	private void toggleUDPServer() {
		if (!isSocketServerRunning()) {
			enableSensorController();
			startUDPServer();
		} else {
			udpServer.stopUDPServer();
			sensorDataStream.stop();
			disableSensorController();
		}
	}

	private void toggleSensors() {
		if (sensorController == null) {
			enableSensorController();
		} else {
			disableSensorController();
		}
	}

	@Override
	public void onSendClientMessage(String message) {
		if(socketServer != null) {
			socketServer.sendMessage(message);
		}
	}

	@Override
	public void onSocketServerEvent(final int event) {
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (event == SocketServer.EVENT_START) {
                    Log.d(LOGTAG, getString(R.string.start_server));
                } else if (event == SocketServer.EVENT_STOP) {
                    Log.d(LOGTAG, getString(R.string.stoped));
                } else if (event == SocketServer.EVENT_CLIENT_CONNECT) {
                    Log.d(LOGTAG, getString(R.string.connection_status));
                } else if (event == SocketServer.EVENT_CLIENT_DISCONNECT) {
                    Log.d(LOGTAG, getString(R.string.disconnected));
                }
            }
        });
	}

	public void cameraStreamStart(View v) {
		Log.d("Camera Stream", "Click");
		cameraStream.start();
	}

	private void initCameraSizes(int mCameraIndex) {

        Spinner spinner = (Spinner) findViewById(R.id.cameraResolution);
		final Camera camera = Camera.open(mCameraIndex);
		final Camera.Parameters params = camera.getParameters();
		// Check what resolutions are supported by your camera
		List<Camera.Size> sizes = params.getSupportedPictureSizes();

        ArrayList<String> options = new ArrayList<String>();
		Camera.Size mSize;
		for (Camera.Size size : sizes) {
			options.add(size.width+"x"+size.height);
		}
        camera.release();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item,options);
        spinner.setAdapter(adapter);
        spinner.setSelection(Integer.valueOf(prefs.getString(CameraStream.PREF_JPEG_SIZE, "0")));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(CameraStream.PREF_JPEG_SIZE, String.valueOf(position));
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

}
