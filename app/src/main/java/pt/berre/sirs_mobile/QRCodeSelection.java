package pt.berre.sirs_mobile;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.notbytes.barcode_reader.BarcodeReaderActivity;

import java.io.IOException;
import java.util.UUID;

public class QRCodeSelection extends AppCompatActivity {

    private static final String TAG = "myTag";
    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private Button btnScan;
    private static final int BARCODE_READER_ACTIVITY_REQUEST = 1208;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_selection);

        btnScan = (Button) findViewById(R.id.btnScan);


        findViewById(R.id.btnScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchIntent = BarcodeReaderActivity.getLaunchIntent(QRCodeSelection.this, true, false);
                startActivityForResult(launchIntent, BARCODE_READER_ACTIVITY_REQUEST);
            }
        });



    }








    //    ----------------------------------------------------------------------
    //    ---------------------------- Receivers -------------------------------
    //    ----------------------------------------------------------------------

    // Broadcast receiver turning the BT ON and OFF
    private final BroadcastReceiver broadcastReceiverOnOff = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "broadcastReceiverOnOff: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "broadcastReceiverOnOff: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "broadcastReceiverOnOff: STATE TURNING ON");
                        break;
                }
            }
        }
    };




    // Broadcast receiver that detects bond state changes (pairing status changes)
    private final BroadcastReceiver broadcastReceiverBondStatus = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //3cases
                //case 1: bonded already
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                }

                //case 2: creating a bond
                if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");

                }

                //case 3: breaking a bond
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");

                }
            }
        }
    };



    //    ----------------------------------------------------------------------
    //    ------------------------ Bluetooth Functions -------------------------
    //    ----------------------------------------------------------------------


    //Creates Bluetooth Socket and connects to it. Returns socket if successful or null otherwise.
    BluetoothSocket createSocket(BluetoothDevice device) {
        BluetoothSocket socket;
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")); //FIXME change this is prob wrong
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
            return null;
        }

        try {
            socket.connect();
            Log.d(TAG, "Connected to " + socket.getRemoteDevice().getName());
            Toast.makeText(getApplicationContext(), "Connected to " + socket.getRemoteDevice().getName(), Toast.LENGTH_SHORT).show();
            return socket;
        } catch (IOException connectException) {
            Toast.makeText(getApplicationContext(), "Error connecting to " + socket.getRemoteDevice().getName(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error connecting to " + socket.getRemoteDevice().getName(), connectException);
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return null;
        }
    }

    void closeSocketConnection(BluetoothSocket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    void sendStringThroughSocket(BluetoothSocket socket, String string) {
        try {
            socket.getOutputStream().write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String readStringFromSocket(BluetoothSocket socket) {
        byte[] mmBuffer = new byte[1024];
        try {
            int size = socket.getInputStream().read(mmBuffer);
            return new String(mmBuffer, 0, size);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    //    ----------------------------------------------------------------------
    //    ---------------------- Notification Functions ------------------------
    //    ----------------------------------------------------------------------


    public void showNotification(String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, builder.build());// fixme change id
    }


}
