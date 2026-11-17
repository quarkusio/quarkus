package io.quarkus.vertx.http.deployment;

import java.util.Map;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.security.EagerSecurityInterceptorCreator;
import io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism;

/**
 * Provides a way for extensions to register eager security interceptor.
 * For example, the Vert.x HTTP extension registers {@link HttpAuthenticationMechanism}
 * and an interceptor that sets annotation value ('@HttpAuthenticationMechanism("basic") => 'basic') as routing context
 * attribute.
 * With disabled proactive authentication, these interceptors are guaranteed to run before any other security code
 * of supported extensions (currently RESTEasy Classic and RESTEasy Reactive).
 * <p>
 * The interceptors are created by the {@link EagerSecurityInterceptorCreator}, which must be registered as a static-init
 * service named with the {@link Class#getName() name} of the interceptor binding annotation, for each of the interceptor
 * bindings.
 */
public final class EagerSecurityInterceptorBindingBuildItem extends MultiBuildItem {

    private final DotName[] annotationBindings;
    private final Map<String, String> bindingToValue;
    /**
     * If this interceptor is always accompanied by {@link io.quarkus.security.spi.runtime.SecurityCheck}.
     * For example, we know that endpoint annotated with {@link HttpAuthenticationMechanism} is always secured.
     */
    private final boolean requiresSecurityCheck;
    private final Function<AnnotationInstance, String> bindingValueExtractor;
    private final boolean allowToRepeatThisInterceptorBinding;

    /**
     *
     * @param interceptorBindings annotation names, 'value' attribute of annotation instances will be passed to the creator
     */
    public EagerSecurityInterceptorBindingBuildItem(DotName... interceptorBindings) {
        this(null, false, interceptorBindings);
    }

    public EagerSecurityInterceptorBindingBuildItem(Function<AnnotationInstance, String> bindingValueExtractor,
            boolean requiresSecurityCheck, DotName... interceptorBindings) {
        this.annotationBindings = interceptorBindings;
        this.bindingToValue = Map.of();
        this.requiresSecurityCheck = requiresSecurityCheck;
        this.bindingValueExtractor = bindingValueExtractor;
        this.allowToRepeatThisInterceptorBinding = false;
    }

    EagerSecurityInterceptorBindingBuildItem(Map<String, String> bindingToValue, DotName... interceptorBindings) {
        this.annotationBindings = interceptorBindings;
        this.bindingToValue = bindingToValue;
        this.requiresSecurityCheck = true;
        this.bindingValueExtractor = null;
        this.allowToRepeatThisInterceptorBinding = true;
    }

    public DotName[] getAnnotationBindings() {
        return annotationBindings;
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
