package cz.davidkuna.remotecontrolserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.Semaphore;

import android.util.Log;

public class CommunicationThread implements Runnable{
	
	private Socket clientSocket;
	private Semaphore connLock;
	private BufferedReader input;
	private BufferedWriter output;
	
	public CommunicationThread(Socket clientSocket, Semaphore connLock) throws IOException{
		this.clientSocket = clientSocket;
		this.connLock = connLock;
		try {

			this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			OutputStream outputStream = this.clientSocket.getOutputStream();
	    	this.output = new BufferedWriter(new OutputStreamWriter(outputStream));

		} catch (IOException e) {
			connLock.release();
			Log.d("SERVER", "Error");
			e.printStackTrace();
		}
	}
	
	public void run(){
		
		while (!Thread.currentThread().isInterrupted()) {

			try {

				String read = input.readLine();
				Log.d(MainActivity.LOGTAG, read);
				//updateConversationHandler.post(new updateUIThread(read));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void close() throws IOException {
		connLock.release();
		this.clientSocket.close();
		Log.d("SERVER", "Socket Closed");
	}
}
