package pt.berre.sirs_mobile;


import android.util.Base64;
import android.util.Log;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

class AESUtil {

    private KeyGenerator keygen;
    private SecretKey key;
    private int keySize;

    AESUtil(int keySize) {
        try {
            keygen = KeyGenerator.getInstance("AES");
            keygen.init(keySize);
            this.keySize = keySize;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

    String generateNewKeyChainKey() {
        return Base64.encodeToString(keygen.generateKey().getEncoded(), Base64.NO_WRAP);
    }

    String generateNewSessionKey() {
        key = keygen.generateKey();
        return Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP);
    }

    String encrypt(String strToEncrypt) {
        byte[] plaintext = strToEncrypt.getBytes();

        // Generating IV.
        byte[] IV = new byte[keySize/16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(IV);

        IvParameterSpec ivSpec = new IvParameterSpec(IV);

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            byte[] cipheredText = cipher.doFinal(plaintext);

            String result = Base64.encodeToString(cipheredText, Base64.NO_WRAP);
            result += " " + Base64.encodeToString(ivSpec.getIV(), Base64.NO_WRAP);

            return result;

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            Log.e("myTag", e.toString());
        }

        return null;
    }

    String decrypt(String strToDecrypt, String ivString) {
        byte[] cipheredText = Base64.decode(strToDecrypt, Base64.NO_WRAP);
        IvParameterSpec ivSpec = new IvParameterSpec(Base64.decode(ivString, Base64.NO_WRAP));

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

            byte[] decryptedText = cipher.doFinal(cipheredText);

            return new String(decryptedText);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        return null;
    }
}


