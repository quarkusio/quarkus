package io.quarkus.test.devui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class DevUIHibernateSearchSmokeTest extends AbstractDevUIHibernateSearchTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:test\n"
                            + "quarkus.hibernate-search-orm.elasticsearch.version=8.12\n"
                            // Start offline, we don't have an Elasticsearch cluster here
                            + "quarkus.hibernate-search-orm.schema-management.strategy=none\n"
                            + "quarkus.hibernate-search-orm.elasticsearch.version-check.enabled=false\n"),
                    "application.properties")
                    .addClasses(MyIndexedEntity.class));

    public DevUIHibernateSearchSmokeTest() {
        super("<default>", MyIndexedEntity.class.getName());
    }
}
