package com.example.graphmasterline;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String DEVICE_ADDRESS = "98:D6:32:35:8F:C6";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private final float ts = 5e-6f; // minimum time between data points
    private final int nPoints = 16; // Number of data points

    ArrayList<Integer> temp = new ArrayList <Integer> ();

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    Handler handler = new Handler();

    //StateVariables
    boolean stopBTThread;
    boolean deviceConnected = false;
    boolean dataReady = false;
    Button connectButton, sendButton, stopButton;

    // Line Chart
    LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("LineChart - Objective 2");

        // Obtain all elements of the User Interface
        connectButton = findViewById(R.id.connectButton);
        sendButton = findViewById(R.id.beginButton);
        stopButton = findViewById(R.id.stopButton);
        lineChart = findViewById(R.id.line_chart);

        // Customization of chart
        customizeChart();


        LineDataSet lineDataSet1 = new LineDataSet(dataValues1(),null);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet1);

        LineData data = new LineData(dataSets);
        lineChart.setData(data);
        lineChart.invalidate();
    }

    private ArrayList<Entry> dataValues1() {
        ArrayList<Entry> dataVals = new ArrayList<>();
        dataVals.add(new Entry(0,0));
        dataVals.add(new Entry(5e-6f,0));
        dataVals.add(new Entry(10e-6f,5));
        dataVals.add(new Entry(15e-6f,5));
        dataVals.add(new Entry(20e-6f,0));
        dataVals.add(new Entry(25e-6f,0));
        dataVals.add(new Entry(30e-6f,5));
        dataVals.add(new Entry(35e-6f,5));
        dataVals.add(new Entry(40e-6f,0));
        dataVals.add(new Entry(45e-6f,0));

        return dataVals;
    }

    private void plotData(ArrayList<Entry> entries) {
        LineDataSet lineDataSet1 = new LineDataSet(entries,null);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet1);
        LineData data = new LineData(dataSets);
        lineChart.setData(data);
        lineChart.invalidate();
    }

    private void customizeChart () {
        lineChart.setBackgroundColor(Color.BLACK);
        lineChart.getLegend().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawBorders(true);
        lineChart.setBorderColor(Color.WHITE);
        lineChart.setBorderWidth(2f);

        //Leave scalling for XAxis
        lineChart.getXAxis().setDrawLabels(false);
        lineChart.getXAxis().setGranularityEnabled(true);
        //Max fs 200kHz, therefore min ts = 5e-6s
        lineChart.getXAxis().setGranularity(ts);
        lineChart.getXAxis().setGridColor(Color.WHITE);
        //Since were displaying 1024 values ts * 1024 = 5.115e-3
        lineChart.getXAxis().setAxisMaximum(ts*nPoints);
        lineChart.getXAxis().setAxisMinimum(0f);

        //Pass Actual values to MPAndroid Chart library and the library will auto scale the Y Axis
        lineChart.getAxisRight().setEnabled(false);

        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getAxisLeft().setGridColor(Color.WHITE);

    }

    //    TODO - use threads to carry out Bluetooth initialization processes
    //enable bluetooth and search for device
    public boolean BTinit() {
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

    public boolean BTconnect() {
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
                deviceConnected=true;
                beginListenForData();
                Toast.makeText(this, "Connected to  Measuring Device!", Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void onClickStartTransmission(View view) {
        //String string = editText.getText().toString();
        String string = "t";
        string.concat("\n");
        try {
            outputStream.write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this,"Sent Data: "+string, Toast.LENGTH_SHORT).show();
    }

    public void onClickStop(View view) throws IOException {
        stopBTThread = true;
        outputStream.close();
        inputStream.close();
        socket.close();
        deviceConnected=false;
        Toast.makeText(this, "Connection Closed!", Toast.LENGTH_SHORT).show();
    }

    void beginListenForData() {
        stopBTThread = false;
        BeginListeningForData runnable = new BeginListeningForData();
        Thread thread  = new Thread(runnable);
        thread.start();
    }

    // Class to receive data
    class BeginListeningForData implements Runnable {

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted() && !stopBTThread)
            {
                try
                {
                    int byteCount = inputStream.available();
                    if(byteCount > 0)
                    {
                        byte[] rawBytes = new byte[byteCount];
                        inputStream.read(rawBytes);

                        for (int i = 0 ; i < byteCount; i++) {
                            //converting to unsigned value
                            byte aByte = rawBytes[i];
                            int number = aByte & 0xff;
                            temp.add(number);
                            Log.d("Data", Integer.toString(number));
                        }

                        // Use this to make the program aware of when the expected data has completely arrived
                        if(temp.size() == (nPoints * 2)) dataReady = true;

                        if(dataReady) {
                            int[] twoByteData = new int[nPoints];
                            final ArrayList<Entry> entries = new ArrayList<>();
                            int a;
                            for (int i = 0; i < nPoints; i++) {
                                a = temp.get(2*i) << 8;
                                twoByteData[i] = a + temp.get(2*i + 1);
                                entries.add(i , new Entry(i*ts, twoByteData[i]));
                                Log.d("twoByteData", Integer.toString(twoByteData[i])+", index: "+Integer.toString(i));
                            }



                            handler.post(new Runnable() {
                                @Override
                                public void run()
                                {
                                    plotData(entries);
                                }
                            });
                        }


                    }
                }
                catch (IOException ex)
                {
                    stopBTThread = true;
                }
            }
        }
    }
}