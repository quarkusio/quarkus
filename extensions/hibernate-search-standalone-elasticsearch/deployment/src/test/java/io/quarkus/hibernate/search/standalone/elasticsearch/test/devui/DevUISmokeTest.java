package io.quarkus.hibernate.search.standalone.elasticsearch.test.devui;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class DevUISmokeTest extends AbstractDevUITest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(
                    (jar) -> jar.addAsResource("application-devui.properties", "application.properties")
                            .addClasses(MyIndexedEntity.class));

    public DevUISmokeTest() {
        super(MyIndexedEntity.class.getName());
    }
}
