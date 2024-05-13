package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.function.Function;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.smallrye.reactivemessaging.runtime.HealthCenterFilterConfig.HealthCenterConfig;
import io.smallrye.reactive.messaging.health.HealthReport;

@Interceptor
@HealthCenterFilter
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 5)
public class HealthCenterInterceptor {

    private final HealthCenterFilterConfig healthCenterFilterConfig;

    @Inject
    public HealthCenterInterceptor(HealthCenterFilterConfig healthCenterFilterConfig) {
        this.healthCenterFilterConfig = healthCenterFilterConfig;
    }

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        if (ctx.getMethod().getName().equals("getReadiness")) {
            HealthReport result = (HealthReport) ctx.proceed();
            return applyFilter(result, HealthCenterConfig::readinessEnabled);
        }
        if (ctx.getMethod().getName().equals("getLiveness")) {
            HealthReport result = (HealthReport) ctx.proceed();
            return applyFilter(result, HealthCenterConfig::livenessEnabled);
        }
        if (ctx.getMethod().getName().equals("getStartup")) {
            HealthReport result = (HealthReport) ctx.proceed();
            return applyFilter(result, HealthCenterConfig::startupEnabled);
        }

        return ctx.proceed();
    }

    private HealthReport applyFilter(HealthReport result, Function<HealthCenterConfig, Boolean> filterType) {
        if (healthCenterFilterConfig.health().isEmpty()) {
            return result;
        }
        HealthReport.HealthReportBuilder builder = HealthReport.builder();
        for (HealthReport.ChannelInfo channel : result.getChannels()) {
            HealthCenterConfig config = healthCenterFilterConfig.health().get(channel.getChannel());
            if (config != null) {
                if (config.enabled() && filterType.apply(config)) {
                    builder.add(channel);
                }
            } else {
                builder.add(channel);
            }
        }
        return builder.build();
    }
}
