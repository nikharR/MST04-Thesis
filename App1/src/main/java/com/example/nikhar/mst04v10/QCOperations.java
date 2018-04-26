package com.example.nikhar.mst04v10;

import android.content.Context;
import android.media.audiofx.NoiseSuppressor;

import java.io.IOException;

/**
 * Created by Nikhar on 2017/11/06.
 */

public class QCOperations {

    //Class Variables//////////////////////////////////////////////////////////////////////////////
    private double mAcceptableRMSLevel = 600;
    private RecordActivity mRecordActivity;
    private short[] mAudioSampleArr;
    ///////////////////////////////////////////////////////////////////////////////////////////////

    //Constructor//////////////////////////////////////////////////////////////////////////////////
    public QCOperations(Context ctx) {
        this.mRecordActivity = (RecordActivity) ctx;
        try {
            mAudioSampleArr = mRecordActivity.byteArrToShortArr(mRecordActivity.getWaveByteArr());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////

    //QC performers///////////////////////////////////////////////////////////////////////////////
    boolean performClippingQC() {
        boolean flag = true;
        for (short aShort : mAudioSampleArr) {
            flag &= Math.abs(aShort) < 32767;
        }
        return flag;
    }

    boolean performVolumeLevelCheck() {

        boolean x =  NoiseSuppressor.isAvailable();
        int numOfSamplesInClip = mAudioSampleArr.length;
        int numOfSamplesInWindow = 800;
        int numOfSamplesInStep = 160;
        int numOfWindows = numOfSamplesInClip / numOfSamplesInStep - 4;

        double sumOfSquaresTotal = 0;
        int totalNoOfSamples = 0;
        for (int i = 0; i < numOfWindows; i++) {
            int sumOfSquaresWindow = 0;
            for (int j = i * (numOfSamplesInStep); j < i * (numOfSamplesInStep) + numOfSamplesInWindow - 1; j++) {
                sumOfSquaresWindow += mAudioSampleArr[j] * mAudioSampleArr[j];
            }
            if (checkWindowRMS(sumOfSquaresWindow)) {
                sumOfSquaresTotal += sumOfSquaresWindow;
                totalNoOfSamples += numOfSamplesInWindow;
            }
        }
        double rmsLevel = Math.sqrt(sumOfSquaresTotal / totalNoOfSamples);
        return rmsLevel > mAcceptableRMSLevel;

    }
    //////////////////////////////////////////////////////////////////////////////////////////////

    //Utilities///////////////////////////////////////////////////////////////////////////////////
    double calcRMSLevel(short[] buffer) {
        double rms = 0;

        for (int i = 0; i < buffer.length; i++) {
            rms += buffer[i] * buffer[i];
        }
        rms = Math.sqrt(rms / buffer.length);

        return rms;
    }

    private boolean checkWindowRMS(int sumOfSquaresWindow) {
        double windowRMS = Math.sqrt(sumOfSquaresWindow / 800);
        return windowRMS > mRecordActivity.getAmbientNoiseLevelRMS();
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
}
