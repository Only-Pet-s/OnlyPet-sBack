package com.op.back.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // access token 용
    private final long AT_EXPIRATION_TIME = 1000 * 60 * 30;

    // refresh token 용
    private final long RT_EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 14;

    public String createToken(String uid, String email){
        return Jwts.builder()
                .setSubject(uid)
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + AT_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();
    }

    public String createRefreshToken(String uid){
        return Jwts.builder()
                .setSubject(uid)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + RT_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();
    }

    public Claims getClaims(String token){
        return Jwts.parser()
                .setSigningKey(jwtSecret.getBytes())
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token){
        try{
            getClaims(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public String getEmail(String token){
        return (String) getClaims(token).get("email");
    }

    public String getUid(String token){
        return getClaims(token).getSubject();
    }
}
