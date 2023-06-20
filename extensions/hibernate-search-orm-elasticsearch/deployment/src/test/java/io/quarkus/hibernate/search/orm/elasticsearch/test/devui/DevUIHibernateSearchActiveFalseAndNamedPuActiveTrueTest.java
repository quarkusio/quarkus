package io.quarkus.hibernate.search.orm.elasticsearch.test.devui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.search.orm.elasticsearch.test.devui.namedpu.MyNamedPuEntity;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIHibernateSearchActiveFalseAndNamedPuActiveTrueTest extends AbstractDevUIHibernateSearchTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:test\n"
                            + "quarkus.datasource.\"nameddatasource\".db-kind=h2\n"
                            + "quarkus.datasource.\"nameddatasource\".jdbc.url=jdbc:h2:mem:default;DB_CLOSE_DELAY=-1\n"
                            // Hibernate Search is inactive for the default PU
                            + "quarkus.hibernate-orm.datasource=<default>\n"
                            + "quarkus.hibernate-orm.packages=io.quarkus.hibernate.search.orm.elasticsearch.test.devui\n"
                            + "quarkus.hibernate-search-orm.active=false\n"
                            + "quarkus.hibernate-search-orm.elasticsearch.version=7\n"
                            // ... but it's (implicitly) active for a named PU
                            + "quarkus.hibernate-orm.\"namedpu\".datasource=nameddatasource\n"
                            + "quarkus.hibernate-orm.\"namedpu\".packages=io.quarkus.hibernate.search.orm.elasticsearch.test.devui.namedpu\n"
                            + "quarkus.hibernate-search-orm.\"namedpu\".elasticsearch.version=7\n"),
                    "application.properties")
                    .addClasses(MyEntity.class)
                    .addClasses(MyNamedPuEntity.class));

    public DevUIHibernateSearchActiveFalseAndNamedPuActiveTrueTest() {
        super("namedpu", "io.quarkus.hibernate.search.orm.elasticsearch.test.devui.namedpu.MyNamedPuEntity");
    }

}
