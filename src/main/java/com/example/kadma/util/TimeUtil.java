package com.example.kadma.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Deterministic, timezone-aware day/time helpers used by duties and catch-up logic. */
public final class TimeUtil {

    private TimeUtil() {
    }

    public static ZoneId zoneFor(int timezoneOffsetMinutes) {
        int safe = Math.max(-14 * 60, Math.min(14 * 60, timezoneOffsetMinutes));
        return ZoneOffset.ofTotalSeconds(safe * 60);
    }

    public static LocalDateTime now(int timezoneOffsetMinutes) {
        return LocalDateTime.now(zoneFor(timezoneOffsetMinutes));
    }

    /** Stable increasing day number used for every daily counter in the mod. */
    public static long dayIndex(LocalDateTime moment) {
        return moment.toLocalDate().toEpochDay();
    }

    public static long currentDay(int timezoneOffsetMinutes) {
        return dayIndex(now(timezoneOffsetMinutes));
    }

    public static LocalDateTime nextReset(int timezoneOffsetMinutes, int resetHour) {
        int hour = Math.max(0, Math.min(23, resetHour));
        LocalDateTime moment = now(timezoneOffsetMinutes);
        LocalDate date = moment.toLocalDate();
        LocalDateTime candidate = date.atTime(hour, 0);
        return candidate.isAfter(moment) ? candidate : candidate.plusDays(1);
    }

    public static long secondsUntilNextReset(int timezoneOffsetMinutes, int resetHour) {
        LocalDateTime moment = now(timezoneOffsetMinutes);
        return java.time.Duration.between(moment, nextReset(timezoneOffsetMinutes, resetHour)).getSeconds();
    }

    /** Human readable "3h 12m" style duration. */
    public static String humaniseSeconds(long seconds) {
        if (seconds <= 0) {
            return "0с";
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder builder = new StringBuilder();
        if (hours > 0) {
            builder.append(hours).append("ч ");
        }
        if (minutes > 0 || hours > 0) {
            builder.append(minutes).append("м ");
        }
        builder.append(secs).append("с");
        return builder.toString();
    }
}
