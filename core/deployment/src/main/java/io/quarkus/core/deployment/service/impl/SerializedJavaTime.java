package io.quarkus.core.deployment.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;

import io.smallrye.serial.Serialized;

/**
 * A {@link Serialized} representation of a {@code java.time} value.
 * <p>
 * Produced by {@link JavaTimeSerializer} for the fourteen standard date/time/zone
 * types in the {@code java.time} package. Each inner record holds the decomposed
 * field values needed to reconstruct the object via its public factory method.
 * <p>
 * Composite types (e.g. {@code ZonedDateTime}) are flattened: their component
 * values are stored directly rather than as nested {@code SerializedJavaTime}
 * instances, so the emitter can construct them in a single sequence of bytecode
 * instructions without intermediate objects on the operand stack management.
 */
sealed abstract class SerializedJavaTime extends Serialized {

    private final int hashCode;
    private String toString;

    private SerializedJavaTime(final int hashCode) {
        this.hashCode = getClass().getName().hashCode() * 31 + hashCode;
    }

    public final int hashCode() {
        return hashCode;
    }

    public final String toString() {
        String toString = this.toString;
        if (toString == null) {
            toString = this.toString = computeToString();
        }
        return toString;
    }

    abstract String computeToString();

    /**
     * {@link Duration}: decomposed into seconds and nanosecond adjustment.
     * Reconstructed via {@code Duration.ofSeconds(long, long)}.
     *
     */
    static final class OfDuration extends SerializedJavaTime {
        private final long seconds;
        private final int nanos;

        /**
         * @param seconds the seconds component
         * @param nanos the nanosecond component (0 to 999,999,999)
         */
        OfDuration(long seconds, int nanos) {
            super(hash(seconds, nanos));
            this.seconds = seconds;
            this.nanos = nanos;
        }

        public long seconds() {
            return seconds;
        }

        public int nanos() {
            return nanos;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfDuration other && equals(other);
        }

        public boolean equals(OfDuration other) {
            return this == other || other != null && seconds == other.seconds && nanos == other.nanos;
        }

        @Override
        String computeToString() {
            return "OfDuration[" +
                    "seconds=" + seconds + ", " +
                    "nanos=" + nanos + ']';
        }
    }

    /**
     * {@link Instant}: decomposed into epoch seconds and nanosecond adjustment.
     * Reconstructed via {@code Instant.ofEpochSecond(long, long)}.
     *
     */
    static final class OfInstant extends SerializedJavaTime {
        private final long seconds;
        private final int nanos;

        /**
         * @param seconds the epoch seconds
         * @param nanos the nanosecond component (0 to 999,999,999)
         */
        OfInstant(long seconds, int nanos) {
            super(hash(seconds, nanos));
            this.seconds = seconds;
            this.nanos = nanos;
        }

        public long seconds() {
            return seconds;
        }

        public int nanos() {
            return nanos;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfInstant other && equals(other);
        }

        public boolean equals(OfInstant other) {
            return this == other || other != null && seconds == other.seconds && nanos == other.nanos;
        }

        @Override
        String computeToString() {
            return "OfInstant[" +
                    "seconds=" + seconds + ", " +
                    "nanos=" + nanos + ']';
        }
    }

    /**
     * {@link LocalDate}: decomposed into year, month, day.
     * Reconstructed via {@code LocalDate.of(int, int, int)}.
     *
     */
    static final class OfLocalDate extends SerializedJavaTime {
        private final int year;
        private final int month;
        private final int day;

        /**
         * @param year the year
         * @param month the month-of-year (1 to 12)
         * @param day the day-of-month (1 to 31)
         */
        OfLocalDate(int year, int month, int day) {
            super(hash(year, month, day));
            this.year = year;
            this.month = month;
            this.day = day;
        }

        public int year() {
            return year;
        }

        public int month() {
            return month;
        }

        public int day() {
            return day;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfLocalDate other && equals(other);
        }

        public boolean equals(OfLocalDate other) {
            return this == other || other != null && year == other.year && month == other.month && day == other.day;
        }

        @Override
        String computeToString() {
            return "OfLocalDate[" +
                    "year=" + year + ", " +
                    "month=" + month + ", " +
                    "day=" + day + ']';
        }
    }

    /**
     * {@link LocalTime}: decomposed into hour, minute, second, nano.
     * Reconstructed via {@code LocalTime.of(int, int, int, int)}.
     *
     */
    static final class OfLocalTime extends SerializedJavaTime {
        private final int hour;
        private final int minute;
        private final int second;
        private final int nano;

        /**
         * @param hour the hour-of-day (0 to 23)
         * @param minute the minute-of-hour (0 to 59)
         * @param second the second-of-minute (0 to 59)
         * @param nano the nanosecond (0 to 999,999,999)
         */
        OfLocalTime(int hour, int minute, int second, int nano) {
            super(hash(hour, minute, second, nano));
            this.hour = hour;
            this.minute = minute;
            this.second = second;
            this.nano = nano;
        }

