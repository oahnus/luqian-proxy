package com.github.oahnus.proxyserver.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Created by oahnus on 2020-04-18
 * 18:59.
 */
public class AESUtils {
    private static String OFFSET = "cliwn;123va9xt)g";
    private static String CODE_TYPE = "UTF-8";
    private static String SECRET = "clojwna)43:f9c23";

    public static String encrypt(String clearText) {
        try {
            IvParameterSpec ips = new IvParameterSpec(OFFSET.getBytes());
            SecretKeySpec sks = new SecretKeySpec(SECRET.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sks, ips);
            byte[] bytes = cipher.doFinal(clearText.getBytes(CODE_TYPE));
            return new String(Base64.getEncoder().encode(bytes));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String decrypt(String secretText) {
        try {
            byte[] bytes = Base64.getDecoder().decode(secretText);
            IvParameterSpec ips = new IvParameterSpec(OFFSET.getBytes());
            SecretKeySpec sks = new SecretKeySpec(SECRET.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sks, ips);
            return new String(cipher.doFinal(bytes));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void main(String... args) {
        String encrypt = encrypt("123456");
        System.out.println(encrypt);
        System.out.println(decrypt(encrypt));
    }
}
