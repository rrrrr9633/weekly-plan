package com.weeklyplan.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
  Optional<AppUser> findByUsername(String username);
  public boolean existsByUsername(String username);
  boolean existsByUsernameAndIdNot(String username, Long id);
}
