package cz.davidkuna.remotecontrolserver.sensors;

import android.os.SystemClock;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;

import cz.davidkuna.remotecontrolserver.helpers.Network;
import cz.davidkuna.remotecontrolserver.helpers.Settings;
import cz.davidkuna.remotecontrolserver.multicast.Multicast;
import cz.davidkuna.remotecontrolserver.socket.StunConnection;

/**
 * Created by David Kuna on 13.3.16.
 */
public class SensorDataStream {

    private final String TAG = "SensorDataStream";
    private final int BUFFER_SIZE = 1000;
    private final int INTERVAL = 250; // miliseconds

    private volatile boolean mRunning = false;
    private Thread mWorker = null;
    private SensorController sensorController;
    private StunConnection connection = null;
    Multicast multicast = null;

    public SensorDataStream(Settings settings, SensorController sensorController) throws IOException {
        if(settings.isUseStun()) {
            connection = new StunConnection(Network.getLocalInetAddress(),
                    settings.getStunServer(),
                    settings.getStunPort(),
                    settings.getRelayServer(),
                    settings.getSensorToken());
            multicast = new Multicast(connection, BUFFER_SIZE);
        } else {
            multicast = new Multicast(settings.getSensorUDPPort(), BUFFER_SIZE);
        }
        this.sensorController = sensorController;

    }

    public void start()
    {
        if (mRunning)
        {
            Log.d(TAG, "SensorDataStream is already running");
            return;
        }

        mRunning = true;
        mWorker = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                workerRun();
            } // run()
        });
        mWorker.start();
    }

    public void stop()
    {
        if (!mRunning)
        {
            Log.d(TAG, "SensorDataStream is already stopped");
            return;
        }

        mRunning = false;
        mWorker.interrupt();
        if (multicast != null)
        {
            multicast.stop();
        }
        if (connection != null) {
            connection.close();
        }
        Log.d(TAG, "STOP");
    }

    private void workerRun()
    {
        while (mRunning)
        {
            try
            {
                acceptAndStream();
            }
            catch (final IOException exceptionWhileStreaming)
            {
                System.err.println(exceptionWhileStreaming);
            }
        }
    }

    private void acceptAndStream() throws IOException {
        DataOutputStream stream = null;

        try {
            try {
                multicast.open();
            } catch (IllegalStateException e) {}
            stream = new DataOutputStream(multicast);

            while (mRunning) {
                String s = sensorController.getData().toString();
                stream.writeBytes(s);
                stream.flush();
                SystemClock.sleep(INTERVAL);
            }
        } finally {
            if (stream != null)
            {
                try
                {
                    Log.d(TAG, "FINALLY");
                    stream.close();
                }
                catch (final IOException closingStream)
                {
                    System.err.println(closingStream);
                }
            }
        }
    }
}
