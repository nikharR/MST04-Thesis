package com.example.nikhar.mst04v10;

/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nikhar.mst04v10.Ringdroid.MarkerView;
import com.example.nikhar.mst04v10.Ringdroid.SamplePlayer;
import com.example.nikhar.mst04v10.Ringdroid.WaveformView;
import com.example.nikhar.mst04v10.Ringdroid.soundfile.SoundFile;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.PatternSyntaxException;


/**
 * The activity for the Ringdroid-based main editor window.  Keeps track of
 * the waveform display, current horizontal offset, marker handles,
 * start / end text boxes, and handles all of the buttons and controls.
 */
public class AlignWaveformActivity extends AppCompatActivity
        implements MarkerView.MarkerListener,
        WaveformView.WaveformListener {
    private long mLoadingLastUpdateTime;
    private boolean mLoadingKeepGoing;
    private boolean mFinishActivity;
    private AlertDialog mAlertDialog;
    private ProgressBar mProgressBar;
    private SoundFile mSoundFile;
    private File mFile;
    private String mFilename;
    private WaveformView mWaveformView;
    private MarkerView mStartMarker;
    private MarkerView mEndMarker;
    private TextView mStartText;
    private TextView mEndText;
    private Button mStartMinusButton;
    private Button mStartPlusButton;
    private Button mEndMinusButton;
    private Button mEndPlusButton;
    private Button mZoomIn;
    private Button mZoomOut;
    private ImageButton mPlayButton;
    private ImageButton mRewindButton;
    private ImageButton mFfwdButton;
    private boolean mKeyDown;
    private int mWidth;
    private int mMaxPos;
    private int mStartPos;
    private int mEndPos;
    private boolean mStartVisible;
    private boolean mEndVisible;
    private int mLastDisplayedStartPos;
    private int mLastDisplayedEndPos;
    private int mOffset;
    private int mOffsetGoal;
    private int mFlingVelocity;
    private int mPlayStartMsec;
    private int mPlayEndMsec;
    private Handler mHandler;
    private boolean mIsPlaying;
    private SamplePlayer mPlayer;
    private boolean mTouchDragging;
    private float mTouchStart;
    private int mTouchInitialOffset;
    private int mTouchInitialStartPos;
    private int mTouchInitialEndPos;
    private long mWaveformTouchStartMsec;
    private float mDensity;
    private int mMarkerLeftInset;
    private int mMarkerRightInset;
    private int mMarkerTopOffset;
    private int mMarkerBottomOffset;
    private ActionBar mActionBar;
    private String mPromptText;
    private String[] mPromptTextArr;
    private int mPromptTextArrLength;
    private int mPromptElementTracker = 0;
    private TextView mFirstText;
    private TextView mMainText;
    private TextView mLastText;
    private PromptAnnotationObject[] mPromptAnnotationObjectArr;
    private Thread mLoadSoundFileThread;
    private Button msaveButton;
    private Button mLoadButton;
    private String mJsonFolder;
    private int mPromptNo;

    // Result codes
    private static final int REQUEST_CODE_CHOOSE_CONTACT = 1;

    /**
     * This is a special intent action that means "edit a sound file".
     */
    public static final String EDIT = "com.ringdroid.action.EDIT";

    //
    // Public methods and protected overrides
    //

    //Main methods/////////////////////////////////////////////////////////////////////////////////

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {

        super.onCreate(icicle);
        setContentView(R.layout.activity_align_waveform);


        mPlayer = null;
        mIsPlaying = false;

        mAlertDialog = null;
        mProgressBar = null;

        mLoadSoundFileThread = null;


        Intent intent = getIntent();

        // If the Ringdroid media select activity was launched via a
        // GET_CONTENT intent, then we shouldn't display a "saved"
        // message when the user saves, we should just return whatever
        // they create.

        mFilename = intent.getStringExtra("filename");
        mPromptText = intent.getStringExtra("prompt_text");
        mJsonFolder = intent.getStringExtra("json_folder");
        mPromptNo = Integer.parseInt(intent.getStringExtra("prompt_no"));

        try {
            mPromptTextArr = mPromptText.split("\\s+");
        } catch (PatternSyntaxException ex) {
            //
        }

        mPromptTextArrLength = mPromptTextArr.length;
        mPromptAnnotationObjectArr = new PromptAnnotationObject[mPromptTextArrLength];

        mSoundFile = null;
        mKeyDown = false;

        mHandler = new Handler();

        loadGui();

        mHandler.postDelayed(mTimerRunnable, 100);
        loadFromFile();


    }

    private void closeThread(Thread thread) {
        if (thread != null && thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Called when the activity is finally destroyed.
     */
    @Override
    protected void onDestroy() {


        mLoadingKeepGoing = false;
        closeThread(mLoadSoundFileThread);

        mLoadSoundFileThread = null;

        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
            mProgressBar = null;
        }
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }

        if (mPlayer != null) {
            if (mPlayer.isPlaying() || mPlayer.isPaused()) {
                mPlayer.stop();
            }
            mPlayer.release();
            mPlayer = null;
        }

        super.onDestroy();
    }

    /**
     * Called with an Activity we started with an Intent returns.
     */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent dataIntent) {
        Log.v("Annotate", "EditActivity onActivityResult");
        if (requestCode == REQUEST_CODE_CHOOSE_CONTACT) {
            // The user finished saving their ringtone and they're
            // just applying it to a contact.  When they return here,
            // they're done.
            finish();
            return;
        }
    }

    /**
     * Called when the orientation changes and/or the keyboard is shown
     * or hidden.  We don't need to recreate the whole activity in this
     * case, but we do need to redo our layout somewhat.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v("Ringdroid", "EditActivity onConfigurationChanged");
        final int saveZoomLevel = mWaveformView.getZoomLevel();
        final PromptAnnotationObject[] saveIt = mPromptAnnotationObjectArr;
        final int saveTracker = mPromptElementTracker;
        super.onConfigurationChanged(newConfig);

        loadGui();

        mHandler.postDelayed(new Runnable() {
            public void run() {
                mStartMarker.requestFocus();
                markerFocus(mStartMarker);

                mWaveformView.setZoomLevel(saveZoomLevel);
                mWaveformView.recomputeHeights(mDensity);

                mPromptAnnotationObjectArr = saveIt;
                mPromptElementTracker = saveTracker;

                updateDisplay();
            }
        }, 500);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            onPlay(mStartPos);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    //
    // WaveformListener
    //

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Every time we get a message that our waveform drew, see if we need to
     * animate and trigger another redraw.
     */

    //Waveform methods/////////////////////////////////////////////////////////////////////////////
    public void waveformDraw() {
        mWidth = mWaveformView.getMeasuredWidth();
        if (mOffsetGoal != mOffset && !mKeyDown)
            updateDisplay();
        else if (mIsPlaying) {
            updateDisplay();
        } else if (mFlingVelocity != 0) {
            updateDisplay();
        }
    }

    public void waveformTouchStart(float x) {
        mTouchDragging = true;
        mTouchStart = x;
        mTouchInitialOffset = mOffset;
        mFlingVelocity = 0;
        mWaveformTouchStartMsec = getCurrentTime();
    }

    public void waveformTouchMove(float x) {
        mOffset = trap((int) (mTouchInitialOffset + (mTouchStart - x)));
        updateDisplay();
    }

    public void waveformTouchEnd() {
        mTouchDragging = false;
        mOffsetGoal = mOffset;

        long elapsedMsec = getCurrentTime() - mWaveformTouchStartMsec;
        if (elapsedMsec < 300) {
            if (mIsPlaying) {
                int seekMsec = mWaveformView.pixelsToMillisecs(
                        (int) (mTouchStart + mOffset));
                if (seekMsec >= mPlayStartMsec &&
                        seekMsec < mPlayEndMsec) {
                    mPlayer.seekTo(seekMsec);
                } else {
                    handlePause();
                }
            } else {
                onPlay((int) (mTouchStart + mOffset));
            }
        }
    }

    public void waveformFling(float vx) {
        mTouchDragging = false;
        mOffsetGoal = mOffset;
        mFlingVelocity = (int) (-vx);
        updateDisplay();
    }

    public void waveformZoomIn() {
        mWaveformView.zoomIn();
        mStartPos = mWaveformView.getStart();
        mEndPos = mWaveformView.getEnd();
        mMaxPos = mWaveformView.maxPos();
        mOffset = mWaveformView.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();
    }

    public void waveformZoomOut() {
        mWaveformView.zoomOut();
        mStartPos = mWaveformView.getStart();
        mEndPos = mWaveformView.getEnd();
        mMaxPos = mWaveformView.maxPos();
        mOffset = mWaveformView.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();
    }

    //
    // MarkerListener
    //

    ///////////////////////////////////////////////////////////////////////////////////////////////

    //Marker methods///////////////////////////////////////////////////////////////////////////////
    public void markerDraw() {
    }

    public void markerTouchStart(MarkerView marker, float x) {
        mTouchDragging = true;
        mTouchStart = x;
        mTouchInitialStartPos = mStartPos;
        mTouchInitialEndPos = mEndPos;
    }

    public void markerTouchMove(MarkerView marker, float x) {
        float delta = x - mTouchStart;

        if (marker == mStartMarker) {
            mStartPos = trap((int) (mTouchInitialStartPos + delta));
            //mEndPos = trap((int)(mTouchInitialEndPos + delta));
        } else {
            mEndPos = trap((int) (mTouchInitialEndPos + delta));
            if (mEndPos < mStartPos)
                mEndPos = mStartPos;
        }

        updateDisplay();
    }

    public void markerTouchEnd(MarkerView marker) {
        mTouchDragging = false;
        if (marker == mStartMarker) {
            setOffsetGoalStart();
        } else {
            setOffsetGoalEnd();
        }
    }

    public void markerLeft(MarkerView marker, int velocity) {
        mKeyDown = true;

        if (marker == mStartMarker) {
            int saveStart = mStartPos;
            mStartPos = trap(mStartPos - velocity);
            mEndPos = trap(mEndPos - (saveStart - mStartPos));
            setOffsetGoalStart();
        }

        if (marker == mEndMarker) {
            if (mEndPos == mStartPos) {
                mStartPos = trap(mStartPos - velocity);
                mEndPos = mStartPos;
            } else {
                mEndPos = trap(mEndPos - velocity);
            }

            setOffsetGoalEnd();
        }

        updateDisplay();
    }

    public void markerRight(MarkerView marker, int velocity) {
        mKeyDown = true;

        if (marker == mStartMarker) {
            int saveStart = mStartPos;
            mStartPos += velocity;
            if (mStartPos > mMaxPos)
                mStartPos = mMaxPos;
            mEndPos += (mStartPos - saveStart);
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;

            setOffsetGoalStart();
        }

        if (marker == mEndMarker) {
            mEndPos += velocity;
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;

            setOffsetGoalEnd();
        }

        updateDisplay();
    }

    public void markerEnter(MarkerView marker) {
    }

    public void markerKeyUp() {
        mKeyDown = false;
        updateDisplay();
    }

    public void markerFocus(MarkerView marker) {
        mKeyDown = false;
        if (marker == mStartMarker) {
            setOffsetGoalStartNoUpdate();
        } else {
            setOffsetGoalEndNoUpdate();
        }

        // Delay updaing the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        mHandler.postDelayed(new Runnable() {
            public void run() {
                updateDisplay();
            }
        }, 100);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    //File handling methods////////////////////////////////////////////////////////////////////////
    private boolean checkJsonFile() {
        File file = new File(mJsonFolder, mPromptNo + "clip.json");
        return file.exists();
    }

    private void loadPrompts() {
        String mFirstTextString = "";
        for (int i = 0; i < mPromptElementTracker; i++) {
            String spacer = " ";
            if (i == (mPromptElementTracker - 1)) {
                spacer = "";
            }
            mFirstTextString = mFirstTextString + mPromptTextArr[i] + spacer;
        }

        String mMainTextString = mPromptTextArr[mPromptElementTracker];
        String mLastTextString = "";
        for (int i = mPromptElementTracker + 1; i < mPromptTextArrLength; i++) {
            String spacer = " ";
            if (i == (mPromptTextArrLength - 1)) {
                spacer = "";
            }
            mLastTextString = mLastTextString + mPromptTextArr[i] + spacer;
        }

        if (mPromptElementTracker == mPromptTextArrLength - 1) {
            mLastTextString = "";
        }

        mFirstText.setText(mFirstTextString);
        mMainText.setText(mMainTextString);
        mLastText.setText(mLastTextString);
    }

    private void loadFromFile() {
        mFile = new File(mFilename);


        mLoadingLastUpdateTime = getCurrentTime();
        mLoadingKeepGoing = true;
        mFinishActivity = false;
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setMax(100);


        final SoundFile.ProgressListener listener =
                new SoundFile.ProgressListener() {
                    public boolean reportProgress(double fractionComplete) {
                        long now = getCurrentTime();

                        if (now - mLoadingLastUpdateTime > 100) {
                            mProgressBar.setIndeterminate(true);
                            mLoadingLastUpdateTime = now;
                        }


                        return mLoadingKeepGoing;
                    }
                };

        // Load the sound file in a background thread
        mLoadSoundFileThread = new Thread() {
            public void run() {
                try {
                    mSoundFile = SoundFile.create(mFile.getAbsolutePath(), listener);

                    if (mSoundFile == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBar.setVisibility(View.GONE);
                            }
                        });
                        String name = mFile.getName().toLowerCase();
                        String[] components = name.split("\\.");
                        String err;
                        if (components.length < 2) {
                            err = getResources().getString(
                                    R.string.no_extension_error);
                        } else {
                            err = getResources().getString(
                                    R.string.bad_extension_error) + " " +
                                    components[components.length - 1];
                        }
                        final String finalErr = err;
                        Runnable runnable = new Runnable() {
                            public void run() {
                                showFinalAlert(new Exception(), finalErr);
                            }
                        };
                        mHandler.post(runnable);
                        return;
                    }
                    mPlayer = new SamplePlayer(mSoundFile);
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setVisibility(View.GONE);
                        }
                    });
                    e.printStackTrace();


                    Runnable runnable = new Runnable() {
                        public void run() {
                            showFinalAlert(e, getResources().getText(R.string.read_error));
                        }
                    };
                    mHandler.post(runnable);
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgressBar != null) {
                            mProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
                if (mLoadingKeepGoing) {
                    Runnable runnable = new Runnable() {
                        public void run() {
                            finishOpeningSoundFile();
                        }
                    };
                    mHandler.post(runnable);
                } else if (mFinishActivity) {

                    AlignWaveformActivity.this.finish();
                }

            }

        };

        mLoadSoundFileThread.start();
    }

    private void finishOpeningSoundFile() {
        mWaveformView.setSoundFile(mSoundFile);
        mWaveformView.recomputeHeights(mDensity);

        mMaxPos = mWaveformView.maxPos();
        mLastDisplayedStartPos = -1;
        mLastDisplayedEndPos = -1;

        mTouchDragging = false;

        mOffset = 0;
        mOffsetGoal = 0;
        mFlingVelocity = 0;
        resetPositions();
        if (mEndPos > mMaxPos)
            mEndPos = mMaxPos;

/*        mCaption =
                mSoundFile.getFiletype() + ", " +
                        mSoundFile.getSampleRate() + " Hz, " +
                        mSoundFile.getAvgBitrateKbps() + " kbps, " +
                        formatTime(mMaxPos) + " " +
                        getResources().getString(R.string.time_seconds);
        //mInfo.setText(mCaption);*/

        updateDisplay();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    //UI methods///////////////////////////////////////////////////////////////////////////////////
    private void loadGui() {
        // Inflate our UI from its XML layout description.
        //setContentView(R.layout.editor);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.density;

        mActionBar = getSupportActionBar();
        msaveButton = findViewById(R.id.saveButton);
        msaveButton.setOnClickListener(mSaveListener);
        mLoadButton = findViewById(R.id.loadButton);
        mLoadButton.setOnClickListener(mLoadListener);

        mFirstText = findViewById(R.id.first_text);
        mMainText = findViewById(R.id.main_text);
        mLastText = findViewById(R.id.last_text);

        mMarkerLeftInset = (int) (46 * mDensity);
        mMarkerRightInset = (int) (48 * mDensity);
        mMarkerTopOffset = (int) (10 * mDensity);
        mMarkerBottomOffset = (int) (10 * mDensity);

        mStartText = findViewById(R.id.starttext);
        mStartText.addTextChangedListener(mTextWatcher);
        mEndText = findViewById(R.id.endtext);
        mEndText.addTextChangedListener(mTextWatcher);

        mPlayButton = findViewById(R.id.play);
        mPlayButton.setOnClickListener(mPlayListener);
        mRewindButton = findViewById(R.id.rew);
        mRewindButton.setOnClickListener(mRewindListener);
        mFfwdButton = findViewById(R.id.ffwd);
        mFfwdButton.setOnClickListener(mFfwdListener);

        mStartMinusButton = findViewById(R.id.start_minus);
        mStartMinusButton.setOnClickListener(mStartMinusListener);
        mStartPlusButton = findViewById(R.id.start_plus);
        mStartPlusButton.setOnClickListener(mStartPlusListener);
        mEndMinusButton = findViewById(R.id.end_minus);
        mEndMinusButton.setOnClickListener(mEndMinusListener);
        mEndPlusButton = findViewById(R.id.end_plus);
        mEndPlusButton.setOnClickListener(mEndPlusListener);

        mZoomIn = findViewById(R.id.zoomin);
        mZoomIn.setOnClickListener(mZoomInListener);
        mZoomOut = findViewById(R.id.zoomout);
        mZoomOut.setOnClickListener(mZoomOutListener);

        TextView markStartButton = findViewById(R.id.mark_start);
        markStartButton.setOnClickListener(mMarkStartListener);
        TextView markEndButton = findViewById(R.id.mark_end);
        markEndButton.setOnClickListener(mMarkEndListener);

        enableDisableButtons();

        mWaveformView = findViewById(R.id.waveform);
        mWaveformView.setListener(this);

        loadPrompts();

        mMaxPos = 0;
        mLastDisplayedStartPos = -1;
        mLastDisplayedEndPos = -1;

        if (mSoundFile != null && !mWaveformView.hasSoundFile()) {
            mWaveformView.setSoundFile(mSoundFile);
            mWaveformView.recomputeHeights(mDensity);
            mMaxPos = mWaveformView.maxPos();
        }

        mStartMarker = findViewById(R.id.startmarker);
        mStartMarker.setListener(this);
        mStartMarker.setAlpha(1f);
        mStartMarker.setFocusable(true);
        mStartMarker.setFocusableInTouchMode(true);
        mStartVisible = true;

        mEndMarker = findViewById(R.id.endmarker);
        mEndMarker.setListener(this);
        mEndMarker.setAlpha(1f);
        mEndMarker.setFocusable(true);
        mEndMarker.setFocusableInTouchMode(true);
        mEndVisible = true;

        updateDisplay();

    }

    private synchronized void updateDisplay() {
        if (mIsPlaying) {
            int now = mPlayer.getCurrentPosition();
            int frames = mWaveformView.millisecsToPixels(now);
            mWaveformView.setPlayback(frames);
            setOffsetGoalNoUpdate(frames - mWidth / 2);
            if (now >= mPlayEndMsec) {
                handlePause();
            }
        }

        if (!mTouchDragging) {
            int offsetDelta;

            if (mFlingVelocity != 0) {
                offsetDelta = mFlingVelocity / 30;
                if (mFlingVelocity > 80) {
                    mFlingVelocity -= 80;
                } else if (mFlingVelocity < -80) {
                    mFlingVelocity += 80;
                } else {
                    mFlingVelocity = 0;
                }

                mOffset += offsetDelta;

                if (mOffset + mWidth / 2 > mMaxPos) {
                    mOffset = mMaxPos - mWidth / 2;
                    mFlingVelocity = 0;
                }
                if (mOffset < 0) {
                    mOffset = 0;
                    mFlingVelocity = 0;
                }
                mOffsetGoal = mOffset;
            } else {
                offsetDelta = mOffsetGoal - mOffset;

                if (offsetDelta > 10)
                    offsetDelta = offsetDelta / 10;
                else if (offsetDelta > 0)
                    offsetDelta = 1;
                else if (offsetDelta < -10)
                    offsetDelta = offsetDelta / 10;
                else if (offsetDelta < 0)
                    offsetDelta = -1;
                else
                    offsetDelta = 0;

                mOffset += offsetDelta;
            }
        }

        mWaveformView.setParameters(mStartPos, mEndPos, mOffset);
        mWaveformView.invalidate();

        mStartMarker.setContentDescription(
                getResources().getText(R.string.start_marker) + " " +
                        formatTime(mStartPos));
        mEndMarker.setContentDescription(
                getResources().getText(R.string.end_marker) + " " +
                        formatTime(mEndPos));

        int startX = mStartPos - mOffset - mMarkerLeftInset;
        if (startX + mStartMarker.getWidth() >= 0) {
            if (!mStartVisible) {
                // Delay this to avoid flicker
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mStartVisible = true;
                        mStartMarker.setAlpha(1f);
                    }
                }, 0);
            }
        } else {
            if (mStartVisible) {
                mStartMarker.setAlpha(0f);
                mStartVisible = false;
            }
            startX = 0;
        }

        int endX = mEndPos - mOffset - mEndMarker.getWidth() + mMarkerRightInset;
        if (endX + mEndMarker.getWidth() >= 0) {
            if (!mEndVisible) {
                // Delay this to avoid flicker
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mEndVisible = true;
                        mEndMarker.setAlpha(1f);
                    }
                }, 0);
            }
        } else {
            if (mEndVisible) {
                mEndMarker.setAlpha(0f);
                mEndVisible = false;
            }
            endX = 0;
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
                startX,
                mMarkerTopOffset,
                -mStartMarker.getWidth(),
                -mStartMarker.getHeight());
        mStartMarker.setLayoutParams(params);

        params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
                endX,
                mWaveformView.getMeasuredHeight() - mEndMarker.getHeight() - mMarkerBottomOffset,
                -mStartMarker.getWidth(),
                -mStartMarker.getHeight());
        mEndMarker.setLayoutParams(params);
    }

    private Runnable mTimerRunnable = new Runnable() {
        public void run() {
            // Updating an EditText is slow on Android.  Make sure
            // we only do the update if the text has actually changed.
            if (mStartPos != mLastDisplayedStartPos &&
                    !mStartText.hasFocus()) {
                mStartText.setText(formatTime(mStartPos));
                mLastDisplayedStartPos = mStartPos;
            }

            if (mEndPos != mLastDisplayedEndPos &&
                    !mEndText.hasFocus()) {
                mEndText.setText(formatTime(mEndPos));
                mLastDisplayedEndPos = mEndPos;
            }

            mHandler.postDelayed(mTimerRunnable, 100);
        }
    };

    private void enableDisableButtons() {
        if (mIsPlaying) {
            mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
            mPlayButton.setContentDescription(getResources().getText(R.string.stop));
        } else {
            mPlayButton.setImageResource(android.R.drawable.ic_media_play);
            mPlayButton.setContentDescription(getResources().getText(R.string.play));
        }
    }

    private void resetPositions() {
        mStartPos = mWaveformView.secondsToPixels(0.0);
        mEndPos = mWaveformView.secondsToPixels(15.0);
    }

    private int trap(int pos) {
        if (pos < 0)
            return 0;
        if (pos > mMaxPos)
            return mMaxPos;
        return pos;
    }

    private void setOffsetGoalStart() {
        setOffsetGoal(mStartPos - mWidth / 2);
    }

    private void setOffsetGoalStartNoUpdate() {
        setOffsetGoalNoUpdate(mStartPos - mWidth / 2);
    }

    private void setOffsetGoalEnd() {
        setOffsetGoal(mEndPos - mWidth / 2);
    }

    private void setOffsetGoalEndNoUpdate() {
        setOffsetGoalNoUpdate(mEndPos - mWidth / 2);
    }

    private void setOffsetGoal(int offset) {
        setOffsetGoalNoUpdate(offset);
        updateDisplay();
    }

    private void setOffsetGoalNoUpdate(int offset) {
        if (mTouchDragging) {
            return;
        }

        mOffsetGoal = offset;
        if (mOffsetGoal + mWidth / 2 > mMaxPos)
            mOffsetGoal = mMaxPos - mWidth / 2;
        if (mOffsetGoal < 0)
            mOffsetGoal = 0;
    }

    private synchronized void handlePause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }
        mWaveformView.setPlayback(-1);
        mIsPlaying = false;
        enableDisableButtons();
    }

    private synchronized void onPlay(int startPosition) {
        if (mIsPlaying) {
            handlePause();
            return;
        }

        if (mPlayer == null) {
            // Not initialized yet
            return;
        }

        try {
            mPlayStartMsec = mWaveformView.pixelsToMillisecs(startPosition);
            if (startPosition < mStartPos) {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mStartPos);
            } else if (startPosition > mEndPos) {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mMaxPos);
            } else {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mEndPos);
            }
            mPlayer.setOnCompletionListener(new SamplePlayer.OnCompletionListener() {
                @Override
                public void onCompletion() {
                    handlePause();
                }
            });
            mIsPlaying = true;

            mPlayer.seekTo(mPlayStartMsec);
            mPlayer.start();
            updateDisplay();
            enableDisableButtons();
        } catch (Exception e) {
            showFinalAlert(e, R.string.play_error);
            return;
        }
    }

    /**
     * Show a "final" alert dialog that will exit the activity
     * after the user clicks on the OK button.  If an exception
     * is passed, it's assumed to be an error condition, and the
     * dialog is presented as an error, and the stack trace is
     * logged.  If there's no exception, it's a success message.
     */
    private void showFinalAlert(Exception e, CharSequence message) {
        CharSequence title;
        if (e != null) {
            Log.e("Ringdroid", "Error: " + message);
            Log.e("Ringdroid", getStackTrace(e));
            title = getResources().getText(R.string.alert_title_failure);
            setResult(RESULT_CANCELED, new Intent());
        } else {
            Log.v("Ringdroid", "Success: " + message);
            title = getResources().getText(R.string.alert_title_success);
        }

        new AlertDialog.Builder(AlignWaveformActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                        R.string.alert_ok_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                finish();
                            }
                        })
                .setCancelable(false)
                .show();
    }

    private void showFinalAlert(Exception e, int messageResourceId) {
        showFinalAlert(e, getResources().getText(messageResourceId));
    }
    //////////////////////////////////////////////////////////////////////////////////////////////

    //Event Listeners/////////////////////////////////////////////////////////////////////////////
    private View.OnClickListener mSaveListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            saveToJson();
        }
    };

    private View.OnClickListener mLoadListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (checkJsonFile()) {
                Gson gson = new Gson();
                mPromptAnnotationObjectArr = gson.fromJson(readJsonFile(), mPromptAnnotationObjectArr.getClass());
                if (!(mPromptAnnotationObjectArr[mPromptElementTracker] == null)) {
                    changeStartPos(mPromptAnnotationObjectArr[mPromptElementTracker].getStart());
                    changeEndPos(mPromptAnnotationObjectArr[mPromptElementTracker].getEnd());
                }
            }
        }
    };

    private View.OnClickListener mPlayListener = new View.OnClickListener() {
        public void onClick(View sender) {
            onPlay(mStartPos);
        }
    };

    private View.OnClickListener mRewindListener = new View.OnClickListener() {
        public void onClick(View sender) {
            recordPromptElementDetails();
            if (mPromptElementTracker > 0) {
                mPromptElementTracker--;
            } else {
                mPromptElementTracker = mPromptTextArrLength-1;
            }
            loadPrompts();

            if (mPromptAnnotationObjectArr[mPromptElementTracker] == null) {
                double endPos = Double.parseDouble(mEndText.getText().toString());
                changeStartPos(endPos);
                changeEndPos(endPos + 0.1);
            } else {
                changeStartPos(mPromptAnnotationObjectArr[mPromptElementTracker].getStart());
                changeEndPos(mPromptAnnotationObjectArr[mPromptElementTracker].getEnd());
            }

        }
    };

    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        public void onClick(View sender) {
            recordPromptElementDetails();
            if (mPromptElementTracker < mPromptTextArrLength - 1) {
                mPromptElementTracker++;
            } else {
                mPromptElementTracker = 0;
            }
            loadPrompts();

            if (mPromptAnnotationObjectArr[mPromptElementTracker] == null) {
                double endPos = Double.parseDouble(mEndText.getText().toString());
                if (endPos < mWaveformView.pixelsToSeconds(mMaxPos) - 0.1) {
                    changeStartPos(endPos);
                    changeEndPos(endPos + 0.1);
                } else {
                    changeStartPos(endPos - 0.1);
                    changeEndPos(endPos);
                }

            } else {
                changeStartPos(mPromptAnnotationObjectArr[mPromptElementTracker].getStart());
                changeEndPos(mPromptAnnotationObjectArr[mPromptElementTracker].getEnd());
            }

        }
    };

    private View.OnClickListener mMarkStartListener = new View.OnClickListener() {
        public void onClick(View sender) {
            if (mIsPlaying) {
                mStartPos = mWaveformView.millisecsToPixels(
                        mPlayer.getCurrentPosition());
                updateDisplay();
            }
        }
    };

    private View.OnClickListener mMarkEndListener = new View.OnClickListener() {
        public void onClick(View sender) {
            if (mIsPlaying) {
                mEndPos = mWaveformView.millisecsToPixels(
                        mPlayer.getCurrentPosition());
                updateDisplay();
                handlePause();
            }
        }
    };

    private View.OnClickListener mStartMinusListener = new View.OnClickListener() {
        public void onClick(View sender) {
            double startPos = Double.parseDouble(mStartText.getText().toString());
            changeStartPos(startPos - 0.01);

        }
    };
    private View.OnClickListener mStartPlusListener = new View.OnClickListener() {
        public void onClick(View sender) {
            double startPos = Double.parseDouble(mStartText.getText().toString());
            changeStartPos(startPos + 0.01);

        }
    };
    private View.OnClickListener mEndMinusListener = new View.OnClickListener() {
        public void onClick(View sender) {
            double endPos = Double.parseDouble(mEndText.getText().toString());
            changeEndPos(endPos - 0.01);

        }
    };
    private View.OnClickListener mEndPlusListener = new View.OnClickListener() {
        public void onClick(View sender) {
            double endPos = Double.parseDouble(mEndText.getText().toString());
            changeEndPos(endPos + 0.01);
        }
    };

    private View.OnClickListener mZoomInListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            waveformZoomIn();
        }
    };

    private View.OnClickListener mZoomOutListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            waveformZoomOut();
        }
    };

    private TextWatcher mTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start,
                                      int count, int after) {
        }

        public void onTextChanged(CharSequence s,
                                  int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            if (mStartText.hasFocus()) {
                try {
                    mStartPos = mWaveformView.secondsToPixels(
                            Double.parseDouble(
                                    mStartText.getText().toString()));
                    updateDisplay();
                } catch (NumberFormatException e) {
                }
            }
            if (mEndText.hasFocus()) {
                try {
                    mEndPos = mWaveformView.secondsToPixels(
                            Double.parseDouble(
                                    mEndText.getText().toString()));
                    updateDisplay();
                } catch (NumberFormatException e) {
                }
            }
        }
    };

    //////////////////////////////////////////////////////////////////////////////////////////////

    //File methods////////////////////////////////////////////////////////////////////////////////
    public void saveToJson() {
        recordPromptElementDetails();
        Gson gson = new Gson();
        String s = gson.toJson(mPromptAnnotationObjectArr);
        writeJsonFile(s);

    }

    public void writeJsonFile(String s) {
        File f = new File(mJsonFolder);
        f.mkdirs();
        FileWriter file = null;
        try {
            file = new FileWriter(mJsonFolder + "/" + mPromptNo + "clip.json");
            file.write(s);
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, mJsonFolder + "/" + mPromptNo + "clip.json", Toast.LENGTH_LONG).show();

    }

    public String readJsonFile() {
        File file = new File(mJsonFolder, mPromptNo + "clip.json");
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
        } catch (IOException e) {
            //You'll need to add proper error handling here
            e.printStackTrace();
        }
        return text.toString();
    }
    //////////////////////////////////////////////////////////////////////////////////////////////

    //Utilities///////////////////////////////////////////////////////////////////////////////////
    private String formatTime(int pixels) {
        if (mWaveformView != null && mWaveformView.isInitialized()) {
            return formatDecimal(mWaveformView.pixelsToSeconds(pixels));
        } else {
            return "";
        }
    }

    private String formatDecimal(double x) {
        int xWhole = (int) x;
        int xFrac = (int) (100 * (x - xWhole) + 0.5);

        if (xFrac >= 100) {
            xWhole++; //Round up
            xFrac -= 100; //Now we need the remainder after the round up
            if (xFrac < 10) {
                xFrac *= 10; //we need a fraction that is 2 digits long
            }
        }

        if (xFrac < 10)
            return xWhole + ".0" + xFrac;
        else
            return xWhole + "." + xFrac;
    }

    private double formatDecimalPlaces(double x) {
        Double y = Math.round(x * 100.0) / 100.0;
        return y;
    }

    private void recordPromptElementDetails() {

        mPromptAnnotationObjectArr[mPromptElementTracker] = new PromptAnnotationObject(mPromptElementTracker,
                mMainText.getText().toString(),
                formatDecimalPlaces(mWaveformView.pixelsToSeconds(mStartPos)),
                formatDecimalPlaces(mWaveformView.pixelsToSeconds(mEndPos)));
    }

    public void changeStartPos(double startPos) {
        mStartText.setText(formatDecimal(startPos));
        try {
            mStartPos = mWaveformView.secondsToPixels(
                    Double.parseDouble(
                            mStartText.getText().toString()));
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    updateDisplay();
                }
            }, 100);
        } catch (NumberFormatException e) {
        }
    }

    public void changeEndPos(double endPos) {
        if (endPos < mWaveformView.pixelsToSeconds(mStartPos)) {
            endPos = mWaveformView.pixelsToSeconds(mStartPos);
        }
        mEndText.setText(formatDecimal(endPos));
        try {
            mEndPos = mWaveformView.secondsToPixels(
                    Double.parseDouble(
                            mEndText.getText().toString()));
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    updateDisplay();
                }
            }, 100);
        } catch (NumberFormatException e) {
        }
    }

    private long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

    private String getStackTrace(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
}
