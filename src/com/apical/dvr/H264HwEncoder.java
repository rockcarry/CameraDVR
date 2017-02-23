package com.apical.dvr;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import java.nio.ByteBuffer;

class H264HwEncoder {
    private static final String TAG = "H264HwEncoder";

    private MediaCodec   mCodec      = null;
    private ByteBuffer[] mInBuffers  = null;
    private ByteBuffer[] mOutBuffers = null;

    public void init(int w, int h, int frate, int bitrate) {
        mCodec = MediaCodec.createEncoderByType("video/avc");
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", w, h);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frate  );
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE  , bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mediaFormat.setInteger("store-metadata-in-buffers", 0);
        mediaFormat.setInteger("prepend-sps-pps-to-idr-frames", 1);
        mCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mCodec.start();

        mInBuffers  = mCodec.getInputBuffers();  
        mOutBuffers = mCodec.getOutputBuffers();
        Log.d(TAG, "mInBuffers  number: " + mInBuffers.length );
        Log.d(TAG, "mOutBuffers number: " + mOutBuffers.length);
    }

    public void free() {
        mCodec.stop();
        mCodec.release();
    }

    public boolean enqueueInputBuffer(byte[] input, long pts, int timeout) {
//      Log.d(TAG, "++enqueueInputBuffer pts=" + pts + ", timeout=" + timeout);
        int id = mCodec.dequeueInputBuffer(timeout);
        if (id < 0) return false;
        mInBuffers[id].clear();
        mInBuffers[id].put(input);
        mCodec.queueInputBuffer(id, 0, input.length, pts, 0);
//      Log.d(TAG, "--enqueueInputBuffer");
        return true;
    }

    public byte[] dequeueOutputBuffer(int timeout) {
//      Log.d(TAG, "++dequeueOutputBuffer timeout=" + timeout);
        byte[] out = null;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int id = mCodec.dequeueOutputBuffer(info, timeout);
        if (id >= 0) {
            out = new byte[info.size];
            mOutBuffers[id].get(out);
            mOutBuffers[id].clear();
            mCodec.releaseOutputBuffer(id, false);
        } else if (id == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            mOutBuffers = mCodec.getOutputBuffers();
        } else if (id == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            // Subsequent data will conform to new format.
            MediaFormat format = mCodec.getOutputFormat();
        }
//      Log.d(TAG, "--dequeueOutputBuffer");
        return out;
    }
}



