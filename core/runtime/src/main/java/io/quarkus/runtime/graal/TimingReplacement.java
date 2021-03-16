package io.quarkus.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.bootstrap.runner.Timing;

@TargetClass(Timing.class)
final class TimingReplacement {

    @Alias
    private static volatile long bootStartTime;

    @Substitute
    public static void mainStarted() {
        bootStartTime = System.nanoTime();
    }

}
