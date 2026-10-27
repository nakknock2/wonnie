package com.woori.wonnie.assistant;

public class AssistantRequestConflictException extends RuntimeException {
    public AssistantRequestConflictException() {
        super("같은 요청 ID가 다른 입력에 이미 사용됐어요.");
    }
}
