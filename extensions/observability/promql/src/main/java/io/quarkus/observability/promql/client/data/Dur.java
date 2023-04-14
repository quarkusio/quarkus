package io.quarkus.observability.promql.client.data;

import java.time.Duration;
import java.time.Period;

import com.fasterxml.jackson.annotation.JsonCreator;

@SuppressWarnings({ "checkstyle:CyclomaticComplexity", "checkstyle:NPathComplexity" })
public class Dur {

    private final Period period;
    private final Duration duration;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Dur(Period period, Duration duration) {
        if (period == null && duration == null) {
            throw new IllegalArgumentException("At least one of 'period' or 'duration' should be specified");
        }
        if (period != null) {
            if (period.isNegative() || period.isZero()) {
                throw new IllegalArgumentException("'period' should be positive");
            }
            if (period.getYears() < 0 || period.getMonths() < 0 || period.getDays() < 0) {
                throw new IllegalArgumentException("'period' fields should not be negative");
            }
            if (period.getMonths() > 0) {
                throw new IllegalArgumentException("'period' months field is not supported and should be zero");
            }
        }
        if (duration != null) {
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("'duration' should be positive");
            }
            if (duration.getSeconds() < 0L || duration.getNano() < 0) {
                throw new IllegalArgumentException("'duration' fields should not be negative");
            }
        }
        this.period = period;
        this.duration = duration;
    }

    public Dur(Period period) {
        this(period, null);
    }

    public Dur(Duration duration) {
        this(null, duration);
    }

    public Period getPeriod() {
        return period;
    }

    public Duration getDuration() {
        return duration;
    }

    // ms, s, m, h, d, w, y

    @Override
    public String toString() {
        var sb = new StringBuilder();
        if (period != null) {
            var y = period.getYears();
            var d = period.getDays();
            var w = d / 7;
            d = d % 7;
            if (y > 0)
                sb.append(y).append('y');
            if (w > 0)
                sb.append(w).append('w');
            if (d > 0)
                sb.append(d).append('d');
        }
        if (duration != null) {
            var s = duration.getSeconds();
            var h = s / 3600;
            s = s % 3600;
            var m = s / 60;
            s = s % 60;
            var ms = duration.getNano() / 1000_000;
            if (h > 0)
                sb.append(h).append('h');
            if (m > 0)
                sb.append(m).append('m');
            if (s > 0)
                sb.append(s).append('s');
            if (ms > 0)
                sb.append(ms).append("ms");
        }
        return sb.toString();
    }
}
