package cz.davidkuna.remotecontrolserver.socket;

import android.util.Log;

import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import cz.davidkuna.remotecontrolserver.helpers.Command;
import cz.davidkuna.remotecontrolserver.helpers.Network;
import cz.davidkuna.remotecontrolserver.helpers.Settings;

/**
 * Created by David Kuna on 16.4.16.
 */
public class ControlCommandListener implements SocketDatagramListener {

    private final String TAG = "ControlCmdListener";
    private static final int MAX_UDP_DATAGRAM_LEN = 4096;
    private DatagramSocket socket = null;
    private boolean mRunning = false;
    private Thread mWorker = null;
    private StunConnection stunConnection = null;
    private Settings settings = null;
    private CommandEventListener commandEventListener = null;

    public ControlCommandListener(Settings settings) {
        this.settings = settings;
        if (settings.isUseStun()) {
            stunConnection = new StunConnection(Network.getLocalInetAddress(),
                    settings.getStunServer(),
                    settings.getStunPort(),
                    settings.getRelayServer(),
                    settings.getControlToken());
            stunConnection.setSocketDatagramListener(this);
        }
    }

    public void open() {
        Log.d(TAG, "OPEN");
        if (mRunning)
        {
            throw new IllegalStateException("ControlCommandListener is already running");
        }

        mRunning = true;
        mWorker = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if (stunConnection != null) {
                    stunConnection.connect();
                } else {
                    receive();
                }
            }
        });
        mWorker.start();
    }

    private void receive() {
        byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
        DatagramPacket incoming = new DatagramPacket(lMsg, lMsg.length);
        try
        {
            socket = new DatagramSocket(settings.getControlUDPPort());
            while(mRunning)
            {
                socket.receive(incoming);
                onDatagramReceived(incoming);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onDatagramReceived(DatagramPacket incoming) {
        byte[] data = incoming.getData();
        String s = new String(data, 0, incoming.getLength());

        Command command = new Gson().fromJson(s, Command.class);

        if (command.getName().equals(Command.MOVE_UP)) {
            Log.d("MOVE", "UP");
        } else if (command.getName().equals(Command.MOVE_DOWN)) {
            Log.d("MOVE", "DOWN");
        } else if (command.getName().equals(Command.MOVE_LEFT)) {
            Log.d("MOVE", "LEFT");
        } else if (command.getName().equals(Command.MOVE_RIGHT)) {
            Log.d("MOVE", "RIGHT");
        }
        if (commandEventListener != null) {
            commandEventListener.onCommandReceived(command);
        }
    }

    public void close() {
        if (socket != null)
        {
            socket.close();
        }
        if (stunConnection != null) {
            stunConnection.close();
        }
    }

    public void setCommandEventListener(CommandEventListener commandEventListener) {
        this.commandEventListener = commandEventListener;
    }
}
