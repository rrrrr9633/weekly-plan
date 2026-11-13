package com.weeklyplan.partner;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PartnerResourceRepository extends JpaRepository<PartnerResource, Long> {
  @EntityGraph(attributePaths = {"roles", "tags"})
  List<PartnerResource> findByCompanyIdOrderByPreferredDescUpdatedAtDesc(Long companyId);
}
