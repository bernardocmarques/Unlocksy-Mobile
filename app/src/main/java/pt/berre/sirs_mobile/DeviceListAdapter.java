package pt.berre.sirs_mobile;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    private LayoutInflater mLayoutInflater;
    private ArrayList<BluetoothDevice> mDevices;
    private int  mViewResourceId;

    DeviceListAdapter(Context context, int tvResourceId, ArrayList<BluetoothDevice> devices){
        super(context, tvResourceId,devices);
        this.mDevices = devices;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = tvResourceId;
    }

    @SuppressLint({"ViewHolder", "SetTextI18n"})
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        convertView = mLayoutInflater.inflate(mViewResourceId, null);

        BluetoothDevice device = mDevices.get(position);

        if (device != null) {
            TextView deviceName = (TextView) convertView.findViewById(R.id.deviceName);
            TextView deviceAddress = (TextView) convertView.findViewById(R.id.deviceAddress);
            TextView deviceBondStatus = (TextView) convertView.findViewById(R.id.bondStatus);

            if (deviceName != null) {
                deviceName.setText(device.getName() != null ? device.getName() : "(No Name)");
            }
            if (deviceAddress != null) {
                deviceAddress.setText(device.getAddress());
            }
            if (deviceBondStatus != null) {

                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    deviceBondStatus.setText("Paired");
                    deviceBondStatus.setTextColor(Color.GREEN);
                } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    deviceBondStatus.setText("Not Paired");
                    deviceBondStatus.setTextColor(Color.RED);
                }
            }
        }

        return convertView;
    }

    public BluetoothDevice getDevice(int id) {
        return mDevices.get(id);
    }
}
