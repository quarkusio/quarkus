package io.quarkus.hibernate.orm.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.deployment.index.IndexWrapper;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.index.PersistentClassIndex;
import io.quarkus.deployment.util.JandexUtil;

/**
 * Test that class name constants point to actual classes and stay up-to-date.
 */
public class ClassNamesTest {

    private static final DotName RETENTION = DotName.createSimple(Retention.class.getName());
    private static final DotName TARGET = DotName.createSimple(Target.class.getName());

    private static Index jpaIndex;
    private static Index hibernateIndex;

    @BeforeAll
    public static void index() throws IOException {
        jpaIndex = IndexingUtil.indexJar(determineJpaJarLocation());
        hibernateIndex = IndexingUtil.indexJar(determineHibernateJarLocation());
    }

    @ParameterizedTest
    @MethodSource("provideConstantsToTest")
    void testClassNameRefersToExistingClass(DotName constant) {
        assertThatCode(() -> getClass().getClassLoader().loadClass(constant.toString()))
                .doesNotThrowAnyException();
    }

    private static Set<DotName> provideConstantsToTest() {
        return ClassNames.CREATED_CONSTANTS;
    }

    @Test
    public void testNoMissingGeneratorClass() {
        Set<DotName> generatorImplementors = findConcreteNamedImplementors(hibernateIndex, "org.hibernate.generator.Generator");

        assertThat(ClassNames.GENERATORS)
                .containsExactlyInAnyOrderElementsOf(generatorImplementors);
    }

    @Test
    public void testNoMissingOptimizerClass() {
        Set<DotName> generatorImplementors = findConcreteNamedImplementors(hibernateIndex,
                "org.hibernate.id.enhanced.Optimizer");

        assertThat(ClassNames.OPTIMIZERS)
                .containsExactlyInAnyOrderElementsOf(generatorImplementors);
    }

    @Test
    public void testNoMissingJpaAnnotation() {
        Set<DotName> jpaMappingAnnotations = findRuntimeAnnotations(jpaIndex);
        jpaMappingAnnotations.removeIf(name -> name.toString().startsWith("jakarta.persistence.metamodel."));

        assertThat(ClassNames.JPA_MAPPING_ANNOTATIONS)
                .containsExactlyInAnyOrderElementsOf(jpaMappingAnnotations);
    }

    @Test
    public void testNoMissingJpaListenerAnnotation() {
        Set<DotName> jpaMappingAnnotations = findRuntimeAnnotations(jpaIndex);
        Pattern listenerAnnotationNamePattern = Pattern.compile(".*\\.(Pre|Post)[^.]+");
        jpaMappingAnnotations = jpaMappingAnnotations.stream()
                .filter(name -> listenerAnnotationNamePattern.matcher(name.toString()).matches())
                .collect(Collectors.toSet());

        assertThat(ClassNames.JPA_LISTENER_ANNOTATIONS)
                .containsExactlyInAnyOrderElementsOf(jpaMappingAnnotations);
    }

    @Test
    public void testNoMissingHibernateAnnotation() {
        Set<DotName> hibernateMappingAnnotations = findRuntimeAnnotations(hibernateIndex);
        hibernateMappingAnnotations.removeIf(name -> name.toString().contains(".internal."));
        hibernateMappingAnnotations.removeIf(name -> name.toString().contains(".spi."));
        ignoreInternalAnnotations(hibernateMappingAnnotations);

        assertThat(ClassNames.HIBERNATE_MAPPING_ANNOTATIONS)
                .containsExactlyInAnyOrderElementsOf(hibernateMappingAnnotations);
    }

    private static void ignoreInternalAnnotations(Set<DotName> annotationSet) {
        annotationSet.removeIf(name -> name.toString().equals("org.hibernate.cfg.Compatibility"));
        annotationSet.removeIf(name -> name.toString().equals("org.hibernate.cfg.Unsafe"));
        annotationSet.removeIf(name -> name.toString().equals("org.hibernate.Incubating"));
        annotationSet.removeIf(name -> name.toString().equals("org.hibernate.Internal"));
        annotationSet.removeIf(name -> name.toString().equals("org.hibernate.Remove"));
        annotationSet.removeIf(name -> name.toString().equals("org.hibernate.service.JavaServiceLoadable"));
    }

    @Test
    public void testNoMissingPackageLevelAnnotation() {
        Set<DotName> packageLevelHibernateAnnotations = findRuntimeAnnotationsByTargetType(jpaIndex, ElementType.PACKAGE);
        packageLevelHibernateAnnotations.addAll(findRuntimeAnnotationsByTargetType(hibernateIndex, ElementType.PACKAGE));
        packageLevelHibernateAnnotations.removeIf(name -> name.toString().contains(".internal."));
        ignoreInternalAnnotations(packageLevelHibernateAnnotations);

        assertThat(ClassNames.PACKAGE_ANNOTATIONS)
                .containsExactlyInAnyOrderElementsOf(packageLevelHibernateAnnotations);
    }

    @Test
    public void testNoMissingInjectServiceAnnotatedClass() {
        Set<DotName> injectServiceAnnotatedClasses = findClassesWithMethodsAnnotatedWith(hibernateIndex,
                ClassNames.INJECT_SERVICE);

        assertThat(ClassNames.ANNOTATED_WITH_INJECT_SERVICE)
                .containsExactlyInAnyOrderElementsOf(injectServiceAnnotatedClasses);
    }

    @Test
    public void testNoMissingJdbcJavaTypeClass() {
        Set<DotName> jdbcJavaTypeNames = new TreeSet<>();
        DotName basicJavaTypeName = DotName.createSimple("org.hibernate.type.descriptor.java.BasicJavaType");
        IndexView hibernateAndJdkIndex = new IndexWrapper(hibernateIndex, Thread.currentThread().getContextClassLoader(),
                new PersistentClassIndex());

        for (ClassInfo basicJavaTypeImplInfo : hibernateIndex.getAllKnownImplementors(basicJavaTypeName)) {
            if (Modifier.isAbstract(basicJavaTypeImplInfo.flags())) {
                continue;
            }
            List<Type> typeParams = JandexUtil.resolveTypeParameters(basicJavaTypeImplInfo.name(), basicJavaTypeName,
                    hibernateAndJdkIndex);
            Type jdbcJavaType = typeParams.get(0);
            if (jdbcJavaType.kind() == Type.Kind.CLASS || jdbcJavaType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                jdbcJavaTypeNames.add(jdbcJavaType.name());
            }
        }

        assertThat(ClassNames.JDBC_JAVA_TYPES)
                .containsExactlyInAnyOrderElementsOf(jdbcJavaTypeNames);
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
        AnnotationInstance targetAnnotation = annotation.declaredAnnotation(TARGET);
        if (targetAnnotation == null) {
            // Can target anything
            return true;
        }

        List<String> allowedTargetTypes = Arrays.asList(targetAnnotation.value().asEnumArray());
        return allowedTargetTypes.contains(targetType.name());
    }

    private Set<DotName> findConcreteNamedImplementors(Index index, String interfaceName) {
        var interfaceDotName = DotName.createSimple(interfaceName);
        assertThat(index.getClassByName(interfaceDotName)).isNotNull();
        return index.getAllKnownImplementors(interfaceDotName).stream()
                .filter(c -> !c.isInterface() && !c.isAbstract()
                // Ignore anonymous classes
                        && c.simpleName() != null)
                .map(ClassInfo::name)
                .collect(Collectors.toSet());
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
