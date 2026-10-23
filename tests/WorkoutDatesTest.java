package com.gymstuff.app;

import java.time.*;

public class WorkoutDatesTest {
    static final ZoneId ZONE = ZoneId.of("Europe/London");
    static long time(String value) { return LocalDateTime.parse(value).atZone(ZONE).toInstant().toEpochMilli(); }
    static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
    public static void main(String[] args) {
        long now = time("2026-10-23T17:00");
        check(WorkoutDates.monday(now, ZONE) == time("2026-10-19T00:00"), "Monday start");
        check(WorkoutDates.eligible(time("2026-10-22T17:00"), now, false, ZONE), "Most recent workout");
        check(!WorkoutDates.eligible(time("2026-10-22T17:00"), now, true, ZONE), "Exclude this week");
        check(WorkoutDates.eligible(time("2026-10-12T00:00"), now, true, ZONE), "Include previous Monday");
        check(WorkoutDates.eligible(time("2026-10-18T23:59"), now, true, ZONE), "Include previous Sunday");
        check(!WorkoutDates.eligible(time("2026-10-11T23:59"), now, true, ZONE), "Exclude earlier weeks");
        check(!WorkoutDates.eligible(now, now, false, ZONE), "Exclude current workout");
        check(!WorkoutDates.eligible(now + 1000, now, false, ZONE), "Exclude future workouts");
        long dst = time("2026-10-26T12:00");
        check(WorkoutDates.eligible(time("2026-10-19T00:00"), dst, true, ZONE), "DST week start");
        check(!WorkoutDates.eligible(time("2026-10-18T23:59"), dst, true, ZONE), "DST previous week boundary");
        check(WorkoutDates.eligible(time("2026-10-25T23:59"), dst, true, ZONE), "DST week end");
        System.out.println("11 workout date checks passed.");
    }
}
