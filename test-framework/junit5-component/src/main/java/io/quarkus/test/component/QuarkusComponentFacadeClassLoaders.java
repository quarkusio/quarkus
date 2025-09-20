package io.quarkus.test.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.test.common.FacadeClassLoaders;

public class QuarkusComponentFacadeClassLoaders implements FacadeClassLoaders {

    private static final Logger LOG = Logger.getLogger(QuarkusComponentFacadeClassLoaders.class);

    // used for continuous testing
    private final Class<?> testClass;
    private final Set<String> tracedClasses;

    public QuarkusComponentFacadeClassLoaders() {
        this(null, Set.of());
    }

    // used in JunitTestRunner
    public QuarkusComponentFacadeClassLoaders(Class<?> testClass, Set<String> tracedClasses) {
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
                inspectionClass = QuarkusComponentFacadeClassLoaders.class.getClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Inspection class not found: %s, CL=%s".formatted(name,
                        QuarkusComponentFacadeClassLoaders.class.getClassLoader()));
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
                LOG.errorf("Unable to build container for %s", name);
            }
        }
        return null;
    }

}
