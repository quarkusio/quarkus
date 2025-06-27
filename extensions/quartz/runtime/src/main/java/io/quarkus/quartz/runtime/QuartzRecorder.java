package io.quarkus.quartz.runtime;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.quartz.runtime.jdbc.JDBCDataSource;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuartzRecorder {

    public Supplier<QuartzSupport> quartzSupportSupplier(QuartzRuntimeConfig runtimeConfig,
            QuartzBuildTimeConfig buildTimeConfig, Optional<String> driverDialect,
            List<JDBCDataSource> dataSources, Set<String> nonconcurrentMethods) {
        return new Supplier<QuartzSupport>() {
            @Override
            public QuartzSupport get() {
                return new QuartzSupport(runtimeConfig, buildTimeConfig, driverDialect, dataSources,
                        nonconcurrentMethods);
            }
        };
    }

}
