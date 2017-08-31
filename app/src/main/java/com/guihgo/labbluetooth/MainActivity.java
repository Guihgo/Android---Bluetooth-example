package com.guihgo.labbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    public static UUID my_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    int REQUEST_ENABLE_BT = 100;

    final int RECIEVE_MESSAGE = 50;

    BluetoothAdapter btAdapter;

    //Views
    Button btnGetPairedDevices, btnLimpaPairedDevices ,btnNewDevices, btnLimpaNewDevices, btnTonarVisivel;
    ListView lvPairedDevices, lvNewDevices;

    boolean isDiscoveringNewDevices = false;
    ArrayAdapter<String> newDevices;

    Handler h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Declarando Views
        btnGetPairedDevices = (Button) findViewById(R.id.btnGetPairedDevices);
        lvPairedDevices = (ListView) findViewById(R.id.lvPairedDevices);
        btnLimpaPairedDevices = (Button) findViewById(R.id.btnLimpaPairedDevices);
        btnNewDevices = (Button) findViewById(R.id.btnNewDevices);
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        btnLimpaNewDevices = (Button) findViewById(R.id.btnLimpaNewDevices);
        btnTonarVisivel = (Button) findViewById(R.id.btnTonarVisivel);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btIsEnabled();


        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:
                        byte[] readBuf = (byte[]) msg.obj;

                        String strIncom = new String(readBuf, 0, msg.arg1);

                        Toast.makeText(MainActivity.this, strIncom, Toast.LENGTH_SHORT).show();
                        break;
                }
            };
        };

        //onClicks dos Buttons
        btnGetPairedDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lvPairedDevices.setAdapter(getPairedDevices());
            }
        });

        btnLimpaPairedDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lvPairedDevices.setAdapter(null);
            }
        });

        btnNewDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btIsEnabled())
                {
                    if(isDiscoveringNewDevices) //se já estava procurando, desliga o scan
                    {
                        btnNewDevices.setText(getString(R.string.discovery_new_devices));
                        btAdapter.cancelDiscovery();

                        isDiscoveringNewDevices = false;
                    }
                    else //se nao estava procurando novos dispositivos, liga o scan
                    {
                        btnNewDevices.setText(getString(R.string.discovering_new_devices));

                        newDevices = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
                        // Register the BroadcastReceiver
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        registerReceiver(mReceiver, filter);

                        btAdapter.startDiscovery();

                        isDiscoveringNewDevices = true;
                    }
                }
            }
        });

        btnLimpaNewDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //lvNewDevices.setAdapter(null);


                AudioManager mAudioManager =
                        (AudioManager) MainActivity.this.getSystemService(Context.AUDIO_SERVICE);
                // Switch to headset
                mAudioManager.setMode(AudioManager.MODE_IN_CALL);

                // Start audio I/O operation
                mAudioManager.startBluetoothSco();



            }
        });


        btnTonarVisivel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent discoverableIntent = new
                        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 66);
                startActivity(discoverableIntent);
            }
        });


        lvPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String  itemValue = getPairedDevices().getItem(position);
                String MAC = itemValue.substring(itemValue.length() - 17);
                BluetoothDevice bluetoothDevice = btAdapter.getRemoteDevice(MAC);
                Toast.makeText(MainActivity.this, String.valueOf(bluetoothDevice.getBluetoothClass().getDeviceClass()) + "\n" + MAC, Toast.LENGTH_SHORT).show();

                // Initiate a connection request in a separate thread
                ConnectingThread t = new ConnectingThread(bluetoothDevice);
                t.start();
            }
        });
    }

    private class ConnectingThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectingThread(BluetoothDevice device) {

            BluetoothSocket temp = null;
            bluetoothDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                temp = bluetoothDevice.createRfcommSocketToServiceRecord(my_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = temp;
        }

        public void run() {
            // Cancel any discovery as it will slow down the connection
            btAdapter.cancelDiscovery();

            try {
                // This will block until it succeeds in connecting to the device
                // through the bluetoothSocket or throws an exception
                bluetoothSocket.connect();
            } catch (IOException connectException) {
                connectException.printStackTrace();
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Conectado ao Server:"+bluetoothSocket.getRemoteDevice().getName(), Toast.LENGTH_SHORT).show();
                }
            });
            // Code to manage the connection in a separate thread

            ConnectedThread connectedThread = new ConnectedThread(bluetoothSocket);
            connectedThread.start();
            connectedThread.write("Ola do Cliente");
        /*
            manageBluetoothConnection(bluetoothSocket);
        */
        }

        // Cancel an open connection and terminate the thread
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                newDevices.add(device.getName() + "\n" + device.getAddress());
                lvNewDevices.setAdapter(newDevices);
            }
        }
    };

    private ArrayAdapter<String> getPairedDevices()
    {
        if(btIsEnabled())
        {
            ArrayAdapter<String> aAdapterPairedDevices = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    aAdapterPairedDevices.add(device.getName() + "\n" + device.getAddress());
                }

            }
            return aAdapterPairedDevices;
        }

        return null;
    }

    private boolean btIsEnabled()
    {
        if(btAdapter != null)  //se o dispositivo não tem recurso de bluetooth
        {
            if(btAdapter.isEnabled()) // se o bluetooth esta OFF
            {
                return true;
            }
            else //inicia intent de ligar pedir para o user ligar o Bluetooth
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                return false;
            }
        }
        else
        {
            Toast.makeText(this, "Este dispositivo não tem o recurso de Bluetooth", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_ENABLE_BT)
        {
            if(resultCode == this.RESULT_OK)
            {
                Toast.makeText(this, "Espere um momento...", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, "operação cancelada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_server) {
            startActivity(new Intent(this, ServerActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    //Toast.makeText(MainActivity.this, String.valueOf(bytes), Toast.LENGTH_SHORT).show();

                    // Send the obtained bytes to the UI activity
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {

            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}
