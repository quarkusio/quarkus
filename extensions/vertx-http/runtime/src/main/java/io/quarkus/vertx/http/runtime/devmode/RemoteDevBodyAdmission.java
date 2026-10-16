package io.quarkus.vertx.http.runtime.devmode;

/**
 * Coordinates aggregate remote-dev request-body capacity across collectors.
 * <p>
 * A reservation starts at the declared content length, or at zero for a chunked request, and grows as chunks arrive.
 * Releasing a reservation is idempotent so every terminal collector path can safely release it.
 */
final class RemoteDevBodyAdmission {

    private final RemoteDevBodyLimits limits;
    private int activeCollectors;
    private long reservedBytes;

    RemoteDevBodyAdmission(RemoteDevBodyLimits limits) {
        this.limits = limits;
    }

    /**
     * Reserves a collector slot and its initially known body size.
     *
     * @return the reservation, or {@code null} when the active-collector or aggregate-capacity limit is reached
     */
    synchronized Reservation tryAcquire(long initialReservation) {
        if (initialReservation < 0 || initialReservation > limits.requestLimit()) {
            throw new IllegalArgumentException("Invalid remote-dev body reservation");
        }
        if (activeCollectors >= limits.activeCollectorLimit()
                || initialReservation > limits.aggregateLimit() - reservedBytes) {
            return null;
        }
        activeCollectors++;
        reservedBytes += initialReservation;
        return new Reservation(initialReservation);
    }

    /**
     * Extends a chunked request reservation before accepting another chunk.
     *
     * @return {@code false} when accepting the chunk would exceed aggregate capacity
     */
    synchronized boolean tryReserve(Reservation reservation, long additionalBytes) {
        if (additionalBytes < 0 || reservation.released) {
            throw new IllegalArgumentException("Invalid remote-dev body reservation");
        }
        if (additionalBytes > limits.aggregateLimit() - reservedBytes) {
            return false;
        }
        reservation.reservedBytes += additionalBytes;
        reservedBytes += additionalBytes;
        return true;
    }

    /**
     * Releases a reservation once its spool has been cleaned up.
     */
    synchronized void release(Reservation reservation) {
        if (reservation.released) {
            return;
        }
        reservation.released = true;
        reservedBytes -= reservation.reservedBytes;
        activeCollectors--;
    }

    synchronized int activeCollectors() {
        return activeCollectors;
    }

    synchronized long reservedBytes() {
        return reservedBytes;
    }

    /**
     * The aggregate capacity held by one collector until its spool is cleaned up.
     */
    static final class Reservation {

        private long reservedBytes;
        private boolean released;

        private Reservation(long reservedBytes) {
            this.reservedBytes = reservedBytes;
        }
    }
}
