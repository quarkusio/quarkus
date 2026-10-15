package io.quarkus.resteasy.reactive.server.deployment;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;

/**
 * Quarkus REST only scans the {@code jakarta.ws.rs} annotations, so resources still annotated with the legacy
 * {@code javax.ws.rs} ones are silently ignored. This warns about them at build time.
 */
public class LegacyJaxRsAnnotationsProcessor {

    private static final Logger log = Logger.getLogger(LegacyJaxRsAnnotationsProcessor.class);

    private static final List<DotName> LEGACY_ANNOTATIONS = List.of(
            DotName.createSimple("javax.ws.rs.Path"),
            DotName.createSimple("javax.ws.rs.ApplicationPath"),
            DotName.createSimple("javax.ws.rs.GET"),
            DotName.createSimple("javax.ws.rs.POST"),
            DotName.createSimple("javax.ws.rs.PUT"),
            DotName.createSimple("javax.ws.rs.DELETE"),
            DotName.createSimple("javax.ws.rs.PATCH"),
            DotName.createSimple("javax.ws.rs.HEAD"),
            DotName.createSimple("javax.ws.rs.OPTIONS"),
            DotName.createSimple("javax.ws.rs.ext.Provider"));

    private static final int MAX_REPORTED_CLASSES = 20;

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    void warnAboutLegacyAnnotations(CombinedIndexBuildItem combinedIndexBuildItem) {
        Set<String> classes = new TreeSet<>();
        for (DotName annotation : LEGACY_ANNOTATIONS) {
            for (AnnotationInstance instance : combinedIndexBuildItem.getIndex().getAnnotations(annotation)) {
                ClassInfo declaringClass = declaringClass(instance.target());
                if (declaringClass != null) {
                    classes.add(declaringClass.name().toString());
                }
            }
        }
        if (classes.isEmpty()) {
            return;
        }
        String reported = classes.size() > MAX_REPORTED_CLASSES
                ? String.join(", ", classes.stream().limit(MAX_REPORTED_CLASSES).toList()) + ", ... ("
                        + (classes.size() - MAX_REPORTED_CLASSES) + " more)"
                : String.join(", ", classes);
        log.warnf("The following classes use the legacy 'javax.ws.rs' annotations, which Quarkus does not scan:"
                + " their endpoints and providers are ignored. Replace the 'javax.ws.rs' imports with 'jakarta.ws.rs': %s",
                reported);
    }

    private static ClassInfo declaringClass(AnnotationTarget target) {
        return switch (target.kind()) {
            case CLASS -> target.asClass();
            case METHOD -> target.asMethod().declaringClass();
            case METHOD_PARAMETER -> target.asMethodParameter().method().declaringClass();
            case FIELD -> target.asField().declaringClass();
            default -> null;
        };
    }
}
