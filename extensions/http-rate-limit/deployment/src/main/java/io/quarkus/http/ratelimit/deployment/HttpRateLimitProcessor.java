package io.quarkus.http.ratelimit.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.telemetry.DevModeRejectionLog;
import io.quarkus.http.ratelimit.runtime.internal.telemetry.RateLimitOutcomeListener;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.common.annotation.Identifier;

public class HttpRateLimitProcessor {

    private static final Logger LOG = Logger.getLogger(HttpRateLimitProcessor.class);

    private static final String FEATURE = "http-rate-limit";

    private static final DotName IDENTIFIER = DotName.createSimple(Identifier.class);
    private static final DotName RATE_LIMIT_KEY_RESOLVER = DotName.createSimple(RateLimitKeyResolver.class);

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
     * Fails the build when the rules are invalid or name a key resolver that does not exist.
     */
    @BuildStep
    void validateConfig(HttpRateLimitBuildTimeConfig config, ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {
        Set<String> keyResolvers = new HashSet<>();
        for (BeanInfo bean : validationPhase.getContext().beans()) {
            Optional<AnnotationInstance> identifier = bean.getQualifier(IDENTIFIER);
            if (identifier.isEmpty()) {
                continue;
            }
            if (bean.hasType(RATE_LIMIT_KEY_RESOLVER)) {
                keyResolvers.add(identifier.get().value().asString());
            }
        }
        List<String> errors = HttpRateLimitBuildConfigValidator.validate(config, keyResolvers);
        if (!errors.isEmpty()) {
            validationErrors.produce(new ValidationErrorBuildItem(new ConfigurationException(String.join("\n", errors))));
        }
    }

    // resolvers are looked up programmatically, by the names in the configuration
    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(RateLimitKeyResolver.class, RateLimitOutcomeListener.class);
    }
}
