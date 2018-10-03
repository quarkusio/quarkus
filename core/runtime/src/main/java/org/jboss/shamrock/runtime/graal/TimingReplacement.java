package org.jboss.shamrock.runtime.graal;

import org.jboss.shamrock.runtime.Timing;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(Timing.class)
final class TimingReplacement {

    @Alias
    private static volatile long bootStartTime = -1;

    @Substitute
    public static void mainStarted() {
        bootStartTime = System.nanoTime();
    }

}
