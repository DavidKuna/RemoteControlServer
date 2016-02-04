package cz.davidkuna.remotecontrolserver.activity;

import cz.davidkuna.remotecontrolserver.R;
import cz.davidkuna.remotecontrolserver.helpers.Network;
import cz.davidkuna.remotecontrolserver.location.GPSTracker;
import cz.davidkuna.remotecontrolserver.sensors.SensorController;
import cz.davidkuna.remotecontrolserver.socket.SendClientMessageListener;
import cz.davidkuna.remotecontrolserver.socket.SocketServer;
import cz.davidkuna.remotecontrolserver.socket.SocketServerEventListener;
import cz.davidkuna.remotecontrolserver.socket.UDPServer;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.util.Log;
import android.view.View.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends ActionBarActivity implements SendClientMessageListener, SocketServerEventListener {
	
	public static final String LOGTAG = "RC_SERVER";
	
	public static final int EVENTS_GPS = 1;
	public static final int EVENTS_ACCELEROMETER = 2;

	private UDPServer udpServer = null;
	private SocketServer socketServer;
	private SensorController sensorController;
	
	private TextView tvServerStatus;
	private TextView tvConnectionStatus;
	
	public static TextView text;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
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
    	sensorController = null;
    }
    
    public void enableGPSTracker() {

    }
    
    public void disableGPSTracker() {
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
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public class PlaceholderFragment extends Fragment implements OnClickListener {
		
		private ToggleButton toggleSocketServer, toggleInternalSensors, toggleGPSTracker;
		
		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			
			text = (TextView) rootView.findViewById(R.id.tvIncomingMessages); 
			tvServerStatus = (TextView) rootView.findViewById(R.id.tvServerStatus);
			tvConnectionStatus = (TextView) rootView.findViewById(R.id.tvConnectionStatus);
			TextView tvLocalIP = (TextView) rootView.findViewById(R.id.tvLocalIP);
			tvLocalIP.setText(Network.getLocalIpAddress());

			toggleSocketServer = (ToggleButton)rootView.findViewById(R.id.toggleSocketServer);
			toggleInternalSensors = (ToggleButton)rootView.findViewById(R.id.toggleInternalSensors);
			toggleGPSTracker = (ToggleButton)rootView.findViewById(R.id.toggleGPSTracker);
			
			toggleSocketServer.setOnClickListener(this);
			toggleInternalSensors.setOnClickListener(this);
			toggleGPSTracker.setOnClickListener(this);
			
			return rootView;
		}
		
		@Override
		public void onClick(View view) {
			if (view.getId() == R.id.toggleSocketServer) {
				//toggleSocketServer();
				toggleUDPServer();
			} else if (view.getId() == R.id.toggleInternalSensors) {
				toggleSensors();
			} else if (view.getId() == R.id.toggleGPSTracker) {
				toggleGPS();
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
				startUDPServer();
			} else {
				udpServer.stopUDPServer();
			}
		}
		
		private void toggleSensors() {
			if (sensorController == null) {
				enableSensorController();
			} else {
				disableSensorController();
			}
		}
		
		private void toggleGPS() {
			if (sensorController == null) {
				enableGPSTracker();
			} else {
				disableGPSTracker();
			}
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
            	if(event == SocketServer.EVENT_START) {
        			tvServerStatus.setText(R.string.start_server);
        		} else if (event == SocketServer.EVENT_STOP) {
        			tvServerStatus.setText(R.string.stoped);
        		} else if (event == SocketServer.EVENT_CLIENT_CONNECT) {
        			tvConnectionStatus.setText(R.string.connection_status);
        		} else if (event == SocketServer.EVENT_CLIENT_DISCONNECT) {
        			tvConnectionStatus.setText(R.string.disconnected);
        		}
            }
		});	
	}

}
