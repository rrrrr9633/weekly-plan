package com.weeklyplan.auth;

import com.weeklyplan.user.AppUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
  private final SecretKey key;
  private final long accessTokenMinutes;
  public JwtService(@Value("${app.security.jwt-secret}") String secret, @Value("${app.security.access-token-minutes}") long accessTokenMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.accessTokenMinutes = accessTokenMinutes;
  }
  public String createAccessToken(AppUser user) {
    Instant now = Instant.now();
    return Jwts.builder().subject(user.getId().toString()).claim("username", user.getUsername()).claim("role", user.getRole().getCode())
      .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(accessTokenMinutes * 60))).signWith(key).compact();
  }
}
