package io.quarkus.hibernate.validator.runtime.clockprovider;

import java.time.ZoneId;

/**
 * A helper class holding a system timezone.
 * <p>
 * It is reloaded at runtime to provide the runtime-system time zone
 * to the constraints based on a {@link jakarta.validation.ClockProvider}.
 * <p>
 * Note, that we do not hold the timezone constant in the clock provider itself as we need to "reinitialize" this class,
 * so that the timezone is set to the actual runtime-system-timezone.
 * Having a constant in the clock provider and asking to reload the provider class leads to native build failure:
 * <p>
 * <em>
 * Error: An object of type 'io.quarkus.hibernate.validator.runtime.clockprovider.HibernateValidatorClockProvider' was found in
 * the image heap. This type, however, is marked for initialization at image run time for the following reason: classes are
 * initialized at run time by default.
 * This is not allowed for correctness reasons: All objects that are stored in the image heap must be initialized at build time.
 * </em>
 * <p>
 * And we do have instances of the clock provider/clock in the Hibernate Validator metadata as we eagerly initialize
 * constraints.
 */
class HibernateValidatorClockProviderSystemZoneIdHolder {
    static final ZoneId SYSTEM_ZONE_ID = ZoneId.systemDefault();
}
