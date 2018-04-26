package com.example.nikhar.mst04v10;

import android.os.Environment;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by Nikhar on 2017/10/18.
 */

public class MFCCOperations {

    //Temporary file initialise////////////////////////////////////////////////////////////////////
    private File fileOfSplitMFCCDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+BuildConfig.APPLICATION_ID+"/tempMFCCFolder");
    private File fileOfSplitPCMDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+BuildConfig.APPLICATION_ID+"/tempPcmFolder");
    private int fileCount;
    ///////////////////////////////////////////////////////////////////////////////////////////////

    //Constructor//////////////////////////////////////////////////////////////////////////////////
    MFCCOperations(int fileCount) {
        this.fileCount = fileCount;
        fileOfSplitMFCCDirectory.mkdirs();
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////

    //Utilities////////////////////////////////////////////////////////////////////////////////////
    String[] getMFCCStringArray(){
        String [] mfccArray = new String[fileCount];
        for (int i = 0; i < fileCount; i++) {
            File inputData = new File(fileOfSplitPCMDirectory,i+".pcm");
            File outputData = new File(fileOfSplitMFCCDirectory,i+".txt");
            doMFCC(inputData.toString(),outputData.toString());
            mfccArray[i]=getMFCCText(outputData);
            inputData.delete();
            outputData.delete();
        }
        return mfccArray;
    }
   /* public void testMFCC()
    {
        File fileOfTestMFCCDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+BuildConfig.APPLICATION_ID+"/testMFCC");
        File fileOfAudioDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+BuildConfig.APPLICATION_ID+"/wavFolder/");
        fileOfTestMFCCDirectory.mkdirs();
        File inputData = new File(fileOfAudioDirectory,"0clip.wav");
        File outputData = new File(fileOfTestMFCCDirectory,"testMFCC.txt");
        doMFCC(inputData.toString(),outputData.toString());
    }*/
    private String getMFCCText(File f)
    {
        String str = null;
        try {
            str = FileUtils.readFileToString(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////

    //Native caller////////////////////////////////////////////////////////////////////////////////
    public native static void doMFCC(String wavBytes, String mfccPath);
    static {
        System.loadLibrary("native-lib");
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
}
