package com.example.graphmaster;


import androidx.appcompat.app.AppCompatActivity;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private final String DEVICE_ADDRESS = "98:D6:32:35:8F:C6";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    ArrayList<Integer> temp = new ArrayList <Integer> ();
    int nPoints = 512; // Number of frequency bins
    Complex[] data = new Complex[nPoints];
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    Handler handler = new Handler();
    byte buffer[];

    //StateVariables
    boolean stopBTThread;
    boolean stopFFTThread;
    boolean deviceConnected = false;
    boolean dataReady = false;
    Button connectButton, sendButton, clearButton, stopButton;


ArrayList<BarEntry> entries = new ArrayList<BarEntry>(); //To pass to BarDataSet
BarDataSet barDataSet = new BarDataSet(entries, "frequencies"); //To pass to BarData
BarData barData = new BarData(barDataSet);  // To pass to BarChart
BarChart barChart;

    boolean probeselect = false;
    boolean channelselect = true;
    private final float STEPSIZE = 4.8828125e-3f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("BarChart - Objective 1");

        //Grab Buttons
        connectButton = findViewById(R.id.connectButton);
        sendButton = findViewById(R.id.beginButton);
        clearButton = findViewById(R.id.graphButton);
        stopButton = findViewById(R.id.stopButton);

        //Grab Chart
        barChart = findViewById(R.id.chart1);

//        for (int i = 0; i<nPoints; i++ ) {
//            if(i%2 == 0)
//                entries.add(new BarEntry(i, 512f));
//            else entries.add(new BarEntry(i, 1023f));
//        }


        barChart.setData(barData);

        XAxis xAxis = new XAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis yAxisL = barChart.getAxisLeft();
        yAxisL.setAxisMinimum(0f);

        barChart.setDrawValueAboveBar(false); //works
        barChart.setDoubleTapToZoomEnabled(false);//works
        barChart.setPinchZoom(false); //works
        barChart.setDrawBarShadow(false);//works
        barChart.setDrawBorders(true); //works
        Description description = new Description();
        description.setText("Frequency Spectrum\nAlick Campbell");
        barChart.setDescription(description);

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

    public void onClickFFT(View view) {
        stopFFTThread = false;
        FastFourierTransform FFT_runnable = new FastFourierTransform(data);
        Thread FFT_thread = new Thread(FFT_runnable);
        FFT_thread.start();
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

    void beginListenForData() {
        stopBTThread = false;
        buffer = new byte[1024];
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
                        if(temp.size() == (nPoints * 2)) {
                            dataReady = true;
                        }

                        if(dataReady) {
                            int[] twoByteData = new int[nPoints];
                            final ArrayList<Entry> entries = new ArrayList<>();
                            int a;
                            for (int i = 0; i < nPoints; i++) {
                                a = temp.get(2*i) << 8;
                                twoByteData[i] = a + temp.get(2*i + 1);
                                data[i] = new Complex(calculateSignal(probeselect,channelselect, twoByteData[i]), 0);
                                Log.d("twoByteData", Integer.toString(twoByteData[i])+", index: "+Integer.toString(i));
                                if(i == (nPoints - 1) ) {
                                    // reset variables so more data can be collected
                                    temp.clear();
                                    dataReady = false;
                                    rawBytes = null;
                                }
                            }


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