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

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

import cz.davidkuna.remotecontrolserver.helpers.Settings;

public final class CameraStreamer extends Object
{
    private static final String TAG = CameraStreamer.class.getSimpleName();

    private static final int MESSAGE_TRY_START_STREAMING = 0;
    private static final int MESSAGE_SEND_PREVIEW_FRAME = 1;

    private static final int MAX_FPS = 30;

    private static final long OPEN_CAMERA_POLL_INTERVAL_MS = 1000L;

    private final Object mLock = new Object();
    private final MovingAverage mAverageSpf = new MovingAverage(50 /* numValues */);

    private final int mCameraIndex;
    private final boolean mUseFlashLight;
    private final Settings settings;
    private final int mPreviewSizeIndex;
    private final int mJpegQuality;
    private final SurfaceHolder mPreviewDisplay;

    private boolean mRunning = false;
    private Looper mLooper = null;
    private Handler mWorkHandler = null;
    private Camera mCamera = null;
    private int mPreviewFormat = Integer.MIN_VALUE;
    private int mPreviewWidth = Integer.MIN_VALUE;
    private int mPreviewHeight = Integer.MIN_VALUE;
    private Rect mPreviewRect = null;
    private int mPreviewBufferSize = Integer.MIN_VALUE;
    private MemoryOutputStream mJpegOutputStream = null;
    private MJpegHttpStreamer mMJpegHttpStreamer = null;
    private MJpegUDPStreamer mMJpegUDPStreamer = null;

    private long mNumFrames = 0L;
    private long mLastTimestamp = Long.MIN_VALUE;
    private long lastSentTimestamp = Long.MIN_VALUE;

    public CameraStreamer(final int cameraIndex, final boolean useFlashLight, Settings settings,
                                 final int previewSizeIndex, final int jpegQuality, final SurfaceHolder previewDisplay)
    {
        super();

        if (previewDisplay == null)
        {
            throw new IllegalArgumentException("previewDisplay must not be null");
        }

        mCameraIndex = cameraIndex;
        mUseFlashLight = useFlashLight;
        this.settings = settings;
        mPreviewSizeIndex = previewSizeIndex;
        mJpegQuality = jpegQuality;
        mPreviewDisplay = previewDisplay;
        Log.d(TAG, "Selected video quality:" + jpegQuality + "| sizeIndex:" + previewSizeIndex);

    }

    private final class WorkHandler extends Handler
    {
        private WorkHandler(final Looper looper)
        {
            super(looper);
        } // constructor(Looper)

        @Override
        public void handleMessage(final Message message)
        {
            switch (message.what)
            {
                case MESSAGE_TRY_START_STREAMING:
                    tryStartStreaming();
                    break;
                case MESSAGE_SEND_PREVIEW_FRAME:
                    final Object[] args = (Object[]) message.obj;
                    sendPreviewFrame((byte[]) args[0], (Camera) args[1], (Long) args[2]);
                    break;
                default:
                    throw new IllegalArgumentException("cannot handle message");
            }
        }
    }

    public void start()
    {
        synchronized (mLock)
        {
            if (mRunning)
            {
                throw new IllegalStateException("CameraStreamer is already running");
            }
            mRunning = true;
        }

        final HandlerThread worker = new HandlerThread(TAG, Process.THREAD_PRIORITY_MORE_FAVORABLE);
        worker.setDaemon(true);
        worker.start();
        mLooper = worker.getLooper();
        mWorkHandler = new WorkHandler(mLooper);
        mWorkHandler.obtainMessage(MESSAGE_TRY_START_STREAMING).sendToTarget();
    }

    /**
     *  Stop the image streamer. The camera will be released during the
     *  execution of stop() or shortly after it returns. stop() should
     *  be called on the main thread.
     */
    public void stop()
    {
        synchronized (mLock)
        {
            if (!mRunning)
            {
                throw new IllegalStateException("CameraStreamer is already stopped");
            }

            mRunning = false;
            if (mMJpegUDPStreamer != null)
            {
                mMJpegUDPStreamer.stop();
            }
            if (mCamera != null)
            {
                mCamera.release();
                mCamera = null;
            }
        }
        mLooper.quit();
    }

    private void tryStartStreaming()
    {
        try
        {
            while (mRunning)
            {
                try
                {
                    startStreamingIfRunning();
                }
                catch (final RuntimeException openCameraFailed)
                {
                    Log.d(TAG, "Open camera failed, retying in " + OPEN_CAMERA_POLL_INTERVAL_MS
                            + "ms", openCameraFailed);
                    Thread.sleep(OPEN_CAMERA_POLL_INTERVAL_MS);
                    continue;
                }
               break;
            }
        }
        catch (final Exception startPreviewFailed)
        {
            Log.w(TAG, "Failed to open camera preview", startPreviewFailed);
        }
    }

