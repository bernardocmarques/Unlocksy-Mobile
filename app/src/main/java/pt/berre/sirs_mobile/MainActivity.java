package pt.berre.sirs_mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.support.annotation.NonNull;
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
import android.widget.TabHost;
import android.widget.Toast;

import android.Manifest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ArrayAdapter aAdapter;
    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private ListView lstvw;
    public ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private Button btnSearch, btnGet;
    private static final String TAG = "myTag";

    // Broadcast receiver turning the BT ON and OFF
    private final BroadcastReceiver broadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(bAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bAdapter.ERROR);

                switch (state){
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
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                aAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, devices);
                aAdapter.notifyDataSetChanged();
                lstvw.setAdapter(aAdapter);

            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                btnSearch.setText("Search");
            }
        }
    };

    // Broadcast receiver that detects bond state changes (pairing status changes)
    private final BroadcastReceiver broadcastReceiver3 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //3cases
                //case 1: bonded already
                if(device.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                }

                //case 2: creating a bond
                if(device.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");

                }

                //case 3: breaking a bond
                if(device.getBondState() == BluetoothDevice.BOND_NONE){
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
        lstvw = (ListView) findViewById(R.id.deviceList);

        checkLocationPermission();


        if (bAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();

        } else {
            if (!bAdapter.isEnabled()){
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
                        //aAdapter.notifyDataSetChanged();
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

            lstvw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    bAdapter.cancelDiscovery();

                    Log.d(TAG, "OnItemClicked: A device was clicked.");
                    String deviceName = devices.get(position).getName();
                    String deviceAddress = devices.get(position).getAddress();

                    Log.d(TAG, "OnItemClicked: deviceName = " + deviceName);
                    Log.d(TAG, "OnItemClicked: deviceAddress = " + deviceAddress);

                    //create the bond
                    Log.d(TAG, "Trying to pair with " + deviceName);
                    devices.get(position).createBond();
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

    //private class ConnectThread extends Thread {
    //    private final BluetoothSocket mmSocket;
    //    private final BluetoothDevice mmDevice;
//
    //    private ConnectThread(BluetoothDevice device) {
    //        // Use a temporary object that is later assigned to mmSocket
    //        // because mmSocket is final.
    //        BluetoothSocket tmp = null;
    //        mmDevice = device;
//
    //        try {
    //            // Get a BluetoothSocket to connect with the given BluetoothDevice.
    //            // MY_UUID is the app's UUID string, also used in the server code.
    //            tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.randomUUID()); //FIXME change this is prob wrong
    //        } catch (IOException e) {
    //            Log.e(TAG, "Socket's create() method failed", e);
    //        }
    //        mmSocket = tmp;
    //    }
//
    //    public void run() {
//
//
    //        // Cancel discovery because it otherwise slows down the connection.
    //        if(bAdapter.isDiscovering()) {
    //            bAdapter.cancelDiscovery();
    //        }
//
    //        try {
    //            // Connect to the remote device through the socket. This call blocks
    //            // until it succeeds or throws an exception.
    //            Log.d(TAG, "connecting...");
    //            Log.d(TAG, mmSocket.getRemoteDevice().getName());
//
    //            mmSocket.connect();
    //            Log.d(TAG, "connected!!!");
    //        } catch (IOException connectException) {
    //            Log.e(TAG, "erro", connectException);
    //            // Unable to connect; close the socket and return.
    //            try {
    //                mmSocket.close();
    //            } catch (IOException closeException) {
    //                Log.e(TAG, "Could not close the client socket", closeException);
    //            }
    //            return;
    //        }
//
    //        // The connection attempt succeeded. Perform work associated with
    //        // the connection in a separate thread.
    //        Log.d(TAG, "Socket's created");
//
    //        manageMyConnectedSocket(mmSocket);
    //    }
//
    //    // Closes the client socket and causes the thread to finish.
    //    public void cancel() {
    //        try {
    //            mmSocket.close();
    //        } catch (IOException e) {
    //            Log.e(TAG, "Could not close the client socket", e);
    //        }
    //    }
//
    //    void manageMyConnectedSocket(BluetoothSocket mmSocket) {
    //        ConnectedThread thread = new ConnectedThread(mmSocket);
//
    //        thread.run();
    //        thread.write("teste".getBytes());
    //    }
//
    //}

    //private Handler handler; // handler that gets info from Bluetooth service
//
    //// Defines several constants used when transmitting messages between the
    //// service and the UI.
    //private interface MessageConstants {
    //    public static final int MESSAGE_READ = 0;
    //    public static final int MESSAGE_WRITE = 1;
    //    public static final int MESSAGE_TOAST = 2;
//
    //    // ... (Add other message types here as needed.)
    //}
//
    //private class ConnectedThread extends Thread {
    //    private final BluetoothSocket mmSocket;
    //    private final InputStream mmInStream;
    //    private final OutputStream mmOutStream;
    //    private byte[] mmBuffer; // mmBuffer store for the stream
//
    //    public ConnectedThread(BluetoothSocket socket) {
    //        mmSocket = socket;
    //        InputStream tmpIn = null;
    //        OutputStream tmpOut = null;
//
    //        // Get the input and output streams; using temp objects because
    //        // member streams are final.
    //        try {
    //            tmpIn = socket.getInputStream();
    //        } catch (IOException e) {
    //            Log.e(TAG, "Error occurred when creating input stream", e);
    //        }
    //        try {
    //            tmpOut = socket.getOutputStream();
    //        } catch (IOException e) {
    //            Log.e(TAG, "Error occurred when creating output stream", e);
    //        }
//
    //        mmInStream = tmpIn;
    //        mmOutStream = tmpOut;
    //    }
//
    //    public void run() {
    //        mmBuffer = new byte[1024];
    //        int numBytes; // bytes returned from read()
//
    //        // Keep listening to the InputStream until an exception occurs.
    //        while (true) {
    //            try {
    //                // Read from the InputStream.
    //                numBytes = mmInStream.read(mmBuffer);
    //                // Send the obtained bytes to the UI activity.
    //                Message readMsg = handler.obtainMessage(
    //                        MessageConstants.MESSAGE_READ, numBytes, -1,
    //                        mmBuffer);
    //                readMsg.sendToTarget();
    //            } catch (IOException e) {
    //                Log.d(TAG, "Input stream was disconnected", e);
    //                break;
    //            }
    //        }
    //    }
//
    //    // Call this from the main activity to send data to the remote device.
    //    public void write(byte[] bytes) {
    //        try {
    //            mmOutStream.write(bytes);
//
    //            // Share the sent message with the UI activity.
    //            Message writtenMsg = handler.obtainMessage(
    //                    MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
    //            writtenMsg.sendToTarget();
    //        } catch (IOException e) {
    //            Log.e(TAG, "Error occurred when sending data", e);
//
    //            // Send a failure message back to the activity.
    //            Message writeErrorMsg =
    //                    handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
    //            Bundle bundle = new Bundle();
    //            bundle.putString("toast",
    //                    "Couldn't send data to the other device");
    //            writeErrorMsg.setData(bundle);
    //            handler.sendMessage(writeErrorMsg);
    //        }
    //    }
//
    //    // Call this method from the main activity to shut down the connection.
    //    public void cancel() {
    //        try {
    //            mmSocket.close();
    //        } catch (IOException e) {
    //            Log.e(TAG, "Could not close the connect socket", e);
    //        }
    //    }
    //}

}
