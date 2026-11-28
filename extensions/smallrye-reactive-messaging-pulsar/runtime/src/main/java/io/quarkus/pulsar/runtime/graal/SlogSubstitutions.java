package io.quarkus.pulsar.runtime.graal;

import java.time.Clock;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.github.merlimat.slog.Logger;

/**
 * Pulsar 5.x logs through slog, which picks Log4j2 or SLF4J at runtime. Quarkus does not ship
 * Log4j2 core and routes SLF4J to JBoss LogManager, so slog always uses its SLF4J backend.
 * Always create the SLF4J logger so that the Log4j2 backend, which references Log4j2 classes
 * that are not on the classpath, is not linked into the native image.
 */
@TargetClass(className = "io.github.merlimat.slog.impl.LoggerDiscovery", onlyWith = IsSlogPresent.class)
final class Target_io_github_merlimat_slog_impl_LoggerDiscovery {

    @Substitute
    static Logger createLogger(String name, Target_io_github_merlimat_slog_impl_AttrChain attrs, Clock clock) {
        return (Logger) (Object) new Target_io_github_merlimat_slog_impl_Slf4jLogger(name, attrs, clock);
    }
}

@TargetClass(className = "io.github.merlimat.slog.impl.Slf4jLogger", onlyWith = IsSlogPresent.class)
final class Target_io_github_merlimat_slog_impl_Slf4jLogger {

    @Alias
    Target_io_github_merlimat_slog_impl_Slf4jLogger(String name, Target_io_github_merlimat_slog_impl_AttrChain attrs,
            Clock clock) {
    }
}

@TargetClass(className = "io.github.merlimat.slog.impl.AttrChain", onlyWith = IsSlogPresent.class)
final class Target_io_github_merlimat_slog_impl_AttrChain {
}

final class IsSlogPresent implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("io.github.merlimat.slog.impl.LoggerDiscovery", false,
                    Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