        public int hour() {
            return hour;
        }

        public int minute() {
            return minute;
        }

        public int second() {
            return second;
        }

        public int nano() {
            return nano;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfLocalTime other && equals(other);
        }

        public boolean equals(OfLocalTime other) {
            return this == other || other != null && hour == other.hour && minute == other.minute && second == other.second
                    && nano == other.nano;
        }

        @Override
        String computeToString() {
            return "OfLocalTime[" +
                    "hour=" + hour + ", " +
                    "minute=" + minute + ", " +
                    "second=" + second + ", " +
                    "nano=" + nano + ']';
        }
    }

    /**
     * {@link LocalDateTime}: flattened date and time components.
     * Reconstructed via {@code LocalDateTime.of(int, int, int, int, int, int, int)}.
     *
     */
    static final class OfLocalDateTime extends SerializedJavaTime {
        private final int year;
        private final int month;
        private final int day;
        private final int hour;
        private final int minute;
        private final int second;
        private final int nano;

        /**
         * @param year the year
         * @param month the month-of-year (1 to 12)
         * @param day the day-of-month (1 to 31)
         * @param hour the hour-of-day (0 to 23)
         * @param minute the minute-of-hour (0 to 59)
         * @param second the second-of-minute (0 to 59)
         * @param nano the nanosecond (0 to 999,999,999)
         */
        OfLocalDateTime(int year, int month, int day, int hour, int minute, int second, int nano) {
            super(hash(year, month, day, hour, minute, second, nano));
            this.year = year;
            this.month = month;
            this.day = day;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
            this.nano = nano;
        }

        public int year() {
            return year;
        }

        public int month() {
            return month;
        }

        public int day() {
            return day;
        }

        public int hour() {
            return hour;
        }

        public int minute() {
            return minute;
        }

        public int second() {
            return second;
        }

        public int nano() {
            return nano;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfLocalDateTime other && equals(other);
        }

        public boolean equals(OfLocalDateTime other) {
            return this == other || other != null && year == other.year && month == other.month && day == other.day
                    && hour == other.hour && minute == other.minute && second == other.second && nano == other.nano;
        }

        @Override
        String computeToString() {
            return "OfLocalDateTime[" +
                    "year=" + year + ", " +
                    "month=" + month + ", " +
                    "day=" + day + ", " +
                    "hour=" + hour + ", " +
                    "minute=" + minute + ", " +
                    "second=" + second + ", " +
                    "nano=" + nano + ']';
        }
    }

    /**
     * {@link ZoneOffset}: decomposed into total offset seconds.
     * Reconstructed via {@code ZoneOffset.ofTotalSeconds(int)}.
     *
     */
    static final class OfZoneOffset extends SerializedJavaTime {
        private final int totalSeconds;

        /**
         * @param totalSeconds the total zone offset in seconds (-64800 to +64800)
         */
        OfZoneOffset(int totalSeconds) {
            super(hash(totalSeconds));
            this.totalSeconds = totalSeconds;
        }

        public int totalSeconds() {
            return totalSeconds;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfZoneOffset other && equals(other);
        }

        public boolean equals(OfZoneOffset other) {
            return this == other || other != null && totalSeconds == other.totalSeconds;
        }

        @Override
        String computeToString() {
            return "OfZoneOffset[" +
                    "totalSeconds=" + totalSeconds + ']';
        }
    }

    /**
     * {@link ZoneId} (non-offset zones, i.e. {@code ZoneRegion}):
     * stored as the zone ID string.
     * Reconstructed via {@code ZoneId.of(String)}.
     *
     */
    static final class OfZoneId extends SerializedJavaTime {
        private final String id;

        /**
         * @param id the zone ID string (e.g. "Europe/Paris")
         */
        OfZoneId(String id) {
            super(id.hashCode());
            this.id = id;
        }

        public String id() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfZoneId other && equals(other);
        }

        public boolean equals(OfZoneId other) {
            return this == other || other != null && id.equals(other.id);
        }

