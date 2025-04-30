package io.quarkus.hibernate.search.standalone.elasticsearch.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.index.IndexingUtil;

/**
 * Test that hardcoded lists of Hibernate Search types stay up-to-date.
 */
public class HibernateSearchTypesTest {

    private static Index hibernateSearchPojoBaseIndex;
    private static Index hibernateSearchPojoStandaloneIndex;

    @BeforeAll
    public static void index() throws IOException {
        hibernateSearchPojoBaseIndex = IndexingUtil.indexJar(determineHibernateSearchPojoBaseJarLocation());
        hibernateSearchPojoStandaloneIndex = IndexingUtil.indexJar(determineHibernateSearchPojoStandaloneJarLocation());
    }

    @Test
    public void testNoMissingRootMappingAnnotation() {
        Set<DotName> rootMappingAnnotations = findAnnotationsAnnotatedWith(hibernateSearchPojoBaseIndex,
                HibernateSearchTypes.ROOT_MAPPING);
        rootMappingAnnotations
                .addAll(findAnnotationsAnnotatedWith(hibernateSearchPojoStandaloneIndex, HibernateSearchTypes.ROOT_MAPPING));

        assertThat(HibernateSearchTypes.BUILT_IN_ROOT_MAPPING_ANNOTATIONS)
                .isNotEmpty()
                .containsExactlyInAnyOrderElementsOf(rootMappingAnnotations);
    }

    private Set<DotName> findAnnotationsAnnotatedWith(Index index, DotName annotationName) {
        Set<DotName> classes = new TreeSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(annotationName)) {
            ClassInfo clazz = annotation.target().asClass();
            classes.add(clazz.name());
        }
        return classes;
    }

    private static File determineHibernateSearchPojoBaseJarLocation() {
        URL url = FullTextField.class.getProtectionDomain().getCodeSource().getLocation();
        if (!url.getProtocol().equals("file")) {
            throw new IllegalStateException("Hibernate Search Pojo Base JAR is not a local file? " + url);
        }
        return new File(url.getPath());
    }

    private static File determineHibernateSearchPojoStandaloneJarLocation() {
        URL url = SearchMapping.class.getProtectionDomain().getCodeSource().getLocation();
        if (!url.getProtocol().equals("file")) {
            throw new IllegalStateException("Hibernate Search Pojo Standalone JAR is not a local file? " + url);
        }
        return new File(url.getPath());
    }
}
