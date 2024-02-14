package io.quarkus.test.devui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class DevUIHibernateSearchActiveFalseTest extends AbstractDevUIHibernateSearchTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:test\n"
                            // Hibernate Search is inactive: the dev console should be empty.
                            + "quarkus.hibernate-search-orm.active=false\n"
                            + "quarkus.hibernate-search-orm.elasticsearch.version=8.12\n"),
                    "application.properties")
                    .addClasses(MyIndexedEntity.class));

    public DevUIHibernateSearchActiveFalseTest() {
        // Hibernate Search is inactive: the dev console should be empty.
        super(null, null);
    }

}
