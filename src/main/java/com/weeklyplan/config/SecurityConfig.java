package com.weeklyplan.config;

import com.weeklyplan.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.util.List;

@Configuration @EnableWebSecurity
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
  @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwt) throws Exception {
    return http.csrf(csrf -> csrf.disable()).cors(cors -> {}).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/auth/**", "/error").permitAll()
        .requestMatchers("/users/me").authenticated()
        .requestMatchers("/users/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.GET, "/projects/**").authenticated()
        .requestMatchers("/projects/**", "/plans/assign").hasRole("ADMIN")
        .requestMatchers("/plans/**").authenticated()
        .anyRequest().authenticated())
      .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
  }
  @Bean CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration(); config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173", "https://tianxiadiyi.xyz")); config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", config); return new CorsFilter(source);
  }
}
