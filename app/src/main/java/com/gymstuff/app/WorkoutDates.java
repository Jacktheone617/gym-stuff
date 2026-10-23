package com.gymstuff.app;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

final class WorkoutDates {
    static long monday(long time, ZoneId zone) {
        return Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone).toInstant().toEpochMilli();
    }

    static boolean eligible(long sessionTime, long workoutTime, boolean previousWeek, ZoneId zone) {
        if (sessionTime >= workoutTime) return false;
        if (!previousWeek) return true;
        long end = monday(workoutTime, zone);
        long start = Instant.ofEpochMilli(end).atZone(zone).minusWeeks(1).toInstant().toEpochMilli();
        return sessionTime >= start && sessionTime < end;
    }
}
