package com.example.spectroscilloscopeomega;

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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    //=============Bluetooth=================================
    private final String DEVICE_ADDRESS = "98:D6:32:35:8F:C6";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    int[] bytesToInt;
    boolean deviceConnected = false;
    Thread thread;
    byte buffer[];
    int bufferPosition;
    boolean stopThread;

    Button receiveButton, connectButton;
    //============Chart===================================
    private ConstraintLayout mainLayout;
    private LineChart mChart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainLayout = (ConstraintLayout) findViewById(R.id.mainLayout);

        //buttons
        connectButton = (Button) findViewById(R.id.connectButton);
        receiveButton = (Button) findViewById(R.id.receiveButton);

        setUIEnabled(false); //button enabling

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

    private void setUIEnabled(boolean state) {

        connectButton.setEnabled(!state);
        receiveButton.setEnabled(state);
    }

    //enable bluetooth and search for device
    public boolean BTinit()
    {
        boolean found=false;
        BluetoothAdapter bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Device doesnt Support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Please Pair the Device first",Toast.LENGTH_SHORT).show();
        }
        else
        {
            for (BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(DEVICE_ADDRESS))
                {
                    device=iterator;
                    found=true;
                    break;
                }
            }
        }
        return found;
    }

    public boolean BTconnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return connected;
    }

    public void onClickConnect(View view) {
        if(BTinit())
        {
            if(BTconnect())
            {
                setUIEnabled(true);
                deviceConnected=true;
                beginListenForData();
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Connected to  Measuring Device!",
                        Toast.LENGTH_SHORT);
                toast.show();
            }

        }
    }


    //Listening for raw data from spectroscilloscope
    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            bytesToInt = new int[byteCount];
                            //Store data into global array so it can be accessed by other function
                            for (int x = 0 ; x < byteCount; x++) {
                                bytesToInt[x] = rawBytes[x];
//                                Log.d("ints", Integer.toString(bytesToInt[x]));
                            }

                            handler.post(new Runnable() {
                                public void run()
                                {
                                    //to update main UI
//                                    textView.append(string);
//                                   textView.append("\n");
                                }
                            });

                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }

//Send t
    public void onClickReceive(View view) {
        //String string = editText.getText().toString();
        String string = "t";
        string.concat("\n");
        try {
            outputStream.write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //method to add data to the graph
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