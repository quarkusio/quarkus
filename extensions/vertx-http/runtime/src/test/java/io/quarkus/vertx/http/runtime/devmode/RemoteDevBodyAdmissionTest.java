package io.quarkus.vertx.http.runtime.devmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.MemorySize;

class RemoteDevBodyAdmissionTest {

    @Test
    void configuredLimitControlsRequestAndAggregateCapacity() {
        RemoteDevBodyLimits limits = RemoteDevBodyLimits.from(Optional.of(MemorySize.of(100)));

        assertThat(limits.requestLimit()).isEqualTo(100);
        assertThat(limits.aggregateLimit()).isEqualTo(200);
        assertThat(limits.activeCollectorLimit()).isEqualTo(4);
    }

    @Test
    void absentLimitUsesFiniteFallback() {
        RemoteDevBodyLimits limits = RemoteDevBodyLimits.from(Optional.empty());

        assertThat(limits.requestLimit()).isEqualTo(10L * 1024 * 1024);
        assertThat(limits.aggregateLimit()).isEqualTo(20L * 1024 * 1024);
    }

    @Test
    void aggregateCapacityIsInclusiveAndReleasedIdempotently() {
        RemoteDevBodyAdmission admission = new RemoteDevBodyAdmission(new RemoteDevBodyLimits(100, 200, 4));
        RemoteDevBodyAdmission.Reservation first = admission.tryAcquire(100);
        RemoteDevBodyAdmission.Reservation second = admission.tryAcquire(100);

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        RemoteDevBodyAdmission.Reservation unknown = admission.tryAcquire(0);
        assertThat(unknown).isNotNull();
        assertThat(admission.tryReserve(unknown, 1)).isFalse();
        assertThat(admission.activeCollectors()).isEqualTo(3);
        assertThat(admission.reservedBytes()).isEqualTo(200);

        admission.release(first);
        admission.release(first);

        assertThat(admission.activeCollectors()).isEqualTo(2);
        assertThat(admission.reservedBytes()).isEqualTo(100);
        assertThat(admission.tryAcquire(100)).isNotNull();
    }

    @Test
    void unknownLengthReservationCannotCrossAggregateCapacity() {
        RemoteDevBodyAdmission admission = new RemoteDevBodyAdmission(new RemoteDevBodyLimits(100, 150, 4));
        RemoteDevBodyAdmission.Reservation known = admission.tryAcquire(100);
        RemoteDevBodyAdmission.Reservation unknown = admission.tryAcquire(0);

        assertThat(admission.tryReserve(unknown, 50)).isTrue();
        assertThat(admission.tryReserve(unknown, 1)).isFalse();
        assertThat(admission.reservedBytes()).isEqualTo(150);

        admission.release(known);
        assertThat(admission.tryReserve(unknown, 1)).isTrue();
        assertThat(admission.reservedBytes()).isEqualTo(51);
    }

    @Test
    void activeCollectorLimitIsIndependentOfByteCapacity() {
        RemoteDevBodyAdmission admission = new RemoteDevBodyAdmission(new RemoteDevBodyLimits(100, 1000, 2));
        assertThat(admission.tryAcquire(0)).isNotNull();
        assertThat(admission.tryAcquire(0)).isNotNull();
        assertThat(admission.tryAcquire(0)).isNull();
    }

    @Test
    void invalidLimitsAndReservationsAreRejected() {
        assertThatThrownBy(() -> new RemoteDevBodyLimits(0, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RemoteDevBodyLimits(2, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RemoteDevBodyLimits(1, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);

        RemoteDevBodyAdmission admission = new RemoteDevBodyAdmission(new RemoteDevBodyLimits(10, 20, 1));
        assertThatThrownBy(() -> admission.tryAcquire(11))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
