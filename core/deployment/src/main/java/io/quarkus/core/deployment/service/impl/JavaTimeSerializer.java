package io.quarkus.core.deployment.service.impl;

import java.io.IOException;
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

import io.smallrye.serial.Serialized;
import io.smallrye.serial.spi.ObjectSerializer;

/**
 * A custom {@link ObjectSerializer} for {@code smallrye-serial} that intercepts
 * {@code java.time} date/time/zone objects, producing a {@link SerializedJavaTime}
 * instead of attempting standard Java serialization.
 * <p>
 * This runs at {@code PRIORITY_REPLACE + 1} to intercept the objects before
 * {@code writeReplace()} would convert them into the package-private
 * {@code java.time.Ser} externalizable proxy. By handling them directly, we
 * avoid the need to parse the binary externalizable protocol and instead use
 * the public API to decompose each object into its constituent field values.
 * <p>
 * Note: {@link ZoneOffset} must be matched before {@link ZoneId} because
 * {@code ZoneOffset} extends {@code ZoneId}.
 */
final class JavaTimeSerializer implements ObjectSerializer {

    /**
     * Singleton instance.
     */
    static final JavaTimeSerializer INSTANCE = new JavaTimeSerializer();

    private JavaTimeSerializer() {
    }

    @Override
    public Serialized serialize(Context ctxt, Object object) throws IOException {
        return switch (object) {
            case Duration d -> new SerializedJavaTime.OfDuration(d.getSeconds(), d.getNano());
            case Instant i -> new SerializedJavaTime.OfInstant(i.getEpochSecond(), i.getNano());
            case LocalDate ld -> new SerializedJavaTime.OfLocalDate(ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
            case LocalTime lt -> new SerializedJavaTime.OfLocalTime(lt.getHour(), lt.getMinute(), lt.getSecond(),
                    lt.getNano());
            case LocalDateTime ldt -> new SerializedJavaTime.OfLocalDateTime(ldt.getYear(), ldt.getMonthValue(),
                    ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano());
            // ZoneOffset before ZoneId: ZoneOffset extends ZoneId
            case ZoneOffset zo -> new SerializedJavaTime.OfZoneOffset(zo.getTotalSeconds());
            case ZonedDateTime zdt -> new SerializedJavaTime.OfZonedDateTime(zdt.getYear(), zdt.getMonthValue(),
                    zdt.getDayOfMonth(), zdt.getHour(), zdt.getMinute(), zdt.getSecond(), zdt.getNano(),
                    zdt.getOffset().getTotalSeconds(), zdt.getZone().getId());
            case OffsetTime ot -> new SerializedJavaTime.OfOffsetTime(ot.getHour(), ot.getMinute(), ot.getSecond(),
                    ot.getNano(), ot.getOffset().getTotalSeconds());
            case OffsetDateTime odt -> new SerializedJavaTime.OfOffsetDateTime(odt.getYear(), odt.getMonthValue(),
                    odt.getDayOfMonth(), odt.getHour(), odt.getMinute(), odt.getSecond(), odt.getNano(),
                    odt.getOffset().getTotalSeconds());
            case Year y -> new SerializedJavaTime.OfYear(y.getValue());
            case YearMonth ym -> new SerializedJavaTime.OfYearMonth(ym.getYear(), ym.getMonthValue());
            case MonthDay md -> new SerializedJavaTime.OfMonthDay(md.getMonthValue(), md.getDayOfMonth());
            case Period p -> new SerializedJavaTime.OfPeriod(p.getYears(), p.getMonths(), p.getDays());
            case ZoneId z -> new SerializedJavaTime.OfZoneId(z.getId());
            default -> ctxt.next();
        };
    }

    @Override
    public int priority() {
        return PRIORITY_REPLACE + 1;
    }
}
