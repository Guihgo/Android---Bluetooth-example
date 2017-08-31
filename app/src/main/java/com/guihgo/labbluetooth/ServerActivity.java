package com.guihgo.labbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class ServerActivity extends ActionBarActivity {

    public static UUID my_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    final int REQUEST_ENABLE_BT = 100;

    final int RECIEVE_MESSAGE = 50;

    BluetoothAdapter btAdapter;
    Button btnServer, btnEnviar;
    TextView tvDeviceConnected;
    EditText etMsg;

    ListeningThread listeningThread;
    ConnectedThread connectedThread;
    Handler h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conectar);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btnServer = (Button) findViewById(R.id.btnServer);
        tvDeviceConnected = (TextView) findViewById(R.id.tvDeviceConnected);
        btnEnviar = (Button) findViewById(R.id.btnEnviar);
        etMsg = (EditText) findViewById(R.id.etMsg);

        btIsEnabled();

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:
                        byte[] readBuf = (byte[]) msg.obj;

                        String strIncom = new String(readBuf, 0, msg.arg1);
                        Toast.makeText(ServerActivity.this, strIncom, Toast.LENGTH_SHORT).show();
                        break;
                }
            };
        };

        //onClicks
        btnServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listeningThread = new ListeningThread();
                listeningThread.start();
            }
        });

        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connectedThread != null)
                {
                    connectedThread.write(etMsg.getText().toString());
                }
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_conectar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    //-----------------
    private class ListeningThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public ListeningThread() {
            BluetoothServerSocket temp = null;
            try {
                temp = btAdapter.listenUsingRfcommWithServiceRecord(getString(R.string.app_name), my_UUID);

            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothServerSocket = temp;
        }

        public void run() {
            BluetoothSocket bluetoothSocket;

            final String name, MAC;

            // This will block while listening until a BluetoothSocket is returned
            // or an exception occurs
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Abriu um server. Aguardando client...",
                            Toast.LENGTH_SHORT).show();
                }
            });
            while (true) {
                try {
                    bluetoothSocket = bluetoothServerSocket.accept();

                } catch (IOException e) {
                    break;
                }
                // If a connection is accepted
                if (bluetoothSocket != null) {
                    name = bluetoothSocket.getRemoteDevice().getName();
                    MAC = bluetoothSocket.getRemoteDevice().getAddress();

                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvDeviceConnected.setText(name+"\n"+MAC);
                            Toast.makeText(getApplicationContext(), "Uma conexão foi aceita.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Manage the connection in a separate thread
                    connectedThread = new ConnectedThread(bluetoothSocket);
                    connectedThread.start();
                    connectedThread.write("Olá do Server");

                    try {
                        bluetoothServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Cancel the listening socket and terminate the thread
        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //-----------------
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
