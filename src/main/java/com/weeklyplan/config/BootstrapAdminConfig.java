package com.weeklyplan.config;

import com.weeklyplan.user.AppUser;
import com.weeklyplan.user.Role;
import com.weeklyplan.user.RoleRepository;
import com.weeklyplan.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdminConfig {
  @Bean
  CommandLineRunner bootstrapAdmin(
    UserRepository users,
    RoleRepository roles,
    PasswordEncoder passwords,
    @Value("${app.bootstrap-admin.username}") String username,
    @Value("${app.bootstrap-admin.password}") String password
  ) {
    return args -> {
      if (users.existsByUsername(username)) return;
      Role adminRole = roles.findByCode("ADMIN").orElseThrow();
      users.save(AppUser.create("ADMIN-001", username, passwords.encode(password), "系统总台", adminRole));
    };
  }
}
