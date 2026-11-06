package io.quarkus.arc.deployment;

import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

public final class SyntheticBeanBuildItemTestHelper {

    private SyntheticBeanBuildItemTestHelper() {
    }

    public static SyntheticBeanMetadata metadata(SyntheticBeanBuildItem item) {
        var configurator = item.configurator();
        return new SyntheticBeanMetadata(configurator.getImplClazz(), configurator.getTypes(),
                configurator.getQualifiers(), !item.isStaticInit());
    }

    public record SyntheticBeanMetadata(DotName implementationClass, Set<Type> types,
            Set<AnnotationInstance> qualifiers, boolean runtimeInit) {
    }
}
