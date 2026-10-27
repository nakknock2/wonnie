package com.woori.wonnie.calendar;

public class CalendarAuthorizationRequiredException extends RuntimeException {
    public CalendarAuthorizationRequiredException() {
        super("Google Calendar를 다시 연결해 주세요.");
    }

    public CalendarAuthorizationRequiredException(Throwable cause) {
        super("Google Calendar를 다시 연결해 주세요.", cause);
    }
}
