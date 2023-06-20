package io.quarkus.hibernate.search.orm.elasticsearch.test.devui;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class DevUIHibernateSearchSmokeTest extends AbstractDevUIHibernateSearchTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource("application.properties")
                    .addClasses(MyEntity.class));

    public DevUIHibernateSearchSmokeTest() {
        super("<default>", "io.quarkus.hibernate.search.orm.elasticsearch.test.devui.MyEntity");
    }
}
