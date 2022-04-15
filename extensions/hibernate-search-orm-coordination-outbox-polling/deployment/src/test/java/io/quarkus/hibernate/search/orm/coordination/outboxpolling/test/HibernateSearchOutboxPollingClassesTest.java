package io.quarkus.hibernate.search.orm.coordination.outboxpolling.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.avro.specific.AvroGenerated;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.hibernate.search.orm.coordination.outboxpolling.HibernateSearchOutboxPollingClasses;

/**
 * Tests that hardcoded lists of classes stay up-to-date.
 */
public class HibernateSearchOutboxPollingClassesTest {

    private static Index searchOutboxPollingIndex;

    @BeforeAll
    public static void index() throws IOException {
        searchOutboxPollingIndex = IndexingUtil.indexJar(determineJarLocation(HibernateOrmMapperOutboxPollingSettings.class,
                "hibernate-search-mapper-orm-coordination-outbox-polling"));
    }

    @Test
    public void testNoMissingAvroGeneratedClass() {
        Set<String> annotatedClasses = new HashSet<>();
        for (AnnotationInstance annotationInstance : searchOutboxPollingIndex
                .getAnnotations(DotName.createSimple(AvroGenerated.class.getName()))) {
            DotName className = extractDeclaringClass(annotationInstance.target()).name();
            annotatedClasses.add(className.toString());
        }

        assertThat(annotatedClasses).isNotEmpty();
        assertThat(HibernateSearchOutboxPollingClasses.AVRO_GENERATED_CLASSES)
                .containsExactlyInAnyOrderElementsOf(annotatedClasses);
    }

    @Test
    public void testNoMissingJpaModelClass() {
        Set<DotName> modelClasses = collectModelClassesRecursively(searchOutboxPollingIndex, Set.of(
                DotName.createSimple(OutboxEvent.class.getName()),
                DotName.createSimple(Agent.class.getName())));

        Set<String> modelClassNames = modelClasses.stream().map(DotName::toString).collect(Collectors.toSet());

        // Despite being referenced from entities, these types are not included in the JPA model.
        modelClassNames.removeAll(Set.of(
                "org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentReference",
                "org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventReference",
                "org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ShardAssignmentDescriptor"));

        assertThat(modelClassNames).isNotEmpty();
        assertThat(HibernateSearchOutboxPollingClasses.JPA_MODEL_CLASSES)
                .containsExactlyInAnyOrderElementsOf(modelClassNames);
    }

    private static Path determineJarLocation(Class<?> classFromJar, String jarName) {
        URL url = classFromJar.getProtectionDomain().getCodeSource().getLocation();
        if (!url.getProtocol().equals("file")) {
            throw new IllegalStateException(jarName + " JAR is not a local file? " + url);
        }
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ClassInfo extractDeclaringClass(AnnotationTarget target) {
        switch (target.kind()) {
            case CLASS:
                return target.asClass();
            case FIELD:
                return target.asField().declaringClass();
            case METHOD:
                return target.asMethod().declaringClass();
            case METHOD_PARAMETER:
                return target.asMethodParameter().method().declaringClass();
            case TYPE:
                return extractDeclaringClass(target.asType().enclosingTarget());
            default:
                throw new IllegalStateException("Unsupported annotation target kind: " + target.kind());
        }
    }

    private static Set<DotName> collectModelClassesRecursively(Index index, Set<DotName> initialClasses) {
        Set<DotName> classes = new HashSet<>();
        for (DotName initialClass : initialClasses) {
            collectModelClassesRecursively(index, initialClass, classes);
        }
        return classes;
    }

    private static void collectModelClassesRecursively(Index index, DotName className, Set<DotName> classes) {
        if (className.toString().startsWith("java.")) {
            return;
        }
        if (!classes.add(className)) {
            return;
        }
        ClassInfo clazz = index.getClassByName(className);
        collectModelClassesRecursively(index, clazz.superName(), classes);
        for (DotName interfaceName : clazz.interfaceNames()) {
            collectModelClassesRecursively(index, interfaceName, classes);
        }
        for (FieldInfo field : clazz.fields()) {
            collectModelClassesRecursively(index, field.type(), classes);
        }
        for (FieldInfo field : clazz.fields()) {
            collectModelClassesRecursively(index, field.type(), classes);
        }
        for (MethodInfo methodInfo : clazz.methods()) {
            if (!methodInfo.parameters().isEmpty()) {
                // Definitely not a getter, just skip.
                continue;
            }
            collectModelClassesRecursively(index, methodInfo.returnType(), classes);
        }
    }

    private static void collectModelClassesRecursively(Index index, Type type, Set<DotName> classes) {
        switch (type.kind()) {
            case CLASS:
                collectModelClassesRecursively(index, type.name(), classes);
                break;
            case ARRAY:
                collectModelClassesRecursively(index, type.asArrayType().component(), classes);
                break;
            case TYPE_VARIABLE:
                for (Type bound : type.asTypeVariable().bounds()) {
                    collectModelClassesRecursively(index, bound, classes);
                }
                break;
            case WILDCARD_TYPE:
                collectModelClassesRecursively(index, type.asWildcardType().extendsBound(), classes);
                collectModelClassesRecursively(index, type.asWildcardType().superBound(), classes);
                break;
            case PARAMETERIZED_TYPE:
                collectModelClassesRecursively(index, type.name(), classes);
                for (Type argument : type.asParameterizedType().arguments()) {
                    collectModelClassesRecursively(index, argument, classes);
                }
                break;
            case PRIMITIVE:
            case VOID:
            case UNRESOLVED_TYPE_VARIABLE:
                // Ignore
                break;
        }
    }
}