        @Override
        String computeToString() {
            return "OfZoneId[" +
                    "id=" + id + ']';
        }
    }

    /**
     * {@link ZonedDateTime}: flattened date/time, offset, and zone components.
     * Reconstructed via {@code ZonedDateTime.ofStrict(LocalDateTime, ZoneOffset, ZoneId)}.
     *
     */
    static final class OfZonedDateTime extends SerializedJavaTime {
        private final int year;
        private final int month;
        private final int day;
        private final int hour;
        private final int minute;
        private final int second;
        private final int nano;
        private final int offsetTotalSeconds;
        private final String zoneId;

        /**
         * @param year the year
         * @param month the month-of-year (1 to 12)
         * @param day the day-of-month (1 to 31)
         * @param hour the hour-of-day (0 to 23)
         * @param minute the minute-of-hour (0 to 59)
         * @param second the second-of-minute (0 to 59)
         * @param nano the nanosecond (0 to 999,999,999)
         * @param offsetTotalSeconds the offset in seconds
         * @param zoneId the zone ID string
         */
        OfZonedDateTime(int year, int month, int day, int hour, int minute, int second, int nano,
                int offsetTotalSeconds, String zoneId) {
            super(hash(year, month, day, hour, minute, second, nano, offsetTotalSeconds, zoneId.hashCode()));
            this.year = year;
            this.month = month;
            this.day = day;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
            this.nano = nano;
            this.offsetTotalSeconds = offsetTotalSeconds;
            this.zoneId = zoneId;
        }

        public int year() {
            return year;
        }

        public int month() {
            return month;
        }

        public int day() {
            return day;
        }

        public int hour() {
            return hour;
        }

        public int minute() {
            return minute;
        }

        public int second() {
            return second;
        }

        public int nano() {
            return nano;
        }

        public int offsetTotalSeconds() {
            return offsetTotalSeconds;
        }

        public String zoneId() {
            return zoneId;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfZonedDateTime other && equals(other);
        }

        public boolean equals(OfZonedDateTime other) {
            return this == other || other != null && year == other.year && month == other.month && day == other.day
                    && hour == other.hour && minute == other.minute && second == other.second && nano == other.nano
                    && offsetTotalSeconds == other.offsetTotalSeconds && zoneId.equals(other.zoneId);
        }

        @Override
        String computeToString() {
            return "OfZonedDateTime[" +
                    "year=" + year + ", " +
                    "month=" + month + ", " +
                    "day=" + day + ", " +
                    "hour=" + hour + ", " +
                    "minute=" + minute + ", " +
                    "second=" + second + ", " +
                    "nano=" + nano + ", " +
                    "offsetTotalSeconds=" + offsetTotalSeconds + ", " +
                    "zoneId=" + zoneId + ']';
        }
    }

    /**
     * {@link OffsetTime}: flattened time and offset components.
     * Reconstructed via {@code OffsetTime.of(LocalTime, ZoneOffset)}.
     *
     */
    static final class OfOffsetTime extends SerializedJavaTime {
        private final int hour;
        private final int minute;
        private final int second;
        private final int nano;
        private final int offsetTotalSeconds;

        /**
         * @param hour the hour-of-day (0 to 23)
         * @param minute the minute-of-hour (0 to 59)
         * @param second the second-of-minute (0 to 59)
         * @param nano the nanosecond (0 to 999,999,999)
         * @param offsetTotalSeconds the offset in seconds
         */
        OfOffsetTime(int hour, int minute, int second, int nano, int offsetTotalSeconds) {
            super(hash(hour, minute, second, nano, offsetTotalSeconds));
            this.hour = hour;
            this.minute = minute;
            this.second = second;
            this.nano = nano;
            this.offsetTotalSeconds = offsetTotalSeconds;
        }

        public int hour() {
            return hour;
        }

        public int minute() {
            return minute;
        }

        public int second() {
            return second;
        }

        public int nano() {
            return nano;
        }

        public int offsetTotalSeconds() {
            return offsetTotalSeconds;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfOffsetTime other && equals(other);
        }

        public boolean equals(OfOffsetTime other) {
            return other == this || other != null && hour == other.hour && minute == other.minute && second == other.second
                    && nano == other.nano && offsetTotalSeconds == other.offsetTotalSeconds;
        }

        @Override
        String computeToString() {
            return "OfOffsetTime[" +
                    "hour=" + hour + ", " +
                    "minute=" + minute + ", " +
                    "second=" + second + ", " +
                    "nano=" + nano + ", " +
                    "offsetTotalSeconds=" + offsetTotalSeconds + ']';
        }
    }

    /**
     * {@link OffsetDateTime}: flattened date/time and offset components.
     * Reconstructed via {@code OffsetDateTime.of(LocalDateTime, ZoneOffset)}.
     *
     */
    static final class OfOffsetDateTime extends SerializedJavaTime {
        private final int year;
        private final int month;
        private final int day;
        private final int hour;
        private final int minute;
        private final int second;
        private final int nano;
        private final int offsetTotalSeconds;

        /**
         * @param year the year
         * @param month the month-of-year (1 to 12)
         * @param day the day-of-month (1 to 31)
         * @param hour the hour-of-day (0 to 23)
         * @param minute the minute-of-hour (0 to 59)
         * @param second the second-of-minute (0 to 59)
         * @param nano the nanosecond (0 to 999,999,999)
         * @param offsetTotalSeconds the offset in seconds
         */
        OfOffsetDateTime(int year, int month, int day, int hour, int minute, int second, int nano,
                int offsetTotalSeconds) {
            super(hash(year, month, day, hour, minute, second, nano, offsetTotalSeconds));
            this.year = year;
            this.month = month;
            this.day = day;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
            this.nano = nano;
            this.offsetTotalSeconds = offsetTotalSeconds;
        }

        public int year() {
            return year;
        }

        public int month() {
            return month;
        }

        public int day() {
            return day;
        }

        public int hour() {
            return hour;
        }

        public int minute() {
            return minute;
        }

        public int second() {
            return second;
        }

        public int nano() {
            return nano;
        }

        public int offsetTotalSeconds() {
            return offsetTotalSeconds;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfOffsetDateTime other && equals(other);
        }

        public boolean equals(OfOffsetDateTime other) {
            return this == other || other != null && year == other.year && month == other.month && day == other.day
                    && hour == other.hour && minute == other.minute && second == other.second && nano == other.nano
                    && offsetTotalSeconds == other.offsetTotalSeconds;
        }

        @Override
        String computeToString() {
            return "OfOffsetDateTime[" +
                    "year=" + year + ", " +
                    "month=" + month + ", " +
                    "day=" + day + ", " +
                    "hour=" + hour + ", " +
                    "minute=" + minute + ", " +
                    "second=" + second + ", " +
                    "nano=" + nano + ", " +
                    "offsetTotalSeconds=" + offsetTotalSeconds + ']';
        }
    }

    /**
     * {@link Year}: decomposed into the year value.
     * Reconstructed via {@code Year.of(int)}.
     *
     */
    static final class OfYear extends SerializedJavaTime {
        private final int year;

        /**
         * @param year the year
         */
        OfYear(int year) {
            super(hash(year));
            this.year = year;
        }

        public int year() {
            return year;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfYear other && equals(other);
        }

        public boolean equals(OfYear other) {
            return this == other || other != null && year == other.year;
        }

        @Override
        String computeToString() {
            return "OfYear[" +
                    "year=" + year + ']';
        }
    }

    /**
     * {@link YearMonth}: decomposed into year and month.
     * Reconstructed via {@code YearMonth.of(int, int)}.
     *
     */
    static final class OfYearMonth extends SerializedJavaTime {
        private final int year;
        private final int month;

        /**
         * @param year the year
         * @param month the month-of-year (1 to 12)
         */
        OfYearMonth(int year, int month) {
            super(hash(year, month));
            this.year = year;
            this.month = month;
        }

        public int year() {
            return year;
        }

        public int month() {
            return month;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfYearMonth other && equals(other);
        }

        public boolean equals(OfYearMonth other) {
            return this == other || other != null && year == other.year && month == other.month;
        }

        @Override
        String computeToString() {
            return "OfYearMonth[" +
                    "year=" + year + ", " +
                    "month=" + month + ']';
        }
    }

    /**
     * {@link MonthDay}: decomposed into month and day.
     * Reconstructed via {@code MonthDay.of(int, int)}.
     *
     */
    static final class OfMonthDay extends SerializedJavaTime {
        private final int month;
        private final int day;

        /**
         * @param month the month-of-year (1 to 12)
         * @param day the day-of-month (1 to 31)
         */
        OfMonthDay(int month, int day) {
            super(hash(month, day));
            this.month = month;
            this.day = day;
        }

        public int month() {
            return month;
        }

        public int day() {
            return day;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfMonthDay other && equals(other);
        }

        public boolean equals(OfMonthDay other) {
            return this == other || other != null && this.month == other.month && this.day == other.day;
        }

        @Override
        String computeToString() {
            return "OfMonthDay[" +
                    "month=" + month + ", " +
                    "day=" + day + ']';
        }
    }

    /**
     * {@link Period}: decomposed into years, months, days.
     * Reconstructed via {@code Period.of(int, int, int)}.
     *
     */
    static final class OfPeriod extends SerializedJavaTime {
        private final int years;
        private final int months;
        private final int days;

        /**
         * @param years the years component
         * @param months the months component
         * @param days the days component
         */
        OfPeriod(int years, int months, int days) {
            super(hash(years, months, days));
            this.years = years;
            this.months = months;
            this.days = days;
        }

        public int years() {
            return years;
        }

        public int months() {
            return months;
        }

        public int days() {
            return days;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OfPeriod other && equals(other);
        }

        public boolean equals(OfPeriod other) {
            return this == other || other != null && years == other.years && months == other.months && days == other.days;
        }

        @Override
        String computeToString() {
            return "OfPeriod[" +
                    "years=" + years + ", " +
                    "months=" + months + ", " +
                    "days=" + days + ']';
        }
    }

    // helpers

    static int hash(long... values) {
        return Arrays.hashCode(values);
    }
}
