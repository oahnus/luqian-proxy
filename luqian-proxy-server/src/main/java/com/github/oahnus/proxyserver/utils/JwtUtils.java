package com.github.oahnus.proxyserver.utils;

import com.github.oahnus.proxyserver.entity.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * Created by oahnus on 2020-05-28
 * 13:33.
 */
public class JwtUtils {
    public static final long EXPIRE = 24 * 3600 * 1000; // 1 day
    private static String secret = "secret";

    public static void init(String initSecret) {
        secret = initSecret;
    }

    private static SecretKey getKey() {
        byte[] encodedKey = Base64.getEncoder().encode(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
    }

    public static String sign(SysUser sysUser) {
        if (sysUser == null || sysUser.getUsername() == null) {
            return null;
        }
        Date expire = new Date(System.currentTimeMillis() + EXPIRE);
        return Jwts.builder()
                .claim("username", sysUser.getUsername())
                .setExpiration(expire)
                .signWith(SignatureAlgorithm.HS256, getKey())
                .compact();
    }

    public static String valid(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getKey())
                .parseClaimsJws(token)
                .getBody();
        return claims.get("username", String.class);
    }
}
