package softwarestudio.course.finalproject.tuner.HarmonicProductSpectrum;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Created by lusa on 2016/06/17.
 */
public class HPS {

    public int GetBaseFrequency(double[] fftData, int timesofDownSample)
        throws IllegalArgumentException {

        if (fftData == null || timesofDownSample == 0)
            throw new IllegalArgumentException();

        // Assume fftData includes only real parts
        int dataLen = fftData.length;

        double[] hpsData = Arrays.copyOf(fftData, dataLen);

        for(int factor = 1; factor<timesofDownSample; factor++) {
            int bound = dataLen / factor;
            for (int i=0; i<bound; i++) {
                hpsData[i] *= fftData[i*factor];
            }
        }

        return IndexOfMax(hpsData);
    }

    private int IndexOfMax(double[] data)
        throws IllegalArgumentException {

        if (data == null)
            throw new IllegalArgumentException();

        int maxIndex = 0;
        double maxValue = data[0];

        for (int i=0; i<data.length; i++) {
            if (data[i] > maxValue) {
                maxValue = data[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }
}
