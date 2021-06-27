package com.example.spectroscilloscopeprime;

import com.github.mikephil.charting.formatter.ValueFormatter;

public class FrequencyValFormatter extends ValueFormatter {

    @Override
    public String getFormattedValue(float value) {
        int index = (int)value;
        float frequencyBinResolution = 0f;
        frequencyBinResolution = (float)MainActivity.SAMPLERATE/MainActivity.nPoints;
        float frequencyBinVal = index * frequencyBinResolution;

        return Float.toString(frequencyBinVal)+"Hz";
    }
}
