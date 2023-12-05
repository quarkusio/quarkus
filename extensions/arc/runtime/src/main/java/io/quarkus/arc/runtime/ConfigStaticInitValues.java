package io.quarkus.arc.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.config.SmallRyeConfig;

/**
 * This is a store for all config values injected (directly or programmatically) during the static init phase.
 * <p>
 * The values are then compared with the current values during the runtime init, i.e. when the application starts. If a mismatch
 * is found the startup fails with an actionable error.
 */
@Singleton
public class ConfigStaticInitValues {

    private final List<InjectedValue> injectedValues = Collections.synchronizedList(new ArrayList<>());

    void recordConfigValue(InjectionPoint injectionPoint, String name, String value) {
        injectedValues.add(new InjectedValue(injectionPoint, name, value));
    }

    void onStart(@Observes @Priority(Integer.MIN_VALUE) StartupEvent event) {
        if (injectedValues.isEmpty()) {
            // No config values were recorded during static init phase
            return;
        }
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        List<String> mismatches = new ArrayList<>();
        for (InjectedValue injectedValue : injectedValues) {
            ConfigValue currentValue = config.getConfigValue(injectedValue.name);
            if (currentValue.getValue() != null
                    && !Objects.equals(currentValue.getValue(), injectedValue.value)) {
                // Config property is set at runtime and the value differs from the value injected during STATIC_INIT bootstrap phase
                mismatches.add(
                        " - the runtime value of '" + injectedValue.name + "' is [" + currentValue.getValue()
                                + "] but the value [" + injectedValue.value
                                + "] was injected into "
                                + injectedValue.injectionPointInfo);
            }
        }
        injectedValues.clear();
        if (!mismatches.isEmpty()) {
            throw new IllegalStateException(
                    "A runtime config property value differs from the value that was injected during the static intialization phase:\n"
                            + String.join("\n", mismatches)
                            + "\n\nIf that's intentional then annotate the injected field/parameter with @io.quarkus.runtime.annotations.StaticInitSafe to eliminate the false positive.");
        }
    }

    private static class InjectedValue {

        private final String injectionPointInfo;
        private final String name;
        private final String value;

        private InjectedValue(InjectionPoint injectionPoint, String name, String value) {
            this.injectionPointInfo = injectionPointToString(injectionPoint);
            this.name = name;
            this.value = value;
        }

    }

    private static String injectionPointToString(InjectionPoint injectionPoint) {
        Annotated annotated = injectionPoint.getAnnotated();
        if (annotated instanceof AnnotatedField) {
            AnnotatedField<?> field = (AnnotatedField<?>) annotated;
            return field.getDeclaringType().getJavaClass().getName() + "#" + field.getJavaMember().getName();
        } else if (annotated instanceof AnnotatedParameter) {
            AnnotatedParameter<?> param = (AnnotatedParameter<?>) annotated;
            if (param.getDeclaringCallable() instanceof AnnotatedConstructor) {
                return param.getDeclaringCallable().getDeclaringType().getJavaClass().getName() + "()";
            } else {
                return param.getDeclaringCallable().getDeclaringType().getJavaClass().getName() + "#"
                        + param.getDeclaringCallable().getJavaMember().getName() + "()";
            }
        }
        return injectionPoint.toString();
    }

}
