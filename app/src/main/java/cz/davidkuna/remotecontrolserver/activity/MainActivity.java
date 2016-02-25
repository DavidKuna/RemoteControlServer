package cz.davidkuna.remotecontrolserver.activity;

import cz.davidkuna.remotecontrolserver.R;
import cz.davidkuna.remotecontrolserver.helpers.Network;
import cz.davidkuna.remotecontrolserver.helpers.Settings;
import cz.davidkuna.remotecontrolserver.sensors.SensorController;
import cz.davidkuna.remotecontrolserver.socket.SendClientMessageListener;
import cz.davidkuna.remotecontrolserver.socket.SocketServer;
import cz.davidkuna.remotecontrolserver.socket.SocketServerEventListener;
import cz.davidkuna.remotecontrolserver.socket.UDPServer;
import cz.davidkuna.remotecontrolserver.video.CameraStream;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class MainActivity extends Activity implements SendClientMessageListener, SocketServerEventListener, OnClickListener {
	
	public static final String LOGTAG = "RC_SERVER";

	private UDPServer udpServer = null;
	private SocketServer socketServer;
	private SensorController sensorController;

    private CameraStream cameraStream = null;
	private ToggleButton toggleSocketServer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		TextView tvLocalIP = (TextView) findViewById(R.id.tvLocalIP);
		tvLocalIP.setText(Network.getLocalIpAddress());

		toggleSocketServer = (ToggleButton)findViewById(R.id.toggleSocketServer);
		toggleSocketServer.setOnClickListener(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        PowerManager powerManager =	(PowerManager) getSystemService(POWER_SERVICE);
        SurfaceHolder mPreviewDisplay = ((SurfaceView) findViewById(R.id.camera)).getHolder();
        cameraStream = new CameraStream(powerManager, prefs, mPreviewDisplay);

        Settings settings = new Settings();
        settings.setServerAddress(Network.getLocalIpAddress())
                .setCameraUDPPort(8080);
        Gson gson = new Gson();
		qrCodeInit(gson.toJson(settings).toString(), 150, 150);
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
		udpServer.runUdpServer(sensorController);
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


}
