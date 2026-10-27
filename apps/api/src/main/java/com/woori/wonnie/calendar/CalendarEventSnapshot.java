package com.woori.wonnie.calendar;

import java.time.LocalDate;

public record CalendarEventSnapshot(String id, String title, LocalDate startsOn, LocalDate endsOn) {}
