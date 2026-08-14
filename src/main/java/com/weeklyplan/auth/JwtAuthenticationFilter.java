package com.weeklyplan.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtReader tokens;
  public JwtAuthenticationFilter(JwtReader tokens) { this.tokens = tokens; }
  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    String value = request.getHeader("Authorization");
    if (value == null || !value.startsWith("Bearer ")) { chain.doFilter(request, response); return; }
    try {
      Claims claims = tokens.parse(value.substring(7));
      String role = claims.get("role", String.class);
      var authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (RuntimeException ignored) { SecurityContextHolder.clearContext(); }
    chain.doFilter(request, response);
  }
}
