package pt.berre.sirs_mobile;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;
import com.notbytes.barcode_reader.BarcodeReaderActivity;

import java.security.Key;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;


public class MainActivity extends AppCompatActivity {
    static final String TAG = "myTagMain";
    private static final int BARCODE_READER_ACTIVITY_REQUEST = 1208;

    final String AndroidKeyStore = "AndroidKeyStore";
    final String AES_MODE_KEYSTORE = "AES/GCM/NoPadding";
    String KEYCHAIN_KEK_ALIAS = "KeychainKEK";


    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    private Bluetooth bt;

    private AESUtil aesUtil = new AESUtil(256);



    Switch onOffSwitch;
    RelativeLayout relativeLayout1;
    KeyStore keyStore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        onOffSwitch = (Switch) findViewById(R.id.on_off_switch);
        relativeLayout1 = (RelativeLayout) findViewById(R.id.relativeLayout1);

        onOffSwitch.setOnCheckedChangeListener((view, isChecked) -> {

            if (isChecked) {
                Intent launchIntent = BarcodeReaderActivity.getLaunchIntent(MainActivity.this, true, false);
                startActivityForResult(launchIntent, BARCODE_READER_ACTIVITY_REQUEST);
            } else {
                bt.disconnect();
            }

            relativeLayout1.setBackgroundColor(isChecked ? (Color.parseColor("#8bc34a")): (Color.parseColor("#e53935")));
        });

        try {
            keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error getting KeyStore", e);
        }

    }

    void disconnect() {
        runOnUiThread(() -> onOffSwitch.setChecked(false));
    }

    void sendCmd(String cmd) {
        bt.sendStringThroughSocket(cmd);
    }


    private String getKeychainKey(String serverID) {

        String keychainKeySharedPrefID = "keychainKey" + serverID;
        String ivSharedPrefID = "keychainKeyIV" + serverID;


        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String keychainKeyEncryptedBase64 = sharedPref.getString(keychainKeySharedPrefID,null);
        String IVBase64 = sharedPref.getString(ivSharedPrefID,null);

        if (keychainKeyEncryptedBase64==null || IVBase64==null){
            return null;
        }

        byte[] keychainKeyEncrypted =  Base64.decode(keychainKeyEncryptedBase64, Base64.DEFAULT);
        byte[] IV = Base64.decode(IVBase64, Base64.DEFAULT);


        try {
            Key KeychainKEK = keyStore.getKey(KEYCHAIN_KEK_ALIAS, null);

            Cipher c = Cipher.getInstance(AES_MODE_KEYSTORE);
            c.init(Cipher.DECRYPT_MODE, KeychainKEK, new GCMParameterSpec(128, IV));
            byte[] decodedBytes = c.doFinal(keychainKeyEncrypted);

            return Base64.encodeToString(decodedBytes, Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e(TAG, "getKeychainKey: Error decrypting KeychainKEK", e);
        }
        return null;
    }

    private void storeKeychainKey(String keychainKeyBase64, String serverID) {
        byte[] keychainKey =  Base64.decode(keychainKeyBase64, Base64.DEFAULT);

        String keychainKeySharedPrefID = "keychainKey" + serverID;
        String ivSharedPrefID = "keychainKeyIV" + serverID;
        String keychainID = KEYCHAIN_KEK_ALIAS + serverID;

        try {
            if (!keyStore.containsAlias(keychainID)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(keychainID,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setRandomizedEncryptionRequired(false)
                                .build());
                keyGenerator.generateKey();
            }
        } catch (Exception e) {
            Log.e(TAG, "storeKeychainKey: Error generating KeychainKEK", e);
        }

        try {
            Key KeychainKEK = keyStore.getKey(keychainID, null);

            Cipher c = Cipher.getInstance(AES_MODE_KEYSTORE);

            byte[] IV = new byte[12];
            SecureRandom random = new SecureRandom();
            random.nextBytes(IV);

            c.init(Cipher.ENCRYPT_MODE, KeychainKEK, new GCMParameterSpec(128, IV));
            byte[] encodedBytes = c.doFinal(keychainKey);
            String keychainKeyEncryptedBase64 = Base64.encodeToString(encodedBytes, Base64.DEFAULT);


            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(keychainKeySharedPrefID, keychainKeyEncryptedBase64);
            editor.putString(ivSharedPrefID, Base64.encodeToString(IV, Base64.DEFAULT));
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "storeKeychainKey: Error encrypting KeychainKEK", e);
        }
    }

    public String getOrGenerateKeychainKey() {
        String serverID = bt.getServerID();
        String keychainKey = getKeychainKey(serverID);

        if (keychainKey == null) {
            keychainKey = aesUtil.generateNewKeyChainKey();
            storeKeychainKey(keychainKey, serverID);
        }
        return keychainKey;
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
            case "RGK":
                String keychainKey = getOrGenerateKeychainKey();

                sendCmd("SGK " + keychainKey);
                break;
            case "RNK":
                String serverID = bt.getServerID();
                keychainKey = aesUtil.generateNewKeyChainKey();
                storeKeychainKey(keychainKey, serverID);

                sendCmd("SNK " + keychainKey);
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
                String mode = qrcodeData[2];

                BluetoothDevice device = bAdapter.getRemoteDevice(deviceAddress);

                bt = new Bluetooth(device, pubKey, mode,this);


            } catch (IllegalArgumentException e) {
                Toast.makeText(this, "Invalid QR code. Try again.", Toast.LENGTH_SHORT).show();
                disconnect();
            }
        }

    }
}
