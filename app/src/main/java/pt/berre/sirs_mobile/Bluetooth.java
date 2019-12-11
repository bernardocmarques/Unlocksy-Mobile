package pt.berre.sirs_mobile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;



public class Bluetooth implements BluetoothInterface {

    private static final String TAG = "myTagBT";

    private BluetoothDevice device;
    private String serverPublicKeyBase64;
    private MainActivity activity;
    private String mode;

    private BluetoothSocket socket;

    private AESUtil aesUtil;
    private RSAUtil rsaUtil;
    private ArrayList<String> nonceList = new ArrayList<>();


    Bluetooth(BluetoothDevice device, String serverPublicKeyBase64, String mode, MainActivity activity) {
        this.device = device;
        this.serverPublicKeyBase64 = serverPublicKeyBase64;
        this.activity = activity;
        this.mode = mode.toLowerCase();

        Log.d(TAG, "Bluetooth: " + mode);

        activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), mode, Toast.LENGTH_SHORT).show());


        this.aesUtil = new AESUtil(256);
        this.rsaUtil = new RSAUtil();

        connect();
    }


    // ------------------------

    private String validateBluetoothMessage(String data) {
        Log.d(TAG, "validateBluetoothMessage: chega");
        BluetoothMessage message;
        try {
            String[] dataSplited = data.split(",");
            String msg = dataSplited[0];
            String nonce = dataSplited[1];
            long t1 = Long.parseLong(dataSplited[2]);
            long t2 = Long.parseLong(dataSplited[3]);
            message = new BluetoothMessage(msg, nonce, t1, t2);
        } catch (Exception e){
            Log.d(TAG, "validateBluetoothMessage: Error creating");
            return null;
        }

        if (nonceList.contains(message.nonce)) {
            Log.d(TAG, "validateBluetoothMessage: Not unique nonce");
            return null;
        } else {
            nonceList.add(message.nonce);
            Date now = new Date();
            Log.d(TAG, "validateBluetoothMessage: Checking Times");
            Log.d(TAG, message.t1.toString());
            Log.d(TAG, now.toString());
            Log.d(TAG, message.t2.toString());
            return message.t1.before(now) && message.t2.after(now) ? message.message : null;
        }
    }

    //    ----------------------------------------------------------------------
    //    ------------------------ Bluetooth Functions -------------------------
    //    ----------------------------------------------------------------------


    @Override
    public void connectionCreatedHandler(BluetoothSocket s) {
        this.socket = s;
        if (s!=null && s.isConnected()) {
            // TODO Connected

            switch (this.mode) {
                case "share":
                    String KCkey = activity.getOrGenerateKeychainKey();
                    sendKeyThroughSocketRSA(KCkey);

                    disconnect();
                    break;
                case "normal":
                default:
                    String key = aesUtil.generateNewSessionKey();
                    sendKeyThroughSocketRSA(key);

                    StartBluetoothServerThread t = new StartBluetoothServerThread(socket, this);
                    t.start();
                    break;
            }
        } else {
            // TODO Not Connected
            activity.disconnect();
        }
    }

    @Override
    public void receiveData(String data) {
        String[] parsedData = data.split(" ");
        String encryptedData = parsedData[0];
        String iv = parsedData[1];
        String decryptedData = validateBluetoothMessage(aesUtil.decrypt(encryptedData, iv));

        if (decryptedData==null) {
            activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), "Invalid Message", Toast.LENGTH_SHORT).show());
        } else {
            activity.executeCommand(decryptedData);
            activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), decryptedData, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public boolean sendStringThroughSocket(String string) {
        try {
            BluetoothMessage message = new BluetoothMessage(string);
            String encryptionResult = aesUtil.encrypt(message.toString());

            socket.getOutputStream().write(encryptionResult.getBytes());

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void sendKeyThroughSocketRSA(String key) {
        try {
            String keyEncrypted = rsaUtil.encrypt(key, serverPublicKeyBase64);
            socket.getOutputStream().write(keyEncrypted.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean closeSocketConnection() {
        try {
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void connect() {
        CreateBluetoothSocketThread t = new CreateBluetoothSocketThread(device, this, activity);
        t.start();
    }

    public void disconnect() {
        if (closeSocketConnection()) {
            activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), "Socket Closed", Toast.LENGTH_SHORT).show());
        } else {
            activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), "Error Closing Socket", Toast.LENGTH_SHORT).show());
        }
        activity.disconnect();
    }
}
