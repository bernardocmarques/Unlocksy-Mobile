package pt.berre.sirs_mobile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

class CreateBluetoothSocketThread extends Thread {
    private static final String TAG = "myTag";


    private BluetoothDevice device;
    private BluetoothSocket socket = null;
    private AppCompatActivity activity;



    CreateBluetoothSocketThread(BluetoothDevice device, AppCompatActivity activity) {
        this.device = device;
        this.activity = activity;
    }

    @Override
    public void run() {
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")); //FIXME change this is prob wrong
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }

        try {
            socket.connect();
            Log.d(TAG, "Connected to " + socket.getRemoteDevice().getName());
            activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), "Connected to " + socket.getRemoteDevice().getName(), Toast.LENGTH_SHORT).show());
        } catch (IOException connectException) {
            activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), "Error connecting to " + socket.getRemoteDevice().getName(), Toast.LENGTH_SHORT).show());
            Log.e(TAG, "Error connecting to " + socket.getRemoteDevice().getName(), connectException);
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
        }

        activity.runOnUiThread(() -> ((BluetoothActivity) activity).connectionHandler(socket));
    }
}