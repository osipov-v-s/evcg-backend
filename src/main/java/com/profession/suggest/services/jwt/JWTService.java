package com.profession.suggest.services.jwt;

import com.profession.suggest.configuration.properties.EnvPropertiesConfig;
import com.profession.suggest.database.entities.auth.Account;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class JWTService {
    private final Environment env;

    public JWTService(Environment env) {
        this.env = env;
    }

    public String generateToken(String id){
        Map<String, Object> claims = new HashMap<>();
        return "Bearer " + createToken(claims, id);
    }
    public String generateTokenWithAccountInfo(Account account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", account.getEmail());
        claims.put("firstLogin", account.getFirstLogin());
        List<String> activeRoles = account.getRoles().stream()
                .map(role -> role.getName())
                .filter(role -> role.isActive())
                .map(Enum::name)
                .sorted()
                .toList();
        claims.put("roles", activeRoles);
        return "Bearer " + createToken(claims, String.valueOf(account.getId()));
    }
    private String createToken(Map<String, Object> claims, String subject){
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000*60*60*24))
                .signWith(SignatureAlgorithm.HS256, env.getProperty(EnvPropertiesConfig.SECRET_KEY).getBytes())
                .compact();
    }
    private boolean isTokenExpired(String token){
        return extractAllClaims(token).getExpiration().before(new Date());
    }
    public boolean isValid(String token) {
        try {
            return extractAllClaims(token).getSubject() != null && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }

    }
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(env.getProperty(EnvPropertiesConfig.SECRET_KEY).getBytes());
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token.replace("Bearer ", "")).getBody();
    }
}
