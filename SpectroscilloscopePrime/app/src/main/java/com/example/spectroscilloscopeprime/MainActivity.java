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
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@SuppressLint("UseSwitchCompatOrMaterialCode")
public class MainActivity extends AppCompatActivity {
    // TODO - Add a high/low frequency switch to provide two sample rate options
    // TODO - Add functionality to the existing switch
    // TODO - Add code to send and recieve configuration data for the channel and samplerate options
    private final String DEVICE_ADDRESS = "98:D6:32:35:8B:3C";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    // string to tag the data that will be sent to the Spectrum Analyzer Activity
    public static final String EXTRA_DATA = "com.example.spectroscilloscopeprime.EXTRA_DATA";
    public static final String EXTRA_FSEL = "com.example.spectroscilloscopeprime.EXTRA_FSEL";
    public static final String EXTRA_PSEL = "com.example.spectroscilloscopeprime.EXTRA_PSEL";
    public static final String EXTRA_CSEL = "com.example.spectroscilloscopeprime.EXTRA_CSEL";
    private float ts = 5e-6f; // minimum time between data points
    public static final int nPoints = 256; // Number of data points
    public static final float STEPSIZE = 4.638671875e-3f; // actual voltage supply and reference
    public static int SAMPLERATE = 0;
    // this is the list that accepts data from the bluetooth socket
    ArrayList<Integer> temp = new ArrayList <Integer> ();

    // this is the list that will be passed to the Spectrum Analyzer Activity
    ArrayList<Integer> twoByteData = new ArrayList <Integer> ();

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    Handler handler = new Handler();

    // indicators
    TextView status;
    TextView samplerate;
    TextView channel;
    TextView VDC;
    TextView PEAK;
    //StateVariables
    boolean stopBTThread;
    boolean deviceConnected = false;
    boolean dataReady = false;

    // Line Chart
    LineChart lineChart;

    // March 27, 2021
    boolean probeselect;
    boolean frequencyselect;
    boolean channelselect;

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
        status = findViewById(R.id.status);
        samplerate = findViewById(R.id.samplerate);
        channel = findViewById(R.id.channel);
        VDC = findViewById(R.id.dc);
        PEAK = findViewById(R.id.peak);
        probe = findViewById(R.id.probeS);
        lineChart = findViewById(R.id.line_chart);

        // default indicators
        status.setText("Disconnected");
        samplerate.setText("HF");
        channel.setText("NaN");

