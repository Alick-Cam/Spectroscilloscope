package com.example.spectroscilloscopeprime;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
    BarDataSet barDataSet = new BarDataSet(entries, "frequencies"); //To pass to BarData
    BarData barData = new BarData(barDataSet);  // To pass to BarChart
    BarChart barChart;

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
//    private void plotFFT() {
//        int[] twoByteData = new int[nPoints];
//        final ArrayList<Entry> entries = new ArrayList<>();
//        int a;
//        for (int i = 0; i < nPoints; i++) {
//            a = temp.get(2*i) << 8;
//            twoByteData[i] = a + temp.get(2*i + 1);;
//            Log.d("twoByteData", Integer.toString(twoByteData[i])+", index: "+Integer.toString(i));
//        }
    }
