package com.example.graphingmodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;


public class MainActivity extends AppCompatActivity {

    private ConstraintLayout mainLayout;
    private LineChart mChart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainLayout = (ConstraintLayout) findViewById(R.id.mainLayout);
        //create line chart
        mChart = new LineChart(this);
        //add to main Layout
        mainLayout.addView(mChart);

        //customize Line Chart
        mChart.setDescription("");
        mChart.setNoDataTextDescription("No data for the moment");


        //enable value highlighting
        mChart.setHighlightEnabled(true);

        //enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);

        //enable pinch zoom to avoid scaling x and y axis separately
        mChart.setPinchZoom(true);

        //alternative background colour
        mChart.setBackgroundColor(Color.BLACK);

        //now we work on data
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        //add data to line chart
        mChart.setData(data);

        //get Legend object
        Legend l = mChart.getLegend();

        //customise legend
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis x1 = mChart.getXAxis();
        x1.setTextColor(Color.WHITE);
        x1.setDrawGridLines(false);
        x1.setAvoidFirstLastClipping(true);

        YAxis y1 = mChart.getAxisLeft();
        y1.setTextColor(Color.WHITE);
        y1.setAxisMaxValue(140f);
        y1.setDrawGridLines(true);

        YAxis y12 = mChart.getAxisRight();
        y12.setEnabled(false);
    }

    //method to add data to the grqph
    public void addEntry() {
        LineData data = mChart.getData();

        if (data != null) {
            LineDataSet set  = data.getDataSetByIndex(0);
            if (set == null) {
                //creation if null
                set = createSet();
                data.addDataSet(set);
            }

            //add a new Random Value
            data.addXValue("");
            data.addEntry(new Entry((float)(Math.random() * 120) + 5f, set.getEntryCount()), 0);

            //notify chart that data has changed
            mChart.notifyDataSetChanged();

            //limit number of visible entries
            mChart.setVisibleXRange(6);

            // scaling can now only be done on x- and y-axis separately
            mChart.setPinchZoom(false);

            //scroll to the last entry
            mChart.moveViewToX(data.getXValCount() - 7);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        //now, we're going to simulate realtime data addition

        new Thread(new Runnable() {
            @Override
            public void run() {
                //add 100 entries
                for(int i = 0; i <100 ; i++) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addEntry(); // chart is notified in addentry method

                        }
                    });
                    //pause between adds
                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    //method to create set
    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "SPL Db");
        set.setDrawCubic(true);
        set.setCubicIntensity(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(2f);
        set.setCircleSize(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244,117,117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);

        return set;
    }
}
