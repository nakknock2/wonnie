package com.woori.woonie.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendarSignalRepository extends JpaRepository<CalendarSignal, String> {
    List<CalendarSignal> findAllByCustomerIdOrderByStartsOnAsc(String customerId);
}
