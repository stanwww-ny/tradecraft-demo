package io.tradecraft.common.domain.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class TradingCalendar {
    // Hardcode for MVP: 16:00 New York
    private static final ZoneId ZONE = ZoneId.of("America/New_York");
    private static final LocalTime EOD = LocalTime.of(16, 0);

    private TradingCalendar() {
    }

    public static Instant endOfDay(long tsNanos) {
        Instant now = Instant.ofEpochSecond(0, tsNanos);
        ZonedDateTime zdt = now.atZone(ZONE);
        LocalDate date = zdt.toLocalDate();
        ZonedDateTime eod = ZonedDateTime.of(date, EOD, ZONE);
        return eod.toInstant();
    }
}

