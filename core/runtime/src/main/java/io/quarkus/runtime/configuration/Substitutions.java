package io.quarkus.runtime.configuration;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;
import com.oracle.svm.core.threadlocal.FastThreadLocalFactory;
import com.oracle.svm.core.threadlocal.FastThreadLocalInt;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 */
final class Substitutions {

    static final FastThreadLocalInt depth = FastThreadLocalFactory.createInt();

    @TargetClass(ConfigExpander.class)
    static final class Target_ConfigExpander {
        @Delete
        @TargetElement(name = "depth")
        static ThreadLocal<int[]> origDepth = null;

        @Substitute
        private static boolean enter() {
            final int val = depth.get();
            if (val == ConfigExpander.MAX_DEPTH) {
                return false;
            } else {
                depth.set(val + 1);
                return true;
            }
        }

        @Substitute
        private static void exit() {
            depth.set(depth.get() - 1);
        }
    }

    @TargetClass(ConfigProviderResolver.class)
    static final class Target_ConfigurationProviderResolver {

        @Alias
        @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
        private static volatile ConfigProviderResolver instance;
    }

}
