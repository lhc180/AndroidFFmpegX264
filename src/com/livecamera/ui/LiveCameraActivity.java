package com.livecamera.ui;


import java.io.IOException;

import com.example.androidffmpegx264.R;
import com.livecamera.stream.video.VideoParam;
import com.livecamera.stream.video.VideoStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class LiveCameraActivity extends Activity implements OnClickListener {
    static final public String TAG = "LiveCameraActivity";
    
    private SurfaceView mSurfaceView;
    private ImageButton mStartStopView;
    
    private VideoStream mVideoStream;
    private VideoParam mVideoParam = new VideoParam(352, 288);
    
    private boolean mStarted = false;
    private long mClickTime = 0;

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);
        
        mSurfaceView = (SurfaceView)findViewById(R.id.camera_view);
        mStartStopView = (ImageButton)findViewById(R.id.live_start_stop);
        mStartStopView.setOnClickListener(this);
        
        //use the back camera
        mVideoStream = new VideoStream(CameraInfo.CAMERA_FACING_BACK);
        mVideoStream.setSurfaceView(mSurfaceView);
        mVideoStream.setPreviewOrientation(90);
        
        //TODO, load the video param from settings
        mVideoStream.setVideoParam(mVideoParam);
        
        //start preview
        //mVideoStream.startPreview();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        if (v == mStartStopView) {
            Log.e(TAG, "click start or stop");
            long currentTime = System.currentTimeMillis();
            if (currentTime - mClickTime < 700) return;
            mClickTime = currentTime;
            
            if (mStarted) {
                mVideoStream.stop();
                mStartStopView.setImageResource(R.drawable.ic_control_play);
                mStarted = false;
            } else {
                try {
                    mVideoStream.start();
                    mStartStopView.setImageResource(R.drawable.ic_control_stop);
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mStarted = true;
            }
        }
    }

}