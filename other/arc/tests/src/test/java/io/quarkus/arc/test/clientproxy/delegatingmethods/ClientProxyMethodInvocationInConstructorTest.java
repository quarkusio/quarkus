package io.quarkus.arc.test.clientproxy.delegatingmethods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.test.ArcTestContainer;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ClientProxyMethodInvocationInConstructorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, Component.class, HasElement.class, HasSize.class);

    @Test
    public void testClientProxy() throws IOException {
        // Just test that the client proxy can be instantiated
        MyBean myBean = Arc.container().instance(MyBean.class).get();
        assertTrue(myBean instanceof ClientProxy);
        assertEquals("an element", myBean.getElement().toString());
    }
}
