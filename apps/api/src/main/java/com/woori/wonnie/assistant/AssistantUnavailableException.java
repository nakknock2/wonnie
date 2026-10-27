package com.woori.wonnie.assistant;
public class AssistantUnavailableException extends RuntimeException {
    private final String stage;

    public AssistantUnavailableException(Throwable cause) {
        this("UNKNOWN", cause);
    }

    public AssistantUnavailableException(String stage, Throwable cause) {
        super("금융 상담 연결에 문제가 있어요. API·MCP 서버와 OpenAI 설정을 확인한 뒤 다시 시도해 주세요.",cause);
        this.stage = stage;
    }

    public String getStage() {
        return stage;
    }
}
