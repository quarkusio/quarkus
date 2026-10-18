package io.quarkus.security.spi;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows to create additional security checks for standard security annotations defined on a class level and
 * security interceptors for security annotations (such as selecting tenant or authentication mechanism).
 * We strongly recommended to secure CDI beans with {@link AdditionalSecuredMethodsBuildItem}
 * if additional security is required. If you decide to use this build item, you must use
 * class security check storage and apply checks manually. Thus, it's only suitable for very special cases and
 * intended for internal use in Quarkus core extensions.
 */
public final class ClassSecurityAnnotationBuildItem extends MultiBuildItem {

    private final DotName classAnnotation;

    /**
     * This will identify classes that require class-level security.
     *
     * @param classAnnotation class-level annotation name
     */
    public ClassSecurityAnnotationBuildItem(DotName classAnnotation) {
        this.classAnnotation = Objects.requireNonNull(classAnnotation);
    }

    public DotName getClassAnnotation() {
        return classAnnotation;
    }

    public static Predicate<ClassInfo> useClassLevelSecurity(List<ClassSecurityAnnotationBuildItem> items) {
        return new Predicate<>() {

            private final Set<String> securityAnnotationNames = items.stream()
                    .map(ClassSecurityAnnotationBuildItem::getClassAnnotation)
                    .map(DotName::toString)
                    .collect(Collectors.toSet());

            @Override
            public boolean test(ClassInfo classInfo) {
                return classInfo.declaredAnnotations().stream()
                        .map(AnnotationInstance::name)
                        .map(DotName::toString)
                        .anyMatch(securityAnnotationNames::contains);
            }
        };
    }
}
