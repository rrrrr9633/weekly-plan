package com.weeklyplan.partner;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface PartnerResourceContactRepository extends JpaRepository<PartnerResourceContact,Long> { List<PartnerResourceContact> findByResourceIdOrderByIdDesc(Long resourceId); }
