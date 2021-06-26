package com.example.spectroscilloscopeprime;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.ArrayList;
import java.util.UUID;

public class SpectrumAnalyzer extends AppCompatActivity  implements OnChartValueSelectedListener{

    public final static int nPoints = 256; // Number of data points
    public final static int lSampleFreq = 256;
    public final static int hSampleFreq = 200000;
    ArrayList<BarEntry> entries = new ArrayList<BarEntry>(); //To pass to BarDataSet
    ArrayList<Integer> twoByteData = new ArrayList<>();
    BarDataSet barDataSet = new BarDataSet(entries, "Magnitude"); //To pass to BarData
    BarData barData = new BarData(barDataSet);  // To pass to BarChart
    BarChart barChart;
    boolean stopFFTThread;
    Handler handler = new Handler();
    Complex[] data = new Complex[nPoints];

    public static boolean probeselect;
    public static boolean channelselect;
    public static boolean frequencyselect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_spectrum_analyzer);

        //Grab Chart
        barChart = findViewById(R.id.barChart);
        barChart.setOnChartValueSelectedListener(this);
        // Customization of chart
        customizeChart();

        // get data from the previous activity
        Intent intent = getIntent();
        twoByteData = intent.getIntegerArrayListExtra(MainActivity.EXTRA_DATA);
        probeselect = intent.getBooleanExtra(MainActivity.EXTRA_PSEL,false);
        channelselect = intent.getBooleanExtra(MainActivity.EXTRA_CSEL,false);
        // frequency = false => high frequency (200kSpS)
        frequencyselect = intent.getBooleanExtra(MainActivity.EXTRA_FSEL,false);

        Log.d("probeselect", Boolean.toString(probeselect));
        Log.d("channelselect", Boolean.toString(channelselect));
        Log.d("frequencyselect", Boolean.toString(frequencyselect));

        // create the data set using values from the Oscilloscope Activity
        for (int i = 0; i < nPoints ; i ++) {
            data[i] = new Complex(MainActivity.calculateSignal(probeselect, channelselect, twoByteData.get(i)), 0);
            Log.d("twoByteData", Integer.toString(twoByteData.get(i))+", index: "+Integer.toString(i));
        }

        // start FFT thread
        stopFFTThread = false;
        FastFourierTransform FFT_runnable = new FastFourierTransform(data);
        Thread FFT_thread = new Thread(FFT_runnable);
        FFT_thread.start();
    }
    private final RectF onValueSelectedRectF = new RectF();

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        if (e == null)
            return;

        RectF bounds = onValueSelectedRectF;
        barChart.getBarBounds((BarEntry) e, bounds);
        MPPointF position = barChart.getPosition(e, YAxis.AxisDependency.LEFT);

        Log.i("bounds", bounds.toString());
        Log.i("position", position.toString());

        Log.i("x-index",
                "low: " + barChart.getLowestVisibleX() + ", high: "
                        + barChart.getHighestVisibleX());

        MPPointF.recycleInstance(position);
    }

    @Override
    public void onNothingSelected() {
        Log.i("Activity", "Nothing selected.");
    }

    public void onClickMainActivity (View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void customizeChart () {
        barChart.setBackgroundColor(Color.BLACK);
        barChart.setDrawBorders(true);
        barChart.setBorderColor(Color.WHITE);
        barChart.setBorderWidth(2f);

        // format legend
        barChart.getLegend().setEnabled(true);
        barChart.getLegend().setTextColor(Color.WHITE);

        // format description text
        barChart.getDescription().setEnabled(true); // false
        barChart.getDescription().setTextColor(Color.WHITE);
        barChart.getDescription().setText("256 point frequency spectrum");


        // customize X Axis
        ValueFormatter xAxisFormatter = new FrequencyValFormatter();

        barChart.getXAxis().setDrawLabels(true); // false
        barChart.getXAxis().setGridColor(Color.WHITE);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setTextColor(Color.WHITE);
        barChart.getXAxis().setAxisMinimum(0f);

        barChart.getXAxis().setValueFormatter(xAxisFormatter);

        // customise Y Axis
        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisLeft().setGridColor(Color.WHITE);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisRight().setAxisMinimum(0f);

        XYMarkerView mv = new XYMarkerView(this, xAxisFormatter);
        mv.setChartView(barChart); // For bounds control
        barChart.setMarker(mv); // Set the marker to the chart
    }

    private void createBarDataSet() {

        barDataSet.setValues(entries);
        Log.d("State?", "Updated BarDataSet with BarEntries");
        Log.d("Entries in Dataset", Integer.toString(barDataSet.getEntryCount()));
        barData.removeDataSet(0);
        barData.notifyDataChanged();
        barData.addDataSet(barDataSet);
        barData.notifyDataChanged();

        // customize data
        barData.setValueTextSize(10f);
        barData.setValueTextColor(Color.WHITE);

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
            for (int a = 0; a < (x.length/2); a++) {
                Log.d("Frequency " + Integer.toString(a), "Re "+Double.toString(FFTValues[a].re())+" Im "+ Double.toString(FFTValues[a].im()));
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
