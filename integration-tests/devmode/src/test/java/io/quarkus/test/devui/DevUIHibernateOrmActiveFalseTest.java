package io.quarkus.test.devui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class DevUIHibernateOrmActiveFalseTest extends AbstractDevUIHibernateOrmTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:test\n"
                            // Hibernate ORM is inactive: the dev console should be empty.
                            + "quarkus.hibernate-orm.active=false\n"),
                    "application.properties")
                    .addClasses(MyEntity.class));

    public DevUIHibernateOrmActiveFalseTest() {
        super(null, null, null, null, false);
    }

}
