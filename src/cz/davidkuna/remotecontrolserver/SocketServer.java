package cz.davidkuna.remotecontrolserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SocketServer extends Thread {
	
	ServerSocket serverSocket;
	public static final int SERVERPORT = 6000;
	private final int MAX_AVAILABLE = 2;
	private Semaphore connLock = new Semaphore(MAX_AVAILABLE);
	long totalBytes = 0;
	
	Handler seconds = new Handler() { 
	    @Override 
	    public void handleMessage(Message msg) { 
	      int sentInt = msg.getData().getInt("what");
	      addToTotal(sentInt); 
	      // twTransferredDataSize.setText("r:"+Integer.toString(sentInt));
	    } 
	  }; 
	
	public void close() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			Log.d(MainActivity.LOGTAG, "Error, probably interrupted in accept(), see log");
			e.printStackTrace();
		}
	}
	
	public void addToTotal(long size){
		totalBytes += size;
	}
	
	public void run() {
        try {
        	Log.d(MainActivity.LOGTAG, "Creating Socket");
            serverSocket = new ServerSocket(SERVERPORT);
            
            while (!Thread.currentThread().isInterrupted()) {
            	Socket socket = null;
            	Log.d(MainActivity.LOGTAG, "Socket Waiting for connection");
                socket = serverSocket.accept();
                Log.d(MainActivity.LOGTAG, "Socket Accepted");
                
                if(connLock.tryAcquire()){                	
					CommunicationThread commThread = new CommunicationThread(socket, connLock);
					new Thread(commThread).start();

                } else {
                	Log.d(MainActivity.LOGTAG, "ERROR 503");
                }
            }
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
        }
    }

}