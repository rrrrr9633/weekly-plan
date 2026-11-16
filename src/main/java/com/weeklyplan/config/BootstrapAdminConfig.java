package com.weeklyplan.config;

import com.weeklyplan.company.CompanyRepository;
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
    CompanyRepository companies,
    UserRepository users,
    RoleRepository roles,
    PasswordEncoder passwords,
    @Value("${app.bootstrap-admin.username}") String username,
    @Value("${app.bootstrap-admin.password}") String password,
    @Value("${app.bootstrap-super-admin.username:}") String superAdminUsername,
    @Value("${app.bootstrap-super-admin.password:}") String superAdminPassword,
    @Value("${app.bootstrap-super-admin.display-name:超级管理员}") String superAdminDisplayName
  ) {
    return args -> {
      Role adminRole = roles.findByCode("ADMIN").orElseThrow();
      if (!users.existsByUsername(username)) {
        AppUser admin = AppUser.create("ADMIN-001", username, passwords.encode(password), "系统总台", adminRole);
        admin.setCompany(companies.findByCode("LIAONING_GUQI_DATA").orElseThrow());
        users.save(admin);
      }
      if (!superAdminUsername.isBlank() && !superAdminPassword.isBlank()) {
        Role superAdminRole = roles.findByCode("SUPER_ADMIN").orElseThrow();
        AppUser superAdmin = users.findByUsername(superAdminUsername).orElseGet(() ->
            AppUser.create("SUPER-001", superAdminUsername, passwords.encode(superAdminPassword), superAdminDisplayName, superAdminRole));
        superAdmin.update(superAdminUsername, superAdminDisplayName, superAdminRole);
        superAdmin.setCompany(null);
        users.save(superAdmin);
      }
    };
  }
}
