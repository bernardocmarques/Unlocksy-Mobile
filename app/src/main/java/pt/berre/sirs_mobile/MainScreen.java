package pt.berre.sirs_mobile;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;
import com.notbytes.barcode_reader.BarcodeReaderActivity;

import java.util.Arrays;
import java.util.concurrent.BlockingDeque;


public class MainScreen extends AppCompatActivity {

    static final String TAG = "myTagMain";
    private static final int BARCODE_READER_ACTIVITY_REQUEST = 1208;

    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private Bluetooth bt;



    Switch onOffSwitch;
    RelativeLayout relativeLayout1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        onOffSwitch = (Switch) findViewById(R.id.on_off_switch);
        relativeLayout1 = (RelativeLayout) findViewById(R.id.relativeLayout1);

        onOffSwitch.setOnCheckedChangeListener((view, isChecked) -> {

            if (isChecked) {
                Intent launchIntent = BarcodeReaderActivity.getLaunchIntent(MainScreen.this, true, false);
                startActivityForResult(launchIntent, BARCODE_READER_ACTIVITY_REQUEST);
            } else {
                bt.disconnect();
            }

            relativeLayout1.setBackgroundColor(isChecked ? (Color.parseColor("#8bc34a")): (Color.parseColor("#e53935")));
        });


    }

    void disconnect() {
        runOnUiThread(() -> onOffSwitch.setChecked(false));
    }

    void sendCmd(String cmd) {
        bt.sendStringThroughSocket(cmd);
    }

    void executeCommand(String command) {
        String[] cmdSplited = command.split(" ");
        String cmd = cmdSplited[0];
        String[] args = Arrays.copyOfRange(cmdSplited, 1, cmdSplited.length);

        switch (cmd) {
            case "RCA":
                int challenge = Integer.parseInt(args[0]);
                sendCmd("SCA " + (challenge + 1));
                break;
            default:
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, "error in  scanning", Toast.LENGTH_SHORT).show();
            disconnect();
            return;
        }

        if (requestCode == BARCODE_READER_ACTIVITY_REQUEST && data != null) {
            Barcode barcode = data.getParcelableExtra(BarcodeReaderActivity.KEY_CAPTURED_BARCODE);


            try {
                String[] qrcodeData = barcode.rawValue.split("\n");
                String deviceAddress = qrcodeData[0];
                String pubKey = qrcodeData[1];

                BluetoothDevice device = bAdapter.getRemoteDevice(deviceAddress);

                bt = new Bluetooth(device, pubKey, this);


            } catch (IllegalArgumentException e) {
                Toast.makeText(this, "Invalid QR code. Try again.", Toast.LENGTH_SHORT).show();
                disconnect();
            }
        }

    }
}
