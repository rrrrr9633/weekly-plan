package com.weeklyplan.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {
  List<RegistrationRequest> findByStatusOrderByCreatedAtAsc(String status);
  boolean existsByUserIdAndStatus(Long userId, String status);
}
