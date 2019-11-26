package pt.berre.sirs_mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class ManualSelection extends AppCompatActivity {

    private ArrayAdapter aAdapter;
    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private ListView listView;
    public ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private Button btnSearch;
    private Button btnGet;
    private static final String TAG = "myTag";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_selection);

        // Get views of activity
        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnGet = (Button) findViewById(R.id.btnGet);
        listView = (ListView) findViewById(R.id.deviceList);

        // Create and set adapter for listView
        aAdapter = new DeviceListAdapter(getApplicationContext(), R.layout.device_adapter_view, devices);
        listView.setAdapter(aAdapter);



        // Main Code
        if (bAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();

        } else {
            if (!bAdapter.isEnabled()) {
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBTIntent);

                IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(broadcastReceiverOnOff, BTIntent);
            }

            // Set listener for search button
            btnSearch.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onClick(View v) {
                    IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    IntentFilter discoverDevicesEnding = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                    registerReceiver(broadcastReceiverFound, discoverDevicesIntent);
                    registerReceiver(broadcastReceiverFound, discoverDevicesEnding);

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


            // Set listener for get paired button
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
            registerReceiver(broadcastReceiverBondStatus, filter);

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


                    //-----------------------------------------
                    Intent intent = new Intent(getBaseContext(), BluetoothActivity.class);
                    intent.putExtra("DEVICE", device);
                    startActivity(intent);
                    //-----------------------------------------

//                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//                        //create the bond
//                        Log.d(TAG, "Trying to pair with " + deviceName);
//                        device.createBond();
//                    }
//                    Log.d(TAG, "Connecting " + deviceName);
//                    BluetoothSocket socket = createSocket(device);
//                    Log.d(TAG, "Socket Created");
//                    sendStringThroughSocket(socket, "Sup");
//                    showNotification("Recivied:", readStringFromSocket(socket));
//                    closeSocketConnection(socket);
//                    Log.d(TAG, "here");
                }
            });
        }
    }


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();

        // Don't forget to unregister the receivers.
        unregisterReceiver(broadcastReceiverOnOff);
        unregisterReceiver(broadcastReceiverFound);
        unregisterReceiver(broadcastReceiverBondStatus);
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


    // Broadcast receiver for listing devices that are not yet paired
    private final BroadcastReceiver broadcastReceiverFound = new BroadcastReceiver() {
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
