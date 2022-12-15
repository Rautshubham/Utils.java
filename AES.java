package com.en.common.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {

	private  SecretKeySpec secretKey;
	//private  byte[] key;
	private static Cipher cipher ;

	public void setKey(String myKey) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException {
		/*MessageDigest sha = null;
		key = myKey.getBytes("UTF-8");
		sha = MessageDigest.getInstance("SHA-1");
		key = sha.digest(key);
		key = Arrays.copyOf(key, 16);
		secretKey = new SecretKeySpec(key, "AES");*/
		cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
	}

	public String encrypt(String strToEncrypt,String staticKey,String dynamicKey)
			throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		secretKey=generateKey(staticKey,dynamicKey);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
	}

	public String decrypt(String strToDecrypt,String staticKey,String dynamicKey) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		secretKey=generateKey(staticKey,dynamicKey);
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));

	}
	private SecretKeySpec generateKey(String staticKey, String dynamicKey) {
        try {
        	MessageDigest sha = null;
        	SecretKeySpec secretKey=null;
        	byte[] key=null;
        	String finalKey=staticKey+dynamicKey;
    		key = finalKey.getBytes("UTF-8");
    		sha = MessageDigest.getInstance("SHA-256");
    		key = sha.digest(key);
    		key = Arrays.copyOf(key, 16);
    		secretKey = new SecretKeySpec(key, "AES");
    		return secretKey;
        } catch (Exception e) {
            throw fail(e);
        }
    }
	 private IllegalStateException fail(Exception e) {
	        return new IllegalStateException(e);
	    }
}
