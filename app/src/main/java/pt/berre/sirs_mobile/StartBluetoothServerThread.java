package pt.berre.sirs_mobile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

class StartBluetoothServerThread extends Thread {
    private static final String TAG = "myTag";

    private BluetoothSocket socket;
    private AppCompatActivity activity;
    private boolean closed = false;



    StartBluetoothServerThread(BluetoothSocket socket, AppCompatActivity activity) {
        this.socket = socket;
        this.activity = activity;
    }

    @Override
    public void run() {
        while (!closed) {
            byte[] mmBuffer = new byte[1024];
            try {
                if (socket!=null && socket.isConnected()) {
                    int size = socket.getInputStream().read(mmBuffer);
                    ((BluetoothInterface) activity).receiveData(new String(mmBuffer, 0, size));
                }
            } catch (IOException e) {
                e.printStackTrace();
                ((BluetoothInterface) activity).disconnect();
                this.closed = true;
            }
        }
    }

}