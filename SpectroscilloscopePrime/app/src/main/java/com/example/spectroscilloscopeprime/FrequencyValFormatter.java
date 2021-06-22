package com.example.spectroscilloscopeprime;

import com.github.mikephil.charting.formatter.ValueFormatter;

public class FrequencyValFormatter extends ValueFormatter {

    @Override
    public String getFormattedValue(float value) {
        int index = (int)value;
        float frequencyBinResolution = 0f;
        if (SpectrumAnalyzer.frequencyselect) {
            frequencyBinResolution = (float)SpectrumAnalyzer.lSampleFreq/SpectrumAnalyzer.nPoints;
        } else {
            frequencyBinResolution = (float)SpectrumAnalyzer.hSampleFreq/SpectrumAnalyzer.nPoints;
        }
        float frequencyBinVal = index * frequencyBinResolution;

        return Float.toString(frequencyBinVal)+"Hz";
    }
}
