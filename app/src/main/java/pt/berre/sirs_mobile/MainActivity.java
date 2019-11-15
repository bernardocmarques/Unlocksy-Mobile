package pt.berre.sirs_mobile;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import android.Manifest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ArrayAdapter aAdapter;
    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private ListView listView;
    public ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private Button btnSearch;
    private Button btnGet;
    private static final String TAG = "myTag";

    // Broadcast receiver turning the BT ON and OFF
    private final BroadcastReceiver broadcastReceiver1 = new BroadcastReceiver() {
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
                        Log.d(TAG, "broadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "broadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "broadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    // Broadcast receiver for listing devices that are not yet paired
    private final BroadcastReceiver broadcastReceiver2 = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                aAdapter.notifyDataSetChanged();

            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                btnSearch.setText("Search");
            }
        }
    };

    // Broadcast receiver that detects bond state changes (pairing status changes)
    private final BroadcastReceiver broadcastReceiver3 = new BroadcastReceiver() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnGet = (Button) findViewById(R.id.btnGet);
        listView = (ListView) findViewById(R.id.deviceList);

        aAdapter = new DeviceListAdapter(getApplicationContext(), R.layout.device_adapter_view, devices);
        listView.setAdapter(aAdapter);


        checkLocationPermission();


        if (bAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();

        } else {
            if (!bAdapter.isEnabled()) {
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBTIntent);

                IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(broadcastReceiver1, BTIntent);

            }

            btnSearch.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onClick(View v) {
                    IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    IntentFilter discoverDevicesEnding = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                    registerReceiver(broadcastReceiver2, discoverDevicesIntent);
                    registerReceiver(broadcastReceiver2, discoverDevicesEnding);

                    if (bAdapter.isDiscovering()) {
                        bAdapter.cancelDiscovery();
                        Log.d(TAG, "btnSearch: Canceling discovery.");

                    } else {
                        btnSearch.setText("Stop");
                        devices.clear();
                        aAdapter.notifyDataSetChanged();
                        bAdapter.startDiscovery();
                        Log.d(TAG, "btnSearch: Starting discovery.");
                    }
                }
            });


            btnGet.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onClick(View v) {
                    devices.clear();
                    aAdapter.notifyDataSetChanged();
                    bAdapter.cancelDiscovery();

                    Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();

                    if (pairedDevices.size() > 0) {
                        devices.addAll(pairedDevices);
                        aAdapter.notifyDataSetChanged();
                    }
                }
            });


            // Broadcasts when bond state changes (ie:pairing)
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            registerReceiver(broadcastReceiver3, filter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    bAdapter.cancelDiscovery();

                    BluetoothDevice device = devices.get(position);

                    Log.d(TAG, "OnItemClicked: A device was clicked.");
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();

                    Log.d(TAG, "OnItemClicked: deviceName = " + deviceName);
                    Log.d(TAG, "OnItemClicked: deviceAddress = " + deviceAddress);

                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        //create the bond
                        Log.d(TAG, "Trying to pair with " + deviceName);
                        device.createBond();
                    }
                    Log.d(TAG, "Connecting " + deviceName);
                    BluetoothSocket socket = createSocket(device);
                    Log.d(TAG, "Socket Created");
                    sendStringThroughSocket(socket, "Sup");
                    Toast.makeText(getApplicationContext(), readStringFromSocket(socket), Toast.LENGTH_SHORT).show();
                    closeSocketConnection(socket);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();

        // Don't forget to unregister the receivers.
        unregisterReceiver(broadcastReceiver1);
        unregisterReceiver(broadcastReceiver2);
        unregisterReceiver(broadcastReceiver3);
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {


                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                MY_PERMISSIONS_REQUEST_LOCATION);
                    }
                };

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

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


//   ----------------------------------------------------------------------------
//   -------------------------- Codigo Copiado ----------------------------------
//   ----------------------------------------------------------------------------

//    private class ConnectedThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final InputStream mmInStream;
//        private final OutputStream mmOutStream;
//
//        ConnectedThread(BluetoothSocket socket) {
//            mmSocket = socket;
//            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
//
//            // Get the input and output streams; using temp objects because
//            // member streams are final.
//            try {
//                tmpIn = socket.getInputStream();
//            } catch (IOException e) {
//                Log.e(TAG, "Error occurred when creating input stream", e);
//            }
//            try {
//                tmpOut = socket.getOutputStream();
//            } catch (IOException e) {
//                Log.e(TAG, "Error occurred when creating output stream", e);
//            }
//
//            mmInStream = tmpIn;
//            mmOutStream = tmpOut;
//        }
//
//        public void run() {
//            // mmBuffer store for the stream
//            byte[] mmBuffer = new byte[1024];
//            int numBytes; // bytes returned from read()
//
//            // Keep listening to the InputStream until an exception occurs.
//            while (true) {
//                try {
//                    // Read from the InputStream.
//                    numBytes = mmInStream.read(mmBuffer);
//
//                    Log.d(TAG, mmBuffer.toString());
////
////                    // Send the obtained bytes to the UI activity.
////                    Message readMsg = handler.obtainMessage(
////                            MessageConstants.MESSAGE_READ, numBytes, -1,
////                            mmBuffer);
////                    readMsg.sendToTarget();
//
//                } catch (IOException e) {
//                    Log.d(TAG, "Input stream was disconnected", e);
//                    break;
//                }
//            }
//        }
//
//        // Call this from the main activity to send data to the remote device.
//        void write(byte[] bytes) {
//            try {
//                mmOutStream.write(bytes);
//            } catch (IOException e) {
//                Log.e(TAG, "Error occurred when sending data", e);
//                Toast.makeText(getApplicationContext(),"Couldn't send data to the other device", Toast.LENGTH_SHORT).show();
//            }
//        }
//
//        // Call this method from the main activity to shut down the connection.
//        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "Could not close the connect socket", e);
//            }
//        }
//    }
//
}
