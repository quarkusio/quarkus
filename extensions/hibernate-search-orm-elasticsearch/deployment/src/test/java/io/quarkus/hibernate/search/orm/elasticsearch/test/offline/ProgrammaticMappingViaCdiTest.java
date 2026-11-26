package io.quarkus.hibernate.search.orm.elasticsearch.test.offline;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.QuarkusExtensionTest;

public class ProgrammaticMappingViaCdiTest {

    @RegisterExtension
    static final QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ProgrammaticallyMappedEntity.class, ProgrammaticMappingConfigurer.class,
                            CdiProgrammaticMappingConfigurer.class)
                    .addAsResource("application-start-offline.properties", "application.properties"))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.hibernate-orm.packages", ProgrammaticallyMappedEntity.class.getPackageName())
            .overrideConfigKey("quarkus.hibernate-orm.\"mapped\".packages",
                    ProgrammaticallyMappedEntity.class.getPackageName())
            .overrideConfigKey("quarkus.hibernate-orm.\"mapped\".datasource", "<default>")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"mapped\".elasticsearch.version",
                    "${quarkus.hibernate-search-orm.elasticsearch.version}")
            .overrideRuntimeConfigKey("quarkus.hibernate-search-orm.\"mapped\".elasticsearch.hosts", "localhost:14800")
            .overrideRuntimeConfigKey("quarkus.hibernate-search-orm.\"mapped\".elasticsearch.version-check.enabled", "false")
            .overrideRuntimeConfigKey("quarkus.hibernate-search-orm.\"mapped\".schema-management.strategy", "none")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:programmatic-mapping");

    @Test
    void cdiConfigurerEnablesSearch() {
        assertThat(Arc.container().instance(SearchMapping.class).get())
                .as("SearchMapping for the default persistence unit").isNull();

        SearchMapping mapping = Arc.container()
                .instance(SearchMapping.class, new PersistenceUnit.PersistenceUnitLiteral("mapped"))
                .get();
        assertThat(mapping).as("SearchMapping for an entity mapped programmatically").isNotNull();
        assertThat(mapping.allIndexedEntities())
                .hasSize(1)
                .element(0)
                .returns(ProgrammaticallyMappedEntity.class, SearchIndexedEntity::javaClass);
    }
}
