package io.quarkus.hibernate.orm.devui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.devui.namedpu.MyNamedPuEntity;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIHibernateOrmActiveFalseAndNamedPuActiveTrueTest extends AbstractDevUIHibernateOrmTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:test\n"
                            + "quarkus.datasource.\"nameddatasource\".db-kind=h2\n"
                            + "quarkus.datasource.\"nameddatasource\".jdbc.url=jdbc:h2:mem:test2\n"
                            // Hibernate ORM is inactive for the default PU
                            + "quarkus.hibernate-orm.active=false\n"
                            + "quarkus.hibernate-orm.datasource=<default>\n"
                            + "quarkus.hibernate-orm.packages=io.quarkus.test.devconsole\n"
                            // ... but it's (implicitly) active for a named PU
                            + "quarkus.hibernate-orm.\"namedpu\".datasource=nameddatasource\n"
                            + "quarkus.hibernate-orm.\"namedpu\".packages=io.quarkus.hibernate.orm.devui.namedpu\n"),
                    "application.properties")
                    .addClasses(MyEntity.class)
                    .addClasses(MyNamedPuEntity.class));

    public DevUIHibernateOrmActiveFalseAndNamedPuActiveTrueTest() {
        super("namedpu", "MyNamedPuEntity", "io.quarkus.hibernate.orm.devui.namedpu.MyNamedPuEntity");
    }

}
