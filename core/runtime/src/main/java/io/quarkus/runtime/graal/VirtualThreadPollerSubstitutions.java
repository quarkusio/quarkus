package io.quarkus.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.util.IsLinux;

/**
 * Makes a native executable wake virtual threads blocked on a socket from platform threads.
 * <p>
 * The JDK's default on Linux ({@code jdk.pollerMode=2}, {@code VTHREAD_POLLERS}) delivers a socket's readiness through
 * sub-pollers that are themselves virtual threads. In a native executable such a sub-poller can lose its wakeup for
 * good, and every virtual thread reading a socket it serves then blocks until the socket is closed: GraalVM's class
 * initialization and monitors wait through {@code LockSupport}, so they can consume the unpark meant for the
 * sub-poller (https://github.com/oracle/graal/issues/15051, https://github.com/oracle/graal/issues/13787).
 * <p>
 * With {@code SYSTEM_THREADS} the pollers are platform threads blocked in {@code epoll_wait}, which never park. This is
 * the mode the JDK uses on the other operating systems.
 * <p>
 * Only the default changes: {@code -Djdk.pollerMode=2} at run time still selects the virtual thread pollers.
 * <p>
 * This is a workaround for https://github.com/oracle/graal/issues/15051 and should be removed once Quarkus requires a
 * GraalVM release that fixes it.
 */
final class VirtualThreadPollerSubstitutions {

    @TargetClass(className = "sun.nio.ch.DefaultPollerProvider", onlyWith = IsLinux.class)
    static final class Target_sun_nio_ch_DefaultPollerProvider {

        @Substitute
        Target_sun_nio_ch_Poller_Mode defaultPollerMode() {
            return Target_sun_nio_ch_Poller_Mode.SYSTEM_THREADS;
        }
    }

    @TargetClass(className = "sun.nio.ch.Poller", innerClass = "Mode", onlyWith = IsLinux.class)
    static final class Target_sun_nio_ch_Poller_Mode {

        @Alias
        static Target_sun_nio_ch_Poller_Mode SYSTEM_THREADS;
    }
}
