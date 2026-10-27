package com.woori.wonnie.assistant;

public class AssistantRequestNotFoundException extends RuntimeException {
    public AssistantRequestNotFoundException() {
        super("상담 요청을 찾을 수 없어요.");
    }
}
