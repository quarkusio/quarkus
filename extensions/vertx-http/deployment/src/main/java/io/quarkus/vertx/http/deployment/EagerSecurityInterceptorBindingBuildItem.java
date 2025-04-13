package io.quarkus.vertx.http.deployment;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
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

    private final DotName[] annotationBindings;
    private final Function<String, Consumer<RoutingContext>> interceptorCreator;
    private final Map<String, String> bindingToValue;
    /**
     * If this interceptor is always accompanied by {@link io.quarkus.security.spi.runtime.SecurityCheck}.
     * For example, we know that endpoint annotated with {@link HttpAuthenticationMechanism} is always secured.
     */
    private final boolean requiresSecurityCheck;

    /**
     *
     * @param interceptorBindings annotation names, 'value' attribute of annotation instances will be passed to the creator
     * @param interceptorCreator accepts 'value' attribute of {@code interceptorBinding} instances and creates interceptor
     */
    public EagerSecurityInterceptorBindingBuildItem(Function<String, Consumer<RoutingContext>> interceptorCreator,
            DotName... interceptorBindings) {
        this.annotationBindings = interceptorBindings;
        this.interceptorCreator = interceptorCreator;
        this.bindingToValue = Map.of();
        this.requiresSecurityCheck = false;
    }

    EagerSecurityInterceptorBindingBuildItem(Function<String, Consumer<RoutingContext>> interceptorCreator,
            Map<String, String> bindingToValue, DotName... interceptorBindings) {
        this.annotationBindings = interceptorBindings;
        this.interceptorCreator = interceptorCreator;
        this.bindingToValue = bindingToValue;
        this.requiresSecurityCheck = true;
    }

    public DotName[] getAnnotationBindings() {
        return annotationBindings;
    }

    Function<String, Consumer<RoutingContext>> getInterceptorCreator() {
        return interceptorCreator;
    }

    public String getBindingValue(AnnotationInstance annotationInstance, DotName annotation,
            AnnotationTarget annotationTarget) {
        if (bindingToValue.containsKey(annotation.toString())) {
            return bindingToValue.get(annotation.toString());
        }
        AnnotationValue annotationValue = annotationInstance.value();
        if (annotationValue == null) {
            throw new ConfigurationException("Annotation '" + annotation + "' placed on '"
                    + toTargetName(annotationTarget) + "' has no value");
        }
        if (annotationValue.kind() == AnnotationValue.Kind.ARRAY) {
            String[] annotationValues = annotationValue.asStringArray();
            if (annotationValues.length == 0) {
                throw new ConfigurationException("Annotation '" + annotation + "' placed on '"
                        + toTargetName(annotationTarget) + "' has no value");
            }
            return String.join(",", annotationValues);
        }
        if (annotationValue.asString().isBlank()) {
            throw new ConfigurationException("Annotation '" + annotation + "' placed on '"
                    + toTargetName(annotationTarget) + "' must not have blank value");
        }
        return annotationInstance.value().asString();
    }

    private static String toTargetName(AnnotationTarget target) {
        if (target.kind() == AnnotationTarget.Kind.METHOD) {
            return target.asMethod().declaringClass().name().toString() + "#" + target.asMethod().name();
        } else {
            return target.asClass().name().toString();
        }
    }

    boolean requiresSecurityCheck() {
        return requiresSecurityCheck;
    }
}
