package cz.davidkuna.remotecontrolserver.socket;

import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.ErrorCode;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.util.UtilityException;

/**
 * Created by David Kuna on 27.2.16.
 */
public class StunConnection {

    private final String TAG = "StunConnection";
    private InetAddress iaddress;
    private String stunServer;
    private String relayServer;
    private int port;
    private MappedAddress ma = null;
    private boolean nodeNatted = true;
    private DatagramSocket socket = null;
    private String relatedIp = null;
    private int relatedPort;
    private boolean related = false;
    private Thread heartBeat = null;
    private SocketDatagramListener socketDatagramListener = null;
    private final String HEART_BEAT = "beat";
    private Relation relation = null;
    private String token;
    private final int CONNECTION_TIMEOUT = 30000; //ms

    private boolean serverActive = true;
    private static final int MAX_UDP_DATAGRAM_LEN = 4096;

    public StunConnection(InetAddress iaddress , String stunServer, int port, String relayServer, String token) {

        this.iaddress = iaddress;
        this.stunServer = stunServer;
        this.port = port;
        this.relayServer = relayServer;
        this.token = token;
    }

    public boolean connect() {
        long startTime = System.currentTimeMillis();
        while (serverActive && (startTime + CONNECTION_TIMEOUT) > System.currentTimeMillis()) {
            try {
                if (loadMappedAddress()) {
                    while (serverActive && (startTime + CONNECTION_TIMEOUT) > System.currentTimeMillis()) {
                        if (createRelation(token)) {

                            heartBeat = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    byte[] data = HEART_BEAT.getBytes();

                                    while (serverActive) {
                                        try {
                                            DatagramPacket send = new DatagramPacket(data, data.length, InetAddress.getByName(relatedIp), relatedPort);
                                            Log.d(TAG, "Beat to " + relatedIp + ":" + relatedPort);
                                            socket.send(send);
                                            Thread.sleep(5000);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                            heartBeat.start();

                            byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
                            while (serverActive) {
                                DatagramPacket incoming = new DatagramPacket(lMsg, lMsg.length);
                                socket.receive(incoming);
                                byte[] receivedData = incoming.getData();
                                String s = new String(receivedData, 0, incoming.getLength());
                                if (!s.equals(HEART_BEAT) && socketDatagramListener != null) {
                                    socketDatagramListener.onDatagramReceived(incoming);
                                }
                            }
                        } else {
                            Log.d(TAG, "Creating relation failed - repeat");
                        }
                    }
                } else {
                    Log.d(TAG, "Getting mapped address failed - repeat");
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                serverActive = false;
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "Connection timeout");
        return true;
    }

    private boolean loadMappedAddress() {
        try {
            socket = new DatagramSocket(new InetSocketAddress(iaddress, 0));
            socket.setReuseAddress(true);

            MessageHeader sendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
            sendMH.generateTransactionID();

            ChangeRequest changeRequest = new ChangeRequest();
            sendMH.addMessageAttribute(changeRequest);

            byte[] data = sendMH.getBytes();
            DatagramPacket send = new DatagramPacket(data, data.length, InetAddress.getByName(stunServer), port);
            socket.send(send);

            MessageHeader receiveMH = new MessageHeader();
            while (!(receiveMH.equalTransactionID(sendMH))) {
                DatagramPacket receive = new DatagramPacket(new byte[200], 200);
                socket.receive(receive);
                receiveMH = MessageHeader.parseHeader(receive.getData());
                receiveMH.parseAttributes(receive.getData());
            }

            ma = (MappedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);
            ErrorCode ec = (ErrorCode) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ErrorCode);
            if (ec != null) {
                Log.d(TAG, "Message header contains an Errorcode message attribute.");
                return false;
            }
            if ((ma == null)) {
                Log.d(TAG, "The server is sending an incomplete response (Mapped Address and Changed Address message attributes are missing). The client should not retry.");
                Log.d(TAG, "Response does not contain a Mapped Address or Changed Address message attribute.");
                return false;
            }

            if ((ma.getPort() == socket.getLocalPort()) && (ma.getAddress().getInetAddress().equals(socket.getLocalAddress()))) {
                Log.d(TAG, "Node is not natted.");
                nodeNatted = false;
            } else {
                Log.d(TAG, "Node is natted.");
            }

            return true;
        } catch (UtilityException e) {
            e.printStackTrace();
        } catch (MessageHeaderParsingException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessageAttributeParsingException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean createRelation(String token) {
        try {
            String charset = "UTF-8";
            String query = String.format("token=%s&ip=%s&port=%s",
                    URLEncoder.encode(token, charset),
                    URLEncoder.encode(String.valueOf(ma.getAddress()), charset),
                    URLEncoder.encode(String.valueOf(ma.getPort()), charset));
            URL relayServerURL = new URL(relayServer + "?" + query);

            //String encoding = Base64.encodeToString(new String("wendy:wendy").getBytes(), Base64.DEFAULT);

            while (!related) {
                HttpURLConnection connection = (HttpURLConnection) relayServerURL.openConnection();
                connection.setRequestMethod("GET");
                connection.setUseCaches(false);
                connection.setAllowUserInteraction(false);
                connection.setRequestProperty("User-Agent", "Mozilla/4.0");
                connection.setRequestProperty("Host", relayServerURL.getHost());
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                connection.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
                connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("Accept-Charset", charset);
                //connection.setRequestProperty("Authorization", "Basic " + encoding);

                int statusCode = connection.getResponseCode();

                InputStream is;

                if (statusCode >= 200 && statusCode < 400) {
                    is = connection.getInputStream();
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(is));
                        String inputLine;

                        while ((inputLine = in.readLine()) != null) {
                            JSONObject json = new JSONObject(inputLine);
                            relatedIp = json.getString("ip");
                            relatedPort = json.getInt("port");
                            related = true;
                            if (relation != null) {
                                relation.onRelationCreated();
                            }
                            Log.d(TAG, "Relation created " + relatedIp + ":" + relatedPort);
                            return true;
                        }

                        in.close();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "Waiting for relation. Code " + statusCode);
                }

                SystemClock.sleep(5000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void send(String messgage) {
        if (serverActive && isRelated()) {
            try {
                DatagramPacket send = null;
                send = new DatagramPacket(messgage.getBytes(), messgage.length(), InetAddress.getByName(relatedIp), relatedPort);
                socket.send(send);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if (heartBeat != null) {
            heartBeat.interrupt();
        }
        serverActive = false;

        if (socket != null) {
            socket.close();
        }
    }

    public void setSocketDatagramListener(SocketDatagramListener listener) {
        socketDatagramListener = listener;
    }

    public MappedAddress getMappedAddress() {
        return ma;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public void setRelation(Relation relation) {
        this.relation = relation;
    }

    public boolean isRelated() {
        return related;
    }
}
