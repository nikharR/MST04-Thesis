package com.example.nikhar.mst04v10;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by Nikhar on 2017/10/30.
 */
public class RecordWaveTask extends AsyncTask<File, Void, Object[]> {

    //Audio Parameters/////////////////////////////////////////////////////////////////////////////
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    private static final int SAMPLE_RATE = 16000; // Hz
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    private final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);
    ///////////////////////////////////////////////////////////////////////////////////////////////

    //Activity Context/////////////////////////////////////////////////////////////////////////////
    private Context ctx;
    private RecordActivity recordActivity;

    RecordWaveTask(Context ctx) {
        setContext(ctx);
        this.recordActivity = (RecordActivity) ctx;
    }

    public void setContext(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Opens up the given file, writes the header, and keeps filling it with raw PCM bytes from
     * AudioRecord until it reaches 4GB or is stopped by the user. It then goes back and updates
     * the WAV header to include the proper final chunk sizes.
     *
     * @param files Index 0 should be the file to write to
     * @return Either an Exception (error) or two longs, the filesize, elapsed time in ms (success)
     */
    ///////////////////////////////////////////////////////////////////////////////////////////////

    //Background task off UI thread////////////////////////////////////////////////////////////////
    @Override
    protected Object[] doInBackground(File... files) {
        AudioRecord audioRecord = null;
        FileOutputStream wavOut = null;
        long startTime = 0;
        long endTime = 0;
        Log.v("RECORDTASK", "ENTERED DOINBACKGROUNG");
        try {
            // Open our two resources
            audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);
            wavOut = new FileOutputStream(files[0]);

            // Write out the wav file header
            writeWavHeader(wavOut, CHANNEL_MASK, SAMPLE_RATE, ENCODING);

            final byte[] buffer = new byte[BUFFER_SIZE];
            boolean run = true;
            int read;
            long total = 0;

            startTime = SystemClock.elapsedRealtime();
            audioRecord.startRecording();
            Log.v("RECORDTASK", "AUDIO IS RECORDING");
            while (run && !isCancelled()) {
                read = audioRecord.read(buffer, 0, buffer.length);
                Log.v("RECORDTASK", "WHILE LOOP ENTERED");
                // WAVs cannot be > 4 GB due to the use of 32 bit unsigned integers.
                if (total + read > 4294967295L) {
                    // Write as many bytes as we can before hitting the max size
                    for (int i = 0; i < read && total <= 4294967295L; i++, total++) {
                        wavOut.write(buffer[i]);
                    }

                    run = false;
                } else {
                    // Write out the entire read buffer
                    wavOut.write(buffer, 0, read);
                    total += read;
                    final short[] shorts = recordActivity.byteArrToShortArr(buffer);
                    Arrays.sort(shorts);
                    recordActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            // some code #3 (Write your code here to run in UI thread)
                            recordActivity.mBarLevel.setLevel(recordActivity.scaleLogarithmic(shorts));

                        }
                    });

                }
                Log.v("RECORDTASK", "WHILE LOOP EXITED");
            }
        } catch (IOException ex) {
            Log.v("RECORDTASK", ex + "");
            return new Object[]{ex};
        } finally {
            if (audioRecord != null) {
                try {
                    if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop();
                        endTime = SystemClock.elapsedRealtime();
                        Log.v("RECORDTASK", "STOPPED RECORDING");
                    }
                } catch (IllegalStateException ex) {
                    //
                }
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.release();
                    Log.v("RECORDTASK", "AUDIO IS RELEASED");
                }
            }
            if (wavOut != null) {
                try {
                    wavOut.close();
                    Log.v("RECORDTASK", "WAV FILE CLOSED");
                } catch (IOException ex) {
                    //
                }
            }

        }

        try {
            // This is not put in the try/catch/finally above since it needs to run
            // after we close the FileOutputStream
            updateWavHeader(files[0]);
        } catch (IOException ex) {
            Log.v("RECORDTASK", ex + "");
            return new Object[]{ex};
        }
        Log.v("RECORDTASK", "OBJECT RETURNED");
        return new Object[]{files[0].length(), endTime - startTime};

    }
    //////////////////////////////////////////////////////////////////////////////////////////////

    //WAV file header methods//////////////////////////////////////////////////////////////////////
    private void writeWavHeader(OutputStream out, int channelMask, int sampleRate, int encoding) throws IOException {
        short channels;
        switch (channelMask) {
            case AudioFormat.CHANNEL_IN_MONO:
                channels = 1;
                break;
            case AudioFormat.CHANNEL_IN_STEREO:
                channels = 2;
                break;
            default:
                throw new IllegalArgumentException("Unacceptable channel mask");
        }

        short bitDepth;
        switch (encoding) {
            case AudioFormat.ENCODING_PCM_8BIT:
                bitDepth = 8;
                break;
            case AudioFormat.ENCODING_PCM_16BIT:
                bitDepth = 16;
                break;
            case AudioFormat.ENCODING_PCM_FLOAT:
                bitDepth = 32;
                break;
            default:
                throw new IllegalArgumentException("Unacceptable encoding");
        }

        writeWavHeader(out, channels, sampleRate, bitDepth);
    }

    private void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
        // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
        byte[] littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((short) (channels * (bitDepth / 8)))
                .putShort(bitDepth)
                .array();

        out.write(new byte[]{
                // RIFF header
                'R', 'I', 'F', 'F', // ChunkID
                0, 0, 0, 0, // ChunkSize (must be updated later)
                'W', 'A', 'V', 'E', // Format
                // fmt subchunk
                'f', 'm', 't', ' ', // Subchunk1ID
                16, 0, 0, 0, // Subchunk1Size
                1, 0, // AudioFormat
                littleBytes[0], littleBytes[1], // NumChannels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                littleBytes[10], littleBytes[11], // BlockAlign
                littleBytes[12], littleBytes[13], // BitsPerSample
                // data subchunk
                'd', 'a', 't', 'a', // Subchunk2ID
                0, 0, 0, 0, // Subchunk2Size (must be updated later)
        });
    }

    private void updateWavHeader(File wav) throws IOException {
        byte[] sizes = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt((int) (wav.length() - 8)) // ChunkSize
                .putInt((int) (wav.length() - 44)) // Subchunk2Size
                .array();

        RandomAccessFile accessWave = null;
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            accessWave = new RandomAccessFile(wav, "rw");
            // ChunkSize
            accessWave.seek(4);
            accessWave.write(sizes, 0, 4);

            // Subchunk2Size
            accessWave.seek(40);
            accessWave.write(sizes, 4, 4);
        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally
            throw ex;
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close();
                } catch (IOException ex) {
                    //
                }
            }
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////

    //Post-task methods on UI thread//////////////////////////////////////////////////////////////
    @Override
    protected void onPostExecute(Object[] results) {
        Log.v("RECORDTASK", "ONPOSTEXECUTE STARTED");
/*        Throwable throwable = null;
        if (results[0] instanceof Throwable) {
            // Error
            throwable = (Throwable) results[0];
            Log.e(RecordWaveTask.class.getSimpleName(), throwable.getMessage(), throwable);
        }*/

        // If we're attached to an activity
        if (ctx != null) {
           /* if (throwable == null) {
                // Display final recording stats
                double size = (long) results[0] / 1000000.00;
                long time = (long) results[1] / 1000;
                Toast.makeText(ctx, String.format(Locale.getDefault(), "%.2f MB / %d seconds",
                        size, time), Toast.LENGTH_LONG).show();
            } else {
                // Error
                Toast.makeText(ctx, throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }*/
            Toast.makeText(ctx,"Recording Saved", Toast.LENGTH_LONG).show();
            Log.v("RECORDTASK", "TOAST DISPLAYED");
        }

    }

    @Override
    protected void onCancelled(Object[] objects) {
        Log.v("RECORDTASK", "ONCANCELLED STARTED");
        onPostExecute(objects);
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
}
