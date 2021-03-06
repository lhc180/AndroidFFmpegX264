package com.livecamera.stream.packetizer;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class H264Packetizer extends AbstractPacketizer implements Runnable{
    public final static String TAG = "H264Packetizer";
    
    private Thread mThread = null;
    private int mNALULen = 0;
    private long mDelay = 0, mOldTime = 0;
    private byte[] mSpsPpsInfo = null;
    private byte[] mHeader = new byte[5];
    private int mStreamType = 1;
    private int mCount = 0;
    
    private RandomAccessFile mRaf = null;

    public H264Packetizer() {
        super();
    }

    @Override
    public void start() {
        //save file first for testing
        try {
            File file = new File("/sdcard/camera1.h264");
            mRaf = new RandomAccessFile(file, "rw");
        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }
        
        if (mClient != null) {
            mClient.start();
        }
        
        if (mThread == null) {
            mThread = new Thread(this);
            mThread.start();
        }
    }

    @Override
    public void stop() {
        Log.i(TAG, "H264Packetizer stop");
        if (mThread != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            mThread.interrupt();
            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mThread = null;
        }
    }

    @Override
    public void run() {
        mCount = 0;
        
        if (mInputStream instanceof MediaCodecInputStream) {
            mStreamType = 1;
        } else {
            mStreamType = 0;
        }
        
        try {
            while (!Thread.interrupted()) {
                send();
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    
    @SuppressLint("NewApi")
    private void send() throws IOException {
        if (mStreamType == 0) {
            read(mHeader, 0, 5);
            mNALULen = mHeader[3]&0xFF | (mHeader[2]&0xFF)<<8 |
                    (mHeader[1]&0xFF)<<16 | (mHeader[0]&0xFF)<<24;
        } else if(mStreamType == 1) {
            read(mHeader, 0, 5);
            mTimeStamp = ((MediaCodecInputStream)mInputStream)
                    .getLastBufferinfo().presentationTimeUs*1000L;
            mNALULen = mInputStream.available() + 1;
            if(!(mHeader[0] == 0 && mHeader[1] == 0 && mHeader[2] == 0)) {
                Log.e(TAG, "NAL units are not 0x00000001");
                mStreamType = 2;
                return;
            }
        } else {
            read(mHeader, 0, 1);
            mHeader[4] = mHeader[0];
            mTimeStamp = ((MediaCodecInputStream)mInputStream)
                    .getLastBufferinfo().presentationTimeUs*1000L;
            mNALULen = mInputStream.available() + 1;
        }
        
        int pos = 0;
        byte[] outData = new byte[mNALULen+5];
        System.arraycopy(mHeader, 0, outData, 0, mHeader.length);
        read(outData, 6, mNALULen-1);
                
        if (mSpsPpsInfo != null) {
            if (mOutput != null) {
                System.arraycopy(outData, 0, mOutput, 0, outData.length);
                pos += outData.length;
            }
        } else {
            ByteBuffer ppsSpsBuffer = ByteBuffer.wrap(outData);
            if (ppsSpsBuffer.getInt() == 0x00000001) {
                Log.i(TAG, "Find pps sps info: " + Arrays.toString(outData));
                mSpsPpsInfo = new byte[outData.length];
                System.arraycopy(outData, 0, mSpsPpsInfo, 0, outData.length);
            } else {
                Log.e(TAG, "Do not find pps sps info: " + Arrays.toString(outData));
                return;
            }
        }
        
        //keyframe, add pps sps info, 00 00 00 01 65
        if (mOutput[4] == 0x65) {
            //add pps sps
            Log.i(TAG, "Find a key frame, add sps pps info, frame length = " +
            pos + ", spspps info length = " + mSpsPpsInfo.length);
            System.arraycopy(mOutput, 0, mEncodedBuf, 0, pos);
            System.arraycopy(mSpsPpsInfo, 0, mOutput, 0, mSpsPpsInfo.length);
            System.arraycopy(mEncodedBuf, 0, mOutput, mSpsPpsInfo.length, pos);
            pos += mSpsPpsInfo.length;
        }
        
        if (pos > 0) {
            //send output
            //super.send(mOutput, (int)mTimeStamp);
            mRaf.write(mOutput, 0, pos);
        }
    }
    
    private int read(byte[] buffer, int offset, int length) throws IOException {
        int ret = 0, len = 0;
        while (ret < length) {
            len = mInputStream.read(buffer, offset+ret, length-ret);
            if (len < 0) {
                throw new IOException("end of stream");
            } else {
                ret += len;
            }
        }
        return ret;
    }
}
