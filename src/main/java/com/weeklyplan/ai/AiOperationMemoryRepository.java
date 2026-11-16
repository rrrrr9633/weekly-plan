package com.weeklyplan.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiOperationMemoryRepository extends JpaRepository<AiOperationMemory, Long> {
  List<AiOperationMemory> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
  void deleteByProposalId(Long proposalId);
}
