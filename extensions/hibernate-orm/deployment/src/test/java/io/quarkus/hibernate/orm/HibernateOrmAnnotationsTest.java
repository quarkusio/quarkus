package io.quarkus.hibernate.orm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.hibernate.orm.deployment.HibernateOrmAnnotations;

/**
 * Test that hardcoded lists of Hibernate ORM annotations stay up-to-date.
 */
public class HibernateOrmAnnotationsTest {

    private static final DotName RETENTION = DotName.createSimple(Retention.class.getName());
    private static final DotName TARGET = DotName.createSimple(Target.class.getName());

    @Test
    public void testNoMissingPackageLevelAnnotation() throws IOException {
        Index index = indexHibernateJar();

        Set<DotName> packageLevelHibernateAnnotations = findRuntimeAnnotationsByTargetType(index, ElementType.PACKAGE);
        packageLevelHibernateAnnotations.removeIf(name -> name.toString().contains(".internal."));

        assertThat(HibernateOrmAnnotations.PACKAGE_ANNOTATIONS)
                .containsExactlyInAnyOrderElementsOf(packageLevelHibernateAnnotations);
    }

    private Set<DotName> findRuntimeAnnotationsByTargetType(Index index, ElementType targetType) {
        Set<DotName> annotations = new HashSet<>();
        for (AnnotationInstance retentionAnnotation : index.getAnnotations(RETENTION)) {
            ClassInfo annotation = retentionAnnotation.target().asClass();
            if (RetentionPolicy.RUNTIME.name().equals(retentionAnnotation.value().asEnum())
                    && allowsTargetType(annotation, targetType)) {
                annotations.add(annotation.name());
            }
        }
        return annotations;
    }

    private boolean allowsTargetType(ClassInfo annotation, ElementType targetType) {
        AnnotationInstance targetAnnotation = annotation.classAnnotation(TARGET);
        if (targetAnnotation == null) {
            // Can target anything
            return true;
        }

        List<String> allowedTargetTypes = Arrays.asList(targetAnnotation.value().asEnumArray());
        return allowedTargetTypes.contains(targetType.name());
    }

    private Index indexHibernateJar() throws IOException {
        return IndexingUtil.indexJar(determineHibernateJarLocation());
    }

    private File determineHibernateJarLocation() {
        URL url = Hibernate.class.getProtectionDomain().getCodeSource().getLocation();
        if (!url.getProtocol().equals("file")) {
            throw new IllegalStateException("Hibernate JAR is not a local file? " + url);
        }
        return new File(url.getPath());
    }
}
