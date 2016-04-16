package cz.davidkuna.remotecontrolserver.helpers;

import android.util.Log;

import org.apache.http.conn.util.InetAddressUtils;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.Enumeration;

/**
 * Created by David Kuna on 4.2.16.
 */
public class Network {

    private static SecureRandom random = new SecureRandom();

    public static InetAddress getLocalInetAddress() {
        String ipv4;
        try {
            final Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements())
            {
                final NetworkInterface intf = en.nextElement();
                final Enumeration<InetAddress> enumIpAddr =
                        intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements())
                {
                    final InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        final String addr = inetAddress.getHostAddress().toUpperCase();
                        if(!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address && !isDebugIP(addr))
                        {
                            return inetAddress;
                        }
                    } // if
                } // while
            } // for
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return null;
    }

    public static String getLocalIpAddress() {
        String ipv4;
        try {
            final Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements())
            {
                final NetworkInterface intf = en.nextElement();
                final Enumeration<InetAddress> enumIpAddr =
                        intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements())
                {
                    final InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        final String addr = inetAddress.getHostAddress().toUpperCase();
                        if (InetAddressUtils.isIPv4Address(addr) && !isDebugIP(addr))
                        {
                            return addr;
                        }
                    } // if
                } // while
            } // for
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return null;
    }

    public static boolean isDebugIP(String addr) {
        return addr.equals("10.0.2.15");
    }

    public static String nextSessionId() {
        return new BigInteger(130, random).toString(32);
    }
}
