package io.quarkus.http.ratelimit.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.RateLimitRejectionWriter;
import io.quarkus.http.ratelimit.runtime.internal.RateLimitBuildTimeInfo;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.telemetry.DevModeRejectionLog;
import io.quarkus.http.ratelimit.runtime.internal.telemetry.RateLimitOutcomeListener;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.deployment.spi.ControlPlanePathBuildItem;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.common.annotation.Identifier;

public class HttpRateLimitProcessor {

    private static final Logger LOG = Logger.getLogger(HttpRateLimitProcessor.class);

    private static final String FEATURE = "http-rate-limit";

    private static final DotName IDENTIFIER = DotName.createSimple(Identifier.class);
    private static final DotName RATE_LIMIT_KEY_RESOLVER = DotName.createSimple(RateLimitKeyResolver.class);
    private static final DotName HTTP_AUTHENTICATION_MECHANISM = DotName.createSimple(HttpAuthenticationMechanism.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void devModeRejectionLog(LaunchModeBuildItem launchMode, BuildProducer<AdditionalBeanBuildItem> beans) {
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            // it has no bean-defining annotation, so that the bean archive of the runtime module does not discover it
            beans.produce(AdditionalBeanBuildItem.builder().addBeanClass(DevModeRejectionLog.class)
                    .setDefaultScope(BuiltinScope.SINGLETON.getName()).build());
        }
    }

    /**
     * Fails the build when the rules are invalid, name a key resolver that does not exist, or have a
     * path outside the root path; warns of rules that are valid but probably not what was intended.
     */
    @BuildStep
    void validateConfig(HttpRateLimitBuildTimeConfig config, VertxHttpBuildTimeConfig httpConfig,
            ValidationPhaseBuildItem validationPhase, BuildProducer<ValidationErrorBuildItem> validationErrors) {
        Set<String> keyResolvers = new HashSet<>();
        boolean mechanisms = false;
        for (BeanInfo bean : validationPhase.getContext().beans()) {
            if (bean.hasType(HTTP_AUTHENTICATION_MECHANISM)) {
                mechanisms = true;
            }
            Optional<AnnotationInstance> identifier = bean.getQualifier(IDENTIFIER);
            if (identifier.isEmpty()) {
                continue;
            }
            if (bean.hasType(RATE_LIMIT_KEY_RESOLVER)) {
                keyResolvers.add(identifier.get().value().asString());
            }
        }
        List<String> errors = HttpRateLimitBuildConfigValidator.validate(config, keyResolvers);
        errors.addAll(HttpRateLimitBuildConfigValidator.validateRootPath(config, httpConfig.rootPath(),
                httpConfig.nonApplicationRootPath()));
        for (String warning : HttpRateLimitBuildConfigValidator.warnings(config, httpConfig.auth().proactive(),
                mechanisms)) {
            LOG.warn(warning);
        }
        if (!errors.isEmpty()) {
            validationErrors.produce(new ValidationErrorBuildItem(new ConfigurationException(String.join("\n", errors))));
        }
    }

    // resolvers are looked up programmatically, by the names in the configuration
    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(RateLimitKeyResolver.class, RateLimitRejectionWriter.class,
                RateLimitOutcomeListener.class);
    }

    @BuildStep
    SyntheticBeanBuildItem buildTimeInfo(ServiceRegistrar serviceRegistrar,
            List<ControlPlanePathBuildItem> controlPlanePathItems) {
        Set<String> controlPlanePaths = new HashSet<>();
        for (ControlPlanePathBuildItem item : controlPlanePathItems) {
            LOG.debugf("Exempting %s from HTTP rate limiting: %s", item.getPath(), item.getReason());
            controlPlanePaths.add(item.getPath());
        }
        RateLimitBuildTimeInfo info = new RateLimitBuildTimeInfo(Set.copyOf(controlPlanePaths));

        serviceRegistrar.forService(RateLimitBuildTimeInfo.class)
                .atPhase(Phase.STATIC_INIT)
                .onStart(ctx -> info);

        return SyntheticBeanBuildItem.configure(RateLimitBuildTimeInfo.class)
                .serviceValue(RateLimitBuildTimeInfo.class)
                .done();
    }
}
