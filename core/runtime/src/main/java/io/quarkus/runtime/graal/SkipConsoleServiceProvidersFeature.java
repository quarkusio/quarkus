package io.quarkus.runtime.graal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.graalvm.nativeimage.hosted.Feature;

/**
 * Removes {@code jdk.internal.io.JdkConsoleProvider} service providers from the {@code ServiceCatalog} in a similar way to
 * GraalVM's {@code ServiceLoaderFeature} which Quarkus disables by default.
 */
public class SkipConsoleServiceProvidersFeature implements Feature {
    static final HashMap<String, Set<String>> omittedServiceProviders;

    @Override
    public String getDescription() {
        return "Skip unsupported console service providers when quarkus.native.auto-service-loader-registration is false";
    }

    static {
        omittedServiceProviders = new HashMap<>(1);
        omittedServiceProviders.put("jdk.internal.io.JdkConsoleProvider",
                new HashSet<>(Arrays.asList("jdk.jshell.execution.impl.ConsoleImpl$ConsoleProviderImpl",
                        "jdk.internal.org.jline.JdkConsoleProviderImpl")));
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<?> serviceCatalogSupport;
        Method singleton;
        Method removeServices;
        try {
            serviceCatalogSupport = Class.forName("com.oracle.svm.core.jdk.ServiceCatalogSupport");
            singleton = serviceCatalogSupport.getDeclaredMethod("singleton");
            removeServices = serviceCatalogSupport.getDeclaredMethod("removeServicesFromServicesCatalog", String.class,
                    Set.class);
            var result = singleton.invoke(null);
            omittedServiceProviders.forEach((key, value) -> {
                try {
                    removeServices.invoke(result, key, value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
