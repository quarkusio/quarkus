package io.quarkus.hibernate.search.orm.elasticsearch.test.devui;

import jakarta.inject.Inject;

import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.search.orm.elasticsearch.test.devui.namedpu.MyNamedPuIndexedEntity;
import io.quarkus.test.QuarkusDevModeTest;
import io.smallrye.common.annotation.Identifier;

public class DevUIActiveFalseAndNamedPuActiveTrueTest extends AbstractDevUITest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(
                            "application-devui-active-false-and-named-pu-active-true.properties",
                            "application.properties")
                    .addClass(MyIndexedEntity.class)
                    .addClass(MyNamedPuIndexedEntity.class)
                    .addClass(RestClientStarterService.class));

    @Inject
    @Identifier("another-es")
    InjectableInstance<RestClient> restClient2;

    public DevUIActiveFalseAndNamedPuActiveTrueTest() {
        super("namedpu", MyNamedPuIndexedEntity.class.getName());
    }
}
