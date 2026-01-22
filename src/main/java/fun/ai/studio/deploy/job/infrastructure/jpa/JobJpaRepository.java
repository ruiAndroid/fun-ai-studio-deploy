package fun.ai.studio.deploy.job.infrastructure.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobJpaRepository extends JpaRepository<JobEntity, String> {
    Page<JobEntity> findByStatusOrderByCreateTimeAsc(String status, Pageable pageable);

    Page<JobEntity> findAllByOrderByCreateTimeDesc(Pageable pageable);
}


