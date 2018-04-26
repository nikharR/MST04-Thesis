/*https://gist.github.com/kmark/d8b1b01fb0d2febf5770
 * Copyright 2016 Kevin Mark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * --
 * An example of how to read in raw PCM data from Android's AudioRecord API (microphone input, for
 * instance) and output it to a valid WAV file. Tested on API 21/23 on Android and API 23 on
 * Android Wear (modified activity) where AudioRecord is the only available audio recording API.
 * MediaRecorder doesn't work. Compiles against min API 15 and probably even earlier.
 *
 * Many thanks to Craig Stuart Sapp for his invaluable WAV specification:
 * http://soundfile.sapp.org/doc/WaveFormat/
 */

package com.example.nikhar.mst04v10;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.util.Arrays.copyOfRange;

public class RecordActivity extends AppCompatActivity {

    //Activity Variables//////////////////////////////////////////////////////////////////////////

    private double mAmbientNoiseLevelRMS;
    private static final int PERMISSION_RECORD_AUDIO = 0;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;

    private File mfileOfAudioDirectory;
    private File mfileOfJsonDirectory;
    private File mfileOfSplitPCMDirectory;

    private static RecordWaveTask mRecordTask = null;
    private static OpenMicTask mMicTask = null;
    private Button mTranslateButton;
    private Button mRecordButton;
    private Button mPlaybackButton;
    private Button mAlignButton;
    private Button mStopButton;
    private Button mSkipButton;
    private Button mMFCCButton;

    private Prompts mPrompts;
    private String [] mPromptArr;
    private int mPromptNo = 0;
    private int mTaskID = 0;
    private TextView mPromptTextView;
    protected BarLevelDrawable mBarLevel;
    protected TextView mdBLevel;
    private ProgressBar mQCProgressBar;

    boolean mIsLevelQCAcceptable;
    boolean mIsClippingQCAcceptable;
    //////////////////////////////////////////////////////////////////////////////////////////////

    //Main methods////////////////////////////////////////////////////////////////////////////////

