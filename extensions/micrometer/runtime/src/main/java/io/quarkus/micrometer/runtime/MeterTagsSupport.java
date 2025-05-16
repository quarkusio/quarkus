package io.quarkus.micrometer.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import io.micrometer.common.annotation.NoOpValueResolver;
import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.common.annotation.ValueResolver;
import io.micrometer.common.util.StringUtils;
import io.micrometer.core.aop.MeterTag;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.quarkus.arc.All;
import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.ClientProxy;

@Singleton
public class MeterTagsSupport {
    private final Map<Class<?>, ValueResolver> valueResolvers;
    private final ValueExpressionResolver valueExpressionResolver;

    public MeterTagsSupport(@All List<ValueResolver> valueResolvers,
            Instance<ValueExpressionResolver> valueExpressionResolver) {
        this.valueResolvers = createValueResolverMap(valueResolvers);
        this.valueExpressionResolver = valueExpressionResolver.isUnsatisfied() ? null : valueExpressionResolver.get();
    }

    Tags getTags(ArcInvocationContext context) {
        return getCommonTags(context)
                .and(getMeterTags(context));
    }

    private Tags getMeterTags(ArcInvocationContext context) {
        List<Tag> tags = new ArrayList<>();
        Method method = context.getMethod();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter methodParameter = parameters[i];
            MeterTag annotation = methodParameter.getAnnotation(MeterTag.class);
            if (annotation != null) {
                Object parameterValue = context.getParameters()[i];

                tags.add(Tag.of(
                        resolveTagKey(annotation, methodParameter.getName()),
                        resolveTagValue(annotation, parameterValue)));
            }
        }
        return Tags.of(tags);
    }

    private static Tags getCommonTags(ArcInvocationContext context) {
        Method method = context.getMethod();
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        return Tags.of("class", className, "method", methodName);
    }

    /*
     * Precedence copied from MeterTagAnnotationHandler
     */
    private String resolveTagValue(MeterTag annotation, Object parameterValue) {
        String value = null;
        if (annotation.resolver() != NoOpValueResolver.class) {
            ValueResolver valueResolver = valueResolvers.get(annotation.resolver());
            value = valueResolver.resolve(parameterValue);
        } else if (StringUtils.isNotBlank(annotation.expression())) {
            if (valueExpressionResolver == null) {
                throw new IllegalArgumentException("No valueExpressionResolver is defined");
            }
            value = valueExpressionResolver.resolve(annotation.expression(), parameterValue);
        } else if (parameterValue != null) {
            value = parameterValue.toString();
        }
        return value == null ? "" : value;
    }

    /*
     * Precedence copied from MeterTagAnnotationHandler
     */
    private static String resolveTagKey(MeterTag annotation, String parameterName) {
        if (StringUtils.isNotBlank(annotation.value())) {
            return annotation.value();
        } else if (StringUtils.isNotBlank(annotation.key())) {
            return annotation.key();
        } else {
            return parameterName;
        }
    }

    private static Map<Class<?>, ValueResolver> createValueResolverMap(List<ValueResolver> valueResolvers) {
        Map<Class<?>, ValueResolver> valueResolverMap = new HashMap<>();
        for (ValueResolver valueResolver : valueResolvers) {
            ValueResolver instance = ClientProxy.unwrap(valueResolver);
            valueResolverMap.put(instance.getClass(), valueResolver);
        }
        return valueResolverMap;
    }
}
