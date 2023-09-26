package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.arc.config.NativeBuildTime;
import io.quarkus.arc.impl.InjectionPointProvider;
import io.quarkus.runtime.ImageMode;

/**
 * The goal of this interceptor is to verify the current ImageMode when a dependent config property is being injected.
 */
@Priority(jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE)
@Interceptor
@NativeBuildConfigCheck
public class NativeBuildConfigCheckInterceptor {

    private static final Logger LOG = Logger.getLogger(NativeBuildConfigCheckInterceptor.class);

    @Inject
    NativeBuildConfigContext nativeBuildConfigContext;

    @AroundInvoke
    Object aroundInvoke(InvocationContext context) throws Exception {
        verifyCurrentImageMode(nativeBuildConfigContext.getBuildAndRunTimeFixed());
        return context.proceed();
    }

    static void verifyCurrentImageMode(Set<String> buildAndRunTimeFixed) {
        if (ImageMode.current() != ImageMode.NATIVE_BUILD) {
            return;
        }
        InjectionPoint injectionPoint = InjectionPointProvider.get();
        if (injectionPoint != null) {
            // Skip injection points annotated with NativeBuildTime
            Annotated annotated = injectionPoint.getAnnotated();
            if (annotated != null && annotated.isAnnotationPresent(NativeBuildTime.class)) {
                return;
            }
            // Skip BUILD_AND_RUN_TIME_FIXED properties
            if (!buildAndRunTimeFixed.isEmpty()) {
                String propertyName = null;
                for (Annotation qualifier : injectionPoint.getQualifiers()) {
                    if (qualifier instanceof ConfigProperty) {
                        propertyName = ((ConfigProperty) qualifier).name();
                    }
                }
                if (propertyName != null && buildAndRunTimeFixed.contains(propertyName)) {
                    return;
                }
            }
        }
        StringBuilder b = new StringBuilder();
        b.append("\n\n");
        b.append("=".repeat(120));
        b.append("\nPOSSIBLE CONFIG INJECTION PROBLEM DETECTED\n");
        b.append("-".repeat(120));
        b.append("\nA config object was injected during the static initialization phase of a native image build.\n");
        b.append(
                "This may result in unexpected errors.\n");
        b.append("The injected value was obtained at native image build time and cannot be updated at runtime.\n\n");
        if (injectionPoint != null) {
            b.append("Injection point: ");
            b.append(injectionPointToString(injectionPoint));
            b.append("\n");
        }
        b.append("Solutions:\n");
        b.append("\t- If that's intentional then annotate the injected field/parameter with @");
        b.append(NativeBuildTime.class.getName());
        b.append(" to eliminate the false positive\n");
        b.append(
                "\t- You can leverage the programmatic lookup to delay the retrieval of a config property; for example '@ConfigProperty(name = \"foo\") Instance<String> foo'\n");
        b.append(
                "\t- You can try to use a normal CDI scope (e.g. @ApplicationScoped) to initialize the bean lazily; this may help if the is only injected but not directly used during the static initialization phase");
        b.append("\n");
        b.append("=".repeat(120));
        b.append("\n\n");

        LOG.error(b.toString());
        throw new IllegalStateException(
                "POSSIBLE CONFIG INJECTION PROBLEM DETECTED: a config object was injected during the static initialization phase of a native image build. See the error message above for more details.");
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
