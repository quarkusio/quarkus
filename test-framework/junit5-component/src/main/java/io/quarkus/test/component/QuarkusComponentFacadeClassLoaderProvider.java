package io.quarkus.test.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.test.common.FacadeClassLoaderProvider;

public class QuarkusComponentFacadeClassLoaderProvider implements FacadeClassLoaderProvider {

    private static final Logger LOG = Logger.getLogger(QuarkusComponentFacadeClassLoaderProvider.class);

    // used for continuous testing
    private final Class<?> testClass;
    private final Set<String> tracedClasses;

    public QuarkusComponentFacadeClassLoaderProvider() {
        this(null, Set.of());
    }

    // used in JunitTestRunner
    public QuarkusComponentFacadeClassLoaderProvider(Class<?> testClass, Set<String> tracedClasses) {
        this.testClass = testClass;
        this.tracedClasses = tracedClasses;
    }

    @Override
    public ClassLoader getClassLoader(String name, ClassLoader parent) {
        QuarkusComponentTestConfiguration configuration = null;
        boolean buildShouldFail = false;
        Class<?> inspectionClass = testClass;
        if (inspectionClass == null) {
            try {
                inspectionClass = QuarkusComponentFacadeClassLoaderProvider.class.getClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                LOG.warnf("Inspection class not found: %s [CL=%s]", name,
                        QuarkusComponentFacadeClassLoaderProvider.class.getClassLoader());
                return null;
            }
        }

        for (Annotation a : inspectionClass.getAnnotations()) {
            if (a.annotationType().getName().equals("io.quarkus.test.component.QuarkusComponentTest")) {
                configuration = QuarkusComponentTestConfiguration.DEFAULT.update(inspectionClass);
                break;
            }
        }
        if (configuration == null) {
            for (Field field : inspectionClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        && field.getType().getName().equals("io.quarkus.test.component.QuarkusComponentTestExtension")) {
                    QuarkusComponentTestExtension extension;
                    try {
                        field.setAccessible(true);
                        extension = (QuarkusComponentTestExtension) field.get(null);
                        buildShouldFail = extension.isBuildShouldFail();
                        configuration = extension.baseConfiguration.update(inspectionClass);
                        break;
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new IllegalStateException("Unable to read configuration from field: " + field, e);
                    }
                }
            }
        }

        if (configuration != null) {
            try {
                LOG.debugf("Created QuarkusComponentTestClassLoader for %s", inspectionClass);
                return new QuarkusComponentTestClassLoader(parent, name,
                        ComponentContainer.build(inspectionClass, configuration, buildShouldFail, tracedClasses));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to build container for %s".formatted(name), e);
            }
        }
        return null;
    }

}
