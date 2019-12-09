package pt.berre.sirs_mobile;

import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import java.security.SecureRandom;
import java.util.Date;


public class BluetoothMessage {

    private static final long ONE_SECOND_IN_MILLIS=1000; //millisecs

    String message;
    String nonce;
    Date t1;
    Date t2;




    BluetoothMessage(String message) {
        this.message = message;
        byte[] nonce = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);
        this.nonce = Base64.encodeToString(nonce, Base64.NO_WRAP);
        Date now = new Date();
        this.t1 = new Date(now.getTime() - 30 * ONE_SECOND_IN_MILLIS);
        this.t2 = new Date(now.getTime() + 30 * ONE_SECOND_IN_MILLIS);
    }

    BluetoothMessage(String message, String nonce, long t1, long t2) {
        this.message = message;
        this.nonce = nonce;
        this.t1 = new Date(t1);
        this.t2 = new Date(t2);
    }


    @NonNull
    @Override
    public String toString() {
        return this.message + "," + this.nonce + "," + this.t1.getTime() + "," + this.t2.getTime();
    }
}
