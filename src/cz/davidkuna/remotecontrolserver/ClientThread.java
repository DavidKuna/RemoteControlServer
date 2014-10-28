package cz.davidkuna.remotecontrolserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;

import android.os.Environment;
import android.util.Log;

public class ClientThread implements Runnable{
	
	private Socket s;
	private Semaphore connLock;
	
	public ClientThread(Socket s, Semaphore connLock) throws IOException{
		this.s = s;
		this.connLock = connLock;
	}
	
	private String getHeaderDate(){
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEE, dd MMM yyy HH:mm:ss z", Locale.UK);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GTM"));
		return dateFormat.format(calendar.getTime());
	}
	
	private String getHeader(int contentLength, String contentType){
		String header = "HTTP/1.1 200 OK\n"
				+ "Contetnt-Type: " + contentType + "\n"
				+ "Date: " + getHeaderDate() + "\n"
				+ "Server: Android\n"
				+ "Content-Length: " + contentLength + "\n\n";
		
		return header;
	}
	
	private String[] parseHeader(BufferedReader in) throws IOException{
		String tmp = in.readLine();
		String [] initial = new String[]{"",""};
		if(tmp !=null &&  !tmp.isEmpty()){
			initial = tmp.split(" ");
		}
		return initial;
	}
	
	private String readFile(File f) throws IOException{
		StringBuffer fileContent = new StringBuffer("");
		@SuppressWarnings("resource")
		FileInputStream fis = new FileInputStream(f);
		byte[] buffer = new byte[1024];
		
		while(fis.read(buffer) != -1){
			fileContent.append(new String(buffer));
		}
		return fileContent.toString();
	}
	
	public void run(){
		try{
			Log.d("THREAD", "TID  ");
			OutputStream o = this.s.getOutputStream();
	    	BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
	    	BufferedReader in = new BufferedReader(new InputStreamReader(this.s.getInputStream()));
	
	    	String [] header = this.parseHeader(in);
	    	String filePath = header[1];
	    	String sdCard = Environment.getExternalStorageDirectory().getAbsolutePath();
	    	String contentType = null;
	    	File f = new File(sdCard + filePath);
	    	
	    	if(filePath.endsWith("html") || filePath.endsWith("htm")){
	    		contentType = "text/html";
	    	}else if(filePath.endsWith("jpg") || filePath.endsWith("jpeg")){
	    		contentType = "image/jpeg";
	    	}else{
	    		contentType = "text/plain";
	    	}
	    	
	    	Log.d("SERVER", "File path:  " + filePath);
	    	
	        String content = this.readFile(f);
	        out.write(getHeader(content.length(), contentType));
	        
	        FileInputStream fis = new FileInputStream(f);
			byte[] buffer = new byte[1024];
			int len;
			while( (len = fis.read(buffer)) != -1){
				o.write(buffer, 0, len);
			}
			
			connLock.release();
			this.s.close();
            Log.d("SERVER", "Socket Closed");
		}
		catch(IOException e){
			connLock.release();
			Log.d("SERVER", "Error");
        	e.printStackTrace();
		}
	}
}