    private void startStreamingIfRunning() throws IOException
    {

        final Camera camera = Camera.open(mCameraIndex);
        final Camera.Parameters params = camera.getParameters();

        final List<Camera.Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
        Log.d(TAG, "Set preview size: " + mPreviewSizeIndex);
        final Camera.Size selectedPreviewSize = supportedPreviewSizes.get(mPreviewSizeIndex);
        params.setPreviewSize(selectedPreviewSize.width, selectedPreviewSize.height);

        if (mUseFlashLight)
        {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }

        final List<int[]> supportedPreviewFpsRanges = params.getSupportedPreviewFpsRange();

        if (supportedPreviewFpsRanges != null)
        {
            final int[] range = supportedPreviewFpsRanges.get(0);
            params.setPreviewFpsRange(range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                    range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
            camera.setParameters(params);
        }

        // Set up preview callback
        mPreviewFormat = params.getPreviewFormat();
        final Camera.Size previewSize = params.getPreviewSize();
        mPreviewWidth = previewSize.width;
        mPreviewHeight = previewSize.height;
        final int BITS_PER_BYTE = 8;
        final int bytesPerPixel = ImageFormat.getBitsPerPixel(mPreviewFormat) / BITS_PER_BYTE;

        mPreviewBufferSize = mPreviewWidth * mPreviewHeight * bytesPerPixel * 3 / 2 + 1;
        camera.addCallbackBuffer(new byte[mPreviewBufferSize]);
        mPreviewRect = new Rect(0, 0, mPreviewWidth, mPreviewHeight);
        camera.setPreviewCallbackWithBuffer(mPreviewCallback);

        mJpegOutputStream = new MemoryOutputStream(mPreviewBufferSize);
        Log.d(TAG, "PreviewBufferSize = " + mPreviewBufferSize);
        final MJpegUDPStreamer streamer = new MJpegUDPStreamer(settings, mPreviewBufferSize);
        streamer.start();

        synchronized (mLock)
        {
            if (!mRunning)
            {
                streamer.stop();
                camera.stopPreview();
                camera.release();
                return;
            }

            try
            {
                camera.setPreviewDisplay(mPreviewDisplay);
            }
            catch (final IOException e)
            {
                streamer.stop();
                camera.stopPreview();
                camera.release();
                throw e;
            }

            mMJpegUDPStreamer = streamer;
            camera.startPreview();
            mCamera = camera;
        }
    }

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback()
    {
        @Override
        public void onPreviewFrame(final byte[] data, final Camera camera)
        {
            final Long timestamp = SystemClock.elapsedRealtime();
            final Message message = mWorkHandler.obtainMessage();
            message.what = MESSAGE_SEND_PREVIEW_FRAME;
            message.obj = new Object[]{ data, camera, timestamp };
            message.sendToTarget();
        } // onPreviewFrame(byte[], Camera)
    };

   private void sendPreviewFrame(final byte[] data, final Camera camera, final long timestamp)
   {
        // Calcalute the timestamp
        final long MILLI_PER_SECOND = 1000L;
        final long timestampSeconds = timestamp / MILLI_PER_SECOND;

       if ((lastSentTimestamp + (MILLI_PER_SECOND / MAX_FPS)) < timestamp) {
           lastSentTimestamp = timestamp;
           // Update and log the frame rate
           final long LOGS_PER_FRAME = 10L;
           mNumFrames++;
           if (mLastTimestamp != Long.MIN_VALUE) {
               mAverageSpf.update(timestampSeconds - mLastTimestamp);
               if (mNumFrames % LOGS_PER_FRAME == LOGS_PER_FRAME - 1) {
                   Log.d(TAG, "FPS: " + 1.0 / mAverageSpf.getAverage());
               }
           }

           mLastTimestamp = timestampSeconds;

           // Create JPEG
           final YuvImage image = new YuvImage(data, mPreviewFormat, mPreviewWidth, mPreviewHeight,
                   null /* strides */);
           image.compressToJpeg(mPreviewRect, mJpegQuality, mJpegOutputStream);

        /*mMJpegHttpStreamer.streamJpeg(mJpegOutputStream.getBuffer(), mJpegOutputStream.getLength(),
                timestamp);*/

           mMJpegUDPStreamer.streamJpeg(mJpegOutputStream.getBuffer(), mJpegOutputStream.getLength(), timestamp);

           // Clean up
           mJpegOutputStream.seek(0);
       }

        camera.addCallbackBuffer(data);
   }


}

