package com.weeklyplan.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AiOperationProposalRepository extends JpaRepository<AiOperationProposal, Long> {
  Optional<AiOperationProposal> findByIdAndRequestedById(Long id, Long userId);
  List<AiOperationProposal> findByRequestedByIdOrderByCreatedAtDesc(Long userId);
}
