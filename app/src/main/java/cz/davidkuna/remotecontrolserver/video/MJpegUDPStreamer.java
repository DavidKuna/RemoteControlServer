/* Copyright 2013 Foxdog Studios Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.davidkuna.remotecontrolserver.video;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;

import cz.davidkuna.remotecontrolserver.helpers.Network;
import cz.davidkuna.remotecontrolserver.helpers.Settings;
import cz.davidkuna.remotecontrolserver.multicast.Multicast;
import cz.davidkuna.remotecontrolserver.socket.StunConnection;

/* package */ final class MJpegUDPStreamer
{
    private static final String TAG = MJpegUDPStreamer.class.getSimpleName();

    private static final String BOUNDARY = "--gc0p4Jq0M2Yt08jU534c0p--";
    private static final String BOUNDARY_LINES = "\r\n" + BOUNDARY + "\r\n";

    private static final String HTTP_HEADER =
            "HTTP/1.0 200 OK\r\n"
                    + "Server: Peepers\r\n"
                    + "Connection: close\r\n"
                    + "Max-Age: 0\r\n"
                    + "Expires: 0\r\n"
                    + "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, "
                    + "post-check=0, max-age=0\r\n"
                    + "Pragma: no-cache\r\n"
                    + "Access-Control-Allow-Origin:*\r\n"
                    + "Content-Type: multipart/x-mixed-replace; "
                    + "boundary=" + BOUNDARY + "\r\n"
                    + BOUNDARY_LINES;

    private boolean mNewJpeg = false;
    private boolean mStreamingBufferA = true;
    private final byte[] mBufferA;
    private final byte[] mBufferB;
    private int mLengthA = Integer.MIN_VALUE;
    private int mLengthB = Integer.MIN_VALUE;
    private long mTimestampA = Long.MIN_VALUE;
    private long mTimestampB = Long.MIN_VALUE;
    private final Object mBufferLock = new Object();

    private Thread mWorker = null;
    private volatile boolean mRunning = false;
    private Multicast multicast = null;
    private StunConnection connection = null;

    /* package */ MJpegUDPStreamer(final int port, final int bufferSize)
    {
        super();
        mBufferA = new byte[bufferSize];
        mBufferB = new byte[bufferSize];
        multicast = new Multicast(port, mBufferA.length);
    }

    /* package */ MJpegUDPStreamer(Settings settings, final int bufferSize)
    {
        super();
        mBufferA = new byte[bufferSize];
        mBufferB = new byte[bufferSize];

        if (settings.isUseStun()) {
            connection = new StunConnection(Network.getLocalInetAddress(),
                    settings.getStunServer(),
                    settings.getStunPort(),
                    settings.getRelayServer(),
                    settings.getCameraToken());
            multicast = new Multicast(connection, mBufferA.length);

        } else {
            multicast = new Multicast(settings.getCameraUDPPort(), mBufferA.length);
        }

    }

    /* package */ void start()
    {
        if (mRunning)
        {
            throw new IllegalStateException("MJpegUDPStreamer is already running");
        } // if

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
    } // open()

    /* package */ void stop()
    {
        if (!mRunning)
        {
            throw new IllegalStateException("MJpegUDPStreamer is already stopped");
        } // if

        mRunning = false;
        mWorker.interrupt();
        if (multicast != null)
        {
            multicast.stop();
        }
        if (connection != null) {
            connection.close();
        }
    } // stop()

    /* package */ void streamJpeg(final byte[] jpeg, final int length, final long timestamp)
    {
        synchronized (mBufferLock)
        {
            final byte[] buffer;
            if (mStreamingBufferA)
            {
                buffer = mBufferB;
                mLengthB = length;
                mTimestampB = timestamp;
            } // if
            else
            {
                buffer = mBufferA;
                mLengthA = length;
                mTimestampA = timestamp;
            } // else
            System.arraycopy(jpeg, 0 /* srcPos */, buffer, 0 /* dstPos */, length);
            mNewJpeg = true;
            mBufferLock.notify();
        } // synchronized
    } // streamJpeg(byte[], int, long)

    private void workerRun()
    {
        while (mRunning)
        {
            try
            {
                acceptAndStream();
            } // try
            catch (final IOException exceptionWhileStreaming)
            {
                System.err.println(exceptionWhileStreaming);
            } // catch
        } // while
    } // mainLoop()

    private void acceptAndStream() throws IOException
    {
        DataOutputStream stream = null;


            try {
                try {
                    multicast.open();
                } catch (IllegalStateException e) {}
            stream = new DataOutputStream(multicast);
            stream.writeBytes(HTTP_HEADER);
            stream.flush();
            while (mRunning) {
                final byte[] buffer;
                final int length;
                final long timestamp;

                synchronized (mBufferLock) {
                    while (!mNewJpeg) {
                        try {
                            mBufferLock.wait();
                        }
                        catch (final InterruptedException stopMayHaveBeenCalled) {
                            return;
                        }
                    }

                    mStreamingBufferA = !mStreamingBufferA;

                    if (mStreamingBufferA) {
                        buffer = mBufferA;
                        length = mLengthA;
                        timestamp = mTimestampA;
                    } // if
                    else {
                        buffer = mBufferB;
                        length = mLengthB;
                        timestamp = mTimestampB;
                    } // else

                    mNewJpeg = false;
                } // synchronized

                stream.writeBytes(
                        "Content-type: image/jpeg\r\n"
                                + "Content-Length: " + length + "\r\n"
                                + "X-Timestamp:" + timestamp + "\r\n"
                                + "\r\n"
                );
                stream.write(buffer, 0 /* offset */, length);
                stream.writeBytes(BOUNDARY_LINES);
                stream.flush();

            } // while
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
    } // try

} // class MJpegHttpStreamer

