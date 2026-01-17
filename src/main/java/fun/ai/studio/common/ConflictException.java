package fun.ai.studio.common;

/**
 * 语义：请求与当前资源状态冲突（例如非法状态流转）。
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}


