package cz.davidkuna.remotecontrolserver.helpers;

/**
 * Created by David Kuna on 25.2.16.
 */
public class Settings {

    private String serverAddress;
    private int cameraUDPPort;
    private int sensorUDPPort;
    private int controlUDPPort;
    private String cameraToken = "";
    private String sensorToken = "";
    private String controlToken = "";
    private String stunServer;
    private int stunPort;
    private String relayServer;
    private boolean useStun;

    public String getServerAddress() {
        return serverAddress;
    }

    public Settings setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
        return this;
    }

    public int getCameraUDPPort() {
        return cameraUDPPort;
    }

    public Settings setCameraUDPPort(int cameraUDPPort) {
        this.cameraUDPPort = cameraUDPPort;
        return this;
    }

    public int getSensorUDPPort() {
        return sensorUDPPort;
    }

    public Settings setSensorUDPPort(int sensorUDPPort) {
        this.sensorUDPPort = sensorUDPPort;
        return this;
    }

    public int getControlUDPPort() {
        return controlUDPPort;
    }

    public Settings setControlUDPPort(int controlUDPPort) {
        this.controlUDPPort = controlUDPPort;
        return this;
    }

    public String getCameraToken() {
        return cameraToken;
    }

    public Settings setCameraToken(String cameraToken) {
        this.cameraToken = cameraToken;
        return this;
    }

    public String getStunServer() {
        return stunServer;
    }

    public Settings setStunServer(String stunServer) {
        this.stunServer = stunServer;
        return this;
    }

    public String getRelayServer() {
        return relayServer;
    }

    public Settings setRelayServer(String relayServer) {
        this.relayServer = relayServer;
        return this;
    }

    public boolean isUseStun() {
        return useStun;
    }

    public Settings setUseStun(boolean useStun) {
        this.useStun = useStun;
        return this;
    }

    public String getSensorToken() {
        return sensorToken;
    }

    public Settings setSensorToken(String sensorToken) {
        this.sensorToken = sensorToken;
        return this;
    }

    public String getControlToken() {
        return controlToken;
    }

    public Settings setControlToken(String controlToken) {
        this.controlToken = controlToken;
        return this;
    }

    public int getStunPort() {
        return stunPort;
    }

    public Settings setStunPort(int stunPort) {
        this.stunPort = stunPort;
        return this;
    }
}
