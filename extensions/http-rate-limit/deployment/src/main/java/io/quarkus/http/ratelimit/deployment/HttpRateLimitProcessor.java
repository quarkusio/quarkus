package io.quarkus.http.ratelimit.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.AmbiguousResolutionException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.LookupConditionsProcessor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanResolver;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.RateLimitRejectionWriter;
import io.quarkus.http.ratelimit.RateLimitResolver;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.RateLimitBuildTimeInfo;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.RateLimitRuleConfig;
import io.quarkus.http.ratelimit.runtime.internal.telemetry.DevModeRejectionLog;
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
    private static final DotName RATE_LIMIT_STORE = DotName.createSimple(RateLimitStore.class);
    private static final DotName RATE_LIMIT_KEY_RESOLVER = DotName.createSimple(RateLimitKeyResolver.class);
    private static final DotName RATE_LIMIT_RESOLVER = DotName.createSimple(RateLimitResolver.class);
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
     * Fails the build when the rules are invalid, name a store, key resolver or limit resolver that does not exist, or have a
     * path outside the root path; warns of rules that are valid but probably not what was intended.
     */
    @BuildStep
    void validateConfig(HttpRateLimitBuildTimeConfig config, VertxHttpBuildTimeConfig httpConfig,
            ValidationPhaseBuildItem validationPhase, CombinedIndexBuildItem index,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {
        Set<String> stores = new HashSet<>();
        Set<String> keyResolvers = new HashSet<>();
        Set<String> limitResolvers = new HashSet<>();
        boolean mechanisms = false;
        for (BeanInfo bean : validationPhase.getContext().beans()) {
            if (bean.hasType(HTTP_AUTHENTICATION_MECHANISM)) {
                mechanisms = true;
            }
            Optional<AnnotationInstance> identifier = bean.getQualifier(IDENTIFIER);
            if (identifier.isEmpty()) {
                continue;
            }
            if (bean.hasType(RATE_LIMIT_STORE)) {
                stores.add(identifier.get().value().asString());
            }
            if (bean.hasType(RATE_LIMIT_KEY_RESOLVER)) {
                keyResolvers.add(identifier.get().value().asString());
            }
            if (bean.hasType(RATE_LIMIT_RESOLVER)) {
                limitResolvers.add(identifier.get().value().asString());
            }
        }
        List<String> errors = HttpRateLimitBuildConfigValidator.validate(config, stores, keyResolvers, limitResolvers);
        validateSelections(config, validationPhase.getBeanResolver(), index.getComputingIndex(), errors);
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

    private static void validateSelections(HttpRateLimitBuildTimeConfig config, BeanResolver resolver, IndexView index,
            List<String> errors) {
        for (Map.Entry<String, HttpRateLimitBuildTimeConfig.Rule> entry : config.rules().entrySet()) {
            HttpRateLimitBuildTimeConfig.Rule rule = entry.getValue();
            String prefix = "quarkus.http.rate-limit.rules." + entry.getKey() + ".";
            String key = RateLimitRuleConfig.key(config, rule);
            if (key != null && key.startsWith("custom:")) {
                validateSelection(resolver, RATE_LIMIT_KEY_RESOLVER, key.substring("custom:".length()),
                        rule.requestKey().isPresent() ? prefix + "request-key" : "quarkus.http.rate-limit.request-key", index,
                        errors);
            }
            rule.limitResolver().or(config::limitResolver).ifPresent(name -> validateSelection(resolver, RATE_LIMIT_RESOLVER,
                    name,
                    rule.limitResolver().isPresent() ? prefix + "limit-resolver" : "quarkus.http.rate-limit.limit-resolver",
                    index, errors));
            String store = RateLimitRuleConfig.store(config, rule);
            if (!"local".equals(store)) {
                validateSelection(resolver, RATE_LIMIT_STORE, store,
                        rule.store().isPresent() ? prefix + "store" : "quarkus.http.rate-limit.store", index, errors);
            }
        }
    }

    private static void validateSelection(BeanResolver resolver, DotName type, String name, String property,
            IndexView index, List<String> errors) {
        Set<BeanInfo> beans = resolver.resolveBeans(ClassType.create(type),
                AnnotationInstance.builder(Identifier.class).add("value", name).build());
        try {
            // Apply CDI resolution, including alternatives and reserve/default beans, just as the runtime lookup does.
            resolver.resolveAmbiguity(beans);
        } catch (AmbiguousResolutionException e) {
            // Lookup conditions use runtime properties. Let CDI select these beans when the application starts.
            if (beans.stream().anyMatch(bean -> hasLookupCondition(bean, index))) {
                return;
            }
            List<String> candidates = beans.stream()
                    .map(bean -> bean.getBeanClass() + " (" + bean.getTarget().map(Object::toString).orElse(bean.toString())
                            + ")")
                    .sorted().toList();
            errors.add(property + "='" + name + "' is ambiguous: matching " + type.local() + " beans " + candidates
                    + "; give the beans distinct @Identifier values or select a CDI alternative");
        }
    }

    private static boolean hasLookupCondition(BeanInfo bean, IndexView index) {
        Set<DotName> visited = new HashSet<>();
        return bean.getTarget().filter(target -> hasLookupCondition(target, index, visited)).isPresent()
                || bean.getStereotypes().stream()
                        .anyMatch(stereotype -> hasLookupCondition(index.getClassByName(stereotype.getName()), index, visited));
    }

    private static boolean hasLookupCondition(AnnotationTarget target, IndexView index, Set<DotName> visited) {
        if (target == null) {
            return false;
        }
        for (AnnotationInstance annotation : target.declaredAnnotations()) {
            if (LookupConditionsProcessor.LOOKUP_BEAN_ANNOTATIONS.contains(annotation.name())) {
                return true;
            }
            if (visited.add(annotation.name())
                    && hasLookupCondition(index.getClassByName(annotation.name()), index, visited)) {
                return true;
            }
        }
        return false;
    }

    // key resolvers, stores and limit resolvers are looked up programmatically, by the names in the configuration
    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(RateLimitKeyResolver.class, RateLimitStore.class,
                RateLimitResolver.class, RateLimitRejectionWriter.class);
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
