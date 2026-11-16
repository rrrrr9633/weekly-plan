package com.weeklyplan.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
  List<AppUser> findByCompanyId(Long companyId);
  boolean existsByCompanyId(Long companyId);
  Optional<AppUser> findByUsername(String username);
  boolean existsByUsername(String username);
  boolean existsByUsernameAndIdNot(String username, Long id);
}
