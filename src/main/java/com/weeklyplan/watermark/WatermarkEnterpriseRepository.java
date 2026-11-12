package com.weeklyplan.watermark;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface WatermarkEnterpriseRepository extends JpaRepository<WatermarkEnterprise, Long> {
  List<WatermarkEnterprise> findByCompanyIdOrderByCityAscCountyAscNameAsc(Long companyId);
  Optional<WatermarkEnterprise> findByCompanyIdAndNameAndCityAndCounty(Long companyId, String name, String city, String county);
}
