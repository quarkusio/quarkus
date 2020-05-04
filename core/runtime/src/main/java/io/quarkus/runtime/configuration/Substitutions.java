package io.quarkus.runtime.configuration;

import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.threadlocal.FastThreadLocalFactory;
import com.oracle.svm.core.threadlocal.FastThreadLocalInt;

import io.smallrye.config.Expressions;

/**
 */
final class Substitutions {
    // 0 = expand so that the default value is to expand
    static final FastThreadLocalInt notExpanding = FastThreadLocalFactory.createInt();

    @TargetClass(ConfigProviderResolver.class)
    static final class Target_ConfigurationProviderResolver {

        @Alias
        @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
        private static volatile ConfigProviderResolver instance;
    }

    @TargetClass(Expressions.class)
    static final class Target_Expressions {
        @Delete
        private static ThreadLocal<Boolean> ENABLE;

        @Substitute
        private static boolean isEnabled() {
            return notExpanding.get() == 0;
        }

        @Substitute
        public static <T> T withoutExpansion(Supplier<T> supplier) {
            if (isEnabled()) {
                notExpanding.set(1);
                try {
                    return supplier.get();
                } finally {
                    notExpanding.set(0);
                }
            } else {
                return supplier.get();
            }
        }
    }
}
