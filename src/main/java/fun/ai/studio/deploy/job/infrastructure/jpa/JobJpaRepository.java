package fun.ai.studio.deploy.job.infrastructure.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface JobJpaRepository extends JpaRepository<JobEntity, String> {
    Page<JobEntity> findByStatusOrderByCreateTimeAsc(String status, Pageable pageable);

    Page<JobEntity> findAllByOrderByCreateTimeDesc(Pageable pageable);

    /**
     * 互斥判断：PENDING 永远算“进行中”；RUNNING 仅在 lease 未过期时算“进行中”。
     */
    @Query("""
            select (count(j) > 0) from JobEntity j
            where j.appId = :appId
              and (
                j.status = 'PENDING'
                or (j.status = 'RUNNING' and j.leaseExpireAt is not null and j.leaseExpireAt > :nowEpochMs)
              )
            """)
    boolean existsActiveJobForApp(@Param("appId") String appId, @Param("nowEpochMs") long nowEpochMs);

    /**
     * 找一个“过期租约”的 RUNNING job（用于回收重试）。
     * leaseExpireAt 为空也视为可回收（兜底）。
     */
    @Query("""
            select j from JobEntity j
            where j.status = 'RUNNING'
              and (j.leaseExpireAt is null or j.leaseExpireAt < :nowEpochMs)
            order by j.createTime asc
            """)
    Page<JobEntity> findExpiredRunning(@Param("nowEpochMs") long nowEpochMs, Pageable pageable);
}


