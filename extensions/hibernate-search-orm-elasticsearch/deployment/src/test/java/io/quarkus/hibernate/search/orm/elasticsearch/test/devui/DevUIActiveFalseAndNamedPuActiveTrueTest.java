package io.quarkus.hibernate.search.orm.elasticsearch.test.devui;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.search.orm.elasticsearch.test.devui.namedpu.MyNamedPuIndexedEntity;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIActiveFalseAndNamedPuActiveTrueTest extends AbstractDevUITest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-devui-active-false-and-named-pu-active-true.properties",
                            "application.properties")
                    .addClasses(MyIndexedEntity.class)
                    .addClasses(MyNamedPuIndexedEntity.class));

    public DevUIActiveFalseAndNamedPuActiveTrueTest() {
        super("namedpu", MyNamedPuIndexedEntity.class.getName());
    }

}
