package org.jboss.shamrock.runtime.graal;

import org.jboss.shamrock.runtime.Shamrock;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * class that avoids the use of reflection in Shamrock when actually running
 * on Graal.
 * <p>
 * Graal does not seem to like registering the program entry point as availble for reflection
 */
@TargetClass(Shamrock.class)
final class ShamrockReplacement {

    @Substitute
    public static void main(String... args) throws Exception {
        GenMain.main(args);
    }


    @TargetClass(className = "org.jboss.shamrock.runner.GeneratedMain")
    static final class GenMain {

        @Alias
        public static void main(String... args) throws Exception {

        }

    }

}
