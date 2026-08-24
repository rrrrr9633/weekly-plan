package com.weeklyplan.partner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PartnerResourceRepository extends JpaRepository<PartnerResource, Long> { List<PartnerResource> findByCompanyIdOrderByPreferredDescUpdatedAtDesc(Long companyId); }
