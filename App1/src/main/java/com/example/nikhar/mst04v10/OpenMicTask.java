package com.example.nikhar.mst04v10;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.Arrays;

/**
 * Created by Nikhar on 2017/10/30.
 */

public class OpenMicTask extends AsyncTask<File, Void, Object[]> {

    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    private static final int SAMPLE_RATE = 16000; // Hz
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    private final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);

    private short[][] mRotatingBuffer;
    @SuppressLint("StaticFieldLeak")
    private Context mContext;
    @SuppressLint("StaticFieldLeak")
    private RecordActivity mRecordActivity;
    private QCOperations mQCOperations;

    OpenMicTask(Context ctx) {
        setContext(ctx);
        this.mRecordActivity = (RecordActivity) ctx;
        this.mQCOperations = new QCOperations(ctx);
    }

    public void setContext(Context ctx) {
        this.mContext = ctx;
    }

    @Override
    protected Object[] doInBackground(File... files) {
        Log.v("MICTASK", "ENTERED DOINBACKGROUNG");
        AudioRecord audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);

        byte[] buffer = new byte[BUFFER_SIZE];
        mRotatingBuffer = new short[3][buffer.length / 2];
        int rotatingBufferMarker = 0;

        audioRecord.startRecording();
        Log.v(" MICTASK", "AUDIO IS RECORDING");
        while (!this.isCancelled()) {
            audioRecord.read(buffer, 0, buffer.length);
            final short[] shorts = mRecordActivity.byteArrToShortArr(buffer);
            Log.v("MICTASK", "WHILE LOOP ENTERED");
            if (rotatingBufferMarker < 3) {
                mRotatingBuffer[rotatingBufferMarker] = shorts;
                rotatingBufferMarker++;
            } else {
                rotatingBufferMarker = 0;
                mRotatingBuffer[rotatingBufferMarker] = shorts;
            }
            Arrays.sort(shorts);
            mRecordActivity.runOnUiThread(new Runnable() {
                public void run() {
                    mRecordActivity.mBarLevel.setLevel(mRecordActivity.scaleLogarithmic(shorts));

                }
            });
        }
        Log.v("MICTASK", "WHILE LOOP EXITED");


        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                    Log.v("MICTASK", "STOPPED RECORDING");
                }
            } catch (IllegalStateException ex) {
                ex.printStackTrace();
            }
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.release();
                Log.v("MICTASK", "AUDIO IS RELEASED");
            }
        }


        Log.v("MICTASK", "OBJECT RETURNED");
        return new Object[0];
    }

    @Override
    protected void onPostExecute(Object[] results) {
        Log.v("MICTASK", "ONPOSTEXECUTE STARTED");
        if (mContext != null) {
            if (mRotatingBuffer != null) {
               performNoiseCalculation();
            }

        }
        Log.v("MICTASK", "ONPOSTEXECUTE FINISHED");
    }

    @Override
    protected void onCancelled(Object[] objects) {
        Log.v("MICTASK", "ONCANCELLED STARTED");
        onPostExecute(objects);
    }

    private void performNoiseCalculation(){
        short[] serialBuffer = new short[mRotatingBuffer[0].length * 3];
        System.arraycopy(mRotatingBuffer[0], 0, serialBuffer, 0, mRotatingBuffer[0].length);
        System.arraycopy(mRotatingBuffer[1], 0, serialBuffer, mRotatingBuffer[0].length, mRotatingBuffer[1].length);
        System.arraycopy(mRotatingBuffer[2], 0, serialBuffer, mRotatingBuffer[1].length * 2, mRotatingBuffer[2].length);
        double noiseLevelRMS = mQCOperations.calcRMSLevel(serialBuffer);
        mRecordActivity.setAmbientNoiseLevelRMS(noiseLevelRMS);
    }
}
