package cz.davidkuna.remotecontrolserver.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import cz.davidkuna.remotecontrolserver.activity.MainActivity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SocketServer {
	public static final int SERVERPORT = 6000;
	public static int EVENT_START = 1;
	public static int EVENT_STOP = 2;
	public static int EVENT_CLIENT_CONNECT = 3;
	public static int EVENT_CLIENT_DISCONNECT = 4;
	public static int EVENT_ERROR = 5;
	
	private ServerSocket serverSocket;
	private CommunicationThread connectedClient;
    private SocketServerEventListener socketServerEventListener;
	
	public SocketServer(){
		this.start();
	}
	
	public void sendMessage(String message){
		if(connectedClient != null) {
			connectedClient.send(message);
		} else {
			Log.d(MainActivity.LOGTAG, "Message can not be sent, due to missing connection.");
		}
	}
	
	Handler seconds = new Handler() { 
	    @Override 
	    public void handleMessage(Message msg) { 
	      // twTransferredDataSize.setText("r:"+Integer.toString(sentInt));
	    } 
	  }; 
	
	public void close() {
		try {
			if(connectedClient != null) {
				connectedClient.close();
				connectedClient = null;
			}
			if(serverSocket != null) {
				serverSocket.close();
			}			
			setEvent(EVENT_STOP);
			setEvent(EVENT_CLIENT_DISCONNECT);
		} catch (IOException e) {
			Log.d(MainActivity.LOGTAG, "Error, probably interrupted in accept(), see log");
			e.printStackTrace();
		}
	}

	
	public void start() {
		
		Thread accept = new Thread() {
			 public void run(){
		
		        try {
		        	Log.d(MainActivity.LOGTAG, "Creating Socket");
		            serverSocket = new ServerSocket(SERVERPORT);
		            serverSocket.setReuseAddress(true);
		            
		            while (!serverSocket.isClosed()) {
		            	Log.d(MainActivity.LOGTAG, "Socket Waiting for connection");
		            	Socket socket = serverSocket.accept();            	
		                Log.d(MainActivity.LOGTAG, "Socket Accepted");
		                
		                if(connectedClient == null || connectedClient.isClosed()){
		                	connectedClient = new CommunicationThread(socket);
		                	setEvent(EVENT_CLIENT_CONNECT);
		                	Log.d(MainActivity.LOGTAG, "New socket connection");
		                } else {
		                	Log.d(MainActivity.LOGTAG, "Socket server is closed");
		                }
		            }
		            Log.d(MainActivity.LOGTAG, "Socket thread was interrupted");
		        } 
		        catch (IOException e) {
		            if (serverSocket != null && serverSocket.isClosed())
		            	Log.d(MainActivity.LOGTAG, "Normal exit");
		            else {
		            	Log.d(MainActivity.LOGTAG, "Error");
		            	e.printStackTrace();
		            }
		        }
		        finally {
		        	serverSocket = null;
		        	if(connectedClient != null){
			        	connectedClient.close();
			        	connectedClient = null;
			        	setEvent(EVENT_STOP);
			        	setEvent(EVENT_CLIENT_DISCONNECT);
		        	}
		        }
			 }
		};
		
		accept.setDaemon(true);
		accept.start();
		if(accept.isAlive()) {
			setEvent(EVENT_START);
		}
    }
	
	public boolean isClosed() {
		return (serverSocket == null || serverSocket.isClosed());
	}
	
	private void setEvent(int event) {
		if(socketServerEventListener != null) {
			socketServerEventListener.onSocketServerEvent(event);
		}
	}
	
	public void setSocketServerEventListener(SocketServerEventListener eventListener) {
		socketServerEventListener = eventListener;
	}

}