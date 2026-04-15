package io.quarkus.hibernate.panache.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;

/**
 * Substitutions for PanacheOperations to avoid traversing hibernate-reactive types
 * when hibernate-reactive is not present, preventing native image build failures.
 */
public class PanacheOperationsSubstitutions {

    @TargetClass(className = "io.quarkus.hibernate.panache.runtime.spi.PanacheOperations", onlyWith = NoHibernateReactive.class)
    static final class Target_ReactiveOperationsHolder {

        @Substitute
        static PanacheReactiveOperations getReactiveManaged() {
            throw new UnsupportedOperationException(
                    "Reactive operations are not available. Add the quarkus-hibernate-reactive extension to use them.");
        }

        @Substitute
        static PanacheReactiveOperations getReactiveStateless() {
            throw new UnsupportedOperationException(
                    "Reactive operations are not available. Add the quarkus-hibernate-reactive extension to use them.");
        }
    }

    static class NoHibernateReactive implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("io.quarkus.hibernate.reactive.panache.Panache", false,
                        Thread.currentThread().getContextClassLoader());
                return false; // hibernate-reactive is present, don't apply substitution
            } catch (ClassNotFoundException e) {
                return true; // hibernate-reactive is not present, apply substitution
            }
        }
    }
}
