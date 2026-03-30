package io.quarkus.smallrye.reactivemessaging.kafka.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Substitution for {@code KafkaShareGroupCommit} when the Kafka client version
 * does not include {@code ShareConsumer.acquisitionLockTimeoutMs()} (added in kafka-clients 4.2.0).
 * <p>
 * Without this substitution, GraalVM's {@code --link-at-build-time} fails with an
 * {@code UnresolvedElementException} because the method reference cannot be resolved.
 */
final class ShareGroupSubstitutions {

    static final class IsAcquisitionLockTimeoutMsMissing implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("org.apache.kafka.clients.consumer.ShareConsumer")
                        .getMethod("acquisitionLockTimeoutMs");
                return false;
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                return true;
            }
        }
    }
}

@TargetClass(className = "io.smallrye.reactive.messaging.kafka.commit.KafkaShareGroupCommit", onlyWith = ShareGroupSubstitutions.IsAcquisitionLockTimeoutMsMissing.class)
final class Target_io_smallrye_reactive_messaging_kafka_commit_KafkaShareGroupCommit {

    @Alias
    private void startRenewTimer() {
    }

    /**
     * Substituted to avoid the unresolvable call to
     * {@code ShareConsumer.acquisitionLockTimeoutMs()} in kafka-clients &lt; 4.2.0.
     * The timer is restarted but no lock renewal or timeout checking is performed.
     */
    @Substitute
    private void renewAndCheckTimeout(long ignored) {
        startRenewTimer();
    }
}
