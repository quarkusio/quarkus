package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.ExecutionMode;
import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.inject.ConfigProducer;

/**
 * Intercepts the producer methods declared on {@link ConfigProducer} and records the config value during the static
 * initialization phase unless the injection point is annotated with {@link StaticInitSafe}. It's no-op for any other execution
 * mode.
 */
@ConfigStaticInitCheck
@Priority(jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE)
@Interceptor
public class ConfigStaticInitCheckInterceptor {

    private static final Logger LOG = Logger.getLogger(ConfigStaticInitCheckInterceptor.class);

    @Inject
    ConfigStaticInitValues configValues;

    @AroundInvoke
    Object aroundInvoke(InvocationContext context) throws Exception {
        InjectionPoint injectionPoint = null;
        for (Object parameter : context.getParameters()) {
            if (parameter instanceof InjectionPoint) {
                injectionPoint = (InjectionPoint) parameter;
                break;
            }
        }
        recordConfigValue(injectionPoint, configValues);
        return context.proceed();
    }

    static void recordConfigValue(InjectionPoint injectionPoint, ConfigStaticInitValues configValues) {
        if (ExecutionMode.current() != ExecutionMode.STATIC_INIT) {
            // No-op for any other execution mode
            return;
        }
        if (injectionPoint == null) {
            throw new IllegalStateException("No current injection point found");
        }
        ConfigProperty configProperty = null;
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier instanceof ConfigProperty) {
                configProperty = ((ConfigProperty) qualifier);
            }
        }
        if (configProperty == null
                || injectionPoint.getAnnotated().isAnnotationPresent(StaticInitSafe.class)) {
            return;
        }
        String propertyName = configProperty.name();
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        String value = config.getConfigValue(propertyName).getValue();
        if (value == null) {
            value = getDefaultValue(injectionPoint, configProperty);
        }
        if (value == null) {
            LOG.debugf("No config value found for %s - recording <null> value", propertyName);
        }
        if (configValues == null) {
            configValues = Arc.container().instance(ConfigStaticInitValues.class).get();
        }
        configValues.recordConfigValue(injectionPoint, propertyName, value);
    }

    private static String getDefaultValue(InjectionPoint injectionPoint, ConfigProperty configProperty) {
        String str = configProperty.defaultValue();
        if (!ConfigProperty.UNCONFIGURED_VALUE.equals(str)) {
            return str;
        }
        if (injectionPoint.getType() instanceof Class && ((Class<?>) injectionPoint.getType()).isPrimitive()) {
            if (injectionPoint.getType() == char.class) {
                return null;
            } else if (injectionPoint.getType() == boolean.class) {
                return "false";
            } else {
                return "0";
            }
        }
        return null;
    }

}
