package pt.berre.sirs_mobile;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class BluetoothActivity extends AppCompatActivity implements BluetoothInterface {

    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private Button btnSend;
    private Button btnDisconnect;
    private EditText sendBox;
    private TextView outputBox;
    private TextView connectionInfo;

    private static final String TAG = "myTag";

    private BluetoothSocket socket;
    private BluetoothDevice device;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        btnSend = findViewById(R.id.btnSend);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        sendBox = findViewById(R.id.sendBox);
        outputBox = findViewById(R.id.outputBox);
        connectionInfo = findViewById(R.id.connectedInfo);

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


            // Broadcasts when bond state changes (ie:pairing)
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            registerReceiver(broadcastReceiverBondStatus, filter); //fixme check if necessary


            device = getIntent().getParcelableExtra("DEVICE");
            String deviceName = device.getName();


            connect();

        }


        btnSend.setOnClickListener(v -> {
            String textToSend = sendBox.getText().toString();

            if (socket == null || !socket.isConnected()) {
                outputBox.append("Error sending text! Socket not connected!\n");
                return;
            }

            if (!textToSend.equals("")) {
                if (sendStringThroughSocket(textToSend)){
                    outputBox.append(String.format("Me: %s\n", textToSend));
                } else {
                    outputBox.append("Error sending text!");
                }
            }

            sendBox.setText("");
        });

        btnDisconnect.setOnClickListener(view -> {

            if (socket == null || !socket.isConnected()) {
                connect();
            } else {
                disconnect();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();

        // Don't forget to unregister the receivers.
        unregisterReceiver(broadcastReceiverOnOff);
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


    void bond() {
        Log.d(TAG, "Trying to pair with " + device.getName());
        device.createBond();
        Log.d(TAG, "Bonded to " + device.getName());
    }

    boolean closeSocketConnection() {
        try {
            socket.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
            return false;
        }
    }

    boolean sendStringThroughSocket(String string) {
        try {
            socket.getOutputStream().write(string.getBytes());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void connectionHandler(BluetoothSocket s) {
        this.socket = s;
        if (s==null) {
            connectionInfo.setText(String.format("Not connected to: %s...", device.getName()));
            outputBox.append("Error connecting to device!\n");
            btnDisconnect.setText("Connect");
        } else {
            connectionInfo.setText(String.format("Connected to: %s...", device.getName()));
            outputBox.append("Connection Created!\n");
            btnDisconnect.setText("Disconnect");

            StartBluetoothServerThread t = new StartBluetoothServerThread(socket, this);  //fixme close this
            t.start();
        }
    }

    @Override
    public void receiveData(String data) {
        outputBox.append(String.format("%s: %s\n", device.getName(), data));
    }

    public void connect() {
        connectionInfo.setText(String.format("Connecting to: %s...", device.getName()));
        CreateBluetoothSocketThread t = new CreateBluetoothSocketThread(device, this);
        t.start();
    }

    @SuppressLint("SetTextI18n")
    public void disconnect() {
        if (closeSocketConnection()) {
            outputBox.append("Connection Closed!\n");
            connectionInfo.setText(String.format("Not connected to: %s...", device.getName()));
        } else {
            outputBox.append("Could not close connection!\n");
        }
        btnDisconnect.setText("Connect");
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


