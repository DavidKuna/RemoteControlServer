package cz.davidkuna.remotecontrolserver.socket;

import java.net.DatagramPacket;

/**
 * Created by David Kuna on 15.4.16.
 */
public interface SocketDatagramListener {

    void onDatagramReceived(DatagramPacket incoming);
}
