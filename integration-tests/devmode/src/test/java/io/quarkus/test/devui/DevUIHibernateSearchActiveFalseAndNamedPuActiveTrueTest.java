package io.quarkus.test.devui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.devui.namedpu.MyNamedPuIndexedEntity;

public class DevUIHibernateSearchActiveFalseAndNamedPuActiveTrueTest extends AbstractDevUIHibernateSearchTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:test\n"
                            + "quarkus.datasource.\"nameddatasource\".db-kind=h2\n"
                            + "quarkus.datasource.\"nameddatasource\".jdbc.url=jdbc:h2:mem:test2\n"
                            // Hibernate Search is inactive for the default PU
                            + "quarkus.hibernate-orm.datasource=<default>\n"
                            + "quarkus.hibernate-orm.packages=io.quarkus.test.devui\n"
                            + "quarkus.hibernate-search-orm.active=false\n"
                            + "quarkus.hibernate-search-orm.elasticsearch.version=8.12\n"
                            // ... but it's (implicitly) active for a named PU
                            + "quarkus.hibernate-orm.\"namedpu\".datasource=nameddatasource\n"
                            + "quarkus.hibernate-orm.\"namedpu\".packages=io.quarkus.test.devui.namedpu\n"
                            + "quarkus.hibernate-search-orm.\"namedpu\".elasticsearch.version=8.12\n"
                            // Start Hibernate Search offline for the named PU,
                            // because we don't have dev services except for the default PU
                            + "quarkus.hibernate-search-orm.\"namedpu\".schema-management.strategy=none\n"
                            + "quarkus.hibernate-search-orm.\"namedpu\".elasticsearch.version-check.enabled=false\n"),
                    "application.properties")
                    .addClasses(MyIndexedEntity.class)
                    .addClasses(MyNamedPuIndexedEntity.class));

    public DevUIHibernateSearchActiveFalseAndNamedPuActiveTrueTest() {
        super("namedpu", MyNamedPuIndexedEntity.class.getName());
    }

}
