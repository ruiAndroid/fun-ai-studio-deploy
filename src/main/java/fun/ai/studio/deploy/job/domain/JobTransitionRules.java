package fun.ai.studio.deploy.job.domain;

/**
 * Job 状态流转规则：仅放领域规则，不依赖 Spring/数据库。
 */
public final class JobTransitionRules {
    private JobTransitionRules() {}

    public static boolean canTransition(JobStatus from, JobStatus to) {
        if (from == null || to == null) return false;
        if (from == to) return true;
        if (from.isTerminal()) return false;

        // PENDING -> RUNNING / CANCELLED
        if (from == JobStatus.PENDING) {
            return to == JobStatus.RUNNING || to == JobStatus.CANCELLED;
        }

        // RUNNING -> SUCCEEDED / FAILED / CANCELLED
        if (from == JobStatus.RUNNING) {
            return to == JobStatus.SUCCEEDED || to == JobStatus.FAILED || to == JobStatus.CANCELLED;
        }

        return false;
    }
}


