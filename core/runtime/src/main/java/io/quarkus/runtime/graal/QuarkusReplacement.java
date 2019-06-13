package io.quarkus.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.Quarkus;

/**
 * Class that avoids the use of reflection in Quarkus when actually running
 * on Graal.
 * <p>
 * Graal does not seem to like registering the program entry point as available for reflection
 */
@TargetClass(Quarkus.class)
final class QuarkusReplacement {

    @Substitute
    public static void main(String... args) throws Exception {
        GenMain.main(args);
    }

    @TargetClass(className = "io.quarkus.runner.GeneratedMain")
    static final class GenMain {

        @Alias
        public static void main(String... args) throws Exception {

        }

    }

}
