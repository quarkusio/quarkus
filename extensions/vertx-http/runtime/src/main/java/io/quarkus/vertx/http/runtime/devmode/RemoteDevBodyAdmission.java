package io.quarkus.vertx.http.runtime.devmode;

final class RemoteDevBodyAdmission {

    private final RemoteDevBodyLimits limits;
    private int activeCollectors;
    private long reservedBytes;

    RemoteDevBodyAdmission(RemoteDevBodyLimits limits) {
        this.limits = limits;
    }

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

    static final class Reservation {

        private long reservedBytes;
        private boolean released;

        private Reservation(long reservedBytes) {
            this.reservedBytes = reservedBytes;
        }
    }
}
