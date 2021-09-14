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
import java.util.TreeSet;

import javax.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.hibernate.orm.deployment.ClassNames;
import io.quarkus.hibernate.orm.deployment.HibernateOrmAnnotations;

/**
 * Test that hardcoded lists of Hibernate ORM annotations stay up-to-date.
 */
public class HibernateOrmAnnotationsTest {

    private static final DotName RETENTION = DotName.createSimple(Retention.class.getName());
    private static final DotName TARGET = DotName.createSimple(Target.class.getName());

    private static Index jpaIndex;
    private static Index hibernateIndex;

    @BeforeAll
    public static void index() throws IOException {
        jpaIndex = IndexingUtil.indexJar(determineJpaJarLocation());
        hibernateIndex = IndexingUtil.indexJar(determineHibernateJarLocation());
    }

    @Test
    public void testNoMissingJpaAnnotation() {
        Set<DotName> jpaMappingAnnotations = findRuntimeAnnotations(jpaIndex);
        jpaMappingAnnotations.removeIf(name -> name.toString().startsWith("javax.persistence.metamodel."));

        assertThat(HibernateOrmAnnotations.JPA_MAPPING_ANNOTATIONS)
                .containsExactlyInAnyOrderElementsOf(jpaMappingAnnotations);
    }

    @Test
    public void testNoMissingHibernateAnnotation() {
        Set<DotName> hibernateMappingAnnotations = findRuntimeAnnotations(hibernateIndex);
        hibernateMappingAnnotations.removeIf(name -> name.toString().contains(".internal."));
        hibernateMappingAnnotations.removeIf(name -> name.toString().contains(".spi."));

        assertThat(HibernateOrmAnnotations.HIBERNATE_MAPPING_ANNOTATIONS)
                .containsExactlyInAnyOrderElementsOf(hibernateMappingAnnotations);
    }

    @Test
    public void testNoMissingPackageLevelAnnotation() {
        Set<DotName> packageLevelHibernateAnnotations = findRuntimeAnnotationsByTargetType(jpaIndex, ElementType.PACKAGE);
        packageLevelHibernateAnnotations.addAll(findRuntimeAnnotationsByTargetType(hibernateIndex, ElementType.PACKAGE));
        packageLevelHibernateAnnotations.removeIf(name -> name.toString().contains(".internal."));

        assertThat(HibernateOrmAnnotations.PACKAGE_ANNOTATIONS)
                .containsExactlyInAnyOrderElementsOf(packageLevelHibernateAnnotations);
    }

    @Test
    public void testNoMissingInjectServiceAnnotatedClass() {
        Set<DotName> injectServiceAnnotatedClasses = findClassesWithMethodsAnnotatedWith(hibernateIndex,
                ClassNames.INJECT_SERVICE);

        assertThat(HibernateOrmAnnotations.ANNOTATED_WITH_INJECT_SERVICE)
                .containsExactlyInAnyOrderElementsOf(injectServiceAnnotatedClasses);
    }

    private Set<DotName> findRuntimeAnnotations(Index index) {
        Set<DotName> annotations = new HashSet<>();
        for (AnnotationInstance retentionAnnotation : index.getAnnotations(RETENTION)) {
            ClassInfo annotation = retentionAnnotation.target().asClass();
            if (RetentionPolicy.RUNTIME.name().equals(retentionAnnotation.value().asEnum())) {
                annotations.add(annotation.name());
            }
        }
        return annotations;
    }

    private Set<DotName> findRuntimeAnnotationsByTargetType(Index index, ElementType targetType) {
        Set<DotName> annotations = new TreeSet<>();
        for (AnnotationInstance retentionAnnotation : index.getAnnotations(RETENTION)) {
            ClassInfo annotation = retentionAnnotation.target().asClass();
            if (RetentionPolicy.RUNTIME.name().equals(retentionAnnotation.value().asEnum())
                    && allowsTargetType(annotation, targetType)) {
                annotations.add(annotation.name());
            }
        }
        return annotations;
    }

    private Set<DotName> findClassesWithMethodsAnnotatedWith(Index index, DotName annotationName) {
        Set<DotName> classes = new TreeSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(annotationName)) {
            ClassInfo clazz = annotation.target().asMethod().declaringClass();
            classes.add(clazz.name());
        }
        return classes;
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

    private static File determineJpaJarLocation() {
        URL url = EntityManager.class.getProtectionDomain().getCodeSource().getLocation();
        if (!url.getProtocol().equals("file")) {
            throw new IllegalStateException("JPA JAR is not a local file? " + url);
        }
        return new File(url.getPath());
    }

    private static File determineHibernateJarLocation() {
        URL url = Hibernate.class.getProtectionDomain().getCodeSource().getLocation();
        if (!url.getProtocol().equals("file")) {
            throw new IllegalStateException("Hibernate JAR is not a local file? " + url);
        }
        return new File(url.getPath());
    }
}
