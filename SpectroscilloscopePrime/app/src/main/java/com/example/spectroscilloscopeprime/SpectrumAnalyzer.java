package com.example.spectroscilloscopeprime;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.UUID;

public class SpectrumAnalyzer extends AppCompatActivity {
    private final float ts = 5e-6f; // minimum time between data points
    private final int nPoints = 512; // Number of data points
    ArrayList<BarEntry> entries = new ArrayList<BarEntry>(); //To pass to BarDataSet
    ArrayList<Integer> twoByteData = new ArrayList<>();
    BarDataSet barDataSet = new BarDataSet(entries, "frequencies"); //To pass to BarData
    BarData barData = new BarData(barDataSet);  // To pass to BarChart
    BarChart barChart;
    boolean stopFFTThread;
    Handler handler = new Handler();
    Complex[] data = new Complex[nPoints];

    boolean probeselect;
    boolean channelselect;
    boolean frequencyselect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_spectrum_analyzer);

        //Grab Chart
        barChart = findViewById(R.id.barChart);

        // Customization of chart
        customizeChart();

        // get data from the previous activity
        Intent intent = getIntent();
        twoByteData = intent.getIntegerArrayListExtra(MainActivity.EXTRA_DATA);
        probeselect = intent.getBooleanExtra(MainActivity.EXTRA_PSEL,false);
        channelselect = intent.getBooleanExtra(MainActivity.EXTRA_CSEL,false);
        frequencyselect = intent.getBooleanExtra(MainActivity.EXTRA_FSEL,false);

        Log.d("probeselect", Boolean.toString(probeselect));
        Log.d("channelselect", Boolean.toString(channelselect));
        Log.d("frequencyselect", Boolean.toString(frequencyselect));

        // create the data set using values from the Oscilloscope Activity
        for (int i = 0; i < nPoints ; i ++) {
            data[i] = new Complex(calculateSignal(probeselect, channelselect, twoByteData.get(i)), 0);
            Log.d("twoByteData", Integer.toString(twoByteData.get(i))+", index: "+Integer.toString(i));
        }

        // start FFT thread
        stopFFTThread = false;
        FastFourierTransform FFT_runnable = new FastFourierTransform(data);
        Thread FFT_thread = new Thread(FFT_runnable);
        FFT_thread.start();
    }

    public float calculateSignal(boolean probeselect, boolean channelselect, int datapoint) {
        // this function will calculate the actucal value of each datapoint and return it
        /*
         * probeselect = true => x10 probe else x1 probe
         * channelselect = true => x10 adc channel gain else x1 adc channel gain was applied*/
        float voltage = datapoint * MainActivity.STEPSIZE;
        // 2.5V was added to shift the signal up for the MCU ADC, Therefore,
        voltage -= 2.5;
        if(channelselect) {
            voltage /= 10; // voltage at input of x10 gain circuit
        }
        /* following voltage at input of the front-end attenuator (voltage divider
        Vin => 909k => voltage => 100k => 0.1u => GND */
        voltage*=1009;
        voltage/=100;
        if(probeselect) {
            // if x10 probe was selected (meaning signal was divided by 10 before arriving)
            voltage*=10;
        }
        return voltage;
    }

    public void onClickMainActivity (View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void customizeChart () {
        barChart.setBackgroundColor(Color.BLACK);
        barChart.getLegend().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.setDrawBorders(true);
        barChart.setBorderColor(Color.WHITE);
        barChart.setBorderWidth(2f);

        //Leave scalling for XAxis
        barChart.getXAxis().setDrawLabels(false);
        barChart.getXAxis().setGranularityEnabled(true);
        //Max fs 200kHz, therefore min ts = 5e-6s
        barChart.getXAxis().setGranularity(ts);
        barChart.getXAxis().setGridColor(Color.WHITE);
        //Since were displaying 256 values ts * 256 = 1.28e-3
        barChart.getXAxis().setAxisMaximum(ts*nPoints);
        barChart.getXAxis().setAxisMinimum(0f);

        //Pass Actual values to MPAndroid Chart library and the library will auto scale the Y Axis
        barChart.getAxisRight().setEnabled(false);

        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisLeft().setGridColor(Color.WHITE);

    }

    private void createBarDataSet() {

        barDataSet.setValues(entries);
        Log.d("State?", "Updated BarDataSet with BarEntries");
        Log.d("Entries in Dataset", Integer.toString(barDataSet.getEntryCount()));
        barData.removeDataSet(0);
        barData.notifyDataChanged();
        barData.addDataSet(barDataSet);
        barData.notifyDataChanged();
        Log.d("Check BarData", Integer.toString(barData.getDataSetCount()));
        Log.d("State?", "Updated BarData with BarDataSet");
        barChart.notifyDataSetChanged();
        barChart.setData(barData);
        Log.d("State?", "Posted Data to Chart");
        barChart.invalidate();
        Log.d("State?", "invalidate()");


    }

//Class to perform Fast Fourier Transform
class FastFourierTransform implements Runnable {
    private Complex[] x;
    public FastFourierTransform(Complex[] x) {
        this.x = x;
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted() && !stopFFTThread) {
            FastFourierTransform fft = new FastFourierTransform(x);
            Complex[] FFTValues;
            FFTValues = fft.fft(x); //Store FFt values
            double[] Abs = new double[nPoints]; // Store magnitudes
            for (int a = 0; a < x.length; a++) {
                Log.d("Frequencies", "Re "+Double.toString(FFTValues[a].re())+" Im "+ Double.toString(FFTValues[a].im()));
                Abs[a] = FFTValues[a].abs();
//                    entries.add(new BarEntry(a, (float) Abs[a]));
                entries.add(a, new BarEntry(a, (float)Abs[a]));
                Log.d("Magnitude", Double.toString(Abs[a]));
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    createBarDataSet();
                }
            });

            stopFFTThread = true;
        }
    }

    // compute the FFT of x[], assuming its length n is a power of 2
    public Complex[] fft(Complex[] x) {
        int n = x.length;

        // base case
        if (n == 1) return new Complex[] { x[0] };

        // radix 2 Cooley-Tukey FFT
        if (n % 2 != 0) {
            throw new IllegalArgumentException("n is not a power of 2");
        }

        // compute FFT of even terms
        Complex[] even = new Complex[n/2];
        for (int k = 0; k < n/2; k++) {
            even[k] = x[2*k];
        }
        Complex[] evenFFT = fft(even);

        // compute FFT of odd terms
        Complex[] odd  = even;  // reuse the array (to avoid n log n space)
        for (int k = 0; k < n/2; k++) {
            odd[k] = x[2*k + 1];
        }
        Complex[] oddFFT = fft(odd);

        // combine
        Complex[] y = new Complex[n];
        for (int k = 0; k < n/2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k]       = evenFFT[k].plus (wk.times(oddFFT[k]));
            y[k + n/2] = evenFFT[k].minus(wk.times(oddFFT[k]));
        }
        return y;
    }
}
    }
