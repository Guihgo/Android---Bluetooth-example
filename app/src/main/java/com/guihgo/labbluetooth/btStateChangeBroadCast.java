package com.guihgo.labbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by Gui on 05/09/2015.
 */
public class btStateChangeBroadCast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.ERROR);
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                Toast.makeText(context, "Bluetooth DESLIGADO", Toast.LENGTH_SHORT).show();
            break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                Toast.makeText(context, "Bluetooth esta desligando...", Toast.LENGTH_SHORT).show();
                break;
            case BluetoothAdapter.STATE_ON:
                Toast.makeText(context, "Bluetooth LIGADO", Toast.LENGTH_SHORT).show();
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                Toast.makeText(context, "Bluetooth esta ligando...", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
