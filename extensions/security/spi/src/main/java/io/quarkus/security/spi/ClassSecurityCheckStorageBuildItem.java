package io.quarkus.security.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Security check storage containing additional security checks created for secured classes
 * matching one of the {@link ClassSecurityAnnotationBuildItem} filters during the static init.
 */
public final class ClassSecurityCheckStorageBuildItem extends SimpleBuildItem {

    private final Map<DotName, Object> classNameToSecurityCheck;

    private ClassSecurityCheckStorageBuildItem(Map<DotName, Object> classNameToSecurityCheck) {
        Objects.requireNonNull(classNameToSecurityCheck);
        this.classNameToSecurityCheck = Map.copyOf(classNameToSecurityCheck);
    }

    /**
     * Returns additional security check created for classes annotated with standard
     * security annotations based on the {@link ClassSecurityAnnotationBuildItem} filter.
     *
     * @param className class name
     * @return security check (see runtime Security SPI for respective class)
     */
    public Object getSecurityCheck(DotName className) {
        return classNameToSecurityCheck.get(className);
    }

    public static final class ClassStorageBuilder {

        private final Map<DotName, Object> classNameToSecurityCheck;

        public ClassStorageBuilder() {
            this.classNameToSecurityCheck = new HashMap<>();
        }

        public ClassStorageBuilder addSecurityCheck(DotName className, Object securityCheck) {
            classNameToSecurityCheck.put(className, securityCheck);
            return this;
        }

        public ClassSecurityCheckStorageBuildItem build() {
            return new ClassSecurityCheckStorageBuildItem(classNameToSecurityCheck);
        }
    }
}
