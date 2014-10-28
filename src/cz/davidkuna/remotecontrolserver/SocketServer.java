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
	public final int port = 12345;
	private final int MAX_AVAILABLE = 2;
	boolean bRunning;
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
			Log.d("SERVER", "Error, probably interrupted in accept(), see log");
			e.printStackTrace();
		}
		bRunning = false;
	}
	
	public void addToTotal(long size){
		totalBytes += size;
	}
	
	public void run() {
        try {
        	Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;
            while (bRunning) {
            	Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept(); 
                Log.d("SERVER", "Socket Accepted");
                
                if(connLock.tryAcquire()){
	                Thread t1 = new Thread(new ClientThread(s, connLock));
	                t1.start(); 
                } else {
                	Log.d("SERVER", "ERROR 503");
                }
            }
        } 
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
            	Log.d("SERVER", "Normal exit");
            else {
            	Log.d("SERVER", "Error");
            	e.printStackTrace();
            }
        }
        finally {
        	serverSocket = null;
        	bRunning = false;
        }
    }

}