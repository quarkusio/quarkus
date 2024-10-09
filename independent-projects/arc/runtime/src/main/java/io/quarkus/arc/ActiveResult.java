package io.quarkus.arc;

import java.util.Objects;

/**
 * Result of the "is active?" question asked against a synthetic bean.
 * Can say whether the bean {@linkplain #value() is active} and if it is not,
 * what is the {@linkplain #inactiveReason() reason}. Optionally,
 * a {@linkplain #inactiveCause() cause} may be set too.
 */
public final class ActiveResult {
    private static final ActiveResult ACTIVE = new ActiveResult(true, null, null);

    private final boolean value;
    private final String inactiveReason;
    private final ActiveResult inactiveCause;

    /**
     * The synthetic bean in question is active.
     *
     * @return the active result
     */
    public static ActiveResult active() {
        return ACTIVE;
    }

    /**
     * The synthetic bean is question is inactive for given {@code reason}.
     *
     * @param reason the reason why the synthetic bean is inactive; must not be {@code null}
     * @return the inactive result
     */
    public static ActiveResult inactive(String reason) {
        return new ActiveResult(false, Objects.requireNonNull(reason), null);
    }

    /**
     * The synthetic bean is question is inactive for given {@code reason}.
     * The given {@code cause} is an {@link ActiveResult} for an underlying bean
     * that is inactive and causes this bean to also become inactive.
     *
     * @param reason the reason why the synthetic bean is inactive; must not be {@code null}
     * @param cause the cause why the synthetic bean is inactive; may be {@code null},
     *        but when it is not, it must be inactive as well
     * @return the inactive result with a cause
     */
    public static ActiveResult inactive(String reason, ActiveResult cause) {
        if (cause != null && cause.value) {
            throw new IllegalArgumentException("The cause of an inactive result must also be inactive");
        }
        return new ActiveResult(false, Objects.requireNonNull(reason), cause);
    }

    private ActiveResult(boolean value, String inactiveReason, ActiveResult inactiveCause) {
        this.value = value;
        this.inactiveReason = inactiveReason;
        this.inactiveCause = inactiveCause;
    }

    /**
     * Returns whether the synthetic bean in question is active.
     *
     * @return whether the synthetic bean in question is active
     */
    public boolean value() {
        return value;
    }

    /**
     * Returns the reason why the synthetic bean is not active.
     * Returns {@code null} only if the synthetic bean is active.
     *
     * @return the reason why the synthetic bean is not active
     */
    public String inactiveReason() {
        return inactiveReason;
    }

    /**
     * Returns the cause of why the synthetic bean is not active.
     *
     * @return the cause of why the synthetic bean is not active; may be {@code null}
     */
    public ActiveResult inactiveCause() {
        return inactiveCause;
    }
}
