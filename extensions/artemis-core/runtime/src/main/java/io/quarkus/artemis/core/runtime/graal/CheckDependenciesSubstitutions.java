package io.quarkus.artemis.core.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

// Epoll and Kqueue are not supported on SVM
@Substitute
@TargetClass(org.apache.activemq.artemis.core.remoting.impl.netty.CheckDependencies.class)
final class CheckDependenciesSubstitutions {

    @Substitute
    public static final boolean isEpollAvailable() {
        return false;
    }

    @Substitute
    public static final boolean isKQueueAvailable() {
        return false;
    }
}