        // default probe
        probeselect = true;
        probe.setChecked(probeselect);

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
        // set the y axis scale depending on the signal path being measured
        if (probeselect == channelselect) {
            // +/- 30
            lineChart.getAxisLeft().setAxisMaximum(30);
            lineChart.getAxisLeft().setAxisMinimum(-30);
        } else if (!probeselect && channelselect) {
            // +/- 3
            lineChart.getAxisLeft().setAxisMaximum(3);
            lineChart.getAxisLeft().setAxisMinimum(-3);
        } else if (probeselect && !channelselect) {
            // +/- 270
            lineChart.getAxisLeft().setAxisMaximum(270);
            lineChart.getAxisLeft().setAxisMinimum(-270);
        }
        customizeChart ();
        LineDataSet lineDataSet1 = new LineDataSet(entries,null);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet1);
        LineData data = new LineData(dataSets);
        lineChart.setData(data);
        lineChart.invalidate();
        Toast.makeText(this, "Plotted data", Toast.LENGTH_SHORT).show();
        // find DC content and peak
        float dccontent = 0f;
        float peak = 0f;
        for (int i = 0 ; i < nPoints; i++){
            dccontent += entries.get(i).getY();
            if (entries.get(i).getY() > peak) {
                peak = entries.get(i).getY();
            }
        }
        dccontent /= nPoints;
        DecimalFormat df = new DecimalFormat("#.##");
        String formatted = df.format(dccontent);
        VDC.setText(formatted+"V");
        formatted = df.format(peak);
        PEAK.setText(formatted+"V");
    }

    public static float calculateSignal(boolean probeselect, boolean channelselect, int datapoint) {
        // this function will calculate the actual value of each datapoint and return it
        /*
         * probeselect = true => x10 probe else x1 probe
         * channelselect = true => x10 adc channel gain else x1 adc channel gain was applied*/
        float voltage = datapoint * STEPSIZE;
        // 2.5V (2.16V) was added to shift the signal up for the MCU ADC, Therefore,
        voltage -= 2.16;
        if(channelselect) {
            voltage /= 10; // voltage at input of x10 gain circuit
        }
        /* following voltage at input of the front-end attenuator (voltage divider
        Vin => 909k => voltage => 100k =>  GND */
        voltage*=1009;
        voltage/=100;
        if(probeselect) {
            // if x10 probe was selected (meaning signal was divided by 10 before arriving)
            voltage/=0.0928;
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
        lineChart.getXAxis().setGridColor(Color.WHITE);
        //Since were displaying 256 values
        lineChart.getXAxis().setAxisMaximum(nPoints);
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
        Log.d("Button clicked", "onClickStartTransmission: ");
        String string;
        if (frequencyselect == false) {
            string = "t"; // Spectroscilloscope will use HF
        }else {
            string = "q"; // Spectroscilloscope will use LF
        }
        string.concat("\n");
        try {
            outputStream.write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Exception", "thown ");
        }
        Toast.makeText(this,"Capturing", Toast.LENGTH_SHORT).show();
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
                status.setText("Connected!");
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
        status.setText("Disconnected");
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

        probe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    probeselect = true; // true when x10 probe

                } else {
                    probeselect = false; // false when x1 probe
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
                    samplerate.setText("LF");
                } else {
                    // false = high frequency so samplesample rate is 200000
                    frequencyselect = false;
                    samplerate.setText("HF");
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
                        Log.d("byteCount", Integer.toString(byteCount));
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
                        // add the 4 because the channel data and sample rate will be stored in the arraylist
                        if(temp.size() == (nPoints * 2)+4) {
                            dataReady = true;
                            int[] sampleRate = new int[2];
                            sampleRate[0] = temp.get(0);
                            sampleRate[1] = temp.get(1);
                            int SRate = sampleRate[0]<<8 + sampleRate[1];
                            if (SRate == 0) {
                                SRate = sampleRate[1];
                            }
                            final int TSRate = SRate;
                            SAMPLERATE = SRate;
                            handler.post(new Runnable() {
                                @Override
                                public void run()
                                {
                                    samplerate.setText(Integer.toString(TSRate) + "SPS");
                                }
                            });
                            Log.d("Sample Rate", Integer.toString((SRate)));
                            int[] channelD = new int[2];
                            channelD[0] = temp.get(2);
                            channelD[1] = temp.get(3);
                            int Channel = channelD[0]<<8 + channelD[1];
                            if(channelD[1] == 1) {
                                channelselect = true;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run()
                                    {
                                        channel.setText("x10 Channel");

                                    }
                                });
                            }else if (channelD[1] == 0) {
                                channelselect = false;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run()
                                    {
                                        channel.setText("x1 Channel");

                                    }
                                });
                            }
                            Log.d("Channel Selected", Integer.toString(Channel));
                            // remove channel and sample rate overhead
                            temp.remove(0);
                            temp.remove(0);
                            temp.remove(0);
                            temp.remove(0);
                        }

                        if(dataReady) {
                            twoByteData.clear();
                            final ArrayList<Entry> entries = new ArrayList<>();
                            int a;
                            for (int i = 0; i < nPoints; i++) {
                                a = temp.get(2*i) << 8;
                                twoByteData.add(a + temp.get(2*i + 1));
                                entries.add(i , new Entry(i, calculateSignal(probeselect,channelselect, twoByteData.get(i))));
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