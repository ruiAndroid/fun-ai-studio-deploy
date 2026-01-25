package fun.ai.studio.deploy.job;

import fun.ai.studio.deploy.job.application.JobService;
import fun.ai.studio.deploy.job.config.JobExecutionProperties;
import fun.ai.studio.deploy.job.domain.Job;
import fun.ai.studio.deploy.job.domain.JobStatus;
import fun.ai.studio.deploy.job.domain.JobType;
import fun.ai.studio.deploy.job.infrastructure.InMemoryJobRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JobQueueTests {

    @Test
    void should_claim_pending_job_and_move_to_running() {
        JobExecutionProperties props = new JobExecutionProperties();
        props.setMaxRunningSeconds(0); // 测试关注 claim 行为，不启用超时
        JobService service = new JobService(new InMemoryJobRepository(), props);
        service.create(JobType.BUILD_AND_DEPLOY, Map.of("appId", "a1"));

        Job claimed = service.claimNext("runner-1", Duration.ofSeconds(30)).orElseThrow();
        assertEquals(JobStatus.RUNNING, claimed.getStatus());
        assertEquals("runner-1", claimed.getRunnerId());
        assertNotNull(claimed.getLeaseExpireAt());
    }
}


