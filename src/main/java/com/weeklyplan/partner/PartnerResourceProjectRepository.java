package com.weeklyplan.partner;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface PartnerResourceProjectRepository extends JpaRepository<PartnerResourceProject, PartnerResourceProject.Key> { List<PartnerResourceProject> findByResourceId(Long resourceId); List<PartnerResourceProject> findByProjectId(Long projectId); boolean existsByResourceIdAndProjectId(Long resourceId,Long projectId); void deleteByResourceIdAndProjectId(Long resourceId,Long projectId); }
