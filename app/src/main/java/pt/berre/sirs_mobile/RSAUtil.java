package pt.berre.sirs_mobile;


import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

class RSAUtil {

    String encrypt(String data, String rsaPublicKeyString) {



        byte[] encryptedBytes = null;

        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");

            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.decode(rsaPublicKeyString, Base64.DEFAULT));
            RSAPublicKey rsaPublicKey = (RSAPublicKey) kf.generatePublic(keySpecX509);

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA256AndMGF1Padding");

            cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
            encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
    }
}