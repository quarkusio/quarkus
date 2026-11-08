package io.quarkus.vertx.http.deployment;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism;
import io.vertx.ext.web.RoutingContext;

/**
 * Provides a way for extensions to register eager security interceptor.
 * For example, the Vert.x HTTP extension registers {@link HttpAuthenticationMechanism}
 * and an interceptor that sets annotation value ('@HttpAuthenticationMechanism("basic") => 'basic') as routing context
 * attribute.
 * With disabled proactive authentication, these interceptors are guaranteed to run before any other security code
 * of supported extensions (currently RESTEasy Classic and RESTEasy Reactive).
 */
public final class EagerSecurityInterceptorBindingBuildItem extends MultiBuildItem {

    private static final String LEGACY_CREATOR_ID_PREFIX = "legacy-";

    private final DotName[] annotationBindings;
    private final String creatorId;
    private final Function<String, Consumer<RoutingContext>> legacyInterceptorCreator;
    private final Map<String, String> bindingToValue;
    /**
     * If this interceptor is always accompanied by {@link io.quarkus.security.spi.runtime.SecurityCheck}.
     * For example, we know that endpoint annotated with {@link HttpAuthenticationMechanism} is always secured.
     */
    private final boolean requiresSecurityCheck;
    private final Function<AnnotationInstance, String> bindingValueExtractor;
    private final boolean allowToRepeatThisInterceptorBinding;

    /**
     * @param creatorId name of the {@link EagerSecurityInterceptorCreator} service; the extension must register
     *        a service of the {@link EagerSecurityInterceptorCreator} type with this name in the static init phase
     * @param interceptorBindings annotation names, 'value' attribute of annotation instances will be passed to the creator
     */
    public EagerSecurityInterceptorBindingBuildItem(String creatorId, DotName... interceptorBindings) {
        this(creatorId, null, false, interceptorBindings);
    }

    public EagerSecurityInterceptorBindingBuildItem(String creatorId,
            Function<AnnotationInstance, String> bindingValueExtractor,
            boolean requiresSecurityCheck, DotName... interceptorBindings) {
        this.annotationBindings = interceptorBindings;
        this.creatorId = creatorId;
        this.legacyInterceptorCreator = null;
        this.bindingToValue = Map.of();
        this.requiresSecurityCheck = requiresSecurityCheck;
        this.bindingValueExtractor = bindingValueExtractor;
        this.allowToRepeatThisInterceptorBinding = false;
    }

    /**
     *
     * @param interceptorBindings annotation names, 'value' attribute of annotation instances will be passed to the creator
     * @param interceptorCreator accepts 'value' attribute of {@code interceptorBinding} instances and creates interceptor
     * @deprecated use {@link #EagerSecurityInterceptorBindingBuildItem(String, DotName...)}; the creator must now be
     *             provided by a service as recorders are deprecated
     */
    @Deprecated(since = "4.0")
    public EagerSecurityInterceptorBindingBuildItem(Function<String, Consumer<RoutingContext>> interceptorCreator,
            DotName... interceptorBindings) {
        this(interceptorCreator, null, false, interceptorBindings);
    }

    /**
     * @deprecated use {@link #EagerSecurityInterceptorBindingBuildItem(String, Function, boolean, DotName...)}; the
     *             creator must now be provided by a service as recorders are deprecated
     */
    @Deprecated(since = "4.0")
    public EagerSecurityInterceptorBindingBuildItem(Function<String, Consumer<RoutingContext>> interceptorCreator,
            Function<AnnotationInstance, String> bindingValueExtractor,
            boolean requiresSecurityCheck, DotName... interceptorBindings) {
        this.annotationBindings = interceptorBindings;
        this.creatorId = LEGACY_CREATOR_ID_PREFIX + Arrays.stream(interceptorBindings).map(DotName::toString)
                .collect(Collectors.joining("_")).replace('.', '_');
        this.legacyInterceptorCreator = interceptorCreator;
        this.bindingToValue = Map.of();
        this.requiresSecurityCheck = requiresSecurityCheck;
        this.bindingValueExtractor = bindingValueExtractor;
        this.allowToRepeatThisInterceptorBinding = false;
    }

    EagerSecurityInterceptorBindingBuildItem(String creatorId, Map<String, String> bindingToValue,
            DotName... interceptorBindings) {
        this.annotationBindings = interceptorBindings;
        this.creatorId = creatorId;
        this.legacyInterceptorCreator = null;
        this.bindingToValue = bindingToValue;
        this.requiresSecurityCheck = true;
        this.bindingValueExtractor = null;
        this.allowToRepeatThisInterceptorBinding = true;
    }

    public DotName[] getAnnotationBindings() {
        return annotationBindings;
    }

    String getCreatorId() {
        return creatorId;
    }

    Function<String, Consumer<RoutingContext>> getLegacyInterceptorCreator() {
        return legacyInterceptorCreator;
    }

    public String getBindingValue(AnnotationInstance annotationInstance, DotName annotation,
            AnnotationTarget annotationTarget) {
        if (bindingValueExtractor != null) {
            return bindingValueExtractor.apply(annotationInstance);
        }
        if (bindingToValue.containsKey(annotation.toString())) {
            return bindingToValue.get(annotation.toString());
        }
        if (annotationInstance.value() == null || annotationInstance.value().asString().isBlank()) {
            throw new ConfigurationException("Annotation '" + annotation + "' placed on '"
                    + toTargetName(annotationTarget) + "' must not have blank value");
        }
        return annotationInstance.value().asString();
    }

    public static String toTargetName(AnnotationTarget target) {
        if (target.kind() == AnnotationTarget.Kind.METHOD) {
            return target.asMethod().declaringClass().name().toString() + "#" + target.asMethod().name();
        } else {
            return target.asClass().name().toString();
        }
    }

    boolean requiresSecurityCheck() {
        return requiresSecurityCheck;
    }

    boolean allowToRepeatThisInterceptorBinding() {
        return allowToRepeatThisInterceptorBinding;
    }
}
