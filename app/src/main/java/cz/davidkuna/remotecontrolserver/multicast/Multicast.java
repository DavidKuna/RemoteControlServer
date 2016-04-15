package cz.davidkuna.remotecontrolserver.multicast;

import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

import cz.davidkuna.remotecontrolserver.helpers.Network;
import cz.davidkuna.remotecontrolserver.socket.SocketDatagramListener;
import cz.davidkuna.remotecontrolserver.socket.StunConnection;

/**
 * Created by David Kuna on 21.2.16.
 */
public class Multicast extends UDPOutputStream implements SocketDatagramListener {

    public static final int DEFAULT_BUFFER_SIZE = 1024;
    public static final int DEFAULT_MAX_BUFFER_SIZE = 8192;
    public static final int MAX_UDP_DATAGRAM_LEN = 4096;

    public final static String REQUEST_JOIN = "join";
    public static final int MAX_CONNECTION_TIME = 10000; //miliseconds
    public static final int CONNECTION_CHECK_INTERVAL = 1000; //miliseconds
    public final String TAG = "Multicast";

    private Thread mWorker = null;
    private Thread mCleaner = null;
    private DatagramSocket ds = null;
    private volatile boolean mRunning = false;
    private int mPort;
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

    int bufferMax = DEFAULT_MAX_BUFFER_SIZE;

    private boolean useSTUN = true;
    private String token;
    private StunConnection stunConnection = null;

    private ArrayList<MulticastClient> clients = new ArrayList<MulticastClient>();

    public Multicast(int port, int bufferMax) {
        this.mPort = port;
        setBufferSize(bufferMax);
    }

    public Multicast(int port) {
        this.mPort = port;
    }

    public Multicast(String token, int bufferMax) {
        this(token);
        setBufferSize(bufferMax);
    }

    public Multicast(String token) {
        this.token = token;
        useSTUN = true;
    }

    public void open()
    {
        Log.d(TAG, "OPEN");
        if (mRunning)
        {
            throw new IllegalStateException("Multicast is already running");
        } // if

        mRunning = true;
        mWorker = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if (useSTUN) {
                    stunWorkerRun();
                } else {
                    workerRun();
                }
            } // run()
        });
        mWorker.start();

        mCleaner = new Thread(new Runnable() {
            @Override
            public void run() {
               connectionCleaner();
            }
        });

        mCleaner.start();
    } // open()

    public void stop()
    {
        Log.d(TAG, "Stopping");
        if (!mRunning)
        {
            throw new IllegalStateException("Multicast is already stopped");
        }

        mRunning = false;
        ds.close();
        mWorker.interrupt();
        mCleaner.interrupt();

        Log.d(TAG, "Stopped");
    } // stop()

    private void workerRun()
    {
        byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
        DatagramPacket incoming = new DatagramPacket(lMsg, lMsg.length);

        try
        {
            ds = new DatagramSocket(mPort);
            Log.d(TAG, "Listening on port " + mPort);
            while(mRunning)
            {
                ds.receive(incoming);
                onDatagramReceived(incoming);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (ds != null && ds.isClosed() == false)
            {
                ds.close();
            }
        }
    }

    public void onDatagramReceived(DatagramPacket incoming) {
        byte[] data = incoming.getData();
        String s = new String(data, 0, incoming.getLength());
        Log.d("onDatagramReceived", s);
        if (s.equals(REQUEST_JOIN)) {
            join(new MulticastClient(incoming.getAddress(), incoming.getPort()));
        }
    }

    private void stunWorkerRun() {
        String stunServer = "stun.sipgate.net";
        String relayServer = "http://punkstore.wendy.netdevelo.cz/RemoteControlRelayServer/";
        int port = 10000;
        stunConnection =  new StunConnection(Network.getLocalInetAddress(), stunServer, port, relayServer);
        stunConnection.setSocketDatagramListener(this);
        stunConnection.connect(token);
    }

    private void join(MulticastClient client) {
        int index;
        synchronized(clients) {
            if ((index = clients.indexOf(client)) >= 0) {
                clients.get(index).setLastTime(System.currentTimeMillis());
            } else {
                try {
                    client.setLastTime(System.currentTimeMillis());
                    if (useSTUN) {
                        client.open(stunConnection.getSocket());
                    } else {
                        client.open();
                    }
                    Log.d(TAG, "New connection " + client.getAddress().getHostAddress());
                    clients.add(client);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void connectionCleaner() {
        while (mRunning) {
            SystemClock.sleep(CONNECTION_CHECK_INTERVAL);
            synchronized (clients) {
                for (MulticastClient client : clients) {
                    if ((client.getLastTime() + MAX_CONNECTION_TIME) < System.currentTimeMillis()) {
                        try {
                            client.getOutputStream().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        clients.remove(client);
                        Log.d(TAG, "Client disconected " + client.getAddress().getHostAddress());
                    }
                }
            }
        }
    }


    public void close() throws IOException {
        synchronized(clients) {
            for (MulticastClient client : clients) {
                client.getOutputStream().close();
            }
        }
        Log.d(TAG, "CLOSE");
    }


    public void flush() throws IOException {
        synchronized(clients) {
            for (MulticastClient client : clients) {
                client.getOutputStream().flush();
            }
        }
    }

    public void write(int value) throws IOException {
        synchronized(clients) {
            for (MulticastClient client : clients) {
                client.getOutputStream().write(value);
            }
        }
    }

    public void write(byte[] data) throws IOException {
        synchronized(clients) {
            for (MulticastClient client : clients) {
                client.getOutputStream().write(data);
            }
        }
    }

    public void write(byte[] data, int off, int len) throws IOException {
        synchronized(clients) {
            for (MulticastClient client : clients) {
                client.getOutputStream().write(data, off, len);
            }
        }
    }

    public int getBufferSize() {
        return buffer.length;
    }

    public void setMaxBufferSize(int max) {
        bufferMax = max;
    }

    public void setBufferSize(int buffSize) {
        synchronized(clients) {
            for (MulticastClient client : clients) {
                client.getOutputStream().setBufferSize(buffSize);
            }
        }
    }
}
