
/**
 * @author  mithape
 * @version 1.0
 * @purpose This class is used to encryption and decryption.
 *
 * @History
 * ===============================================================================================================================================
 *     @Version         @Date           @Author                 @Purpose
 * ===============================================================================================================================================
 *     1.0                      15-01-18        Mayur I                 This class is used to encryption and decryption.
 * ===============================================================================================================================================
 *
 */
package com.en.common.util;

import java.io.UnsupportedEncodingException;

import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

public class AesUtil {
    private final int    keySize;
    private final int    iterationCount;
    private final Cipher cipher;

    public AesUtil(int keySize, int iterationCount) {
        this.keySize        = keySize;
        this.iterationCount = iterationCount;

        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (Exception e) {
            throw fail(e);
        }
    }

    public static String base64(byte[] bytes) {
        return Base64.encodeBase64String(bytes);
    }

    public static byte[] base64(String str) {
        return Base64.decodeBase64(str);
    }

    public String decrypt(String salt, String iv, String passphrase, String ciphertext,String dynamicKey) {
        try {
            SecretKey key       = generateKey(salt, passphrase,dynamicKey);
            byte[]    decrypted = doFinal(Cipher.DECRYPT_MODE, key, iv, base64(ciphertext));

            return new String(decrypted, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw fail(e);
        }
    }

    private byte[] doFinal(int encryptMode, SecretKey key, String iv, byte[] bytes) {
        try {
            cipher.init(encryptMode, key, new IvParameterSpec(hex(iv)));

            return cipher.doFinal(bytes);
        } catch (Exception e) {
            throw fail(e);
        }
    }

    public String encrypt(String salt, String iv, String passphrase, String plaintext,String dynamicKey) {
        try {
            SecretKey key       = generateKey(salt, passphrase,dynamicKey);
            byte[]    encrypted = doFinal(Cipher.ENCRYPT_MODE, key, iv, plaintext.getBytes("UTF-8"));

            return base64(encrypted);
        } catch (UnsupportedEncodingException e) {
            throw fail(e);
        }
    }

    private IllegalStateException fail(Exception e) {
        return new IllegalStateException(e);
    }

    private SecretKey generateKey(String salt, String passphrase,String dynamicKey) {
        try {
        	String finalKey=passphrase+dynamicKey;
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec          spec    = new PBEKeySpec(finalKey.toCharArray(), hex(salt), iterationCount, keySize);
            SecretKey        key     = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

            return key;
        } catch (Exception e) {
            throw fail(e);
        }
    }

    public static String hex(byte[] bytes) {
        return Hex.encodeHexString(bytes);
    }

    public static byte[] hex(String str) {
        try {
            return Hex.decodeHex(str.toCharArray());
        } catch (DecoderException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String random(int length) {
        byte[] salt = new byte[length];

        new SecureRandom().nextBytes(salt);

        return hex(salt);
    }
}


//~ Formatted by Jindent --- http://www.jindent.com
