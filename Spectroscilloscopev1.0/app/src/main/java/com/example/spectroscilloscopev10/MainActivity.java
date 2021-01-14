package com.example.spectroscilloscopev10;

import androidx.annotation.LongDef;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.spectroscilloscope.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    //    private final String DEVICE_NAME="Spectroscilloscope";
    private final String DEVICE_ADDRESS = "98:D6:32:35:8F:C6";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    final Handler handler = new Handler();
    ArrayList<Integer> list = new ArrayList <Integer> ();
    int nPoint = 16; // Number of frequency bins
    Complex[] data = new Complex[nPoint];
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    Button startButton, sendButton, clearButton, stopButton, buttonFFT;
    TextView textView;
    EditText editText;

    byte buffer[];
    int bufferPosition;
    //StateVariables
    boolean stopBTThread;
    boolean stopFFTThread;
    boolean deviceConnected = false;
    boolean calculateFFT = false;
    boolean dataReady = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        buttonFFT = (Button) findViewById(R.id.buttonFFT);
        setUiEnabled(false);
    }

    //enable the user interface upon startup
    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);

    }

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

    public void onClickStart(View view) {
        if(BTinit())
        {
            if(BTconnect())
            {

                setUiEnabled(true);
                deviceConnected=true;
                beginListenForData();
                textView.append("\nConnected to  Measuring Device!\n");
            }

        }
    }

    void beginListenForData() {
        stopBTThread = false;
        buffer = new byte[1024];
        BeginListeningForData runnable = new BeginListeningForData();
        Thread thread  = new Thread(runnable);
        thread.start();
    }

    public void onClickSend(View view) {
        //String string = editText.getText().toString();
        String string = "t";
        string.concat("\n");
        try {
            outputStream.write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        textView.append("\nSent Data:"+string+"\n");

    }

    public void onClickStop(View view) throws IOException {
        stopBTThread = true;
        outputStream.close();
        inputStream.close();
        socket.close();
        setUiEnabled(false);
        deviceConnected=false;
        textView.append("\nConnection Closed!\n");
    }

    public void onClickClear(View view) {
        textView.setText("");
    }

    public void onClickFFT(View view) {
        stopFFTThread = false;
        FastFourierTransform FFT_runnable = new FastFourierTransform(data);
        Thread FFT_thread = new Thread(FFT_runnable);
        FFT_thread.start();
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

                    for (int x = 0 ; x < byteCount; x++) {
                        //converting to unsigned value
                        byte aByte = rawBytes[x];
                        int number = aByte & 0xff;
                        list.add(number);
                        Log.d("Data", Integer.toString(number));
                    }

                    // Use this to make the program aware of when the expected data has completely arrived
                    if(list.size() == 32) calculateFFT = true;

                    if(calculateFFT) {
                        final String string=new String(rawBytes,"UTF-8");
                        int[] twoByteData = new int[16];
                        int a;
                        for (int i = 0; i < 16; i++) {
                            a = 0;
                            a = list.get(2*i) << 8;
                            twoByteData[i] = a + list.get(2*i + 1);
                            data[i] = new Complex(twoByteData[i], 0);
                            Log.d("twoByteData", Integer.toString(twoByteData[i]));
                        }

//                            handler.post(new Runnable() {
//                                @Override
//                                public void run()
//                                {
//                                    textView.append(string);
//                                }
//                            });
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
                FFTValues = fft.fft(x);
                for (int i = 0; i < x.length; i++) {
                    Log.d("Frequencies", "Re "+Double.toString(FFTValues[i].re())+" Im "+ Double.toString(FFTValues[i].im()));
                }
                double[] Abs = new double[16];
                for(int a = 0; a<16; a++) {
                	Abs[a] = FFTValues[a].abs();
                    Log.d("Magnitude", Double.toString(Abs[a]));
                }
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