package cz.davidkuna.remotecontrolserver.socket;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import cz.davidkuna.remotecontrolserver.helpers.Command;
import cz.davidkuna.remotecontrolserver.sensors.SensorController;

/**
 * Created by David Kuna on 6.1.16.
 */
public class UDPServer {
    private AsyncTask<Void, Void, Void> async;
    private boolean serverActive = true;
    private int localPort;
    private static final int MAX_UDP_DATAGRAM_LEN = 4096;

    @SuppressLint("NewApi")
    public void runUdpServer(final int localPort, final SensorController sensorController)
    {
        this.localPort = localPort;
        async = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
                DatagramPacket incoming = new DatagramPacket(lMsg, lMsg.length);
                DatagramSocket ds = null;

                try
                {
                    ds = new DatagramSocket(localPort);

                    while(serverActive)
                    {
                        ds.receive(incoming);
                        byte[] data = incoming.getData();
                        String s = new String(data, 0, incoming.getLength());

                        //Log.i("UDP packet received", incoming.getAddress().getHostAddress() + " : " + incoming.getPort() + " - " + s);

                        Command command = new Gson().fromJson(s, Command.class);

                        if (command.getName().equals(Command.GET_DATA)) {
                            s = sensorController.getData().toString();
                            DatagramPacket dp = new DatagramPacket(s.getBytes(), s.getBytes().length, incoming.getAddress(), 8001);
                            //Log.i("UDP packet sent", incoming.getAddress().getHostAddress() + " : " + 8001 + " - " + s);
                            //ds.send(dp);
                        } else if (command.getName().equals(Command.MOVE_UP)) {
                            Log.d("MOVE", "UP");
                        } else if (command.getName().equals(Command.MOVE_DOWN)) {
                            Log.d("MOVE", "DOWN");
                        } else if (command.getName().equals(Command.MOVE_LEFT)) {
                            Log.d("MOVE", "LEFT");
                        } else if (command.getName().equals(Command.MOVE_RIGHT)) {
                            Log.d("MOVE", "RIGHT");
                        }

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
