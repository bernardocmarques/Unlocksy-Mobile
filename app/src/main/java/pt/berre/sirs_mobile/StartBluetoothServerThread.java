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
    private BluetoothInterface btInterface;
    private boolean closed = false;



    StartBluetoothServerThread(BluetoothSocket socket, BluetoothInterface btInterface) {
        this.socket = socket;
        this.btInterface = btInterface;
    }

    @Override
    public void run() {
        while (!closed) {
            byte[] mmBuffer = new byte[1024];
            try {
                if (socket!=null && socket.isConnected()) {
                    int size = socket.getInputStream().read(mmBuffer);
                    btInterface.receiveData(new String(mmBuffer, 0, size));
                }
            } catch (IOException e) {
                e.printStackTrace();
                btInterface.disconnect();
                this.closed = true;
            }
        }
    }

}