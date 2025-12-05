package io.quarkus.hibernate.orm.deployment;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.hibernate.Hibernate;
import org.hibernate.annotations.processing.Find;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.index.IndexingUtil;

/**
 * Tests that the list of the 'org.hibernate.annotations.processing' package annotations for which Hibernate Processor
 * creates Hibernate repository is up-to-date.
 */
public class HibernateRepositoryAnnotationsTest {

    private static final DotName RETENTION = DotName.createSimple(Retention.class.getName());
    private static final DotName TARGET = DotName.createSimple(Target.class.getName());
    private static final String ANNOTATIONS_PACKAGE = Find.class.getPackage().getName();

    private static Index hibernateIndex;

    @BeforeAll
    public static void index() throws IOException {
        hibernateIndex = IndexingUtil.indexJar(determineHibernateJarLocation());
    }

    @Test
    void testAllRepositoryDefiningAnnotationsListed() {
        var allProcessingAnnotations = findAllProcessingAnnotations();
        var knowRepositoryDefiningAnnotations = HibernateOrmProcessor.HIBERNATE_REPOSITORY_ANNOTATIONS.stream()
                .map(Class::getName).collect(toSet());
        assertThat(allProcessingAnnotations)
                .isNotEmpty()
                .contains(knowRepositoryDefiningAnnotations.toArray(new String[0]));
        if (knowRepositoryDefiningAnnotations.size() != allProcessingAnnotations.size()) {
            for (String annotation : allProcessingAnnotations) {
                if (!knowRepositoryDefiningAnnotations.contains(annotation)) {
                    // if this ever happen, we can introduce a whitelist
                    Assertions.fail("Annotation " + annotation + " has not been vetted yet."
                            + "Please verify this annotation cannot be used as a repository defining annotation.");
                }
            }
        }
    }

    private static Set<String> findAllProcessingAnnotations() {
        Set<String> annotations = new HashSet<>();
        for (AnnotationInstance retentionAnnotation : hibernateIndex.getAnnotations(RETENTION)) {
            ClassInfo annotation = retentionAnnotation.target().asClass();
            if (annotation.name().packagePrefix().equals(ANNOTATIONS_PACKAGE) && allowsMethodTargetType(annotation)) {
                annotations.add(annotation.name().toString());
            }
        }
        return annotations;
    }

    private static boolean allowsMethodTargetType(ClassInfo annotation) {
        AnnotationInstance targetAnnotation = annotation.declaredAnnotation(TARGET);
        if (targetAnnotation == null) {
            // Can target anything
            return true;
        }

        List<String> allowedTargetTypes = Arrays.asList(targetAnnotation.value().asEnumArray());
        return allowedTargetTypes.contains(ElementType.METHOD.name());
    }

    private static File determineHibernateJarLocation() {
        URL url = Hibernate.class.getProtectionDomain().getCodeSource().getLocation();
        if (!url.getProtocol().equals("file")) {
            throw new IllegalStateException("Hibernate JAR is not a local file? " + url);
        }
        return new File(url.getPath());
    }
}
