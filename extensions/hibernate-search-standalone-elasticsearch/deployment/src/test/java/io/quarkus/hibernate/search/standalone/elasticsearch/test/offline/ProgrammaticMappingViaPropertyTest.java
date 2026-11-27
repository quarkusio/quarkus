package io.quarkus.hibernate.search.standalone.elasticsearch.test.offline;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class ProgrammaticMappingViaPropertyTest {

    @RegisterExtension
    static final QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ProgrammaticallyMappedEntity.class, ProgrammaticMappingConfigurer.class)
                    .addAsResource("application-start-offline.properties", "application.properties"))
            .overrideConfigKey("quarkus.hibernate-search-standalone.mapping.configurer",
                    "class:" + ProgrammaticMappingConfigurer.class.getName());

    @Test
    void propertyConfigurerEnablesSearch() {
        SearchMapping mapping = Arc.container().instance(SearchMapping.class).get();
        assertThat(mapping).as("SearchMapping for an entity mapped programmatically").isNotNull();
        assertThat(mapping.allIndexedEntities())
                .hasSize(1)
                .element(0)
                .returns(ProgrammaticallyMappedEntity.class, SearchIndexedEntity::javaClass);
    }
}