    public RecordActivity() {
        mIsClippingQCAcceptable = true;
        mIsClippingQCAcceptable = true;
        mPrompts = new Prompts();
        mPromptArr = mPrompts.getPromptArr();
        mfileOfAudioDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+BuildConfig.APPLICATION_ID+"/wavFolder/");
        mfileOfJsonDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+BuildConfig.APPLICATION_ID+"/jsonFolder/");
        mfileOfSplitPCMDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+BuildConfig.APPLICATION_ID+"/tempPcmFolder");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        mfileOfAudioDirectory.mkdirs();

        mPromptTextView = findViewById(R.id.promptText);
        mBarLevel = findViewById(R.id.bar_level_drawable_view);
        mdBLevel = findViewById(R.id.dBLevel);
        mQCProgressBar= findViewById(R.id.qcProgessBar);
        setUI();
        mStopButton.setEnabled(false);
        loadPrompt();
        checkMicPermissionAndInitiate();




    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    //Activity Methods////////////////////////////////////////////////////////////////////////////

    private PromptAnnotationObject[] getJsonAnnotationObjectArray() {
        PromptAnnotationObject[] annotateArr = new PromptAnnotationObject[getNoOfWordsInPrompt()];
        File promptJsonFile = new File(mfileOfJsonDirectory,getJsonFileName());
        if(checkJsonFile(promptJsonFile)) {
            Gson gson = new Gson();
            annotateArr = gson.fromJson(readJsonFile(promptJsonFile), annotateArr.getClass());
            return annotateArr;
        }
        else {
            return null;
        }
    }

    private void loadPrompt()
    {
        if(mPromptNo<mPromptArr.length)
        {
            this.mPromptTextView.setText(mPromptArr[mPromptNo]);

        }
        else{
            mPromptNo = 0;
            this.mPromptTextView.setText(mPromptArr[mPromptNo]);
        }
    }
    private void setUI() {
        mTranslateButton = findViewById(R.id.translateButton);
        mRecordButton = findViewById(R.id.recordButton);
        mPlaybackButton = findViewById(R.id.playbackButton);
        mAlignButton = findViewById(R.id.alignButton);
        mStopButton =findViewById(R.id.stopButton);
        mSkipButton = findViewById(R.id.skipButton);
        mMFCCButton = findViewById(R.id.mfccButton);
        mMFCCButton.setVisibility(View.VISIBLE);
        mdBLevel.setText(null);
        //noinspection ConstantConditions
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStopOpenMicTask();
                checkRecordPermissionAndInitiate();
                mRecordButton.setEnabled(false);
                mStopButton.setEnabled(true);
            }
        });
        //noinspection ConstantConditions
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStopRecordTask();
                mRecordButton.setEnabled(true);
                mStopButton.setEnabled(false);
                //mdBLevel.setText((int)mAmbientNoiseLevelRMS+ "");
            }
        });

        mPlaybackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playCurrentFile(RecordActivity.this);
            }
        });

        mAlignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAligner();
            }
        });

        mSkipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPromptNo++;
                loadPrompt();
                checkStopOpenMicTask();
                checkMicPermissionAndInitiate();
            }
        });
        mMFCCButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (!initMFCCCalculations()){

                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    /*MFCCOperations m = new MFCCOperations(1);
                    m.testMFCC();*/
                    }
                }).start();


            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted

                } else {
                    // Permission denied
                    Toast.makeText(this, "\uD83D\uDE41", Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted

                } else {
                    // Permission denied
                    Toast.makeText(this, "\uD83D\uDE41", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        Object task = null;
        switch(mTaskID){
            case 0:
                mMicTask.setContext(null);
                task = mMicTask;
                break;
            case 1:
                mRecordTask.setContext(null);
                task = mRecordTask;
                break;
        }
        return task;
    }

    private void displayQCAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(RecordActivity.this);
        builder.setTitle("Quality Control Alert");
        builder.setMessage("The recorded audio is either clipped or too soft! Please record again.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

   //File handling methods ////////////////////////////////////////////////////////////////////////

    private void setMFCCsToJSON(String[] mfccArr) {
        PromptAnnotationObject[] annotateArray = getJsonAnnotationObjectArray();
        for (int i = 0; i < annotateArray.length; i++) {
            annotateArray[i].setMfcc(mfccArr[i]);
        }
        Gson gson = new Gson();
        String s = gson.toJson(annotateArray);
        writeJsonFile(s);
    }

    public void writeJsonFile(String s)
    {
        mfileOfJsonDirectory.mkdirs();
        FileWriter file = null;
        try {
            file = new FileWriter(mfileOfJsonDirectory+"/"+mPromptNo+"clip.json");
            file.write(s);
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Toast.makeText(this,mfileOfJsonDirectory+"/"+mPromptNo+"clip.json", Toast.LENGTH_LONG).show();

    }
    public String readJsonFile(File file)
    {

//Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
//                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            e.printStackTrace();
        }
        return  text.toString();
    }
    private boolean checkJsonFile(File file) {

        return file.exists();
    }

    public String getWaveFileName() {
        return (mPromptNo + "clip.wav");
    }

    public String getJsonFileName(){return (mPromptNo + "clip.json");}

    private void playCurrentFile(Context context) throws IllegalStateException {
        Uri uri = Uri.fromFile(new File(mfileOfAudioDirectory, this.getWaveFileName()));
        final MediaPlayer mP = MediaPlayer.create(context,uri);
        mP.start();
        mP.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mP.release();
            }
        });

    }

    private void writePCMFileForWord(int startSample, int endSample, int i, byte[] wavBytes) throws IOException {
        File f = new File(mfileOfSplitPCMDirectory,i+".pcm");
        mfileOfSplitPCMDirectory.mkdirs();
        FileOutputStream fos = new FileOutputStream(f);
        for(int k = startSample; k < endSample ;k++){
            fos.write(wavBytes[k]);
        }
        fos.flush();
        fos.close();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    //Permission checks and initiators////////////////////////////////////////////////////////////

    private void checkMicPermissionAndInitiate() {
        mTaskID = 0;
        if (ContextCompat.checkSelfPermission(RecordActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(RecordActivity.this,
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    PERMISSION_RECORD_AUDIO);
            return;
        }
        createMicTask();
        launchMicTask();
    }

    private void checkRecordPermissionAndInitiate()
    {
        mTaskID = 1;
        if (ContextCompat.checkSelfPermission(RecordActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(RecordActivity.this,
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    PERMISSION_RECORD_AUDIO);
            return;
        }
        if (ContextCompat.checkSelfPermission(RecordActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(RecordActivity.this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    PERMISSION_WRITE_EXTERNAL_STORAGE);
            return;
        }
        // Permission already available
        createRecordTask();
        launchRecordTask();
    }
    private void startAligner() {


        String filename = mfileOfAudioDirectory.toString()+"/"+ getWaveFileName();

        Intent alignIntent = new Intent(RecordActivity.this,AlignWaveformActivity.class);
        alignIntent.putExtra("filename",filename);
        alignIntent.putExtra("json_folder",mfileOfJsonDirectory.toString());
        alignIntent.putExtra("prompt_text",mPromptTextView.getText().toString());
        alignIntent.putExtra("prompt_no",""+mPromptNo);
        startActivity(alignIntent);
    }

    private boolean initMFCCCalculations() throws IOException {
        //Record seg start time
        checkStopOpenMicTask();
        long startSegTime = SystemClock.elapsedRealtimeNanos();
        splitWavIntoSegments();
        //Record seg end time
        long endSegTime = SystemClock.elapsedRealtimeNanos();
        //Output to Logcat
        Log.v("Time for Segmentation","" + (endSegTime - startSegTime) / 1000000.00);
        MFCCOperations mfccoperations = new MFCCOperations(getNoOfWordsInPrompt());
        //Record mfcc start time
        long startMFCCTime = SystemClock.elapsedRealtimeNanos();
        String [] mfccArr = mfccoperations.getMFCCStringArray();
        //Record mfcc end time
        long endMFCCTime = SystemClock.elapsedRealtimeNanos();
        //Output to Logcat
        Log.v("Time for MFCC","" + (endMFCCTime - startMFCCTime) / 1000000.00);
        setMFCCsToJSON(mfccArr);
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    //Methods Dealing With Task Handling//////////////////////////////////////////////////////////

    private void checkStopOpenMicTask() {
        if (!mMicTask.isCancelled() && mMicTask.getStatus() == AsyncTask.Status.RUNNING) {
            clearAsyncTask(mMicTask);
        }
    }

    private void launchMicTask() {
        switch (mMicTask.getStatus()) {
            case FINISHED:
                mMicTask = new OpenMicTask(this);
                break;
            case PENDING:
                if (mMicTask.isCancelled()) {
                    mMicTask = new OpenMicTask(this);
                }
        }

        mMicTask.execute();
    }

    private void createMicTask() {
        mMicTask = (OpenMicTask) getLastCustomNonConfigurationInstance();
        if (mMicTask == null) {
            mMicTask = new OpenMicTask(this);
        } else {
            mMicTask.setContext(this);
        }
    }

    private void createRecordTask() {
        // Restore the previous task or create a new one if necessary
        mRecordTask = (RecordWaveTask) getLastCustomNonConfigurationInstance();
        if (mRecordTask == null) {
            mRecordTask = new RecordWaveTask(this);
        } else {
            mRecordTask.setContext(this);
        }
    }

    private void checkStopRecordTask(){
        if (mRecordTask!=null && !mRecordTask.isCancelled() && mRecordTask.getStatus() == AsyncTask.Status.RUNNING) {
            clearAsyncTask(mRecordTask);
            //Record Start Time
            long startTime = SystemClock.elapsedRealtimeNanos();
            startQC();
            //Record End Time
            long endTime = SystemClock.elapsedRealtimeNanos();
            //Output to Logcat
            Log.v("Time for QC","" + (endTime - startTime) / 1000000.00);
        } else {
            Toast.makeText(RecordActivity.this, "Task not running.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startQC() {
        final QCOperations mQCOperations = new QCOperations(this);
        mIsLevelQCAcceptable = mQCOperations.performVolumeLevelCheck();
        mIsClippingQCAcceptable = mQCOperations.performClippingQC();
        if(!(mIsClippingQCAcceptable&&mIsLevelQCAcceptable)){
            displayQCAlert();
        }
    }

    private void launchRecordTask() {
        switch (mRecordTask.getStatus()) {
            case RUNNING:
                Toast.makeText(this, "Task already running...", Toast.LENGTH_SHORT).show();
                return;
            case FINISHED:
                mRecordTask = new RecordWaveTask(this);
                break;
            case PENDING:
                mRecordTask = new RecordWaveTask(this);

        }
        mfileOfAudioDirectory.mkdirs();
        File wavFile = new File(mfileOfAudioDirectory, this.getWaveFileName());
        Toast.makeText(this, wavFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        mRecordTask.execute(wavFile);
    }

    public static void clearAsyncTask(AsyncTask<?, ?, ?> asyncTask) {
        if (asyncTask != null) {
            if (!asyncTask.isCancelled()) {
                asyncTask.cancel(true);
            }
            asyncTask = null;
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////

    //Utility Methods////////////////////////////////////////////////////////////////////////////

    private int getNoOfWordsInPrompt(){
        return mPromptArr[mPromptNo].split("\\s+").length;
    }

    private void splitWavIntoSegments() throws IOException {
        byte[] wavDataArray = getWaveByteArr();
        int numOfWavBytes = wavDataArray.length;
        PromptAnnotationObject[] annotateArray = getJsonAnnotationObjectArray();
        double wavLengthInSec = numOfWavBytes / 16000.0 / 2.0;
        for (int i = 0; i < annotateArray.length; i++) {
            //Record Start Time
            long st = SystemClock.elapsedRealtimeNanos();
            //Record End Time
            double startTime = annotateArray[i].getStart();
            double endTime = annotateArray[i].getEnd();
            int startSample = (int) (startTime / wavLengthInSec * numOfWavBytes);
            int endSample = (int) (endTime / wavLengthInSec * numOfWavBytes);
            startSample = pinToFactorOf8(startSample);
            endSample = pinToFactorOf8(endSample);
            long en = SystemClock.elapsedRealtimeNanos();
            writePCMFileForWord(startSample, endSample, i, wavDataArray);

            //Output to Logcat
            Log.v("Time for Split of word:","" + (en - st) / 1000000.00);
        }
    }

    private int pinToFactorOf8(int sampleNumber) {
        if(sampleNumber%8 != 0){
            sampleNumber = sampleNumber-sampleNumber%8;
        }
        return sampleNumber;
    }

    public byte[] getWaveByteArr ()throws IOException{
        FileInputStream fis = new FileInputStream(new File(mfileOfAudioDirectory, getWaveFileName()));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte [] buffer  = new byte[1024];
        try {
            for (int readNum; (readNum = fis.read(buffer)) != -1;) {
                bos.write(buffer, 0, readNum); //no doubt here is 0
                //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
                System.out.println("read " + readNum + " bytes,");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        byte [] waveBytes = bos.toByteArray();

        byte [] waveByteData = copyOfRange(waveBytes,44,waveBytes.length);

        return waveByteData;
    }

    protected double scaleLogarithmic(short[] shorts){

        double numerator = Math.log(Math.abs(shorts[shorts.length -1]));
        double denominator = Math.log(Math.pow(2,16)/2-1);
        return (numerator/denominator);
    }

    public short[] byteArrToShortArr(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }
    /////////////////////////////////////////////////////////////////////////////////////////////

    //Getter and Setters////////////////////////////////////////////////////////////////////////

    public double getAmbientNoiseLevelRMS() {
        return mAmbientNoiseLevelRMS;
    }

    public void setAmbientNoiseLevelRMS(double noiseLevelRMS) {
        mAmbientNoiseLevelRMS = noiseLevelRMS;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////

}