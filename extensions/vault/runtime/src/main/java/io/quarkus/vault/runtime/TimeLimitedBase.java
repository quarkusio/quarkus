package io.quarkus.vault.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

public abstract class TimeLimitedBase {

    private static final Logger log = Logger.getLogger(TimeLimitedBase.class.getName());

    Supplier<Instant> nowSupplier = () -> Instant.now();
    Instant created = created();

    public boolean renewable;
    public long leaseDurationSecs;

    public TimeLimitedBase(boolean renewable, long leaseDurationSecs) {
        this.renewable = renewable;
        this.leaseDurationSecs = leaseDurationSecs;
    }

    public TimeLimitedBase(TimeLimitedBase other) {
        this.renewable = other.renewable;
        this.leaseDurationSecs = other.leaseDurationSecs;
    }

    public boolean isExpired() {
        return now().isAfter(getExpireInstant());
    }

    public boolean shouldExtend(Duration gracePeriod) {
        return !isExpired() && renewable && now().plus(gracePeriod).isAfter(getExpireInstant());
    }

    private Instant created() {
        return now();
    }

    private Instant now() {
        return nowSupplier.get();
    }

    public Instant getExpireInstant() {
        return created.plusSeconds(leaseDurationSecs);
    }

    public Date getExpiredDate() {
        return new Date(getExpireInstant().toEpochMilli());
    }

    /**
     * true if the lease is smaller than the grace period. as long as we are far away fro the ttl, lease durations will
     * be a constant value (e.g. 3600 secs). if we are less than 'lease duration' away from ttl, lease durations
     * will start to reduce to not go over the ttl.
     *
     * @param gracePeriod
     * @return
     */
    public boolean expiresSoon(Duration gracePeriod) {
        return leaseDurationSecs < gracePeriod.getSeconds();
    }

    public String info() {
        return "renewable: " + renewable + ", leaseDuration: " + leaseDurationSecs + "s, valid_until: " + getExpiredDate();
    }

    public void leaseDurationSanityCheck(String nickname, Duration gracePeriod) {
        if (leaseDurationSecs < gracePeriod.getSeconds()) {
            log.warn(nickname + " lease duration " + leaseDurationSecs
                    + "s is smaller than the renew grace period "
                    + gracePeriod.getSeconds() + "s");
        }
    }

}
