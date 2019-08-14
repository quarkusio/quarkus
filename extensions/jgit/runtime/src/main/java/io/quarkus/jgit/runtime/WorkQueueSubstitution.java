package io.quarkus.jgit.runtime;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.eclipse.jgit.lib.internal.WorkQueue")
@Substitute
public final class WorkQueueSubstitution {

    private static final ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors
            .newScheduledThreadPool(1);

    @Substitute
    public static ScheduledThreadPoolExecutor getExecutor() {
        return executor;
    }
}
