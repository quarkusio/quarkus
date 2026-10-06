package io.quarkus.hibernate.envers.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.envers.RevisionEntity;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.index.IndexingUtil;

class HibernateEnversModelClassesTest {

    @Test
    void allBuiltInModelClassesAreRegistered() throws Exception {
        Index index = IndexingUtil.indexJar(Path.of(RevisionEntity.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()));
        Set<String> modelClasses = new TreeSet<>();
        for (Class<?> annotationType : List.of(Entity.class, MappedSuperclass.class, Embeddable.class)) {
            for (AnnotationInstance annotation : index.getAnnotations(DotName.createSimple(annotationType.getName()))) {
                ClassInfo modelClass = annotation.target().asClass();
                modelClasses.add(modelClass.name().toString());
                // Envers maps its concrete revision entities through XML, so they have no @Entity annotation.
                for (ClassInfo subclass : index.getAllKnownSubclasses(modelClass.name())) {
                    modelClasses.add(subclass.name().toString());
                }
            }
        }

        assertThat(modelClasses).as("Model classes discovered in the Hibernate Envers JAR").isNotEmpty();
        assertThat(HibernateEnversProcessor.ENVERS_CLASSES_FOR_REFLECTION)
                .as("Envers model classes registered for indexing, enhancement, reflection and accessors")
                .containsExactlyInAnyOrderElementsOf(modelClasses);
    }
}
