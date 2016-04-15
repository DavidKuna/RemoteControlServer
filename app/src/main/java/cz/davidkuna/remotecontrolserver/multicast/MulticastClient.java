package cz.davidkuna.remotecontrolserver.multicast;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by David Kuna on 21.2.16.
 */
public class MulticastClient {

    private InetAddress mAddress;
    private int mPort;
    private long lastTime;
    private UDPOutputStream outputStream;

    public MulticastClient(InetAddress address, int port) {
        mAddress = address;
        mPort = port;
        outputStream = new UDPOutputStream();
    }

    public InetAddress getAddress() {
        return mAddress;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public UDPOutputStream getOutputStream() {
        return outputStream;
    }

    public void open() throws IOException {
        outputStream.open(mAddress, mPort);
    }

    public void open(DatagramSocket socket) throws IOException {
        outputStream.open(socket, mAddress, mPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!MulticastClient.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final MulticastClient other = (MulticastClient) obj;

        if (!mAddress.getHostAddress().equals(other.getAddress().getHostAddress())) {
            return false;
        }

        if (mPort != other.getPort()) {
            return false;
        }

        return true;
    }

    public int getPort() {
        return mPort;
    }
}
