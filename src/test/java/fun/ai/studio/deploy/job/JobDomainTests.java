package fun.ai.studio.deploy.job;

import fun.ai.studio.common.ConflictException;
import fun.ai.studio.deploy.job.domain.Job;
import fun.ai.studio.deploy.job.domain.JobStatus;
import fun.ai.studio.deploy.job.domain.JobType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JobDomainTests {

    @Test
    void should_create_job_pending() {
        Job job = Job.create(JobType.BUILD_AND_DEPLOY, Map.of("appId", "a1"));
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertNotNull(job.getId());
        assertNotNull(job.getCreatedAt());
    }

    @Test
    void should_allow_pending_to_running_to_succeeded() {
        Job job = Job.create(JobType.BUILD_AND_DEPLOY, Map.of());
        job = job.claim("runner-1", java.time.Instant.now().plusSeconds(30));
        assertEquals(JobStatus.RUNNING, job.getStatus());
        job = job.succeed();
        assertEquals(JobStatus.SUCCEEDED, job.getStatus());
    }

    @Test
    void should_reject_terminal_transition() {
        Job job = Job.create(JobType.BUILD_AND_DEPLOY, Map.of())
                .claim("runner-1", java.time.Instant.now().plusSeconds(30))
                .succeed();
        assertThrows(ConflictException.class, job::cancel);
    }
}


