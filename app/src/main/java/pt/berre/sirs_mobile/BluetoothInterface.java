package pt.berre.sirs_mobile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

interface BluetoothInterface {

    void connectionHandler(BluetoothSocket s);

    void receiveData(String data);

}
