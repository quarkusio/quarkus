package io.quarkus.hibernate.search.orm.elasticsearch.test.devui;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class DevUIActiveFalseTest extends AbstractDevUITest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(
                    (jar) -> jar.addAsResource("application-devui-active-false.properties", "application.properties")
                            .addClass(MyIndexedEntity.class)
                            .addClass(RestClientStarterService.class));

    public DevUIActiveFalseTest() {
        // Hibernate Search is inactive: the dev console should be empty.
        super(null, null);
    }

}
