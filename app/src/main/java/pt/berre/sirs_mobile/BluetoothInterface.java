package pt.berre.sirs_mobile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

interface BluetoothInterface {

    void connectionCreatedHandler(BluetoothSocket s);

    void receiveData(String data);

    boolean closeSocketConnection();

    boolean sendStringThroughSocket(String string);

    void sendKeyThroughSocketRSA(String key);

    void disconnect();

    void connect();
}
