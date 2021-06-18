package com.example.spectroscilloscopeprime;


import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@SuppressLint("UseSwitchCompatOrMaterialCode")
public class MainActivity extends AppCompatActivity {
    // TODO - Add a high/low frequency switch to provide two sample rate options
    // TODO - Add functionality to the existing switch
    // TODO - Add code to send and recieve configuration data for the channel and samplerate options
    private final String DEVICE_ADDRESS = "98:D6:32:35:8F:C6";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    // string to tag the data that will be sent to the Spectrum Analyzer Activity
    public static final String EXTRA_DATA = "com.example.spectroscilloscopeprime.EXTRA_DATA";
    public static final String EXTRA_FSEL = "com.example.spectroscilloscopeprime.EXTRA_FSEL";
    public static final String EXTRA_PSEL = "com.example.spectroscilloscopeprime.EXTRA_PSEL";
    public static final String EXTRA_CSEL = "com.example.spectroscilloscopeprime.EXTRA_CSEL";
    private final float ts = 5e-6f; // minimum time between data points
    private final int nPoints = 512; // Number of data points
    public static final float STEPSIZE = 4.8828125e-3f;

    // this is the list that accepts data from the bluetooth socket
    ArrayList<Integer> temp = new ArrayList <Integer> ();

    // this is the list that will be passed to the Spectrum Analyzer Activity
    ArrayList<Integer> twoByteData = new ArrayList <Integer> ();

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

    // March 27, 2021
    boolean probeselect = true;
    boolean frequencyselect;
    boolean channelselect = false;

    // April 1, 2021
    Switch probe;
    Switch freq;


    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        setTitle("LineChart - Objective 2");

        // Obtain all elements of the User Interface
        connectButton = findViewById(R.id.connectButton);
        sendButton = findViewById(R.id.beginButton);
        stopButton = findViewById(R.id.stopButton);
        lineChart = findViewById(R.id.line_chart);

        // connect to Spectroscilloscope on the creation of the activity
        initiateConnection();

        // probeSelect
        probeSelector();

        // frequency Select
        freqSelector();

        // Customization of chart
        customizeChart();



    }

    private void plotData(ArrayList<Entry> entries) {
        LineDataSet lineDataSet1 = new LineDataSet(entries,null);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet1);
        LineData data = new LineData(dataSets);
        lineChart.setData(data);
        lineChart.invalidate();
        Toast.makeText(this, "Plotted data", Toast.LENGTH_SHORT).show();
    }

    public float calculateSignal(boolean probeselect, boolean channelselect, int datapoint) {
        // this function will calculate the actucal value of each datapoint and return it
        /*
         * probeselect = true => x10 probe else x1 probe
         * channelselect = true => x10 adc channel gain else x1 adc channel gain was applied*/
        float voltage = datapoint * STEPSIZE;
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
        //Since were displaying 256 values ts * 256 = 1.28e-3
        lineChart.getXAxis().setAxisMaximum(ts*nPoints);
        lineChart.getXAxis().setAxisMinimum(0f);

        //Pass Actual values to MPAndroid Chart library and the library will auto scale the Y Axis
        lineChart.getAxisRight().setEnabled(false);

        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getAxisLeft().setGridColor(Color.WHITE);

    }

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
        initiateConnection();
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

    public void onClickStop(View view) {
        try {
            breakConnection();
        }
        catch (IOException ex) {
            Log.d("IOException from stop", "Failed");
        }
    }

    public void onClickSpectrumAnalysis(View view) {
        try {
            breakConnection();
        }
        catch (IOException ex) {
            Log.d("IOException from stop", "Failed");
        }

        Intent intent = new Intent(this, SpectrumAnalyzer.class);
        intent.putIntegerArrayListExtra(EXTRA_DATA, twoByteData);
        intent.putExtra(EXTRA_PSEL, probeselect);
        intent.putExtra(EXTRA_FSEL, frequencyselect);
        intent.putExtra(EXTRA_CSEL, channelselect);
        startActivity(intent);
    }

    void initiateConnection() {
        if(BTinit())
        {
            if(BTconnect())
            {
                deviceConnected=true;
                beginListenForData();
                Toast.makeText(this, "Connected to  Spectroscilloscope!", Toast.LENGTH_SHORT).show();
            }

        }
    }

    void breakConnection() throws IOException{
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

    // used to change the probe
    public void probeSelector() {
        probe = (Switch) findViewById(R.id.probeS);
        probe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    probeselect = true; // true when x1 probe
                } else {
                    probeselect = false; // false when x10 probe
                }
            }
        });
    }

    // use to change the sample rate of the embedded system
    public void freqSelector() {
        freq = (Switch) findViewById(R.id.freqS);
        freq.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    // true = low frequency so samplerate = 256
                    frequencyselect = true;
                } else {
                    // false = high frequency so sample rate > 256
                    frequencyselect = false;
                }
            }
        });
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
                        if(temp.size() == (nPoints * 2)) {
                            dataReady = true;
                        }

                        if(dataReady) {
                            twoByteData.clear();
                            final ArrayList<Entry> entries = new ArrayList<>();
                            int a;
                            for (int i = 0; i < nPoints; i++) {
                                a = temp.get(2*i) << 8;
                                twoByteData.add(a + temp.get(2*i + 1));
                                entries.add(i , new Entry(i*ts, calculateSignal(probeselect,channelselect, twoByteData.get(i))));
                                Log.d("twoByteData", Integer.toString(twoByteData.get(i))+", index: "+Integer.toString(i));
                                if(i == (nPoints - 1) ) {
                                    // reset variables so more data can be collected
                                    temp.clear();
                                    dataReady = false;
                                    rawBytes = null;
                                }
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