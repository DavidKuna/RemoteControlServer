package cz.davidkuna.remotecontrolserver.socket;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import cz.davidkuna.remotecontrolserver.sensors.SensorController;

/**
 * Created by David Kuna on 6.1.16.
 */
public class UDPServer {
    private AsyncTask<Void, Void, Void> async;
    private boolean serverActive = true;
    public static final int SERVERPORT = 8000;
    private static final int MAX_UDP_DATAGRAM_LEN = 4096;

    @SuppressLint("NewApi")
    public void runUdpServer(final SensorController sensorController)
    {
        async = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                String lText;
                byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
                DatagramPacket incoming = new DatagramPacket(lMsg, lMsg.length);
                DatagramSocket ds = null;

                try
                {
                    ds = new DatagramSocket(SERVERPORT);

                    while(serverActive)
                    {
                        ds.receive(incoming);
                        byte[] data = incoming.getData();
                        String s = new String(data, 0, incoming.getLength());

                        Log.i("UDP packet received", incoming.getAddress().getHostAddress() + " : " + incoming.getPort() + " - " + s);

                        s = sensorController.getData().toString();
                        DatagramPacket dp = new DatagramPacket(s.getBytes() , s.getBytes().length , incoming.getAddress() , 8001);
                        ds.send(dp);

                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    if (ds != null)
                    {
                        ds.close();
                    }
                }

                return null;
            }
        };

        if (Build.VERSION.SDK_INT >= 11) async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else async.execute();
    }

    public void stopUDPServer()
    {
        serverActive = false;
    }
}
