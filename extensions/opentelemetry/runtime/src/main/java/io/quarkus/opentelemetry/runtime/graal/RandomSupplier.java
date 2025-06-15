package io.quarkus.opentelemetry.runtime.graal;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.opentelemetry.sdk.internal.RandomSupplier")
final class RandomSupplier {

    @Substitute
    public static Supplier<Random> platformDefault() {
        // removed delegation to AndroidFriendlyRandomHolder (which has a Random constant), making it effectively
        // unreachable
        return ThreadLocalRandom::current;
    }
}
