package cz.davidkuna.remotecontrolserver.sensors;

import android.os.SystemClock;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;

import cz.davidkuna.remotecontrolserver.multicast.Multicast;

/**
 * Created by David Kuna on 13.3.16.
 */
public class SensorDataStream {

    private final String TAG = "SensorDataStream";
    private final int BUFFER_SIZE = 2000;
    private final int INTERVAL = 100; // miliseconds

    private int mPort;
    private volatile boolean mRunning = false;
    private Thread mWorker = null;
    private SensorController sensorController;

    private boolean useSTUN = false;
    private String token;

    public SensorDataStream(int port, SensorController sensorController) throws IOException {
        mPort = port;
        this.sensorController = sensorController;
    }

    public SensorDataStream(String token, SensorController sensorController) throws IOException {
        this.token = token;
        useSTUN = true;
        this.sensorController = sensorController;
    }

    public void start()
    {
        if (mRunning)
        {
            throw new IllegalStateException("SensorDataStream is already running");
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
            throw new IllegalStateException("SensorDataStream is already stopped");
        }

        mRunning = false;
        mWorker.interrupt();
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
        Multicast multicast = null;
        try {
            if (useSTUN) {
                multicast = new Multicast(token, BUFFER_SIZE);
            } else {
                multicast = new Multicast(mPort, BUFFER_SIZE);
            }
            multicast.open();
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
            if (multicast != null)
            {
                multicast.stop();
            }
        }
    }
}
