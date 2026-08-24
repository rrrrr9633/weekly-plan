package com.weeklyplan.partner;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface PartnerResourceFollowUpRepository extends JpaRepository<PartnerResourceFollowUp,Long> { List<PartnerResourceFollowUp> findByResourceIdOrderByFollowUpDateDescCreatedAtDesc(Long resourceId); }
