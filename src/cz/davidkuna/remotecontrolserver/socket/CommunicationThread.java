package cz.davidkuna.remotecontrolserver.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

import cz.davidkuna.remotecontrolserver.activity.MainActivity;
import android.util.Log;

public class CommunicationThread{
	
	private Socket clientSocket;
	private BufferedReader input;
	private BufferedWriter output;
	private Thread thread;
	
	/**
	 * @param clientSocket
	 * @param connLock
	 * @throws IOException
	 */
	public CommunicationThread(Socket clientSocket) throws IOException{
		this.clientSocket = clientSocket;
		try {

			this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			OutputStream outputStream = this.clientSocket.getOutputStream();
	    	this.output = new BufferedWriter(new OutputStreamWriter(outputStream));
	    	this.start();

		} catch (IOException e) {
			Log.d("RC_SERVER", "Error");
			e.printStackTrace();
		}
	}
	
	private void start(){
		thread = new Thread(){
			public void run(){
				while (clientSocket.isConnected()) {
					try {
						
						String read = input.readLine();
						if(read == null) {
							Log.d(MainActivity.LOGTAG, "Input null exception");
							break;
						}
						Log.d(MainActivity.LOGTAG, read);
						//updateConversationHandler.post(new updateUIThread(read));

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				close();
			}		
		};
		thread.start();
	}
	
	/**
	 * Send message to client
	 * @param message
	 * @throws IOException
	 */
	public void send(String message){
		try {			
			output.write(message);
			output.flush();
		} catch (Exception e) {
			Log.d("RC_SERVER", "Message could not be sent");
		}		
	}
	
	public void close() {	
		try {			
			thread.interrupt();
			input.close();
			output.close();
			clientSocket.close();
			
		} catch (IOException e) {
			Log.d("RC_SERVER", "Client socket could not be closed");
			e.printStackTrace();
		}		
		Log.d("RC_SERVER", "Socket Closed");
	}
	
	public boolean isClosed() {
		return clientSocket.isClosed();
	}
}
