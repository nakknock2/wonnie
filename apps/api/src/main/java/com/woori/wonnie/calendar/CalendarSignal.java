package com.woori.wonnie.calendar;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "calendar_signal")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarSignal {

    @Id
    private String id;
    private String customerId;
    @Column(name = "label") private String title;
    private LocalDate startsOn;
    private LocalDate endsOn;
    private String signalType;
    private BigDecimal confidence;
}
