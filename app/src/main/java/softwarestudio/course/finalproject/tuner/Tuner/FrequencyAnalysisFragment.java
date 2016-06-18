package softwarestudio.course.finalproject.tuner.Tuner;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import softwarestudio.course.finalproject.tuner.FastFourierTransform.RealDoubleFFT;
import softwarestudio.course.finalproject.tuner.HarmonicProductSpectrum.HPS;
import softwarestudio.course.finalproject.tuner.R;

public class FrequencyAnalysisFragment extends Fragment {

    private boolean analysisStarted = false;
    private boolean cancellFlag = true;

    private View rootView = null;

    private final static int FREQUENCY = 8000;
    private final static int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private final static int BLOCK_SIZE = 512;

    private RealDoubleFFT transformer = new RealDoubleFFT(BLOCK_SIZE);
    private HPS hpsAnalysis = new HPS();
    private AudioRecord audioRecord = null;
    private FrequencyAnalysisTask frequencyAnalysisTask = null;

    public FrequencyAnalysisFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_frequency_analysis, container, false);
        TextView textView = (TextView)rootView.findViewById(R.id.freq_als_display_text);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (analysisStarted) {
                    analysisStarted = false;
                    cancellFlag = true;
                    /*
                    try {
                        audioRecord.stop();
                    } catch (IllegalStateException e) {
                        Log.e("audio stop failed", e.toString());
                    }
                    */
                } else {
                    analysisStarted = true;
                    cancellFlag = false;
                    frequencyAnalysisTask = new FrequencyAnalysisTask();
                    frequencyAnalysisTask.execute();
                }
            }
        });
        return rootView;
    }

    public void onStop() {
        super.onStop();
        frequencyAnalysisTask.cancel(true);
        /*
        try {
            audioRecord.stop();
        } catch (IllegalStateException e) {
            Log.e("audio stop failed", e.toString());
        }
        */
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private class FrequencyAnalysisTask extends AsyncTask<Void, Double, Boolean> {

        private final String LOG_TAG = FrequencyAnalysisTask.class.getSimpleName();

        private double mainfrequency = 0;
        private final static int MIN_FREQ_BIN = 25;
        private final static int MAX_FREQ_BIN = 338;

        private double[] frequencyList = {220, 233.08, 246.94,
                261.63, 277.18, 293.66, 311.13, 329.63, 349.23, 369.99, 392, 415.3, 440, 466.16, 493.88,
                523.25, 554.37, 587.33, 622.25, 659.25, 698.46, 739.99, 783.99, 830.61, 880, 932.33, 987.77,
                1046.50, 1108.73, 1174.66, 1244.51, 1318.51, 1396.91, 1479.98, 1567.98, 1611.22, 1760, 1864.66, 1975.53,
                2093, 2217.46, 2349.32, 2489.02, 2637.02};

        @Override
        protected Boolean doInBackground(Void... params) {

            Log.d(LOG_TAG, "Frequency Analysis Task Start");

            int buffersize = 2 * AudioRecord.getMinBufferSize(
                    FREQUENCY,
                    CHANNEL_CONFIGURATION, AUDIO_ENCODING
            );
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    FREQUENCY,
                    CHANNEL_CONFIGURATION,
                    AUDIO_ENCODING,
                    buffersize
            );

            short[] buffer = new short[BLOCK_SIZE];
            double[] toTransform = new double[BLOCK_SIZE];

            try {
                audioRecord.startRecording();
            } catch (IllegalStateException e) {
                Log.e(LOG_TAG, e.toString());
            }

            while (analysisStarted) {
                if (isCancelled() || cancellFlag) {
                    analysisStarted = false;
                    break;
                } else {
                    // clean previous detection
                    for (int i=0; i<BLOCK_SIZE; i++) {
                        toTransform[i] = 0;
                        buffer[i] = 0;
                    }

                    int bufferReadResultLen = audioRecord.read(buffer, 0, BLOCK_SIZE);
                    for (int i=0; i<BLOCK_SIZE && i<bufferReadResultLen; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
                    }

                    transformer.ft(toTransform);
                    /*
                    double mainFrequencyBin = hpsAnalysis.GetBaseFrequency(
                            toTransform, 6
                    );
                    */

                    double mainFrequencyBin = 0;
                    double maxAmplitude = toTransform[0];
                    for (int i=0; i<BLOCK_SIZE && i<bufferReadResultLen; i++) {
                        if (maxAmplitude < toTransform[i]) {
                            maxAmplitude = toTransform[i];
                            mainFrequencyBin = (double)i;
                        }
                    }

                    Log.d(LOG_TAG, "main frequency bin = " + Double.toString(mainFrequencyBin));

                    if (mainFrequencyBin> MIN_FREQ_BIN
                            && mainFrequencyBin < MAX_FREQ_BIN) {
                        mainfrequency =
                                mainFrequencyBin * (double)FREQUENCY / (double)BLOCK_SIZE / 2;
                    } else {
                        mainfrequency = 0;
                    }

                    Log.d(LOG_TAG, "domain frequency = " + Double.toString(mainfrequency));

                    publishProgress(FrequencyApproximation(mainfrequency));
                }
                try{
                    Thread.sleep(120);
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, e.toString());
                }
            }

            return true;
        }

        @Override
        protected void onProgressUpdate(Double... progess) {

            Log.d(LOG_TAG, "Displaying in progress");
            Log.d(LOG_TAG, "get frequency: " + progess[0].toString());

            // change the view of fragment
            if (rootView != null) {
                TextView textView = (TextView)rootView.findViewById(R.id.freq_als_display_text);
                textView.setText(progess[0].toString() + " Hz");
                textView.invalidate();
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(LOG_TAG, "Frequency Analysis Task Quit");
            super.onPostExecute(result);
            try{
                audioRecord.release();
            }
            catch(IllegalStateException e){
                Log.e(LOG_TAG, e.toString());
            }
        }

        private double FrequencyApproximation(double target) {

            if (target <= 0.01) return 0;

            for (int i=1; i<frequencyList.length-1; i++) {
                double lowerbound = (frequencyList[i] - frequencyList[i-1]) / 2;
                double upperbound = (frequencyList[i+1] - frequencyList[i]) / 2;
                if (target >= lowerbound && target <= frequencyList[i]) {
                    return DiscreteLinearApproximation(target, frequencyList[i], lowerbound);
                } else if (target >= frequencyList[i] && target <= upperbound) {
                    return DiscreteLinearApproximation(target, upperbound, frequencyList[i]);
                }
            }

            return 0;
        }

        private double DiscreteLinearApproximation (double sample, double upperbound, double lowerbound) {

            if (sample <= 0.001) return 0;

            final double numofInterval = 30;

            double approximatedValue = 0;

            double interval = (upperbound - lowerbound) / numofInterval;

            double curValue = lowerbound;
            while (curValue <= upperbound + 0.01) {
                if (sample >= curValue - interval/2
                        && sample <= curValue + interval/2) {
                    approximatedValue = curValue;
                }
                curValue += interval;
            }

            return Math.floor(approximatedValue * 100) / 100;
        }
    }
}
